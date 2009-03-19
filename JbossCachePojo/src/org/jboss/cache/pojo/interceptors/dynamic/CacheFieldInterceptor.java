/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.interceptors.dynamic;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.Advisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.array.ArrayRegistry;
import org.jboss.aop.joinpoint.FieldInvocation;
import org.jboss.aop.joinpoint.FieldReadInvocation;
import org.jboss.aop.joinpoint.FieldWriteInvocation;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheAlreadyDetachedException;
import org.jboss.cache.pojo.collection.CachedArray;
import org.jboss.cache.pojo.collection.CachedArrayRegistry;
import org.jboss.cache.pojo.impl.CachedType;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.impl.PojoInstance;
import org.jboss.cache.pojo.impl.PojoUtil;
import org.jboss.cache.pojo.util.ObjectUtil;

/**
 * Main dynamic interceptor to intercept for field replication.
 *
 * @author Ben Wang
 */

public class CacheFieldInterceptor implements BaseInterceptor
{
   private final Log log_ = LogFactory.getLog(CacheFieldInterceptor.class);
   Cache<Object, Object> cache_;
   private PojoCacheImpl pCache_;
   Fqn fqn_;
   private String name_;
   private PojoInstance pojoInstance_;
   private PojoUtil util_;

   public CacheFieldInterceptor(PojoCacheImpl pCache, Fqn fqn, CachedType type)
   {
      this.pCache_ = pCache;
      this.cache_ = pCache_.getCache();
      this.fqn_ = fqn;
      util_ = new PojoUtil();
   }

   private CacheFieldInterceptor()
   {
   }

   public PojoInstance getAopInstance()
   {
      return pojoInstance_;
   }

   public Object clone()
   {
      BaseInterceptor interceptor = new CacheFieldInterceptor();
      interceptor.setFqn(getFqn());
      interceptor.setAopInstance(getAopInstance());
      return interceptor;
   }

   public void setInterceptor(Interceptor intcptr)
   {
      BaseInterceptor interceptor = (BaseInterceptor) intcptr;
      setFqn(interceptor.getFqn());
      setAopInstance(interceptor.getAopInstance());
   }

   public void setAopInstance(PojoInstance pojoInstance)
   {
      this.pojoInstance_ = pojoInstance;
   }

   public String getName()
   {
      if (name_ == null)
      {
         this.name_ = "CacheFieldInterceptor on [" + fqn_ + "]";
      }
      return name_;
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      // Kind of ad hoc now. MethodInvocation should not invoke this.
      if (invocation instanceof MethodInvocation)
         return invocation.invokeNext();

      if (invocation instanceof FieldWriteInvocation)
      {
         Object target = invocation.getTargetObject();
         FieldInvocation fieldInvocation =
               (FieldInvocation) invocation;

         Advisor advisor = fieldInvocation.getAdvisor();
         Field field = fieldInvocation.getField();
         if (log_.isTraceEnabled())
         {
            log_.trace("invoke(): field write interception for fqn: " + fqn_ + " and field: " + field);
         }

         verifyAttached(target);

         // Only if this field is replicatable. static, transient and final are not.
         CachedType fieldType = pCache_.getCachedType(field.getType());
         Object value = ((FieldWriteInvocation) fieldInvocation).getValue();
         if (!CachedType.isNonReplicable(field))
         {
            if (CachedType.isSimpleAttribute(field))
            {
               cache_.put(fqn_, field.getName(), value);
            }
            else
            {
               pCache_.attach(fqn_, value, field.getName(), target);
            }

            util_.inMemorySubstitution(target, field, value);
         }
      }
      else if (invocation instanceof FieldReadInvocation)
      {
         Object target = invocation.getTargetObject();
         FieldInvocation fieldInvocation =
               (FieldInvocation) invocation;
         Field field = fieldInvocation.getField();
         Advisor advisor = fieldInvocation.getAdvisor();

         // Only if this field is replicatable
         CachedType fieldType = pCache_.getCachedType(field.getType());
         if (!CachedType.isNonReplicable(field))
         {
            Object result;
            if (CachedType.isSimpleAttribute(field))
            {
               result = cache_.get(fqn_, field.getName());
            }
            else
            {
               result = pCache_.find(fqn_, field.getName(), target);

               // Work around AOP issue with field reads
               if (result != null && result.getClass().isArray())
                  registerArrayWithAOP(target, field.getName(), result);
            }

            // If the result is null, the object might have been detached
            if (result == null)
               verifyAttached(target);

            // Update last known state associated with this pojo.
            util_.inMemorySubstitution(target, field, result);

            // Allow interceptor chain to process, but ignore the result
            invocation.invokeNext();

            return result;
         }
      }

      return invocation.invokeNext();
   }

   private void registerArrayWithAOP(Object owner, String fieldName, Object array)
   {
      CachedArray cached = CachedArrayRegistry.lookup(array);
      if (cached != null)
         ArrayRegistry.getInstance().addFieldReference(owner, fieldName, array);
   }

   /**
    * Check if the pojo is detached already.
    */
   private void verifyAttached(Object target)
   {
      if (cache_.get(fqn_, PojoInstance.KEY) != null)
         return;

      String identity = ObjectUtil.identityString(target);
      throw new PojoCacheAlreadyDetachedException(identity + " has possibly been detached remotely. Internal id: " + fqn_);
   }

   public Fqn getFqn()
   {
      return fqn_;
   }

   public void setFqn(Fqn fqn)
   {
      this.fqn_ = fqn;
   }

}