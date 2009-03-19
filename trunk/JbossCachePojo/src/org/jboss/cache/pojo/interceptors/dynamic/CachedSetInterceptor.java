/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.interceptors.dynamic;

import org.jboss.aop.advice.Interceptor;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.collection.CachedSetImpl;
import org.jboss.cache.pojo.collection.CollectionInterceptorUtil;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Set interceptor that delegates underlying impl.
 *
 * @author Ben Wang
 */
@SuppressWarnings({"CanBeFinal"})
public class CachedSetInterceptor extends AbstractCollectionInterceptor
{
//   protected static final Log log_=LogFactory.getLog(CachedSetInterceptor.class);

   private Map methodMap_;
   private static final Map managedMethods_ =
         CollectionInterceptorUtil.getManagedMethods(Set.class);
   private Set cacheImpl_;
   private Set current_;
   private Set inMemImpl_;

   public CachedSetInterceptor(PojoCacheImpl cache, Fqn fqn, Class clazz, Set obj)
   {
      super(cache, fqn);
      methodMap_ = CollectionInterceptorUtil.getMethodMap(clazz);
      cacheImpl_ = new CachedSetImpl(cache, this);
      inMemImpl_ = obj;
      current_ = cacheImpl_;
   }

   private CachedSetInterceptor(PojoCacheImpl cache, Fqn fqn)
   {
      super(cache, fqn);
   }

   public Object clone()
   {
      CachedSetInterceptor interceptor = new CachedSetInterceptor(cache, fqn);
      interceptor.setFqn(getFqn());
      interceptor.setAopInstance(getAopInstance());
      interceptor.setCurrentCopy(getCurrentCopy());
      interceptor.setInMemoryCopy(getInMemoryCopy());
      interceptor.setCacheCopy(getCacheCopy());
      return interceptor;
   }

   public void setInterceptor(Interceptor intcptr)
   {
      CachedSetInterceptor interceptor = (CachedSetInterceptor) intcptr;
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
      inMemImpl_ = (Set) obj;
   }

   Object getInMemoryCopy()
   {
      return inMemImpl_;
   }

   void setCacheCopy(Object obj)
   {
      cacheImpl_ = (Set) obj;
   }

   Object getCacheCopy()
   {
      return cacheImpl_;
   }

   void setCurrentCopy(Object obj)
   {
      current_ = (Set) obj;
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
         throw new IllegalStateException("CachedSetInterceptor.toCache(). inMemImpl is null.");

      for (Iterator it = inMemImpl_.iterator(); it.hasNext();)
      {
         Object obj = it.next();
         it.remove();
         cacheImpl_.add(obj);
      }

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
         inMemImpl_ = new HashSet();
      }

      // TODO. This needs optimization.
      inMemImpl_.clear();
      for (Iterator it = cacheImpl_.iterator(); it.hasNext();)
      {
         Object obj = it.next();
         if (removeFromCache)
            it.remove();
         inMemImpl_.add(obj);
      }
   }

   public Object getSerializationCopy()
   {
      if (current_ == inMemImpl_)
         return inMemImpl_;

      Set set;
      Object mem = inMemImpl_;
      if (mem == null)
      {
         set = new HashSet();
      }
      else
      {
         set = (Set) copyOrConstruct(mem);
         if (set == null)
            throw new PojoCacheException("Could not serialize class, since it can not be copied: " + mem.getClass().getName());
      }

      set.clear();
      Iterator it = cacheImpl_.iterator();
      while (it.hasNext())
      {
         Object obj = it.next();
         set.add(obj);
      }
      return set;
   }

   public String getName()
   {
      return "CachedSetInterceptor";
   }

   public Object invoke(org.jboss.aop.joinpoint.Invocation invocation) throws Throwable
   {
      if (current_ == null)
         throw new IllegalStateException("CachedSetInterceptor.invoke(). current_ is null.");

      return CollectionInterceptorUtil.invoke(invocation,
                                              this,
                                              current_,
                                              methodMap_, managedMethods_);
   }
}
