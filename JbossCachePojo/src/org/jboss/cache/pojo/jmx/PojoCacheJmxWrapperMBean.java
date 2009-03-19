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

import org.jboss.cache.CacheStatus;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.jmx.LegacyConfiguration;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheAlreadyDetachedException;
import org.jboss.cache.pojo.PojoCacheException;

/**
 * StandardMBean interface for {@link PojoCacheJmxWrapperMBean}.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 5431 $
 */
public interface PojoCacheJmxWrapperMBean extends LegacyConfiguration
{
   /** The lifecycle method stop has completed */
   public static final int STOPPED  = 0;
   /** The lifecycle method stop has been invoked */
   public static final int STOPPING = 1;
   /** The lifecycle method start has been invoked */
   public static final int STARTING = 2;
   /** The lifecycle method start has completed */
   public static final int STARTED  = 3;
   /** There has been an error during some operation */
   public static final int FAILED  = 4;
   /** The lifecycle method destroy has completed */
   public static final int DESTROYED = 5;
   /** The lifecycle method create has completed */
   public static final int CREATED = 6;
   /** The MBean has been instantiated but has not completed MBeanRegistration.postRegister */
   public static final int UNREGISTERED = 7;
   /** The MBean has been instantiated and has completed MBeanRegistration.postRegister */
   public static final int REGISTERED = 8;
   
   /**
    * Lifecycle method to start PojoCache.
    *
    * @throws PojoCacheException
    */
   void create() throws PojoCacheException;

   /**
    * Lifecycle method to start PojoCache.
    *
    * @throws PojoCacheException
    */
   void start() throws PojoCacheException;

   /**
    * Lifecycle method to stop PojoCache. Note that PojoCache can be stopped and started
    * repeatedly.
    *
    * @throws PojoCacheException
    */
   void stop() throws PojoCacheException;

   /**
    * Lifecycle method to destroy PojoCache.
    *
    * @throws PojoCacheException
    */
   void destroy() throws PojoCacheException;
   
   /**
    * Gets where this object is in its lifecycle transitions.
    * 
    * @return the current status. Will not return <code>null</code>
    */
   CacheStatus getCacheStatus();
   
   /**
    * Legacy attribute to expose the {@link #getCacheStatus() cache status} 
    * in terms of the JBoss AS ServiceMBean values.  This interface does
    * not extend ServiceMBean, but this attribute is retained to provide
    * compatibility with the JBoss AS JSR-77 integration layer.
    * 
    * @return the current status, e.g. {@link #STARTED}.
    */
   int getState();
   
   /**
    * Returns the PojoCache.
    * 
    * @return the PojoCache
    */
   PojoCache getPojoCache(); 

   /**
    * Retrieves an immutable configuration.
    */
   Configuration getConfiguration();
   
   /**
    * Gets whether this object should register a {@link PojoCacheJmxWrapperMBean}
    * for the underlying {@link PojoCache} with JMX.
    * <p/>
    * Default is <code>true</code>.
    */
   boolean getRegisterPlainCache();

   /**
    * Sets whether this object should register a {@link PojoCacheJmxWrapperMBean}
    * for the underlying {@link PojoCache} with JMX.
    * <p/>
    * Default is <code>true</code>.
    * <p/>
    * If <code>true</code>, the <code>PojoCacheJmxWrapperMBean</code> will be
    * instantiated and registered either as part of the registration of
    * this object, or during the call to {@link #create()}.
    */
   public void setRegisterPlainCache(boolean registerPlainCache);
   
   /**
    * Gets whether this object should register the cache's interceptors
    * with JMX.
    * <p/>
    * This property is only relevant if {@link #setRegisterPlainCache(boolean) registerPlainCache}
    * is <code>true</code>.
    * <p/>
    * Default is <code>true</code>.
    */
   boolean getRegisterInterceptors();
   
   /**
    * Sets whether this object should register the cache's interceptors
    * with JMX.
    * <p/>
    * This property is only relevant if {@link #setRegisterPlainCache(boolean) registerPlainCache}
    * is <code>true</code>.
    * <p/>
    * Default is <code>true</code>.
    */
   void setRegisterInterceptors(boolean register);
   
   /**
    * Return number of POJO attach operations for this particular id.
    * @return Number of attach operation.
    */
//   public long getNumberOfAttach();

   /**
    * Return number of POJO detach operations for this particular id.
    * @return Number of detach operation.
    */
//   public long getNumberOfDetach();

   /**
    * Return number of POJO field read operations for this particulxar id.
    * @param pojo That is associated with this POJO. If null, it means all POJOs in this cache system.
    * @return Number of field read operation.
    * @throws PojoCacheAlreadyDetachedException if pojo has been detached already.
    */
//   public long getNumberOfFieldRead(Object pojo) throws PojoCacheAlreadyDetachedException;

   /**
    * Return number of POJO field write operations for this particular id.
    * @param pojo That is associated with this POJO. If null, it means all POJOs in this cache system.
    * @return Number of field read operation.
    * @throws PojoCacheAlreadyDetachedException if pojo has been detached already.
    */
//   public long getNumberOfFieldWrite(Object pojo) throws PojoCacheAlreadyDetachedException;

   /**
    * Reset all stats.
    */
//   public void reset();

   /**
    * Obtain the internal location of this pojo stored under PojoCache.
    * @param pojo That is associated with this POJO. If null, it means all POJOs in this cache system.
    * @return String that indicates the location.
    * @throws PojoCacheAlreadyDetachedException if pojo has been detached already.
    */
   public String getInternalLocation(Object pojo) throws PojoCacheAlreadyDetachedException;

   /**
    * Get the MBean object name that the underlying replicated cache is using.
    */
   public String getUnderlyingCacheObjectName();
}
