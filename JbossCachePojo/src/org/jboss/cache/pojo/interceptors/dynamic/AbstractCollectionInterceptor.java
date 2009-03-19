/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.interceptors.dynamic;

import org.jboss.aop.proxy.ClassProxy;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheAlreadyDetachedException;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.impl.PojoInstance;
import org.jboss.cache.pojo.util.ObjectUtil;

/**
 * Abstract base class for collection interceptor.
 *
 * @author Ben Wang
 * @version $Id: AbstractCollectionInterceptor.java 6048 2008-06-26 02:21:26Z jason.greene@jboss.com $
 */
@SuppressWarnings({"CanBeFinal"})
public abstract class AbstractCollectionInterceptor implements BaseInterceptor
{
   Fqn fqn;
   PojoCacheImpl cache;
   ClassProxy boundProxy;

   private boolean attached_ = true;
   private PojoInstance pojoInstance_;

   AbstractCollectionInterceptor(PojoCacheImpl cache, Fqn fqn)
   {
      this.fqn = fqn;
      this.cache = cache;
   }

   @SuppressWarnings({"CanBeFinal"})
   public Fqn getFqn()
   {
      return fqn;
   }

   @SuppressWarnings({"CanBeFinal"})
   public void setFqn(Fqn fqn)
   {
      this.fqn = fqn;
   }

   @SuppressWarnings({"CanBeFinal"})
   public PojoInstance getAopInstance()
   {
      return pojoInstance_;
   }

   public void setAopInstance(PojoInstance pojoInstance)
   {
      this.pojoInstance_ = pojoInstance;
   }

   /**
    * Attaching the Collection to PojoCache.
    */
   public void attach(Fqn fqn, boolean copyToCache)
   {
      // This is a hook to allow re-attching the Collection without specifying the fqn.
      if (fqn != null)
      {
         setFqn(fqn);
      }
      attached_ = true;
      // Reattach anything in-memory to cache
   }

   public void detach(boolean removeFromCache)
   {
      attached_ = false;
      // Detach by tranferring the cache content to in-memory copy
   }

   public boolean isAttached()
   {
      return attached_;
   }

   // Verify an attached collection is truly attached
   public void verifyAttached(Object target)
   {
      // If locally detached, we use the local in-memory copy
      if (! isAttached())
         return;

      if (cache.getCache().get(fqn, PojoInstance.KEY) != null)
         return;

      String identity = ObjectUtil.identityString(target);
      throw new PojoCacheAlreadyDetachedException(identity + " has possibly been detached remotely. Internal id: " + fqn);
   }

   Object copyOrConstruct(Object mem)
   {
      if (mem instanceof Cloneable) {
         try
         {
            return mem.getClass().getMethod("clone").invoke(mem);
         }
         catch (Exception e)
         {
         }
      }

      try
      {
         return mem.getClass().newInstance();
      }
      catch (Exception e)
      {
      }

      return null;
   }

   abstract void setInMemoryCopy(Object obj);
   abstract Object getInMemoryCopy();
   public abstract Object getSerializationCopy();
   abstract void setCacheCopy(Object obj);
   abstract Object getCacheCopy();
   abstract void setCurrentCopy(Object obj);
   public abstract Object getCurrentCopy();

   public ClassProxy getBoundProxy()
   {
      return boundProxy;
   }

   public void setBoundProxy(ClassProxy boundProxy)
   {
      this.boundProxy = boundProxy;
   }
}
