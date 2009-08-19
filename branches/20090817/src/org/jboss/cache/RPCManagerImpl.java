/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.cache;

import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.GroupConfiguration;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.NotJoinedException;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.ProtocolFactory;
import net.sf.jgcs.Service;
import net.sf.jgcs.membership.BlockListener;
import net.sf.jgcs.membership.BlockSession;
import net.sf.jgcs.membership.MembershipListener;
import net.sf.jgcs.membership.MembershipSession;
import net.sf.jgcs.membership.View;
import net.sf.jgcs.utils.FactoryUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.commands.ReplicableCommand;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.Configuration.NodeLockingScheme;
import org.jboss.cache.config.RuntimeConfig;
import org.jboss.cache.factories.ComponentRegistry;
import org.jboss.cache.factories.annotations.Inject;
import org.jboss.cache.factories.annotations.Start;
import org.jboss.cache.factories.annotations.Stop;
import org.jboss.cache.interceptors.InterceptorChain;
import org.jboss.cache.invocation.InvocationContextContainer;
import org.jboss.cache.jmx.annotations.MBean;
import org.jboss.cache.jmx.annotations.ManagedAttribute;
import org.jboss.cache.jmx.annotations.ManagedOperation;
import org.jboss.cache.lock.LockManager;
import org.jboss.cache.lock.LockUtil;
import org.jboss.cache.lock.TimeoutException;
import org.jboss.cache.marshall.CommandAwareRpcDispatcher;
import org.jboss.cache.marshall.InactiveRegionAwareRpcDispatcher;
import org.jboss.cache.marshall.Marshaller;
import org.jboss.cache.marshall.MarshallerWrapper;
import org.jboss.cache.notifications.Notifier;


import org.jboss.cache.statetransfer.DefaultStateTransferManager;
import org.jboss.cache.transaction.GlobalTransaction;
import org.jboss.cache.transaction.TransactionTable;
import org.jboss.cache.util.concurrent.ReclosableLatch;
import org.jboss.cache.util.reflect.ReflectionUtil;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelFactory;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedMembershipListener;
import org.jgroups.JChannel;



import org.jgroups.blocks.GroupRequest;

import org.jgroups.protocols.TP;
import org.jgroups.stack.ProtocolStack;


import br.unifor.g2cl.MarshalDataSession;
import br.unifor.g2cl.Rsp;
import br.unifor.g2cl.RspFilter;
import br.unifor.g2cl.RspList;
import br.unifor.g2cl.StateTransferDataSession;

import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager that handles all RPC calls between JBoss Cache instances
 *
 * @author <a href="mailto:manik AT jboss DOT org">Manik Surtani (manik AT jboss DOT org)</a>
 */
@MBean(objectName = "RPCManager", description = "Manages RPC connections to remote caches")
public class RPCManagerImpl implements RPCManager {
   private Channel channel;
   private SocketAddress localAddress;
   private final Log log = LogFactory.getLog(RPCManagerImpl.class);
   private volatile List<SocketAddress> members;
   private long replicationCount;
   private long replicationFailures;
   private boolean statisticsEnabled = false;
   
   private StateTransferDataSession dataSession;
   private MembershipSession controlSession;

   private final Object coordinatorLock = new Object();
   /**
    * True if this Cache is the coordinator.
    */
   private volatile boolean coordinator = false;

   /**
    * The most recent state transfer source
    */
   volatile SocketAddress lastStateTransferSource;

   /**
    * JGroups RpcDispatcher in use.
    */
   private CommandAwareRpcDispatcher rpcDispatcher = null;

   /**
    * JGroups message listener.
    */
   private ChannelMessageListener messageListener;
   Configuration configuration;
   private Notifier notifier;
   private CacheSPI spi;
   private InvocationContextContainer invocationContextContainer;
   private final boolean trace = log.isTraceEnabled();
   private Marshaller marshaller;
   private TransactionManager txManager;
   private TransactionTable txTable;
   private InterceptorChain interceptorChain;

   private boolean isUsingBuddyReplication;
   private volatile boolean isInLocalMode;
   private ComponentRegistry componentRegistry;
   private LockManager lockManager;
   private FlushTracker flushTracker = new FlushTracker();


   @Inject
   public void setupDependencies(ChannelMessageListener messageListener, Configuration configuration, Notifier notifier,
                                 CacheSPI spi, Marshaller marshaller, TransactionTable txTable,
                                 TransactionManager txManager, InvocationContextContainer container, InterceptorChain interceptorChain,
                                 ComponentRegistry componentRegistry, LockManager lockManager) {
      this.messageListener = messageListener;
      this.configuration = configuration;
      this.notifier = notifier;
      this.spi = spi;
      this.marshaller = marshaller;
      this.txManager = txManager;
      this.txTable = txTable;
      this.invocationContextContainer = container;
      this.interceptorChain = interceptorChain;
      this.componentRegistry = componentRegistry;
      this.lockManager = lockManager;
   }

   public class FlushTracker {
      private final ReclosableLatch flushBlockGate = new ReclosableLatch();
      private final AtomicInteger flushCompletionCount = new AtomicInteger();
      private final ReclosableLatch flushWaitGate = new ReclosableLatch(false);

      public void block() {
         flushBlockGate.close();
         flushWaitGate.open();
      }

      public void unblock() {
         flushWaitGate.close();
         flushCompletionCount.incrementAndGet();
         flushBlockGate.open();
      }

      public int getFlushCompletionCount() {
         return flushCompletionCount.get();
      }

      public void waitForFlushCompletion(long timeout) {
         for (; ;) {
            try {
               if (channel.flushSupported() && !flushBlockGate.await(timeout, TimeUnit.MILLISECONDS)) {
                  throw new TimeoutException("State retrieval timed out waiting for flush to block. (timeout = " + timeout + " millis) ");
               }
               return;
            }
            catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }
      }

      public void waitForFlushStart(long timeout) {
         for (; ;) {
            try {
               if (channel.flushSupported() && !flushWaitGate.await(timeout, TimeUnit.MILLISECONDS)) {
                  throw new TimeoutException("State retrieval timed out waiting for flush to block. (timeout = " + timeout + " millis) ");
               }
               return;
            }
            catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }
      }
   }

   // ------------ START: Lifecycle methods ------------

   @Start(priority = 15)
   public void start() {

      switch (configuration.getCacheMode()) {
         case LOCAL:
            log.debug("cache mode is local, will not create the channel");
            isInLocalMode = true;
            isUsingBuddyReplication = false;
            break;
         case REPL_SYNC:
         case REPL_ASYNC:
         case INVALIDATION_ASYNC:
         case INVALIDATION_SYNC:
            isInLocalMode = false;
            isUsingBuddyReplication = configuration.getBuddyReplicationConfig() != null && configuration.getBuddyReplicationConfig().isEnabled();
            if (log.isDebugEnabled()) {
               log.debug("Cache mode is " + configuration.getCacheMode());
            }

            boolean fetchState = shouldFetchStateOnStartup();
            try {
				initialiseChannelAndRpcDispatcher(fetchState);
			} catch (JGCSException e) {
				throw new CacheException("Unable to connect to JGroups channel", e);
			} catch (FileNotFoundException e) {
				throw new CacheException("Unable to connect to JGroups channel", e);
			} catch (IOException e) {
				throw new CacheException("Unable to connect to JGroups channel", e);
			}

            if (!fetchState) {
               try {
                  channel.connect(configuration.getClusterName());
                  if (log.isInfoEnabled()) {
                     log.info("Cache local address is " + getLocalAddress());
                  }
               }
               catch (ChannelException e) {
                  throw new CacheException("Unable to connect to JGroups channel", e);
               }

               if (!fetchState) {
                  return;
               }
            }


            long start = System.currentTimeMillis();
            try {
               channel.connect(configuration.getClusterName(), null, null, configuration.getStateRetrievalTimeout());
               if (log.isInfoEnabled()) {
                  log.info("Cache local address is " + getLocalAddress());
               }

               if (getMembers().size() > 1) {
                  messageListener.waitForState();
               }
            }
            catch (ChannelException e) {
               throw new CacheException("Unable to connect to JGroups channel", e);
            }
            catch (Exception ex) {
               // make sure we disconnect from the channel before we throw this exception!
               // JBCACHE-761
               disconnect();
               throw new CacheException("Unable to fetch state on startup", ex);
            }

            if (log.isInfoEnabled()) {
               log.info("state was retrieved successfully (in " + (System.currentTimeMillis() - start) + " milliseconds)");
            }
      }

   }

   public void disconnect() {
      if (channel != null && channel.isOpen()) {
         log.info("Disconnecting and closing the Channel");
         channel.disconnect();
         channel.close();
      }
   }

   @Stop(priority = 8)
   public void stop() {
      try {
         disconnect();
      }
      catch (Exception toLog) {
         log.error("Problem closing channel; setting it to null", toLog);
      }

      channel = null;
      configuration.getRuntimeConfig().setChannel(null);
      if (rpcDispatcher != null) {
         log.info("Stopping the RpcDispatcher");
         rpcDispatcher.stop();
      }

      if (members != null) members = null;

      coordinator = false;

      rpcDispatcher = null;
   }

   /**
    * @return true if we need to fetch state on startup.  I.e., initiate a state transfer.
    */
   private boolean shouldFetchStateOnStartup() {
      boolean loaderFetch = configuration.getCacheLoaderConfig() != null && configuration.getCacheLoaderConfig().isFetchPersistentState();
      return !configuration.isInactiveOnStartup() && !isUsingBuddyReplication && (configuration.isFetchInMemoryState() || loaderFetch);
   }

   @SuppressWarnings("deprecation")
   private void initialiseChannelAndRpcDispatcher(boolean fetchState) throws JGCSException, FileNotFoundException, IOException {
	   	   
	   
	   FactoryUtil jgcsConf = new FactoryUtil(getClass().getResourceAsStream("/jgroups.properties"));
       
       ProtocolFactory o = (ProtocolFactory) jgcsConf.getInstance("jgcsProtocol");
       Protocol p = o.createProtocol();
       GroupConfiguration group = (GroupConfiguration) jgcsConf.getInstance("jgcsGroup");
       Service service = (Service) jgcsConf.getInstance("jgcsService");
   	        	
       String jgcsHAGroupName = group.toString();

       DataSession dataSession = p.openDataSession(group);
       controlSession = (MembershipSession)p.openControlSession(group);
       
       log.info("jgcs HA group name is: " + jgcsHAGroupName);

       //log.info("jgcs HA jndi properties is: " + jgcs_properties);
   	
       this.dataSession = new StateTransferDataSession(new MarshalDataSession(dataSession), service, controlSession);
       this.dataSession.setStateListener(messageListener);
       
       if (configuration.isUseRegionBasedMarshalling()) {
           rpcDispatcher = new InactiveRegionAwareRpcDispatcher(this.dataSession,controlSession, service, spi, invocationContextContainer, interceptorChain, componentRegistry);
        } else {
           rpcDispatcher = new CommandAwareRpcDispatcher(this.dataSession,controlSession, service,
                                                         invocationContextContainer, invocationContextContainer, interceptorChain, componentRegistry);
        }
       
       rpcDispatcher.setMessageDispatcherListener(rpcDispatcher);
       MembershipListenerAdaptor member = new MembershipListenerAdaptor();
       rpcDispatcher.setMembershipListener(member);
       ((BlockSession)controlSession).setBlockListener(member);
       
       //TODO - Verificar isto
       rpcDispatcher.setLocal(false);
       
       rpcDispatcher.setReqMarshaller((br.unifor.g2cl.Marshaller)new MarshallerWrapper(marshaller));
       //rpcDispatcher.setReqMarshaller((br.unifor.g2cl.Marshaller)new MarshallerWrapper(marshaller));
       
       /*
       ConnectionManager connectionManager = new ConnectionManager(timeout, messageDispatcher, IMessageDispatcher.class);
       connectionManager.setControlSession(controlSession);
       rpcDispatcher.setMembershipListener(connectionManager);
       
       
       controlSession.join();
       
       dispatcher = (IMessageDispatcher) Proxy.newProxyInstance(IMessageDispatcher.class.getClassLoader(), new Class[]{IMessageDispatcher.class}, connectionManager);
		
       messages = new LinkedList<HaMessageData>();

       processThread = new ProcessThread(messages);
       processThread.setDaemon(true);
       processThread.start();
*/
       
      
      //checkAppropriateConfig();
      
   }

   public Channel getChannel() {
      return channel;
   }


   private JChannel getMultiplexerChannel() throws CacheException {
      String stackName = configuration.getMultiplexerStack();

      RuntimeConfig rtc = configuration.getRuntimeConfig();
      ChannelFactory channelFactory = rtc.getMuxChannelFactory();
      JChannel muxchannel = null;

      if (channelFactory != null) {
         try {
            muxchannel = (JChannel) channelFactory.createMultiplexerChannel(stackName, configuration.getClusterName());
         }
         catch (Exception e) {
            throw new CacheException("Failed to create multiplexed channel using stack " + stackName, e);
         }
      }

      return muxchannel;
   }


   @Deprecated
   private void removeLocksForDeadMembers(NodeSPI node, List deadMembers) {
      Set<GlobalTransaction> deadOwners = new HashSet<GlobalTransaction>();
      Object owner = lockManager.getWriteOwner(node);

      if (isLockOwnerDead(owner, deadMembers)) deadOwners.add((GlobalTransaction) owner);


      for (Object readOwner : lockManager.getReadOwners(node)) {
         if (isLockOwnerDead(readOwner, deadMembers)) deadOwners.add((GlobalTransaction) readOwner);
      }

      for (GlobalTransaction deadOwner : deadOwners) {
         boolean localTx = deadOwner.getAddress().equals(getLocalAddress());
         boolean broken = LockUtil.breakTransactionLock(node.getFqn(), lockManager, deadOwner, localTx, txTable, txManager);

         if (broken && trace) log.trace("Broke lock for node " + node.getFqn() + " held by " + deadOwner);
      }

      // Recursively unlock children
      for (Object child : node.getChildrenDirect()) {
         removeLocksForDeadMembers((NodeSPI) child, deadMembers);
      }
   }


   /**
    * Only used with MVCC.
    */
   private void removeLocksForDeadMembers(InternalNode<?, ?> node, List deadMembers) {
      Set<GlobalTransaction> deadOwners = new HashSet<GlobalTransaction>();
      Object owner = lockManager.getWriteOwner(node.getFqn());

      if (isLockOwnerDead(owner, deadMembers)) deadOwners.add((GlobalTransaction) owner);

      // MVCC won't have any read locks.

      for (GlobalTransaction deadOwner : deadOwners) {
         boolean localTx = deadOwner.getAddress().equals(getLocalAddress());
         boolean broken = LockUtil.breakTransactionLock(node.getFqn(), lockManager, deadOwner, localTx, txTable, txManager);

         if (broken && trace) log.trace("Broke lock for node " + node.getFqn() + " held by " + deadOwner);
      }

      // Recursively unlock children
      for (InternalNode child : node.getChildren()) removeLocksForDeadMembers(child, deadMembers);
   }

   private boolean isLockOwnerDead(Object owner, List deadMembers) {
      boolean result = false;
      if (owner != null && owner instanceof GlobalTransaction) {
         Object addr = ((GlobalTransaction) owner).getAddress();
         result = deadMembers.contains(addr);
      }
      return result;
   }

   // ------------ END: Lifecycle methods ------------

   // ------------ START: RPC call methods ------------

   public List<Object> callRemoteMethods(Vector<SocketAddress> recipients, ReplicableCommand command, int mode, long timeout, boolean useOutOfBandMessage) throws Exception {
      return callRemoteMethods(recipients, command, mode, timeout, null, useOutOfBandMessage);
   }

   public List<Object> callRemoteMethods(Vector<SocketAddress> recipients, ReplicableCommand command, boolean synchronous, long timeout, boolean useOutOfBandMessage) throws Exception {
      return callRemoteMethods(recipients, command, synchronous ? GroupRequest.GET_ALL : GroupRequest.GET_NONE, timeout, useOutOfBandMessage);
   }

   public List<Object> callRemoteMethods(Vector<SocketAddress> recipients, ReplicableCommand command, int mode, long timeout, RspFilter responseFilter, boolean useOutOfBandMessage) throws Exception {
      boolean success = true;
      try {
         // short circuit if we don't have an RpcDispatcher!
         if (rpcDispatcher == null) return null;
         int modeToUse = mode;
         int preferredMode;
         if ((preferredMode = spi.getInvocationContext().getOptionOverrides().getGroupRequestMode()) > -1) {
            modeToUse = preferredMode;
         }
         if (trace) {
            log.trace("callRemoteMethods(): valid members are " + recipients + " methods: " + command + " Using OOB? " + useOutOfBandMessage + " modeToUse: " + modeToUse);
         }

         flushTracker.waitForFlushCompletion(configuration.getStateRetrievalTimeout());

         useOutOfBandMessage = false;
         RspList rsps = rpcDispatcher.invokeRemoteCommands(recipients, command, modeToUse, timeout, isUsingBuddyReplication, useOutOfBandMessage, responseFilter);
         if (mode == GroupRequest.GET_NONE) return Collections.emptyList();// async case
         if (trace) {
            log.trace("(" + getLocalAddress() + "): responses for method " + command.getClass().getSimpleName() + ":\n" + rsps);
         }
         // short-circuit no-return-value calls.
         if (rsps == null) return Collections.emptyList();
         List<Object> retval = new ArrayList<Object>(rsps.size());
         for (Rsp rsp : rsps.values()) {
            if (rsp.wasSuspected() || !rsp.wasReceived()) {
               CacheException ex;
               if (rsp.wasSuspected()) {
                  ex = new SuspectException("Suspected member: " + rsp.getSender());
               } else {
                  ex = new TimeoutException("Replication timeout for " + rsp.getSender());
               }
               retval.add(new ReplicationException("rsp=" + rsp, ex));
               success = false;
            } else {
               Object value = rsp.getValue();
               if (value instanceof Exception && !(value instanceof ReplicationException)) {
                  // if we have any application-level exceptions make sure we throw them!!
                  if (trace) log.trace("Recieved exception'" + value + "' from " + rsp.getSender());
                  throw (Exception) value;
               }
               retval.add(value);
               success = true;
            }
         }
         return retval;
      }
      catch (Exception e) {
         success = false;
         throw e;
      }
      finally {
         computeStats(success);
      }
   }

   // ------------ START: Partial state transfer methods ------------

   public void fetchPartialState(List<SocketAddress> sources, Fqn sourceTarget, Fqn integrationTarget) throws Exception {
      String encodedStateId = sourceTarget + DefaultStateTransferManager.PARTIAL_STATE_DELIMITER + integrationTarget;
      fetchPartialState(sources, encodedStateId);
   }

   public void fetchPartialState(List<SocketAddress> sources, Fqn subtree) throws Exception {
      if (subtree == null) {
         throw new IllegalArgumentException("Cannot fetch partial state. Null subtree.");
      }
      fetchPartialState(sources, subtree.toString());
   }

   private void fetchPartialState(List<SocketAddress> sources, String stateId) throws Exception {
      if (sources == null || sources.isEmpty() || stateId == null) {
         // should this really be throwing an exception?  Are there valid use cases where partial state may not be available? - Manik
         // Yes -- cache is configured LOCAL but app doesn't know it -- Brian
         //throw new IllegalArgumentException("Cannot fetch partial state, targets are " + sources + " and stateId is " + stateId);
         if (log.isWarnEnabled()) {
            log.warn("Cannot fetch partial state, targets are " + sources + " and stateId is " + stateId);
         }
         return;
      }

      List<SocketAddress> targets = new LinkedList<SocketAddress>(sources);

      //skip *this* node as a target
      targets.remove(getLocalAddress());

      if (targets.isEmpty()) {
         // Definitely no exception here -- this happens every time the 1st node in the
         // cluster activates a region!! -- Brian
         if (log.isDebugEnabled()) log.debug("Cannot fetch partial state. There are no target members specified");
         return;
      }

      if (log.isDebugEnabled()) {
         log.debug("Node " + getLocalAddress() + " fetching partial state " + stateId + " from members " + targets);
      }
      boolean successfulTransfer = false;
      for (SocketAddress target : targets) {
         try {
            if (log.isDebugEnabled()) {
               log.debug("Node " + getLocalAddress() + " fetching partial state " + stateId + " from member " + target);
            }
            messageListener.setStateSet(false);
            successfulTransfer = getState(stateId, target);
            if (successfulTransfer) {
               try {
                  messageListener.waitForState();
               }
               catch (Exception transferFailed) {
                  if (log.isTraceEnabled()) log.trace("Error while fetching state", transferFailed);
                  successfulTransfer = false;
               }
            }
            if (log.isDebugEnabled()) {
               log.debug("Node " + getLocalAddress() + " fetching partial state " + stateId + " from member " + target + (successfulTransfer ? " successful" : " failed"));
            }
            if (successfulTransfer) break;
         }
         catch (IllegalStateException ise) {
            // thrown by the JGroups channel if state retrieval fails.
            if (log.isInfoEnabled()) {
               log.info("Channel problems fetching state.  Continuing on to next provider. ", ise);
            }
         }
      }

      if (!successfulTransfer && log.isDebugEnabled()) {
         log.debug("Node " + getLocalAddress() + " could not fetch partial state " + stateId + " from any member " + targets);
      }

   }

   private boolean getState(String stateId, SocketAddress target) throws ChannelNotConnectedException, ChannelClosedException {
	   throw new NullPointerException ("Nada!!");
	   //TODO - ser�?!
      //lastStateTransferSource = target;
      //return ((JChannel) channel).getState(target, stateId, configuration.getStateRetrievalTimeout(), true);
   }


   // ------------ END: Partial state transfer methods ------------

   // ------------ START: Informational methods ------------

   @ManagedAttribute(description = "Local address")
   public String getLocalAddressString() {
	   SocketAddress address = getLocalAddress();
      return address == null ? "null" : address.toString();
   }

   public SocketAddress getLastStateTransferSource() {
      return lastStateTransferSource;
   }

   
   public SocketAddress getLocalAddress() {
	   return localAddress != null ? localAddress : null;
   }

   @ManagedAttribute(description = "Cluster view")
   public String getMembersString() {
      List l = getMembers();
      return l == null ? "null" : l.toString();
   }

   public List<SocketAddress> getMembers() {
      if (isInLocalMode) return null;
      if (members == null) {
         return Collections.emptyList();
      } else {
         return members;
      }
   }

   public boolean isCoordinator() {
      return coordinator;
   }

   public SocketAddress getCoordinator() {
      if (channel == null) {
         return null;
      }

      synchronized (coordinatorLock) {
         while (members == null || members.isEmpty()) {
            log.debug("getCoordinator(): waiting on viewAccepted()");
            try {
               coordinatorLock.wait();
            }
            catch (InterruptedException e) {
               log.error("getCoordinator(): Interrupted while waiting for members to be set", e);
               break;
            }
         }
         return members != null && members.size() > 0 ? members.get(0) : null;
      }
   }

   // ------------ END: Informational methods ------------

   /*----------------------- MembershipListener ------------------------*/

   protected class MembershipListenerAdaptor implements MembershipListener, BlockListener {

      public void viewAccepted(View newView) {
         try {
            Vector<SocketAddress> newMembers = null;
          //TODO - Conversar com os caras
            //newView.getMembers();
            if (log.isInfoEnabled()) log.info("Received new cluster view: " + newView);
            synchronized (coordinatorLock) {
               boolean needNotification = false;
               if (newMembers != null) {
                  if (members != null) {
                     // we had a membership list before this event.  Check to make sure we haven't lost any members,
                     // and if so, determine what members have been removed
                     // and roll back any tx and break any locks
                     List<SocketAddress> removed = new ArrayList<SocketAddress>(members);
                     removed.removeAll(newMembers);
                     spi.getInvocationContext().getOptionOverrides().setSkipCacheStatusCheck(true);
                     NodeSPI root = spi.getRoot();
                     if (root != null) {
                        // UGH!!!  What a shameless hack!
                        if (configuration.getNodeLockingScheme() == NodeLockingScheme.MVCC) {

                           removeLocksForDeadMembers(root.getDelegationTarget(), removed);
                        } else {
                           removeLocksForDeadMembers(root, removed);
                        }
                     }
                  }

                  members = new ArrayList<SocketAddress>(newMembers); // defensive copy.

                  needNotification = true;
               }

               // Now that we have a view, figure out if we are the coordinator
               coordinator = (members != null && members.size() != 0 && members.get(0).equals(getLocalAddress()));

               // now notify listeners - *after* updating the coordinator. - JBCACHE-662
               if (needNotification && notifier != null) {
                  InvocationContext ctx = spi.getInvocationContext();
                  notifier.notifyViewChange(newView, ctx);
               }

               // Wake up any threads that are waiting to know about who the coordinator is
               coordinatorLock.notifyAll();
            }
         }
         catch (Throwable e) {
            //do not rethrow! jgroups might behave funny, resulting even in deadlock
            log.error("Error found while processing view accepted!!!", e);
         }
      }

      /**
       * Called when a member is suspected.
       */
      public void suspect(SocketAddress suspected_mbr) {
      }

      /**
       * Indicates that a channel has received a BLOCK event from FLUSH protocol.
       */
      public void block() {
         try {
            if (log.isDebugEnabled()) log.debug("Block received at " + getLocalAddress());

            flushTracker.block();
            notifier.notifyCacheBlocked(true);
            notifier.notifyCacheBlocked(false);

            if (log.isDebugEnabled()) log.debug("Block processed at " + getLocalAddress());
         }
         catch (Throwable e) {
            //do not rethrow! jgroups might behave funny, resulting even in deadlock
            log.error("Error found while processing block()", e);
         }
      }

      /**
       * Indicates that a channel has received a UNBLOCK event from FLUSH protocol.
       */
      public void unblock() {
         try {
            if (log.isDebugEnabled()) log.debug("UnBlock received at " + getLocalAddress());

            notifier.notifyCacheUnblocked(true);
            notifier.notifyCacheUnblocked(false);
            flushTracker.unblock();

            if (log.isDebugEnabled()) log.debug("UnBlock processed at " + getLocalAddress());
         }
         catch (Throwable e) {
            //do not rethrow! jgroups might behave funny, resulting even in deadlock
            log.error("Error found while processing unblock", e);
         }
      }


      @Override
      public void onExcluded() {
    	  try {
  			controlSession.leave();
  		} catch (ClosedSessionException e) {
  			log.info("Ao fechar a sessao", e);			
  		} catch (JGCSException e) {
  			log.info("Ao fechar a sessao", e);
  		}
		
      }

      public void onMembershipChange() {
  		SocketAddress coordenador;
  		try {
  			
  			coordenador = controlSession.getMembership().getMemberAddress(controlSession.getMembership().getCoordinatorRank());
  			//TODO - Conversar com os caras  			
  			//View newView=new View(id,socketAddressToAddress(controlSession.getMembership().getMembershipList()));
  			//viewAccepted(newView);			
  			
  			
  		} catch (NotJoinedException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		} catch (UnsupportedOperationException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
  	}

      @Override
      public void onBlock() {
    	  this.block();
		
      }

   }

   //jmx operations
   private void computeStats(boolean success) {
      if (statisticsEnabled && rpcDispatcher != null) {
         if (success) {
            replicationCount++;
         } else {
            replicationFailures++;
         }
      }
   }

   @ManagedOperation
   public void resetStatistics() {
      this.replicationCount = 0;
      this.replicationFailures = 0;
   }

   @ManagedAttribute(description = "number of successful replications")
   public long getReplicationCount() {
      return replicationCount;
   }

   @ManagedAttribute(description = "number of failed replications")
   public long getReplicationFailures() {
      return replicationFailures;
   }

   @ManagedAttribute(description = "whether or not jmx statistics are enabled")
   public boolean isStatisticsEnabled() {
      return statisticsEnabled;
   }

   @ManagedAttribute(description = "whether or not the RPCManager is used in this cache instance")
   public boolean isEnabled() {
      return !isInLocalMode;
   }

   @ManagedAttribute
   public void setStatisticsEnabled(boolean statisticsEnabled) {
      this.statisticsEnabled = statisticsEnabled;
   }

   @ManagedAttribute(description = "RPC call success ratio")
   public String getSuccessRatio() {
      if (replicationCount == 0 || !statisticsEnabled) {
         return "N/A";
      }
      double totalCount = replicationCount + replicationFailures;
      double ration = (double) replicationCount / totalCount * 100d;
      return NumberFormat.getInstance().format(ration) + "%";
   }

   /**
    * Checks to see whether the cache is using an appropriate JGroups config.
    */
   private void checkAppropriateConfig() {
      //if we use a shared transport do not log any warn message
      if (configuration.getMultiplexerStack() != null) {
         return;
      }
      //bundling is not good for sync caches
      Configuration.CacheMode cacheMode = configuration.getCacheMode();
      if (!cacheMode.equals(Configuration.CacheMode.LOCAL) && configuration.getCacheMode().isSynchronous()) {
         ProtocolStack stack = ((JChannel) channel).getProtocolStack();
         TP transport = stack.getTransport();
         if (transport.isEnableBundling()) {
            log.warn("You have enabled jgroups's message bundling, which is not recommended for sync replication. If there is no particular " +
                  "reason for this we strongly recommend to disable message bundling in JGroups config (enable_bundling=\"false\").");
         }
      }
      //bundling is good for async caches
      if (!cacheMode.isSynchronous()) {
         ProtocolStack stack = ((JChannel) channel).getProtocolStack();
         TP transport = stack.getTransport();
         if (!transport.isEnableBundling()) {
            log.warn("You have disabled jgroups's message bundling, which is not recommended for async replication. If there is no particular " +
                  "reason for this we strongly recommend to enable message bundling in JGroups config (enable_bundling=\"true\").");
         }
      }
   }

   public FlushTracker getFlushTracker() {
      return flushTracker;
   }
   
   static class RspFilterWrapper implements RspFilter {
	   private org.jgroups.blocks.RspFilter responseFilter;
	   public RspFilterWrapper(org.jgroups.blocks.RspFilter responseFilter){
		   this.responseFilter = responseFilter;
	   }
	   public boolean isAcceptable(Object response, SocketAddress sender) {
		   //TODO Tem que ver
		   return true;		   
	   }
	   public boolean needMoreResponses() {
		   return responseFilter.needMoreResponses();
	   }
	   
   }
}
