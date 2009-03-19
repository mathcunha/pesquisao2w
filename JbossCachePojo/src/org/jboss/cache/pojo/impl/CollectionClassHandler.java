/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.proxy.ClassProxy;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.collection.CollectionInterceptorUtil;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.BaseInterceptor;

/**
 * Handling the Collection class management. Has no consideration of object graph here.
 *
 * @author Ben Wang
 *         Date: Aug 4, 2005
 * @version $Id: CollectionClassHandler.java 7052 2008-11-02 18:13:25Z bstansberry@jboss.com $
 */
class CollectionClassHandler extends AbstractHandler
{
   private final Log log = LogFactory.getLog(CollectionClassHandler.class);
   private Cache<Object, Object> cache_;
   private PojoCacheImpl pCache_;
   private InternalHelper internal_;

   public CollectionClassHandler(PojoCacheImpl pCache, InternalHelper internal)
   {
      pCache_ = pCache;
      cache_ = pCache_.getCache();
      internal_ = internal;
   }

   protected Fqn<?> getFqn(Object collection)
   {
      if (! (collection instanceof ClassProxy))
         return null;

      BaseInterceptor interceptor = CollectionInterceptorUtil.getInterceptor((ClassProxy) collection);
      return interceptor != null ? interceptor.getFqn() : null;
   }

   protected Object get(Fqn fqn, Class clazz, PojoInstance pojoInstance)
         throws CacheException
   {
      Object obj = null;
      try
      {
         if (Map.class.isAssignableFrom(clazz))
         {
            Object map = clazz.newInstance();
            obj = CollectionInterceptorUtil.createMapProxy(pCache_, fqn, clazz, (Map) map);
         }
         else if (List.class.isAssignableFrom(clazz))
         {
            Object list = clazz.newInstance();
            obj = CollectionInterceptorUtil.createListProxy(pCache_, fqn, clazz, (List) list);
         }
         else if (Set.class.isAssignableFrom(clazz))
         {
            Object set = clazz.newInstance();
            obj = CollectionInterceptorUtil.createSetProxy(pCache_, fqn, clazz, (Set) set);
         }
      }
      catch (Exception e)
      {
         throw new CacheException("failure creating proxy", e);
      }

      return obj;
   }

   protected void put(Fqn fqn, Reference reference, Object obj) throws CacheException
   {
      boolean isCollection = false;

      CachedType type = null;
      if (obj instanceof ClassProxy)
      {
         throw new IllegalStateException("CollectionClassHandler.put(): obj is an ClassProxy instance " + obj);
      }

      type = pCache_.getCachedType(obj.getClass());

      //JBCACHE-760: for collection - put initialized aopInstance in fqn
      if (!(obj instanceof Map || obj instanceof List || obj instanceof Set))
      {
         return;
      }

      // Always initialize the ref count so that we can mark this as an AopNode.
      PojoInstance pojoInstance = InternalHelper.initializeAopInstance(reference);
      pojoInstance.set(obj);
      pojoInstance.setPojoClass(type.getType());
      cache_.put(fqn, PojoInstance.KEY, pojoInstance);

      if (obj instanceof Map)
      {
         if (log.isTraceEnabled())
         {
            log.trace("collectionPutObject(): aspectized obj is a Map type of size: " + ((Map) obj).size());
         }

         // Let's replace it with a proxy if necessary
         Map map = (Map) obj;
         if (!(obj instanceof ClassProxy))
         {
            Class clazz = obj.getClass();
            try
            {
               obj = CollectionInterceptorUtil.createMapProxy(pCache_, fqn, clazz, (Map) obj);
            }
            catch (Exception e)
            {
               throw new CacheException("failure creating proxy", e);
            }

            checkMapRecursion(map, obj);
         }

         isCollection = true;
         // populate via the proxied collection
         for (Iterator i = map.entrySet().iterator(); i.hasNext();)
         {
            Map.Entry entry = (Map.Entry) i.next();
            ((Map) obj).put(entry.getKey(), entry.getValue());
         }

      }
      else if (obj instanceof List)
      {
         if (log.isTraceEnabled())
         {
            log.trace("collectionPutObject(): aspectized obj is a List type of size: "
                      + ((List) obj).size());
         }

         List list = (List) obj;

         // Let's replace it with a proxy if necessary
         if (!(obj instanceof ClassProxy))
         {
            Class clazz = obj.getClass();
            try
            {
               obj = CollectionInterceptorUtil.createListProxy(pCache_, fqn, clazz, (List) obj);
            }
            catch (Exception e)
            {
               throw new CacheException("failure creating proxy", e);
            }

            checkListRecursion(list, obj);
         }

         isCollection = true;
         // populate via the proxied collection
         for (Iterator i = list.iterator(); i.hasNext();)
         {
            ((List) obj).add(i.next());
         }

      }
      else if (obj instanceof Set)
      {
         if (log.isTraceEnabled())
         {
            log.trace("collectionPutObject(): aspectized obj is a Set type of size: "
                      + ((Set) obj).size());
         }

         Set set = (Set) obj;

         // Let's replace it with a proxy if necessary
         if (!(obj instanceof ClassProxy))
         {
            Class clazz = obj.getClass();
            try
            {
               obj = CollectionInterceptorUtil.createSetProxy(pCache_, fqn, clazz, (Set) obj);
            }
            catch (Exception e)
            {
               throw new CacheException("failure creating proxy", e);
            }

            checkSetRecursion(set, obj);
         }

         isCollection = true;
         // populate via the proxied collection
         for (Iterator i = set.iterator(); i.hasNext();)
         {
            ((Set) obj).add(i.next());
         }

      }

      if (isCollection)
      {
         // Need to reset it here in case this is a new proxy instance
         pojoInstance.set(obj);
         // Attach pojoReference to that interceptor
         BaseInterceptor baseInterceptor = (BaseInterceptor) CollectionInterceptorUtil.getInterceptor(
               (ClassProxy) obj);
         baseInterceptor.setAopInstance(pojoInstance);
      }
   }

   private void checkListRecursion(List list, Object obj)
   {
      while (true)
      {
         int i = list.indexOf(list); // check for recursion
         if (i == -1) break;

         list.remove(list);
         list.add(i, obj);
      }
   }

   private void checkSetRecursion(Set set, Object obj)
   {
      if (set.remove(set))
      {
         set.add(obj); // replace with proxy
         throw new PojoCacheException("CollectionClassHandler.checkSetRecursion(): " +
                                      "detect a recursive set (e.g., set inside the same set). This will fail to " +
                                      "replicate even outside of PojoCache with HashSet. " + set);
      }
   }

   private void checkMapRecursion(Map map, Object obj)
   {
      Map m = java.util.Collections.unmodifiableMap(map);

      for (Object k : m.keySet())
      {
         if (m == k)
         {
            throw new PojoCacheException("CollectionClassHandler.checkMapRecursion(): " +
                                         " Can't handle the recursion map where it is nested in a constant key " + map);
         }

         Object v = m.get(k);
         if (v == map)
         {
            throw new PojoCacheException("CollectionClassHandler.checkMapRecursion(): " +
                                         "detect a recursive map (e.g., map inside the same map). This will fail to " +
                                         "replicate even outside of PojoCache with HashMap because of hashCode. " + map);
            // recursion here, replace it with proxy
//            map.put(k, obj);
         }
      }
   }

   @Override
   protected Object remove(Fqn<?> fqn, Reference referencingFqn, Object obj) throws CacheException
   {
      if (!(obj instanceof ClassProxy))
      {
         throw new PojoCacheException("CollectionClassHandler.collectionRemoveObject(): object is not a proxy :" + obj);
      }

      AbstractCollectionInterceptor interceptor = (AbstractCollectionInterceptor) CollectionInterceptorUtil.getInterceptor((ClassProxy) obj);
      // detach the interceptor. This will trigger a copy and remove.
      (new PojoUtil()).detachCollectionInterceptor(interceptor);
      cache_.removeNode(fqn);

      return interceptor.getCurrentCopy();
   }


   @Override
   protected boolean handles(Class<?> clazz)
   {
      return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
   }
}
