/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.cache.pojo.impl;

import java.lang.reflect.Field;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.Advised;
import org.jboss.aop.ClassInstanceAdvisor;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.interceptors.dynamic.BaseInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CacheFieldInterceptor;
import org.jboss.cache.pojo.memory.FieldPersistentReference;
import org.jboss.cache.pojo.util.AopUtil;
import org.jboss.cache.pojo.util.Instantiator;
import org.jboss.cache.pojo.util.ObjectUtil;

/**
 * Handling the advised pojo operations. No consideration of object graph here.
 *
 * @author Ben Wang
 *         Date: Aug 4, 2005
 * @version $Id: AdvisedPojoHandler.java 7052 2008-11-02 18:13:25Z bstansberry@jboss.com $
 */
class AdvisedPojoHandler extends AbstractHandler
{
   private final Log log = LogFactory.getLog(AdvisedPojoHandler.class);
   private Cache<Object, Object> cache_;
   private PojoCacheImpl pCache_;
   private PojoUtil util_;

   public AdvisedPojoHandler(PojoCacheImpl pCache, InternalHelper internal,
                             PojoUtil util)
   {
      pCache_ = pCache;
      cache_ = pCache_.getCache();
      util_ = util;
   }

   @Override
   protected Fqn<?> getFqn(Object obj)
   {
      if (obj instanceof Advised)
      {
         InstanceAdvisor advisor = ((Advised) obj)._getInstanceAdvisor();
         if (advisor == null)
            throw new PojoCacheException("_putObject(): InstanceAdvisor is null for: " + obj);

         // Step Check for cross references
         BaseInterceptor interceptor = AopUtil.findCacheInterceptor(advisor);
         if (interceptor != null)
            return interceptor.getFqn();
      }

      return null;
   }

   @Override
   protected Object get(Fqn<?> fqn, Class<?> clazz, PojoInstance pojoInstance) throws CacheException
   {
      CachedType type = pCache_.getCachedType(clazz);
      Object obj = Instantiator.newInstance(clazz);

      // Eager initialize final fields, since these may not be intercepted

      try
      {
         for (FieldPersistentReference ref : type.getFinalFields())
         {
            Field field = ref.getField();
            Object result;

            if (CachedType.isSimpleAttribute(field))
               result = cache_.get(fqn, field.getName());
            else
               result = pCache_.find(fqn, field.getName(), obj);

            field.set(obj, result);
         }
      }
      catch (Exception e)
      {
         log.warn("Could not initialize final fields on object: " + ObjectUtil.identityString(obj));
      }

      InstanceAdvisor advisor = ((Advised) obj)._getInstanceAdvisor();
      CacheFieldInterceptor interceptor = new CacheFieldInterceptor(pCache_, fqn, type);
      interceptor.setAopInstance(pojoInstance);
      util_.attachInterceptor(obj, advisor, interceptor);
      return obj;
   }

   @Override
   protected void put(Fqn<?> fqn, Reference reference, Object obj) throws CacheException
   {
      CachedType type = pCache_.getCachedType(obj.getClass());
      // We have a clean slate then.
      InstanceAdvisor advisor = ((Advised) obj)._getInstanceAdvisor();
      // TODO workaround for deserialiased objects
      if (advisor == null)
      {
         advisor = new ClassInstanceAdvisor(obj);
         ((Advised) obj)._setInstanceAdvisor(advisor);
      }

      // Let's do batch update via Map instead
      Map map = new HashMap();
      // Always initialize the ref count so we can mark this as an AopNode.
      PojoInstance pojoInstance = InternalHelper.initializeAopInstance(reference);
      map.put(PojoInstance.KEY, pojoInstance);
      pojoInstance.setPojoClass(type.getType());
      // we will do it recursively.
      // Map of sub-objects that are non-primitive
      Map subPojoMap = new HashMap();

      for (Iterator i = type.getFields().iterator(); i.hasNext();)
      {
         Field field = (Field) (((FieldPersistentReference) i.next())).get();
         Object value = null;
         try
         {
            value = field.get(obj);
         }
         catch (IllegalAccessException e)
         {
            throw new CacheException("field access failed", e);
         }

         // we simply treat field that has @Serializable as a primitive type.
         if (CachedType.isSimpleAttribute(field))
         {
            // switched using batch update
            map.put(field.getName(), value);
         }
         else
         {
            subPojoMap.put(field, value);
         }
      }

      // Use option to skip locking since we have parent lock already.
//      cache_.getInvocationContext().getOptionOverrides().setSuppressLocking(true);

      cache_.getRoot().addChild(fqn).putAll(map);

      // Insert interceptor after PojoInstance has been written to the cache
      // This prevents JBCACHE-1078 with pessimistic locking, optimistic is still a problem
      CacheFieldInterceptor interceptor = new CacheFieldInterceptor(pCache_, fqn, type);
      interceptor.setAopInstance(pojoInstance);
      util_.attachInterceptor(obj, advisor, interceptor);

//      cache_.getInvocationContext().getOptionOverrides().setSuppressLocking(false);
      // This is in-memory operation only
      InternalHelper.setPojo(pojoInstance, obj);

      for (Object o : subPojoMap.keySet())
      {
         Field field = (Field) o;
         Object value = subPojoMap.get(field);
         if (value == null) continue; // really no need to map the POJO.
         pCache_.attach(fqn, value, field.getName(), obj);
         // If it is Collection classes, we replace it with dynamic proxy.
         // But we will have to ignore it if value is null
         if (value instanceof Map || value instanceof List || value instanceof Set)
         {
            Object newValue = pCache_.find(fqn, field.getName(), obj);
            util_.inMemorySubstitution(obj, field, newValue);
         }
      }

      // Need to make sure this is behind put such that obj.toString is done correctly.
      if (log.isTraceEnabled())
      {
         log.trace("internalPut(): inserting with fqn: " + fqn);
      }
   }

   @Override
   protected Object remove(Fqn<?> fqn, Reference referencingFqn, Object result) throws CacheException
   {
      CachedType type = pCache_.getCachedType(result.getClass());
      InstanceAdvisor advisor = ((Advised) result)._getInstanceAdvisor();
      for (Iterator i = type.getFields().iterator(); i.hasNext();)
      {
         Field field = (Field) (((FieldPersistentReference) i.next())).get();
         Object value = null;

         if (! CachedType.isSimpleAttribute(field))
         {
            value = pCache_.detach(fqn, field.getName(), result);

            // Check for Collection. If it is, we need to reset the original reference.
            if ((value instanceof Map || value instanceof List || value instanceof Set))
            {
               // If this Collection class, we are returning the original value already
               util_.inMemorySubstitution(result, field, value);
            }
         }
         else
         {
            // Update last known field state
            value = cache_.get(fqn,  field.getName());
            util_.inMemorySubstitution(result, field, value);
         }
      }

      cache_.removeNode(fqn);
      // Determine if we want to keep the interceptor for later use.
      CacheFieldInterceptor interceptor = (CacheFieldInterceptor) AopUtil.findCacheInterceptor(advisor);
      // Remember to remove the interceptor from in-memory object but make sure it belongs to me first.
      if (interceptor != null)
      {
         if (log.isTraceEnabled())
         {
            log.trace("regularRemoveObject(): removed cache interceptor fqn: " + fqn + " interceptor: " + interceptor);
         }
         util_.detachInterceptor(advisor, interceptor);
      }

      return result;
   }

   @Override
   protected boolean handles(Class<?> clazz)
   {
      return Advised.class.isAssignableFrom(clazz);
   }
}
