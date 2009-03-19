/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.Advised;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Region;
import org.jboss.cache.pojo.impl.InternalConstant;
import org.jboss.cache.pojo.interceptors.dynamic.BaseInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CacheFieldInterceptor;
import org.jboss.util.id.GUID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Unitlity methods for put, get and remove Collection classes object.
 *
 * @author Ben Wang
 */
public final class AopUtil
{
   static final Log log = LogFactory.getLog(AopUtil.class.getName());
   public static final String SEPARATOR = "/";

   /**
    * Find cache interceptor with exact fqn.
    *
    * @param advisor
    * @param fqn
    * @return Interceptor
    */
   static public Interceptor findCacheInterceptor(InstanceAdvisor advisor, Fqn fqn)
   {
      org.jboss.aop.advice.Interceptor[] interceptors = advisor.getInterceptors();
      // Step Check for cross references
      for (int i = 0; i < interceptors.length; i++)
      {
         Interceptor interceptor = interceptors[i];
         if (interceptor instanceof CacheFieldInterceptor)
         {
            CacheFieldInterceptor inter = (CacheFieldInterceptor) interceptor;
            if (inter != null && inter.getFqn().equals(fqn))
            {
               return interceptor;
            }
         }
      }
      return null;
   }

   /**
    * Find existing cache interceptor. Since there is supposedly only one cache interceptor per
    * pojo, this call should suffice. In addition, in cases of cross or circular reference,
    * fqn can be different anyway.
    *
    * @param advisor
    * @return Interceptor
    */
   static public CacheFieldInterceptor findCacheInterceptor(InstanceAdvisor advisor)
   {
      // TODO we assume there is only one interceptor now.
      Interceptor[] interceptors = advisor.getInterceptors();
      // Step Check for cross references
      for (int i = 0; i < interceptors.length; i++)
      {
         Interceptor interceptor = interceptors[i];
         if (interceptor instanceof CacheFieldInterceptor)
         {
            return (CacheFieldInterceptor)interceptor;
         }
      }
      return null;
   }

   /**
    * Find existing Collection interceptor. Since there is supposedly only one Collection interceptor per
    * instance, this call should suffice. In addition, in cases of cross or circular reference,
    * fqn can be different anyway.
    *
    * @param advisor
    * @return Interceptor
    */
   static public Interceptor findCollectionInterceptor(InstanceAdvisor advisor)
   {
      // TODO we assume there is only one interceptor now.
      Interceptor[] interceptors = advisor.getInterceptors();
      // Step Check for cross references
      for (int i = 0; i < interceptors.length; i++)
      {
         Interceptor interceptor = interceptors[i];
         if (interceptor instanceof BaseInterceptor)
         {
            return interceptor;
         }
      }
      return null;
   }

   /**
    * Check whether the object type is valid. An object type is valid if it is either: aspectized,
    * Serializable, or primitive type. Otherwise a runtime exception is thrown.
    *
    * @param obj
    */
   public static void checkObjectType(Object obj)
   {
      if (obj == null) return;
      if (!(obj instanceof Advised))
      {
         boolean allowedType = (obj instanceof Serializable) || (obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive());

         if (!allowedType)
         {
            throw new IllegalArgumentException("PojoCache.putObject(): Object type is neither " +
                                               " aspectized nor Serializable nor an array of primitives. Object class name is " + obj.getClass().getName());
         }
      }
   }


   public static Fqn constructFqn(Fqn baseFqn, Object relative)
   {
      // TODO Don't know why. But this will fail the CachedSetAopTest clear() method since look up is always
      // Null at the last index. But why?
      // TODO also see JBCACHE-282
      return new Fqn(baseFqn, relative.toString());

//      String tmp = baseFqn.toString() +"/" + relative.toString();
//      return Fqn.fromString(tmp);
//      Fqn fqn = new Fqn((String)relative);
//      return new Fqn(baseFqn, fqn);
   }

   /**
    * Internal fqn is now structured as:
    * a) If no region -- /__JBossInternal__/trueId/__ID__/xxxx
    * b) If there is region -- /region/__JBossInternal__/xxx
    */
   public static Fqn createInternalFqn(Fqn fqn, Cache cache)
   {
      // Extract the original id as it will be fqn - JBOSS_INTERNAL_ID_SEP
      // Also the current fqn can be like: /person/__JBossInternal__/test1/_ID_, for region specific ops
      Fqn trueId = null;
      if (fqn.hasElement(InternalConstant.JBOSS_INTERNAL_ID_SEP_STRING))
      {
         List list = new ArrayList();
         for (int i = 0; i < fqn.size(); i++)
         {
            if (fqn.get(i).equals(InternalConstant.JBOSS_INTERNAL_STRING))
            {
               continue;
            }

            if (fqn.get(i).equals(InternalConstant.JBOSS_INTERNAL_ID_SEP_STRING))
            {
               break;
            }
            list.add(fqn.get(i));
         }
         trueId = new Fqn(list);
      }
      else
      {
         trueId = fqn;
      }

      boolean createIfAbsent = false;
      Region region = cache.getRegion(trueId, createIfAbsent);
      // Use guid that is cluster-wide unique.
      GUID guid = new GUID();

      if (region == null || region.getFqn().equals(Fqn.ROOT))
      {
         // Move id under JBInternal to promote concurrency
         Fqn f0 = new Fqn(InternalConstant.JBOSS_INTERNAL, trueId);
         Fqn f = new Fqn(f0, InternalConstant.JBOSS_INTERNAL_ID_SEP);
         return new Fqn(f, Fqn.fromString(guid.toString()));
      }
      else
      {
         // Create it under region first.
         Fqn rf = region.getFqn();
         // Extract rest of fqn id
         Fqn childf = trueId.getSubFqn(rf.size(), trueId.size());
         Fqn f0 = new Fqn(InternalConstant.JBOSS_INTERNAL, childf);
         Fqn f = new Fqn(f0, InternalConstant.JBOSS_INTERNAL_ID_SEP);
         Fqn f1 = new Fqn(rf, f);
         return new Fqn(f1, Fqn.fromString(guid.toString()));
      }
   }

}
