/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.Reference;

/**
 * Handle Serializable object cache management.
 *
 * @author Ben Wang
 * @version $Id: SerializableObjectHandler.java 7052 2008-11-02 18:13:25Z bstansberry@jboss.com $
 */
class SerializableObjectHandler extends AbstractHandler
{
   private Cache<Object, Object> cache;
   private PojoCacheImpl pojoCache;
   private InternalHelper internal_;
   private final Log log_ = LogFactory.getLog(SerializableObjectHandler.class);

   public SerializableObjectHandler(PojoCacheImpl cache, InternalHelper internal)
   {
      pojoCache = cache;
      this.cache = pojoCache.getCache();
      internal_ = internal;
   }

   protected Fqn<?> getFqn(Object obj)
   {
      // Not supported
      return null;
   }

   @Override
   protected boolean handles(Class<?> clazz)
   {
      return Serializable.class.isAssignableFrom(clazz);
   }

   @Override
   protected Object get(Fqn fqn, Class clazz, PojoInstance pojoInstance) throws CacheException
   {
      Object obj = internal_.get(fqn, InternalConstant.SERIALIZED);
      return obj;
   }


   @Override
   protected void put(Fqn<?> fqn, Reference reference, Object obj) throws CacheException
   {
      // Note that JBoss Serialization can serialize any type now.
      if (log_.isTraceEnabled())
      {
         log_.trace("put(): obj (" + obj.getClass() + ") is non-advisable but serialize it anyway. "
                    + "Note that if it is non-serializable we require to use JBoss Serialization.");
      }

      putIntoCache(fqn, obj);
   }

   private void putIntoCache(Fqn fqn, Object obj)
         throws CacheException
   {
      Map map = new HashMap();

      // Special optimization here.
      PojoInstance pojoInstance = new PojoInstance();
      pojoInstance.set(obj);
      pojoInstance.setPojoClass(obj.getClass());
      map.put(PojoInstance.KEY, pojoInstance);
      // Note that we will only have one key in this fqn.
      map.put(InternalConstant.SERIALIZED, obj);
      internal_.put(fqn, map);
   }

   @Override
   protected Object remove(Fqn<?> fqn, Reference reference, Object result) throws CacheException
   {
      cache.removeNode(fqn);
      return result;
   }
}