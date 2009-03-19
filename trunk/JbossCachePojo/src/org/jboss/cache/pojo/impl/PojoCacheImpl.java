/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Version;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.PojoCacheThreadContext;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.interceptors.PojoTxSynchronizationHandler;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Implementation class for PojoCache interface
 *
 * @author Ben Wang
 * @version $Id: PojoCacheImpl.java 6747 2008-09-17 23:24:20Z jason.greene@jboss.com $
 */
public class PojoCacheImpl implements PojoCache
{
   private CacheSPI<Object, Object> cache = null;
   protected final Log log_ = LogFactory.getLog(PojoCacheImpl.this.getClass());
   private PojoCacheDelegate delegate_;
   // Class -> CachedType
   // use WeakHashMap to allow class reloading
   private Map cachedTypes_ = new WeakHashMap();
   private boolean hasCreate_ = false;
   private CacheListenerAdaptor listenerAdaptor = new CacheListenerAdaptor(this);
   private PojoCacheThreadContext threadContext = new PojoCacheThreadContextImpl();

   public PojoCacheImpl(String configStr, boolean toStart)
   {
      try
      {
         cache = (CacheSPI<Object, Object>)DefaultCacheFactory.getInstance().createCache(configStr, toStart);
         delegate_ = new PojoCacheDelegate(this);
      }
      catch (Exception e)
      {
         throw new PojoCacheException("Failed to start " + configStr, e);
      }
   }

   public PojoCacheImpl(Configuration config, boolean toStart)
   {
      try
      {
         cache = (CacheSPI<Object, Object>) DefaultCacheFactory.getInstance().createCache(config, toStart);
      }
      catch (Exception e)
      {
         throw new PojoCacheException("init " + config + " failed", e);
      }

      delegate_ = new PojoCacheDelegate(this);
   }

   public CacheSPI<Object, Object> getCacheSPI()
   {
      return cache;
   }

   public Object attach(String id, Object pojo) throws PojoCacheException
   {
      return attach(Fqn.fromString(id), pojo);
   }

   public Object attach(Fqn<?> id, Object pojo) throws PojoCacheException
   {
      return attach(id, pojo, null, null);
   }

   public Object attach(Fqn<?> id, Object pojo, String field, Object source) throws PojoCacheException
   {
      TransactionManager tm = getTransactionManager();
      boolean createdTransaction = setupTransaction(tm);
      try
      {
         Object obj = delegate_.putObject(id, pojo, field, source);
         return obj;
      }
      catch (Throwable t)
      {
         setRollbackOnly(tm);
         if (t instanceof PojoCacheException)
            throw (PojoCacheException)t;

         throw new PojoCacheException("attach failed " + id, t);
      }
      finally
      {
         if (createdTransaction)
            endTransaction(tm, id);
      }
   }

   public Object detach(Fqn<?> id, String field, Object source) throws PojoCacheException
   {
      TransactionManager tm = getTransactionManager();
      boolean createdTransaction = setupTransaction(tm);
      try
      {
         Object obj = delegate_.removeObject(id, field, source);
         return obj;
      }
      catch (Throwable t)
      {
         setRollbackOnly(tm);
         if (t instanceof PojoCacheException)
            throw (PojoCacheException)t;
         throw new PojoCacheException("detach failed " + id, t);
      }
      finally
      {
         if (createdTransaction)
            endTransaction(tm, id);
      }
   }

   private void endTransaction(TransactionManager tm, Fqn<?> id)
   {
      try
      {
         switch (tm.getStatus())
         {
            case Status.STATUS_PREPARING:
            case Status.STATUS_PREPARED:
            case Status.STATUS_ACTIVE:
               tm.commit();
               break;
            case Status.STATUS_MARKED_ROLLBACK:
               tm.rollback();
               break;
         }
      }
      catch (Throwable t)
      {
         if (log_.isWarnEnabled())
            log_.warn("Could not end transaction for operation on: " + id, t);
      }
   }

   private void setRollbackOnly(TransactionManager tm)
   {
      try
      {
         if (tm.getStatus() != Status.STATUS_MARKED_ROLLBACK)
            tm.setRollbackOnly();
      }
      catch (Throwable t)
      {
         if (log_.isWarnEnabled())
            log_.warn("Could not rollback transaction!", t);
      }
   }

   private boolean setupTransaction(TransactionManager tm)
   {
      boolean created = false;
      try
      {
         Transaction transaction = tm.getTransaction();
         if (transaction == null)
         {
            tm.begin();
            transaction = tm.getTransaction();
            created = true;
         }

         transaction.registerSynchronization(PojoTxSynchronizationHandler.create());
      }
      catch (Exception e)
      {
         throw new PojoCacheException("Error creating transaction", e);
      }

      return created;
   }

   private TransactionManager getTransactionManager()
   {
      TransactionManager tm = cache.getConfiguration().getRuntimeConfig().getTransactionManager();
      if (tm == null)
         tm = BatchModeTransactionManager.getInstance();

      return tm;
   }

   public Object detach(String id) throws PojoCacheException
   {
      return detach(Fqn.fromString(id));
   }



   public Object detach(Fqn<?> id) throws PojoCacheException
   {
      return detach(id, null, null);
   }

   public Fqn<?> getInternalFqn(Object object)
   {
      return delegate_.getInternalFqn(object);
   }

   public Collection<Reference> getReferences(Object object)
   {
      return delegate_.getReferences(object);
   }

   public boolean exists(Fqn<?> id)
   {
      return delegate_.exists(id);
   }

   public Object find(String id) throws PojoCacheException
   {
      return find(Fqn.fromString(id));
   }

   public Object find(Fqn<?> id) throws PojoCacheException
   {
      try
      {
         return find(id, null, null);
      }
      catch (CacheException e)
      {
         throw new PojoCacheException("find " + id + " failed ", e);
      }
   }

   public Object find(Fqn<?> id, String field, Object source) throws CacheException
   {
      return delegate_.getObject(id, field, source);
   }


   public Map<Fqn<?>, Object> findAll(String id) throws PojoCacheException
   {
      return findAll(Fqn.fromString(id));
   }

   public Map<Fqn<?>, Object> findAll(Fqn<?> id) throws PojoCacheException
   {
      // Should produce "/"
      if (id == null) id = Fqn.ROOT;

      try
      {
         return delegate_.findObjects(id);
      }
      catch (CacheException e)
      {
         throw new PojoCacheException("findAll " + id + " failed", e);
      }
   }

   public String getVersion()
   {
      return Version.printVersion();
   }

   public void create() throws PojoCacheException
   {
      log_.info("PojoCache version: " + getVersion());
      try
      {
         cache.create();
      }
      catch (Exception e)
      {
         throw new PojoCacheException("PojoCache create exception", e);
      }

      hasCreate_ = true;
   }

   public void start() throws PojoCacheException
   {
      if (!hasCreate_)
      {
         create();
      }

      try
      {
         log_.info("PojoCache version: " + getVersion());
         cache.start();
      }
      catch (Exception e)
      {
         throw new PojoCacheException("Failed starting " + e, e);
      }
   }

   public void stop() throws PojoCacheException
   {
      cache.stop();
   }

   public void destroy() throws PojoCacheException
   {
      cache.destroy();
   }

   public Collection<Object> getListeners()
   {
      return listenerAdaptor.getListeners();
   }

   public void addListener(Object listener)
   {
      addListener(listener, null);
   }

   public void addListener(Object listener, Pattern pattern)
   {
      // Add and remove listner operations must be serialized to ensure that
      // the adaptor is always present only once, when at least one listener
      // is registered.
      synchronized (listenerAdaptor)
      {
         try
         {
            boolean wasEmpty = listenerAdaptor.isEmpty();
            listenerAdaptor.addListener(listener, pattern);
            if (wasEmpty)
               cache.addCacheListener(listenerAdaptor);
         }
         catch (IllegalArgumentException e)
         {
            // simplify stack trace for user
            e.fillInStackTrace();
            throw e;
         }
      }
   }

   public void removeListener(Object listener)
   {
      synchronized (listenerAdaptor)
      {
         listenerAdaptor.removeListener(listener);
         if (listenerAdaptor.isEmpty())
            cache.removeCacheListener(listenerAdaptor);
      }
   }

   public PojoCacheThreadContext getThreadContext()
   {
      return threadContext;
   }

   public Cache<Object,Object> getCache()
   {
      return cache;
   }

   /**
    * Obtain a cache aop type for user to traverse the defined "primitive" types in aop.
    * Note that this is not a synchronized call now for speed optimization.
    *
    * @param clazz The original pojo class
    * @return CachedType
    */
   public synchronized CachedType getCachedType(Class clazz)
   {
      CachedType type = (CachedType) cachedTypes_.get(clazz);
      if (type == null)
      {
         type = new CachedType(clazz);
         cachedTypes_.put(clazz, type);
         return type;
      }
      else
      {
         return type;
      }
   }

   public String toString()
   {
      return getClass().getName() +
              " cache=" + cache +
              " delegate=" + delegate_ +
              " types=" + cachedTypes_.size();
   }
}
