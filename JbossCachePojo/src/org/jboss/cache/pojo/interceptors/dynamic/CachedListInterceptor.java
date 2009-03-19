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
import org.jboss.cache.pojo.collection.CachedListImpl;
import org.jboss.cache.pojo.collection.CollectionInterceptorUtil;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * List ineterceptor that delegates to underlying implementation.
 *
 * @author Ben Wang
 */

@SuppressWarnings({"CanBeFinal"})
public class CachedListInterceptor extends AbstractCollectionInterceptor
{

   //   protected static final Log log_ = LogFactory.getLog(CachedListInterceptor.class);
   private static final Map managedMethods_ =
         CollectionInterceptorUtil.getManagedMethods(List.class);

   private Map methodMap_;
   // This is the copy in cache store when it is attached.
   private List cacheImpl_;
   // This is the copy in-memory when the state is detached.
   private List inMemImpl_;
   // Whichever is used now.
   private List current_;

   public CachedListInterceptor(PojoCacheImpl cache, Fqn fqn, Class clazz, List obj)
   {
      super(cache, fqn);
      methodMap_ = CollectionInterceptorUtil.getMethodMap(clazz);
      cacheImpl_ = new CachedListImpl(cache, this);
      inMemImpl_ = obj;   // lazy initialization here.
      current_ = cacheImpl_;
   }

   private CachedListInterceptor(PojoCacheImpl cache, Fqn fqn)
   {
      super(cache, fqn);
   }

   public Object clone()
   {
      CachedListInterceptor interceptor = new CachedListInterceptor(cache, fqn);
      interceptor.setFqn(getFqn());
      interceptor.setAopInstance(getAopInstance());
      interceptor.setCurrentCopy(getCurrentCopy());
      interceptor.setInMemoryCopy(getInMemoryCopy());
      interceptor.setCacheCopy(getCacheCopy());
      return interceptor;
   }

   public void setInterceptor(Interceptor intcptr)
   {
      CachedListInterceptor interceptor = (CachedListInterceptor) intcptr;
      setFqn(interceptor.getFqn());
      setAopInstance(interceptor.getAopInstance());
      setCurrentCopy(interceptor.getCurrentCopy());
      setInMemoryCopy(interceptor.getInMemoryCopy());
      setCacheCopy(interceptor.getCacheCopy());
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
         throw new IllegalStateException("CachedListInterceptor.toCache(). inMemImpl is null.");

      // TODO This may not be optimal
      List tmpList = new ArrayList();
      for (int i = inMemImpl_.size(); i > 0; i--)
      {
         Object obj = inMemImpl_.remove(i - 1);
         tmpList.add(obj);
      }

      int size = tmpList.size();
      for (int i = 0; i < tmpList.size(); i++)
      {
         cacheImpl_.add(tmpList.get(size - i - 1));
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

   public Object getCurrentCopy()
   {
      return current_;
   }

   void setInMemoryCopy(Object obj)
   {
      inMemImpl_ = (List) obj;
   }

   Object getInMemoryCopy()
   {
      return inMemImpl_;
   }

   Object getCacheCopy()
   {
      return cacheImpl_;
   }

   void setCacheCopy(Object obj)
   {
      cacheImpl_ = (List) obj;
   }

   void setCurrentCopy(Object obj)
   {
      current_ = (List) obj;
   }

   private void toMemory(boolean removeFromCache)
   {
      if (inMemImpl_ == null)
      {
         inMemImpl_ = new ArrayList();
      }

      // Optimization since remove from the beginning is very expensive.
      List tmpList = new ArrayList();
      for (int i = cacheImpl_.size(); i > 0; i--)
      {
         int j = i - 1;
         Object obj = null;
         if (removeFromCache)
         {
            obj = cacheImpl_.remove(j);
         }
         else
         {
            obj = cacheImpl_.get(j);
         }

         tmpList.add(obj);
      }

      int size = tmpList.size();
      inMemImpl_.clear();
      for (int i = 0; i < tmpList.size(); i++)
      {
         inMemImpl_.add(tmpList.get(size - i - 1));
      }
   }

   public Object getSerializationCopy()
   {
      if (current_ == inMemImpl_)
         return inMemImpl_;

      List list;
      Object mem = inMemImpl_;
      if (mem == null)
      {
         list = new ArrayList();
      }
      else
      {
         list = (List) copyOrConstruct(mem);
         if (list == null)
            throw new PojoCacheException("Could not serialize class, since it can not be copied: " + mem.getClass().getName());
      }

      list.clear();
      int size = cacheImpl_.size();
      for (int i = 0; i < size; i++)
         list.add(cacheImpl_.get(i));

      return list;
   }

   public String getName()
   {
      return "CachedListInterceptor";
   }

   public Object invoke(org.jboss.aop.joinpoint.Invocation invocation) throws Throwable
   {
      if (current_ == null)
         throw new IllegalStateException("CachedListInterceptor.invoke(). current_ is null.");

      return CollectionInterceptorUtil.invoke(invocation,
                                              this,
                                              current_,
                                              methodMap_, managedMethods_);
   }
}
