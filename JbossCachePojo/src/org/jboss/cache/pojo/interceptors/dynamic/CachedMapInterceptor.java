/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.interceptors.dynamic;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.proxy.ClassProxy;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.collection.CachedMapImpl;
import org.jboss.cache.pojo.collection.CollectionInterceptorUtil;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Map interceptor that delegates to the underlying impl.
 *
 * @author Ben Wang
 */
@SuppressWarnings({"CanBeFinal"})
public class CachedMapInterceptor extends AbstractCollectionInterceptor
{

   //   protected static final Log log_ = LogFactory.getLog(CachedMapInterceptor.class);
   private static final Map managedMethods_ =
         CollectionInterceptorUtil.getManagedMethods(Map.class);
   private Map methodMap_;
   private Map cacheImpl_;
   private Map inMemImpl_;
   private Map current_;

   public CachedMapInterceptor(PojoCacheImpl cache, Fqn fqn, Class clazz, Map obj)
   {
      super(cache, fqn);
      methodMap_ = CollectionInterceptorUtil.getMethodMap(clazz);
      cacheImpl_ = new CachedMapImpl(cache, this);
      inMemImpl_ = obj;
      current_ = cacheImpl_;
   }

   private CachedMapInterceptor(PojoCacheImpl cache, Fqn fqn)
   {
      super(cache, fqn);
   }

   public Object clone()
   {
      CachedMapInterceptor interceptor = new CachedMapInterceptor(cache, fqn);
      interceptor.setFqn(getFqn());
      interceptor.setAopInstance(getAopInstance());
      interceptor.setCurrentCopy(getCurrentCopy());
      interceptor.setInMemoryCopy(getInMemoryCopy());
      interceptor.setCacheCopy(getCacheCopy());
      return interceptor;
   }

   public void setInterceptor(Interceptor intcptr)
   {
      CachedMapInterceptor interceptor = (CachedMapInterceptor) intcptr;
      setFqn(interceptor.getFqn());
      setAopInstance(interceptor.getAopInstance());
      setCurrentCopy(interceptor.getCurrentCopy());
      setInMemoryCopy(interceptor.getInMemoryCopy());
      setCacheCopy(interceptor.getCacheCopy());
   }

   public Object getCurrentCopy()
   {
      return current_;
   }

   void setInMemoryCopy(Object obj)
   {
      inMemImpl_ = (Map) obj;
   }

   Object getInMemoryCopy()
   {
      return inMemImpl_;
   }

   void setCacheCopy(Object obj)
   {
      cacheImpl_ = (Map) obj;
   }

   Object getCacheCopy()
   {
      return cacheImpl_;
   }

   void setCurrentCopy(Object obj)
   {
      current_ = (Map) obj;
   }

   /**
    * When we want to associate this proxy with the cache again. We will need to translate the in-memory
    * content to the cache store first.
    */
   public void attach(Fqn fqn, boolean copyToCache)
   {
      super.attach(fqn, copyToCache);

      if (copyToCache)
         toCache();

      current_ = cacheImpl_;
   }

   private void toCache()
   {
      if (inMemImpl_ == null)
         throw new IllegalStateException("CachedMapInterceptor.toCache(). inMemImpl is null.");

      for (Object key : inMemImpl_.keySet())
      {
         Object val = inMemImpl_.get(key);
         cacheImpl_.put(key, val);
      }

      inMemImpl_.clear();
      inMemImpl_ = null;   // we are done with this.
   }

   /**
    * When we want to separate this proxy from the cache. We will destroy the cache content and copy them to
    * the in-memory copy.
    */
   public void detach(boolean removeFromCache)
   {
      super.detach(removeFromCache);

      toMemory(removeFromCache);

      current_ = inMemImpl_;
   }

   private void toMemory(boolean removeFromCache)
   {
      if (inMemImpl_ == null)
      {
         inMemImpl_ = new HashMap();
      }

      Iterator it = cacheImpl_.keySet().iterator();
      inMemImpl_.clear();
      while (it.hasNext())
      {
         Object key = it.next();
         Object val = null;
         if (removeFromCache)
         {
            val = cacheImpl_.remove(key);
         }
         else
         {
            val = cacheImpl_.get(key);
         }
         inMemImpl_.put(key, val);
      }
   }

   public Object getSerializationCopy()
   {
      if (current_ == inMemImpl_)
         return inMemImpl_;

      Map map;
      Object mem = inMemImpl_;
      if (mem == null)
      {
         map = new HashMap();
      }
      else
      {
         map = (Map) copyOrConstruct(mem);
         if (map == null)
            throw new PojoCacheException("Could not serialize class, since it can not be copied: " + mem.getClass().getName());
      }

      map.clear();
      Iterator it = cacheImpl_.keySet().iterator();
      while (it.hasNext())
      {
         Object key = it.next();
         Object val = cacheImpl_.get(key);
         map.put(key, val);
      }
      return map;
   }

   public String getName()
   {
      return "CachedMapInterceptor";
   }

   public Object invoke(org.jboss.aop.joinpoint.Invocation invocation) throws Throwable
   {
      if (current_ == null)
         throw new IllegalStateException("CachedMapInterceptor.invoke(). current_ is null.");

      return CollectionInterceptorUtil.invoke(invocation,
                                              this,
                                              current_,
                                              methodMap_, managedMethods_);
   }

}
