/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.Advised;
import org.jboss.aop.Advisor;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.proxy.ClassProxy;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.Region;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.collection.CollectionInterceptorUtil;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.BaseInterceptor;
import org.jboss.cache.pojo.memory.FieldPersistentReference;
import org.jboss.cache.pojo.util.AopUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Delegate class for PojoCache, the real implementation code happens here.
 *
 * @author Ben Wang
 */
public class PojoCacheDelegate
{
   private PojoCacheImpl pojoCache;
   private Cache<Object, Object> cache;
   private final static Log log = LogFactory.getLog(PojoCacheDelegate.class);
   private InternalHelper internal_;
   private AdvisedPojoHandler advisedHandler_;
   private ObjectGraphHandler graphHandler_;
   private CollectionClassHandler collectionHandler_;
   private ArrayHandler arrayHandler;
   private SerializableObjectHandler serializableHandler_;
   // Use ThreadLocal to hold a boolean isBulkRemove
   private PojoUtil util_ = new PojoUtil();

   public PojoCacheDelegate(PojoCacheImpl cache)
   {
      pojoCache = cache;
      this.cache = pojoCache.getCache();
      internal_ = new InternalHelper(cache);
      graphHandler_ = new ObjectGraphHandler(pojoCache, internal_);
      collectionHandler_ = new CollectionClassHandler(pojoCache, internal_);
      serializableHandler_ = new SerializableObjectHandler(pojoCache, internal_);
      advisedHandler_ = new AdvisedPojoHandler(pojoCache, internal_, util_);
      arrayHandler = new ArrayHandler(pojoCache);
   }

   public Object getObject(Fqn fqn, String field, Object source) throws CacheException
   {
      // TODO Must we really to couple with BR? JBCACHE-669
      Object pojo = internal_.getPojo(fqn, field);
      if (pojo != null)
      {
         // we already have an advised instance
         if (log.isTraceEnabled())
         {
            log.trace("getObject(): id: " + fqn + " retrieved from existing instance directly. ");
         }
         return pojo;
      }

      // OK. So we are here meaning that this is a failover or passivation since the transient
      // pojo instance is not around. Let's also make sure the right classloader is used
      // as well.
      ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
      try
      {
         Region region = cache.getRegion(fqn, false);
         if (region != null && region.getClassLoader() != null)
            Thread.currentThread().setContextClassLoader(region.getClassLoader());

         return getObjectInternal(fqn, field, source);
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(prevCL);
      }
   }

   /**
    * Note that caller of this method will take care of synchronization within the <code>fqn</code> sub-tree.
    */
   public Object putObject(Fqn fqn, Object obj, String field, Object source) throws CacheException
   {
      internal_.lockPojo(fqn);

      // Skip some un-necessary update if obj is the same class as the old one
      Object oldValue = internal_.getPojo(fqn, field);
      boolean allowArray = source instanceof ArrayInterceptable;
      if (oldValue == obj && skipDuplicateAttach(obj, allowArray))
      {
         if (log.isTraceEnabled())
         {
            log.trace("putObject(): id: " + fqn + " pojo is already in the cache. Return right away.");
         }
         return obj;
      }

      // remove old value before overwriting it. This is necessary to detach any interceptor.
      // TODO Or can we simply walk thru that somewhere? Well, there is also implication of Collection though
      pojoCache.detach(fqn, field, source);

      if (obj == null)
         return oldValue;// we are done

      AbstractHandler handler = getHandler(obj.getClass(), allowArray);
      Fqn<?> internalFqn = handler.getFqn(obj);

      Reference reference = new ReferenceImpl(fqn, field);
      if (internalFqn != null)
      {
         // Lock the internal fqn, before the ref count is checked
         internal_.lockPojo(internalFqn);
         graphHandler_.put(internalFqn, reference , obj);
      }
      else
      {
         internalFqn = createInternalFqn(fqn, obj);
         if (log.isTraceEnabled())
            log.trace("attach(): id: " + fqn + " will store the pojo in the internal area: " + internalFqn);

         handler.put(internalFqn, reference, obj);

         // Used by notification sub-system
         cache.put(internalFqn, InternalConstant.POJOCACHE_STATUS, "ATTACHED");
      }

      setPojoReference(fqn, obj, field, internalFqn);

      return oldValue;
   }

   private boolean skipDuplicateAttach(Object obj, boolean allowArray)
   {
      return obj == null || getHandler(obj.getClass(), allowArray) != serializableHandler_;
   }

   private AbstractHandler getHandler(Class<?> clazz, boolean allowArray)
   {
      if (advisedHandler_.handles(clazz))
         return advisedHandler_;

      if (collectionHandler_.handles(clazz))
         return collectionHandler_;

      if (allowArray && arrayHandler.handles(clazz))
         return arrayHandler;

      if (serializableHandler_.handles(clazz))
         return serializableHandler_;

      throw new CacheException("Can not manage object. It must be either instrumented, a collection, an array, or Serializable: "
            + clazz.getName());
   }


   private Fqn createInternalFqn(Fqn fqn, Object obj) throws CacheException
   {
      // Create an internal Fqn name
      return AopUtil.createInternalFqn(fqn, cache);
   }

   private Fqn setPojoReference(Fqn fqn, Object obj, String field, Fqn internalFqn) throws CacheException
   {
      // Create PojoReference
      CachedType type = pojoCache.getCachedType(obj.getClass());
      PojoReference pojoReference = new PojoReference();
      pojoReference.setPojoClass(type.getType());

      // store PojoReference
      pojoReference.setFqn(internalFqn);
      internal_.putPojoReference(fqn, pojoReference, field);
      if (log.isTraceEnabled())
      {
         log.trace("put(): inserting PojoReference with id: " + fqn);
      }
      // store obj in the internal fqn
      return internalFqn;
   }

   private void createChildNodeFirstWithoutLocking(Fqn internalFqn)
   {
      int size = internalFqn.size();
      Fqn f = internalFqn.getSubFqn(0, size - 1);
      Fqn child = internalFqn.getSubFqn(size - 1, size);

      Node base = cache.getRoot().getChild(f);
      if (base == null)
      {
         log.trace("The node retrieved is null from fqn: " + f);
         return;
      }
      base.addChild(child);
   }

   /**
    * Note that caller of this method will take care of synchronization within the <code>fqn</code> sub-tree.
    *
    * @param fqn
    * @return detached object
    * @throws CacheException
    */
   public Object removeObject(Fqn fqn, String field, Object source) throws CacheException
   {
      internal_.lockPojo(fqn);

      // the class attribute is implicitly stored as an immutable read-only attribute
      PojoReference pojoReference = internal_.getPojoReference(fqn, field);
      if (pojoReference == null)
      {
         //  clazz and pojoReference can be not null if this node is the replicated brother node.
         if (log.isTraceEnabled())
         {
            log.trace("removeObject(): clazz is null. id: " + fqn + " No need to remove.");
         }
         return null;
      }

      Fqn<?> internalFqn = pojoReference.getFqn();



      if (log.isTraceEnabled())
      {
         log.trace("removeObject(): removing object from id: " + fqn
                   + " with the corresponding internal id: " + internalFqn);
      }

      Object result = pojoCache.find(internalFqn);
      if (result == null)
         return null;

      // Lock the internal fqn, before the ref count is checked
      internal_.lockPojo(internalFqn);

      Reference reference = new ReferenceImpl(fqn, field);
      if (graphHandler_.isMultipleReferenced(internalFqn))
      {
         graphHandler_.remove(internalFqn, reference, result);
      }
      else
      {
         cache.put(internalFqn, InternalConstant.POJOCACHE_STATUS, "DETACHING");
         boolean allowArray = source instanceof ArrayInterceptable;
         result = getHandler(result.getClass(), allowArray).remove(internalFqn, reference, result);
      }

      internal_.cleanUp(fqn, field);
      return result;
   }

   public Map findObjects(Fqn fqn) throws CacheException
   {

      // Traverse from fqn to do getObject, if it return a pojo we then stop.
      Map map = new HashMap();
      Object pojo = getObject(fqn, null, null);
      if (pojo != null)
      {
         map.put(fqn, pojo);// we are done!
         return map;
      }

      findChildObjects(fqn, map);
      if (log.isTraceEnabled())
      {
         log.trace("_findObjects(): id: " + fqn + " size of pojos found: " + map.size());
      }
      return map;
   }

   private Object getObjectInternal(Fqn<?> fqn, String field, Object source) throws CacheException
   {
      Fqn<?> internalFqn = fqn;
      PojoReference pojoReference = internal_.getPojoReference(fqn, field);
      if (pojoReference != null)
      {
         internalFqn = pojoReference.getFqn();
      }
      else if (field != null)
      {
         return null;
      }

      if (log.isTraceEnabled())
         log.trace("getObject(): id: " + fqn + " with a corresponding internal id: " + internalFqn);

      /**
       * Reconstruct the managed POJO
       */
      Object obj;

      PojoInstance pojoInstance = internal_.getPojoInstance(internalFqn);

      if (pojoInstance == null)
         return null;
         //throw new PojoCacheException("PojoCacheDelegate.getObjectInternal(): null PojoInstance for fqn: " + internalFqn);

      Class<?> clazz = pojoInstance.getPojoClass();
      boolean allowArray = source instanceof ArrayInterceptable;
      obj = getHandler(clazz, allowArray).get(internalFqn, clazz, pojoInstance);

      InternalHelper.setPojo(pojoInstance, obj);
      return obj;
   }

   private void findChildObjects(Fqn fqn, Map map) throws CacheException
   {
      // We need to traverse then
      Node root = cache.getRoot();
      Node current = root.getChild(fqn);

      if (current == null) return;

      Collection<Node> col = current.getChildren();
      if (col == null) return;
      for (Node n : col)
      {
         Fqn newFqn = n.getFqn();
         if (InternalHelper.isInternalNode(newFqn)) continue;// skip

         Object pojo = getObject(newFqn, null, null);
         if (pojo != null)
         {
            map.put(newFqn, pojo);
         }
         else
         {
            findChildObjects(newFqn, map);
         }
      }
   }

   public boolean exists(Fqn<?> id)
   {
      return internal_.getPojoReference(id, null) != null || internal_.getPojoInstance(id) != null;
   }

   public Fqn<?> getInternalFqn(Object object)
   {
      AbstractHandler handler = getHandler(object.getClass(), true);
      Fqn<?> internalFqn = handler.getFqn(object);
      return internalFqn;
   }

   public Collection<Reference> getReferences(Object object)
   {
      Fqn<?> fqn = getInternalFqn(object);
      if (fqn == null)
         return Collections.emptyList();

      PojoInstance pojoInstance = internal_.getPojoInstance(fqn);
      return pojoInstance.getReferences();
   }
}
