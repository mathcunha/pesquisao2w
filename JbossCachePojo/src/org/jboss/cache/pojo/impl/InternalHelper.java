/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.*;
import org.jboss.cache.lock.UpgradeException;
import org.jboss.cache.pojo.*;
import org.jboss.cache.pojo.util.ObjectUtil;

/**
 * Internal helper class to handle internal cache sotre, that is, the portion that is not part of
 * user's data.
 *
 * @author Ben Wang
 */
public class InternalHelper
{
   private static  Log log = LogFactory.getLog(InternalHelper.class.getName());

   private Cache<Object, Object> cache;
   private PojoCache pcache;

   InternalHelper(PojoCache pcache)
   {
      this.cache = pcache.getCache();
      this.pcache = pcache;
   }

   PojoInstance getPojoInstance(Fqn fqn) throws CacheException
   {
      return (PojoInstance) get(fqn, PojoInstance.KEY, true);
   }

   PojoReference getPojoReference(Fqn fqn, String field) throws CacheException
   {
      if (field == null)
         field = PojoReference.KEY;

      return (PojoReference) get(fqn, field, true);
   }

   PojoReference getPojoReference(Fqn fqn) throws CacheException
   {
      return getPojoReference(fqn, null);
   }


   static PojoInstance initializeAopInstance(Reference reference)
   {
      PojoInstance pojoInstance = new PojoInstance();

      pojoInstance.incrementRefCount(reference);
      return pojoInstance;
   }

   /**
    * Increment reference count for the pojo. Note that this is not thread safe or atomic.
    * @param reference TODO
    */
   int incrementRefCount(Fqn originalFqn, Reference reference) throws CacheException
   {
      PojoInstance pojoInstance = getPojoInstance(originalFqn);
      if (pojoInstance == null)
         throw new PojoCacheException("InternalDelegate.incrementRefCount(): null pojoReference for fqn: " + originalFqn);

      int count = pojoInstance.incrementRefCount(reference);
      // need to update it.
      put(originalFqn, PojoInstance.KEY, pojoInstance);
      return count;
   }

   /**
    * Has a delegate method so we can use the switch.
    */

   Object get(Fqn fqn, Object key) throws CacheException
   {
      return get(fqn, key, false);
   }

   private Object get(Fqn fqn, Object key, boolean gravitate) throws CacheException
   {
      // Only gravitate when we have to and only when the user has enabled it
      if (gravitate && pcache.getThreadContext().isGravitationEnabled())
      {
         cache.getInvocationContext().getOptionOverrides().setForceDataGravitation(true);
         Object obj = cache.get(fqn, key);
         cache.getInvocationContext().getOptionOverrides().setForceDataGravitation(false);
         return obj;
      }

      return cache.get(fqn, key);
   }

   private void put(Fqn fqn, Object key, Object value) throws CacheException
   {
      cache.put(fqn, key, value);
   }

   void put(Fqn fqn, Map map) throws CacheException
   {
      cache.put(fqn, map);
   }


   /**
    * decrement reference count for the pojo. Note that this is not thread safe or atomic.
    */
   int decrementRefCount(Fqn originalFqn, Reference reference) throws CacheException
   {
      PojoInstance pojoInstance = getPojoInstance(originalFqn);
      if (pojoInstance == null)
         throw new PojoCacheException("InternalDelegate.decrementRefCount(): null pojoReference.");

      int count = pojoInstance.decrementRefCount(reference);

      if (count < -1)  // can't dip below -1
         throw new PojoCacheException("InternalDelegate.decrementRefCount(): null pojoReference.");

      // need to update it.
      put(originalFqn, PojoInstance.KEY, pojoInstance);
      return count;
   }

   static boolean isReferenced(PojoInstance pojoInstance)
   {
      // If ref counter is greater than 0, we fqn is being referenced.
      return (pojoInstance.getRefCount() > 0);
   }

   Object getPojo(Fqn fqn, String field) throws CacheException
   {
      PojoReference pojoReference = getPojoReference(fqn, field);
      Fqn realFqn = null;
      if (pojoReference != null)
      {
         // This is outward facing node
         realFqn = pojoReference.getFqn();
      }
      else
      {
         // If we are looking for a field then there must be a reference
         if (field != null)
            return null;

         // This is the internal node.
         realFqn = fqn;
      }

      PojoInstance pojoInstance = getPojoInstance(realFqn);
      if (pojoInstance == null)
         return null;

      return pojoInstance.get();
   }

   void setPojo(Fqn fqn, Object pojo) throws CacheException
   {
      PojoInstance pojoInstance = getPojoInstance(fqn);
      if (pojoInstance == null)
      {
         pojoInstance = new PojoInstance();
         put(fqn, PojoInstance.KEY, pojoInstance);
      }

      pojoInstance.set(pojo);
      // No need to do a cache put since pojo is transient anyway.
   }

   static boolean isMultipleReferenced(PojoInstance pojoInstance)
   {
      if (pojoInstance.getRefCount() > (PojoInstance.INITIAL_COUNTER_VALUE + 1)) return true;

      return false;
   }

   static void setPojo(PojoInstance pojoInstance, Object pojo)
   {
      // No need to do a cache put since pojo is transient anyway.
      pojoInstance.set(pojo);
   }

   void setPojo(Fqn fqn, Object pojo, PojoInstance pojoInstance) throws CacheException
   {
      if (pojoInstance == null)
      {
         pojoInstance = new PojoInstance();
         put(fqn, PojoInstance.KEY, pojoInstance);
      }

      pojoInstance.set(pojo);
      // No need to do a cache put since pojo is transient anyway.
   }

   void putPojoReference(Fqn fqn, PojoReference pojoReference) throws CacheException
   {
      putPojoReference(fqn, pojoReference, PojoReference.KEY);
   }

   void putPojoReference(Fqn fqn, PojoReference pojoReference, String field) throws CacheException
   {
      if (field == null)
         field = PojoReference.KEY;

      put(fqn, field, pojoReference);
   }

   void putAopClazz(Fqn fqn, Class clazz) throws CacheException
   {
      put(fqn, InternalConstant.CLASS_INTERNAL, clazz);
   }

   /**
    * We store the class name in string and put it in map instead of directly putting
    * it into cache for optimization.
    */
   static void putAopClazz(Class clazz, Map map)
   {
      map.put(InternalConstant.CLASS_INTERNAL, clazz);
   }

   Class peekAopClazz(Fqn fqn) throws CacheException
   {
      return (Class) get(fqn, InternalConstant.CLASS_INTERNAL);
   }

   void removeInternalAttributes(Fqn fqn) throws CacheException
   {
      cache.remove(fqn, PojoInstance.KEY);
      cache.remove(fqn, InternalConstant.CLASS_INTERNAL);
   }

   void cleanUp(Fqn fqn, String field) throws CacheException
   {
      if (field != null)
      {
         cache.remove(fqn, field);
         return;
      }

      // We can't do a brute force remove anymore?
      if (cache.getRoot().getChild(fqn).getChildren().size() == 0)
      {
         // remove everything
         cache.removeNode(fqn);
//         cache_.getRoot().getChild(fqn).clearData();
//         removeNodeWithoutInterceptor(fqn);
      }
      else
      {
         // Assume everything here is all PojoCache data for optimization
         cache.getRoot().getChild(fqn).clearData();
         if (log.isTraceEnabled())
         {
            log.trace("cleanup(): fqn: " + fqn + " is not empty. That means it has sub-pojos. Will not remove node");
         }
      }
   }

   String createIndirectFqn(String fqn) throws CacheException
   {
      String indirectFqn = getIndirectFqn(fqn);
      Fqn internalFqn = getInternalFqn(fqn);
      put(internalFqn, indirectFqn, fqn);
      return indirectFqn;
   }

   private Fqn getInternalFqn(String fqn)
   {
      if (fqn == null || fqn.length() == 0)
         throw new IllegalStateException("InternalDelegate.getInternalFqn(). fqn is either null or empty!");

      String indirectFqn = getIndirectFqn(fqn);
      return new Fqn(InternalConstant.JBOSS_INTERNAL_MAP, indirectFqn);
//      return JBOSS_INTERNAL_MAP;
   }

   static String getIndirectFqn(String fqn)
   {
      // TODO This is not unique. Will need to come up with a better one in the future.
      return ObjectUtil.getIndirectFqn(fqn);
   }

   void removeIndirectFqn(String oldFqn) throws CacheException
   {
      String indirectFqn = getIndirectFqn(oldFqn);
      cache.remove(getInternalFqn(oldFqn), indirectFqn);
   }

   void setIndirectFqn(String oldFqn, String newFqn) throws CacheException
   {
      String indirectFqn = getIndirectFqn(oldFqn);
      Fqn tmpFqn = getInternalFqn(oldFqn);
      put(tmpFqn, indirectFqn, newFqn);
   }

   void updateIndirectFqn(Fqn originalFqn, Fqn newFqn) throws CacheException
   {
      put(getInternalFqn(originalFqn.toString()), getIndirectFqn(originalFqn.toString()), newFqn.toString());
   }

   private String getRefFqnFromAlias(String aliasFqn) throws CacheException
   {
      return (String) get(getInternalFqn(aliasFqn), aliasFqn, true);
   }

   /**
    * Test if this internal node.
    *
    * @param fqn
    */
   public static boolean isInternalNode(Fqn fqn)
   {
      // we ignore all the node events corresponding to JBOSS_INTERNAL
      if (fqn.isChildOrEquals(InternalConstant.JBOSS_INTERNAL)) return true;

      return false;
   }

   public boolean lockPojo(Fqn id) throws CacheException
   {
      final int RETRY = 5;

      if (log.isTraceEnabled())
         log.trace("lockPojo(): id:" + id);

      boolean isNeeded = true;
      int retry = 0;

      while (isNeeded)
      {
         try
         {
            cache.put(id, InternalConstant.POJOCACHE_LOCK, "LOCK");
            isNeeded = false;
         }
         catch (UpgradeException upe)
         {
            log.warn("lockPojo(): can't upgrade the lock during lockPojo. Will re-try. id: " + id
                     + " retry times: " + retry);
            if (retry++ > RETRY)
               return false;

            // try to sleep a little as well.
            try
            {
               Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
            }
         }
      }

      return true;
   }
}