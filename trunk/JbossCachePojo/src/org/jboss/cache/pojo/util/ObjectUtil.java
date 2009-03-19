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
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.impl.CachedType;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.memory.FieldPersistentReference;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Unitlity methods for pojo object.
 *
 * @author Ben Wang
 */
public final class ObjectUtil
{
   private static final Log log = LogFactory.getLog(ObjectUtil.class.getName());

   /**
    * Static methos to check if <code>thisObject</code> is reachable from <code>originalObject</code>.
    *
    * @param cache
    * @param originalObject
    * @param thisObject
    * @return true if reachable
    * @throws CacheException
    */
   public static boolean isReachable(PojoCacheImpl cache, Object originalObject, Object thisObject)
           throws CacheException
   {
      HashSet objSet = new HashSet();

      return isReachableInner(cache, originalObject, thisObject, objSet);
   }

   private static boolean isReachableInner(PojoCacheImpl cache, Object originalObject,
                                           Object thisObject, Set objSet)
           throws CacheException
   {
      // Currently we don't support recursive Collection
      if (!(originalObject instanceof Advised))
         throw new PojoCacheException("ObjectUtil.isReachable(): originalObject is not Advised.");

      if (log.isTraceEnabled())
      {
         log.trace("isReachable(): current object: " + originalObject + " this object: " + thisObject);
      }

      if (originalObject.equals(thisObject))
      {
         if (log.isTraceEnabled())
         {
            log.trace("isReachable(): object found reachable.");
         }

         return true;
      }

      if (!objSet.contains(originalObject))
      {
         objSet.add(originalObject);
      } else
      {  // We have been here before so let's return.
         return false;
      }

      CachedType type = cache.getCachedType(originalObject.getClass());
      for (Iterator i = type.getFields().iterator(); i.hasNext();)
      {
         Field field = (Field) (((FieldPersistentReference) i.next())).get();
         Object value = null;
         try
         {
            value = field.get(originalObject); // Reflection may not work here.
         }
         catch (IllegalAccessException e)
         {
            throw new CacheException("field access failed", e);
         }
         CachedType fieldType = cache.getCachedType(field.getType());
         if (fieldType.isImmediate())
         {
         } else
         {
            if (value instanceof Map)
            {
               Set set = ((Map) value).keySet();
               for (Iterator it = set.iterator(); it.hasNext();)
               {
                  if (isReachableInner(cache, it.next(), thisObject, objSet))
                     return true;
               }

               continue;
            } else if (value instanceof Collection)
            {
               for (Iterator it = ((Collection) value).iterator(); it.hasNext();)
               {
                  if (isReachableInner(cache, it.next(), thisObject, objSet))
                     return true;
               }

               continue;
            }

            if (!(value instanceof Advised))
               continue;   // TODO We don't care about Collection now.

            if (isReachableInner(cache, value, thisObject, objSet))
               return true;
         }
      }

      return false;
   }

   public static String getIndirectFqn(Fqn fqn)
   {
      // TODO Need to generate a unique id here
      // Let's strip off the line separator and use underscoe instead.
      return getIndirectFqn(fqn.toString());
   }

   public static String getIndirectFqn(String fqn)
   {
      // TODO Need to generate a unique id here
      // Let's strip off the line separator and use underscoe instead.
      return fqn.replace('/', '_');
   }

   public static String identityString(Object object)
   {
      return object.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(object));
   }
}
