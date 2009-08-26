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
package org.jboss.cache.config;

import org.jboss.cache.RPCManager;
import org.jboss.cache.buddyreplication.BuddyGroup;
import org.jboss.cache.util.Util;
import org.jboss.cache.util.ThreadFactory;

import javax.transaction.TransactionManager;
import java.util.concurrent.ExecutorService;

public class RuntimeConfig extends ConfigurationComponent
{
   /**
    * The serialVersionUID
    */
   private static final long serialVersionUID = 5626847485703341794L;

   private transient TransactionManager transactionManager;
   private transient BuddyGroup buddyGroup;
   private RPCManager rpcManager;
   private transient ThreadFactory evictionTimerThreadFactory;
   private transient ExecutorService asyncSerializationExecutor;
   private transient ExecutorService asyncCacheListenerExecutor;

   /**
    * Resets the runtime to default values.
    */
   public void reset()
   {      
      rpcManager = null;
   }

   public TransactionManager getTransactionManager()
   {
      return transactionManager;
   }

   public void setTransactionManager(TransactionManager transactionManager)
   {
      testImmutability("transactionManager");
      this.transactionManager = transactionManager;
   }

   /**
    * This is only relevant if an eviction timer thread factory has been set using {@link #setEvictionTimerThreadFactory(org.jgroups.util.ThreadFactory)}.
    * Will return a null if the eviction timer thread factory needs to be created internally.
    * <p/>
    *
    * @return the thread factory used by the eviction timer's scheduled executor.
    * @since 3.0
    */
   public ThreadFactory getEvictionTimerThreadFactory()
   {
      return evictionTimerThreadFactory;
   }

   /**
    * Sets the eviction timer thread factory to use when creating a scheduled executor.  If this is not set, the eviction
    * timer task will use a default thread factory.
    *
    * @param evictionTimerThreadFactory factory to use
    * @since 3.0
    */
   public void setEvictionTimerThreadFactory(ThreadFactory evictionTimerThreadFactory)
   {
      this.evictionTimerThreadFactory = evictionTimerThreadFactory;
   }

   /**
    * This is only relevant if the async cache replication executor has been set using {@link #setAsyncSerializationExecutor(java.util.concurrent.ExecutorService)}.
    * If the executor is created internally, this method will return null.
    * <p/>
    *
    * @return the executor used for async replication.
    * @since 3.0
    */
   public ExecutorService getAsyncSerializationExecutor()
   {
      return asyncSerializationExecutor;
   }

   /**
    * This is used to set the executor to use for async cache replucation, and effectively overrides {@link org.jboss.cache.config.Configuration#setSerializationExecutorPoolSize(int)}
    * <p/>
    *
    * @param asyncSerializationExecutor executor to set
    * @since 3.0
    */
   public void setAsyncSerializationExecutor(ExecutorService asyncSerializationExecutor)
   {
      this.asyncSerializationExecutor = asyncSerializationExecutor;
   }

   /**
    * This is only relevant if the async cache listener executor has been set using {@link #setAsyncCacheListenerExecutor(java.util.concurrent.ExecutorService)}.
    * If the executor is created internally, this method will return null.
    * <p/>
    *
    * @return the executor to use for async cache listeners
    * @since 3.0
    */
   public ExecutorService getAsyncCacheListenerExecutor()
   {
      return asyncCacheListenerExecutor;
   }

   /**
    * This is used to set the executor to use for async cache listeners, and effectively overrides {@link org.jboss.cache.config.Configuration#setListenerAsyncPoolSize(int)}
    * <p/>
    *
    * @param asyncCacheListenerExecutor the executor to use for async cache listeners
    * @since 3.0
    */
   public void setAsyncCacheListenerExecutor(ExecutorService asyncCacheListenerExecutor)
   {
      this.asyncCacheListenerExecutor = asyncCacheListenerExecutor;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }

      if (obj instanceof RuntimeConfig)
      {
         RuntimeConfig other = (RuntimeConfig) obj;
         return Util.safeEquals(transactionManager, other.transactionManager)               
               && Util.safeEquals(rpcManager, other.rpcManager)               
               && Util.safeEquals(evictionTimerThreadFactory, other.evictionTimerThreadFactory)
               && Util.safeEquals(asyncCacheListenerExecutor, other.asyncCacheListenerExecutor)
               && Util.safeEquals(asyncSerializationExecutor, other.asyncSerializationExecutor);
      }

      return false;
   }

   @Override
   public int hashCode()
   {
      int result = 17;
      result = result * 29 + (transactionManager == null ? 0 : transactionManager.hashCode());      
      result = result * 29 + (rpcManager == null ? 0 : rpcManager.hashCode());      
      result = result * 29 + (evictionTimerThreadFactory == null ? 0 : evictionTimerThreadFactory.hashCode());
      result = result * 29 + (asyncCacheListenerExecutor == null ? 0 : asyncCacheListenerExecutor.hashCode());
      result = result * 29 + (asyncSerializationExecutor == null ? 0 : asyncSerializationExecutor.hashCode());
      return result;
   }

   public void setBuddyGroup(BuddyGroup buddyGroup)
   {
      this.buddyGroup = buddyGroup;
   }

   public BuddyGroup getBuddyGroup()
   {
      return buddyGroup;
   }

   public void setRPCManager(RPCManager rpcManager)
   {
      this.rpcManager = rpcManager;
   }

   public RPCManager getRPCManager()
   {
      return rpcManager;
   }

   @Override
   public RuntimeConfig clone() throws CloneNotSupportedException
   {
      return (RuntimeConfig) super.clone();
   }


}
