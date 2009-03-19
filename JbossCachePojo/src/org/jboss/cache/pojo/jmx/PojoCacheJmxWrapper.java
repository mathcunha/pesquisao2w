/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.cache.pojo.jmx;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.management.AttributeChangeNotification;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.buddyreplication.NextMemberBuddyLocator;
import org.jboss.cache.config.BuddyReplicationConfig;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.ConfigurationException;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.RuntimeConfig;
import org.jboss.cache.config.BuddyReplicationConfig.BuddyLocatorConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig.SingletonStoreConfig;
import org.jboss.cache.config.parsing.JGroupsStackParser;
import org.jboss.cache.config.parsing.XmlConfigHelper;
import org.jboss.cache.config.parsing.XmlConfigurationParser2x;
import org.jboss.cache.config.parsing.element.BuddyElementParser;
import org.jboss.cache.config.parsing.element.EvictionElementParser;
import org.jboss.cache.config.parsing.element.LoadersElementParser;
import org.jboss.cache.jmx.CacheJmxWrapper;
import org.jboss.cache.jmx.CacheNotificationListener;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheAlreadyDetachedException;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.PojoCacheFactory;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jgroups.Channel;
import org.jgroups.ChannelFactory;
import org.jgroups.jmx.JChannelFactoryMBean;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




public class PojoCacheJmxWrapper
        implements PojoCacheJmxWrapperMBean, MBeanRegistration, NotificationEmitter
{
   private Log log = LogFactory.getLog(getClass().getName());

   private boolean registerInterceptors = true;
   private Configuration config;
   private MBeanServer server;
   private String cacheObjectName;
   private PojoCache pojoCache;
   private CacheJmxWrapper plainCacheWrapper;
   private boolean registerPlainCache = true;
   private boolean plainCacheRegistered;
   private CacheStatus cacheStatus;
   private boolean registered;
   private final Set<NotificationListenerArgs> pendingListeners =
           new HashSet<NotificationListenerArgs>();

   // Legacy config support

   private Element buddyReplConfig;
   private Element evictionConfig;
   private Element cacheLoaderConfig;
   private Element clusterConfig;
   private JChannelFactoryMBean multiplexerService;

   private BuddyElementParser buddyElementParser = new BuddyElementParser();
   private LoadersElementParser loadersElementParser = new LoadersElementParser();
   private EvictionElementParser evictionElementParser = new EvictionElementParser();
   private JGroupsStackParser stackParser = new JGroupsStackParser();

   /**
    * Default constructor.
    */
   public PojoCacheJmxWrapper()
   {
      cacheStatus = CacheStatus.INSTANTIATED;
   }

   /**
    * Creates a PojoCacheJmxWrapper that wraps the given PojoCache.
    *
    * @param toWrap the cache
    */
   public PojoCacheJmxWrapper(PojoCache toWrap)
   {
      this();
      setPojoCache(toWrap);
   }

   // PojoCacheMBean

   public PojoCache getPojoCache()
   {
      return pojoCache;
   }

   public Configuration getConfiguration()
   {
      Configuration cfg = (pojoCache == null ? config : pojoCache.getCache().getConfiguration());
      if (cfg == null)
      {
         cfg = config = new Configuration();
      }
      return cfg;
   }

   public String getInternalLocation(Object pojo) throws PojoCacheAlreadyDetachedException
   {
      return pojoCache.getInternalFqn(pojo).toString();
   }

   public String getUnderlyingCacheObjectName()
   {
      return plainCacheWrapper == null ? null : plainCacheWrapper.getCacheObjectName();
   }

   public void create() throws PojoCacheException
   {
      if (cacheStatus.createAllowed() == false)
      {
         if (cacheStatus.needToDestroyFailedCache())
            destroy();
         else
            return;
      }

      try
      {
         cacheStatus = CacheStatus.CREATING;

         if (pojoCache == null)
         {
            if (config == null)
            {
               throw new ConfigurationException("Must call setConfiguration() " +
                       "or setPojoCache() before call to create()");
            }

            constructCache();
         }

         pojoCache.create();

         registerPlainCache();

         plainCacheWrapper.create();

         cacheStatus = CacheStatus.CREATED;
      }
      catch (Throwable t)
      {
         handleLifecycleTransitionFailure(t);
      }
   }

   public void start() throws PojoCacheException
   {
      if (cacheStatus.startAllowed() == false)
      {
         if (cacheStatus.needToDestroyFailedCache())
            destroy();
         if (cacheStatus.needCreateBeforeStart())
            create();
         else
            return;
      }

      try
      {
         int oldState = getState();
         cacheStatus = CacheStatus.STARTING;
         int startingState = getState();
         sendStateChangeNotification(oldState, startingState, getClass().getSimpleName() + " starting", null);

         pojoCache.start();

         plainCacheWrapper.start();
         cacheStatus = CacheStatus.STARTED;
         sendStateChangeNotification(startingState, getState(), getClass().getSimpleName() + " started", null);
      }
      catch (Throwable t)
      {
         handleLifecycleTransitionFailure(t);
      }
   }

   public void stop()
   {
      if (cacheStatus.stopAllowed() == false)
      {
         return;
      }

      // Trying to stop() from FAILED is valid, but may not work
      boolean failed = cacheStatus == CacheStatus.FAILED;

      try
      {
         int oldState = getState();
         cacheStatus = CacheStatus.STOPPING;
         int stoppingState = getState();
         sendStateChangeNotification(oldState, stoppingState, getClass().getSimpleName() + " stopping", null);

         cacheStatus = CacheStatus.STOPPING;

         pojoCache.stop();

         plainCacheWrapper.stop();
         cacheStatus = CacheStatus.STOPPED;
         sendStateChangeNotification(stoppingState, getState(), getClass().getSimpleName() + " stopped", null);
      }
      catch (Throwable t)
      {
         if (failed)
         {
            log.warn("Attempted to stop() from FAILED state, " +
                    "but caught exception; try calling destroy()", t);
         }
         handleLifecycleTransitionFailure(t);
      }
   }

   public void destroy()
   {
      if (cacheStatus.destroyAllowed() == false)
      {
         if (cacheStatus.needStopBeforeDestroy())
            stop();
         else
            return;
      }

      try
      {
         cacheStatus = CacheStatus.DESTROYING;

         if (pojoCache != null)
            pojoCache.destroy();

         // The cache is destroyed, so we shouldn't leave it registered
         // in JMX even if we didn't register it in create()
         unregisterPlainCache();

         if (plainCacheWrapper != null)
            plainCacheWrapper.destroy();
      }
      finally
      {
         // We always proceed to DESTROYED
         cacheStatus = CacheStatus.DESTROYED;
      }
   }

   public CacheStatus getCacheStatus()
   {
      return cacheStatus;
   }

   public int getState()
   {
      switch (cacheStatus)
      {
         case INSTANTIATED:
         case CREATING:
            return registered ? REGISTERED : UNREGISTERED;
         case CREATED:
            return CREATED;
         case STARTING:
            return STARTING;
         case STARTED:
            return STARTED;
         case STOPPING:
            return STOPPING;
         case STOPPED:
         case DESTROYING:
            return STOPPED;
         case DESTROYED:
            return registered ? DESTROYED : UNREGISTERED;
         case FAILED:
         default:
            return FAILED;
      }
   }

   public boolean getRegisterPlainCache()
   {
      return registerPlainCache;
   }

   public void setRegisterPlainCache(boolean registerPlainCache)
   {
      this.registerPlainCache = registerPlainCache;
   }

   public boolean getRegisterInterceptors()
   {
      return registerInterceptors;
   }

   public void setRegisterInterceptors(boolean register)
   {
      this.registerInterceptors = register;
   }

   // ----------------------------------------------------  LegacyConfiguration

   public Element getBuddyReplicationConfig()
   {
      return buddyReplConfig;
   }

   public Element getCacheLoaderConfig()
   {
      return cacheLoaderConfig;
   }

   public Element getCacheLoaderConfiguration()
   {
      return getCacheLoaderConfig();
   }

   public String getCacheMode()
   {
      return getConfiguration().getCacheModeString();
   }

   public String getClusterName()
   {
      return getConfiguration().getClusterName();
   }

   public String getClusterProperties()
   {
      return getConfiguration().getClusterConfig();
   }

   public Element getClusterConfig()
   {
      return clusterConfig;
   }

   public Element getEvictionPolicyConfig()
   {
      return evictionConfig;
   }

   public boolean getExposeManagementStatistics()
   {
      return getConfiguration().getExposeManagementStatistics();
   }

   public boolean getUseInterceptorMbeans()
   {
      return getExposeManagementStatistics();
   }

   public boolean getFetchInMemoryState()
   {
      return getConfiguration().isFetchInMemoryState();
   }

   public long getStateRetrievalTimeout()
   {
      return getConfiguration().getStateRetrievalTimeout();
   }

   @Deprecated
   public void setInitialStateRetrievalTimeout(long timeout)
   {
      setStateRetrievalTimeout(timeout);
   }

   public String getIsolationLevel()
   {
      return getConfiguration().getIsolationLevelString();
   }

   public long getLockAcquisitionTimeout()
   {
      return getConfiguration().getLockAcquisitionTimeout();
   }

   public JChannelFactoryMBean getMultiplexerService()
   {
      return multiplexerService;
   }

   public String getMultiplexerStack()
   {
      return getConfiguration().getMultiplexerStack();
   }

   public ChannelFactory getMuxChannelFactory()
   {
      return getConfiguration().getRuntimeConfig().getMuxChannelFactory();
   }

   public String getNodeLockingScheme()
   {
      return getConfiguration().getNodeLockingSchemeString();
   }

   public long getReplQueueInterval()
   {
      return getConfiguration().getReplQueueInterval();
   }

   public int getReplQueueMaxElements()
   {
      return getConfiguration().getReplQueueMaxElements();
   }

   public String getReplicationVersion()
   {
      return getConfiguration().getReplVersionString();
   }

   public boolean getSyncCommitPhase()
   {
      return getConfiguration().isSyncCommitPhase();
   }

   public long getSyncReplTimeout()
   {
      return getConfiguration().getSyncReplTimeout();
   }

   public boolean getSyncRollbackPhase()
   {
      return getConfiguration().isSyncRollbackPhase();
   }

   public TransactionManager getTransactionManager()
   {
      return getConfiguration().getRuntimeConfig().getTransactionManager();
   }

   public String getTransactionManagerLookupClass()
   {
      return getConfiguration().getTransactionManagerLookupClass();
   }

   public boolean getUseRegionBasedMarshalling()
   {
      return getConfiguration().isUseRegionBasedMarshalling();
   }

   public boolean getUseReplQueue()
   {
      return getConfiguration().isUseReplQueue();
   }

   public boolean isInactiveOnStartup()
   {
      return getConfiguration().isInactiveOnStartup();
   }

   public void setBuddyReplicationConfig(Element config)
   {
      BuddyReplicationConfig brc = null;
      if (config != null)
      {
         brc = XmlConfigurationParser2x.parseBuddyReplicationConfig(config);
      }
      getConfiguration().setBuddyReplicationConfig(brc);
      this.buddyReplConfig = config;
   }

   public void setCacheLoaderConfig(Element cache_loader_config)
   {
      CacheLoaderConfig clc = null;
      if (cache_loader_config != null)
      {
         clc = XmlConfigurationParser2x.parseCacheLoaderConfig(cache_loader_config);
      }
      getConfiguration().setCacheLoaderConfig(clc);
      this.cacheLoaderConfig = cache_loader_config;
   }

   public void setCacheLoaderConfiguration(Element config)
   {
      log.warn("MBean attribute 'CacheLoaderConfiguration' is deprecated; " +
              "use 'CacheLoaderConfig'");
      setCacheLoaderConfig(config);
   }

   public void setCacheMode(String mode) throws Exception
   {
      getConfiguration().setCacheModeString(mode);
   }

   public void setClusterConfig(Element config)
   {
      String props = null;
      if (config != null)
      {
         props =  stackParser.parseClusterConfigXml(config);
      }
      getConfiguration().setClusterConfig(props);
      this.clusterConfig = config;
   }

   @Deprecated
   public long getInitialStateRetrievalTimeout()
   {
      return getStateRetrievalTimeout();
   }

   public void setClusterName(String name)
   {
      getConfiguration().setClusterName(name);
   }

   public void setClusterProperties(String cluster_props)
   {
      getConfiguration().setClusterConfig(cluster_props);
   }

   public void setEvictionPolicyConfig(Element config)
   {
      EvictionConfig ec = null;
      if (config != null)
      {
         ec = XmlConfigurationParser2x.parseEvictionConfig(config);
      }
      getConfiguration().setEvictionConfig(ec);
      this.evictionConfig = config;
   }

   public void setExposeManagementStatistics(boolean expose)
   {
      getConfiguration().setExposeManagementStatistics(expose);
   }

   public void setUseInterceptorMbeans(boolean use)
   {
      log.warn("MBean attribute 'UseInterceptorMbeans' is deprecated; " +
              "use 'ExposeManagementStatistics'");
      setExposeManagementStatistics(use);
   }

   public void setFetchInMemoryState(boolean flag)
   {
      getConfiguration().setFetchInMemoryState(flag);
   }

   public void setInactiveOnStartup(boolean inactiveOnStartup)
   {
      getConfiguration().setInactiveOnStartup(inactiveOnStartup);
   }

   public void setStateRetrievalTimeout(long timeout)
   {
      getConfiguration().setStateRetrievalTimeout(timeout);
   }

   public void setIsolationLevel(String level)
   {
      getConfiguration().setIsolationLevelString(level);
   }

   public void setLockAcquisitionTimeout(long timeout)
   {
      getConfiguration().setLockAcquisitionTimeout(timeout);
   }

   public void setMultiplexerService(JChannelFactoryMBean muxService)
   {
      this.multiplexerService = muxService;
   }

   public void setMultiplexerStack(String stackName)
   {
      getConfiguration().setMultiplexerStack(stackName);
   }

   public void setMuxChannelFactory(ChannelFactory factory)
   {
      getConfiguration().getRuntimeConfig().setMuxChannelFactory(factory);
   }

   public void setNodeLockingScheme(String nodeLockingScheme)
   {
      getConfiguration().setNodeLockingSchemeString(nodeLockingScheme);
   }

   public void setReplQueueInterval(long interval)
   {
      getConfiguration().setReplQueueInterval(interval);
   }

   public void setReplQueueMaxElements(int max_elements)
   {
      getConfiguration().setReplQueueMaxElements(max_elements);
   }

   public void setReplicationVersion(String version)
   {
      getConfiguration().setReplVersionString(version);
   }

   public void setSyncCommitPhase(boolean sync_commit_phase)
   {
      getConfiguration().setSyncCommitPhase(sync_commit_phase);
   }

   public void setSyncReplTimeout(long timeout)
   {
      getConfiguration().setSyncReplTimeout(timeout);
   }

   public void setSyncRollbackPhase(boolean sync_rollback_phase)
   {
      getConfiguration().setSyncRollbackPhase(sync_rollback_phase);
   }

   public void setTransactionManager(TransactionManager manager)
   {
      getConfiguration().getRuntimeConfig().setTransactionManager(manager);
   }

   public void setTransactionManagerLookupClass(String cl) throws Exception
   {
      getConfiguration().setTransactionManagerLookupClass(cl);
   }

   public void setUseRegionBasedMarshalling(boolean isTrue)
   {
      getConfiguration().setUseRegionBasedMarshalling(isTrue);
   }

   public void setUseReplQueue(boolean flag)
   {
      getConfiguration().setUseReplQueue(flag);
   }

   // ------------------------------------------------------  MBeanRegistration

   /**
    * Caches the provided <code>server</code> and <code>objName</code>.
    */
   public ObjectName preRegister(MBeanServer server, ObjectName objName)
           throws Exception
   {
      this.server = server;

      if (cacheObjectName == null)
      {
         cacheObjectName = objName.getCanonicalName();
      }

      if (plainCacheWrapper != null)
         plainCacheWrapper.setNotificationServiceName(cacheObjectName);

      return new ObjectName(cacheObjectName);
   }

   /**
    * Registers the CacheJmxWrapperMBean,
    * if {@link #getRegisterPlainCache()} is <code>true</code>.
    */
   public void postRegister(Boolean registrationDone)
   {
      if (Boolean.TRUE.equals(registrationDone) && registerPlainCache)
      {
         log.debug("Registered in JMX under " + cacheObjectName);

         if (plainCacheWrapper != null)
         {
            try
            {
               registerPlainCache();
            }
            catch (Exception e)
            {
               log.error("Caught exception registering plain cache with JMX", e);
            }
         }

         registered = true;
      }
   }

   /**
    * No-op.
    */
   public void preDeregister() throws Exception
   {
   }

   /**
    * Unregisters the CacheJmxWrapper, if {@link #getRegisterPlainCache()} is
    * <code>true</code>.
    */
   public void postDeregister()
   {
      if (plainCacheWrapper != null)
      {
         unregisterPlainCache();
      }

      registered = false;
   }

   // ----------------------------------------------------  NotificationEmitter

   public void removeNotificationListener(NotificationListener listener,
                                          NotificationFilter filter,
                                          Object handback)
           throws ListenerNotFoundException
   {
      synchronized (pendingListeners)
      {
         boolean found = pendingListeners.remove(new NotificationListenerArgs(listener, filter, handback));

         if (plainCacheWrapper != null)
         {
            plainCacheWrapper.removeNotificationListener(listener, filter, handback);
         }
         else if (!found)
         {
            throw new ListenerNotFoundException();
         }
      }
   }

   public void addNotificationListener(NotificationListener listener,
                                       NotificationFilter filter,
                                       Object handback)
           throws IllegalArgumentException
   {
      synchronized (pendingListeners)
      {
         if (plainCacheWrapper != null)
         {
            plainCacheWrapper.addNotificationListener(listener, filter, handback);
         }
         else
         {
            // Add it for addition to the plainCacheWrapper when it's created
            pendingListeners.add(new NotificationListenerArgs(listener, filter, handback));
         }
      }

   }

   public MBeanNotificationInfo[] getNotificationInfo()
   {
      return CacheNotificationListener.getNotificationInfo();
   }

   public void removeNotificationListener(NotificationListener listener)
           throws ListenerNotFoundException
   {
      synchronized (pendingListeners)
      {
         boolean found = false;
         for (Iterator<NotificationListenerArgs> iter = pendingListeners.iterator();
              iter.hasNext();)
         {
            NotificationListenerArgs args = iter.next();
            if (safeEquals(listener, args.listener))
            {
               found = true;
               iter.remove();
            }
         }

         if (plainCacheWrapper != null)
         {
            plainCacheWrapper.removeNotificationListener(listener);
         }
         else if (!found)
         {
            throw new ListenerNotFoundException();
         }

      }
   }

   // ---------------------------------------------------------  Public methods

   public MBeanServer getMBeanServer()
   {
      return server;
   }

   /**
    * Sets the configuration that the underlying cache should use.
    *
    * @param config the configuration
    */
   public void setConfiguration(Configuration config)
   {
      this.config = config;
   }

   public void setPojoCache(PojoCache cache)
   {
      if (cacheStatus != CacheStatus.INSTANTIATED
              && cacheStatus != CacheStatus.DESTROYED)
         throw new IllegalStateException("Cannot set underlying cache after call to create()");

      this.pojoCache = cache;
      if (pojoCache == null)
      {
         this.config = null;
         this.plainCacheWrapper = null;
      }
      else
      {
         this.config = cache.getCache().getConfiguration();
         this.plainCacheWrapper = buildPlainCacheWrapper(pojoCache);
      }
   }

   // ---------------------------------------------------------------  Private methods

   private void constructCache() throws ConfigurationException
   {
      pojoCache = (PojoCacheImpl) PojoCacheFactory.createCache(config, false);

      plainCacheWrapper = buildPlainCacheWrapper(pojoCache);
      if (multiplexerService != null)
      {
         injectMuxChannel();
      }
   }

   private CacheJmxWrapper buildPlainCacheWrapper(PojoCache pojoCache)
   {
      CacheJmxWrapper plainCache = new CacheJmxWrapper();
      //plainCache.setRegisterInterceptors(getRegisterInterceptors());
      plainCache.setCache(pojoCache.getCache());
      // It shouldn't send out lifecycle state change notifications for itself;
      // we do it
      plainCache.setDisableStateChangeNotifications(true);

      if (server != null)
      {
         plainCache.setNotificationServiceName(cacheObjectName);
      }

      // Add any NotificationListeners we registered before creating
      // the CacheJmxWrapper
      synchronized (pendingListeners)
      {
         for (NotificationListenerArgs args : pendingListeners)
         {
            plainCache.addNotificationListener(args.listener, args.filter, args.handback);
         }
      }
      return plainCache;
   }

   private boolean registerPlainCache() throws CacheException
   {
      if (registerPlainCache && !plainCacheRegistered && server != null)
      {
         try
         {
            ObjectName ourName = new ObjectName(cacheObjectName);
            ObjectName plainName = JmxUtil.getPlainCacheObjectName(ourName);
            log.debug("Registering plain cache under name " + plainName.getCanonicalName());
            org.jboss.cache.jmx.JmxUtil.registerCacheMBean(server, plainCacheWrapper, plainName.getCanonicalName());
            plainCacheRegistered = true;
            return true;
         }
         catch (JMException e)
         {
            throw new CacheException("Failed to register plain cache", e);
         }
      }

      return false;
   }

   private void unregisterPlainCache()
   {
      if (registerPlainCache && plainCacheRegistered && server != null)
      {
         log.debug("Unregistering plain cache");
         try
         {
            org.jboss.cache.jmx.JmxUtil.unregisterCacheMBean(server, plainCacheWrapper.getCacheObjectName());
         }
         catch (Exception e)
         {
            log.error("Could not unregister plain cache", e);
         }
         plainCacheRegistered = false;
      }
   }

   private void injectMuxChannel() throws CacheException
   {
      Configuration cfg = getConfiguration();
      RuntimeConfig rtcfg = cfg.getRuntimeConfig();

      // Only inject if there isn't already a channel or factory
      if (rtcfg.getMuxChannelFactory() != null && rtcfg.getChannel() != null)
      {
         Channel ch;
         try
         {
            ch = multiplexerService.createMultiplexerChannel(cfg.getMultiplexerStack(), cfg.getClusterName());
         }
         catch (Exception e)
         {
            throw new CacheException("Exception creating multiplexed channel", e);
         }
         rtcfg.setChannel(ch);
      }

   }

   /**
    * Helper for sending out state change notifications
    */
   private void sendStateChangeNotification(int oldState, int newState, String msg, Throwable t)
   {
      if (plainCacheWrapper != null)
      {
         long now = System.currentTimeMillis();

         AttributeChangeNotification stateChangeNotification = new AttributeChangeNotification(
                 this,
                 plainCacheWrapper.getNextNotificationSequenceNumber(), now, msg,
                 "State", "java.lang.Integer",
                 new Integer(oldState), new Integer(newState)
         );
         stateChangeNotification.setUserData(t);

         plainCacheWrapper.sendNotification(stateChangeNotification);
      }
   }

   /**
    * Sets the cacheStatus to FAILED and rethrows the problem as one
    * of the declared types. Converts any non-RuntimeException Exception
    * to CacheException.
    *
    * @param t
    * @throws PojoCacheException
    * @throws RuntimeException
    * @throws Error
    */
   private void handleLifecycleTransitionFailure(Throwable t)
           throws PojoCacheException, RuntimeException, Error
   {
      int oldState = getState();
      cacheStatus = CacheStatus.FAILED;
      sendStateChangeNotification(oldState, getState(), getClass().getSimpleName() + " failed", t);

      if (t instanceof PojoCacheException)
         throw (PojoCacheException) t;
      if (t instanceof CacheException)
         throw (CacheException) t;
      else if (t instanceof RuntimeException)
         throw (RuntimeException) t;
      else if (t instanceof Error)
         throw (Error) t;
      else
         throw new PojoCacheException(t);
   }

   private static boolean safeEquals(Object us, Object them)
   {
      return (us == null ? them == null : us.equals(them));
   }

   private static class NotificationListenerArgs
   {
      NotificationListener listener;
      NotificationFilter filter;
      Object handback;

      NotificationListenerArgs(NotificationListener listener,
                               NotificationFilter filter,
                               Object handback)
      {
         this.listener = listener;
         this.filter = filter;
         this.handback = handback;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj) return true;

         if (obj instanceof NotificationListenerArgs)
         {
            NotificationListenerArgs other = (NotificationListenerArgs) obj;
            if (safeEquals(listener, other.listener)
                    && safeEquals(filter, other.filter)
                    && safeEquals(handback, other.handback))
            {
               return true;
            }
         }
         return false;
      }

      @Override
      public int hashCode()
      {
         int result = 17;
         result = 29 * result + (listener != null ? listener.hashCode() : 0);
         result = 29 * result + (filter != null ? filter.hashCode() : 0);
         result = 29 * result + (handback != null ? handback.hashCode() : 0);
         return result;
      }
   }

}
