/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.collection.CachedArray;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;

/**
 * Method declarations for rollback method mostly.
 *
 * @author Ben Wang
 * @version $Revision: 6374 $
 */
public class MethodDeclarations
{
   public static final Method attachInterceptor;
   public static final Method detachInterceptor;
   public static final Method detachCollectionInterceptor;
   public static final Method undoAttachInterceptor;
   public static final Method undoDetachInterceptor;
   public static final Method undoDetachCollectionInterceptor;
   ;
   public static final Method inMemorySubstitution;
   ;
   public static final Method undoInMemorySubstitution;
   ;
   public static final Method incrementReferenceCount;
   public static final Method decrementReferenceCount;
   public static final Method undoIncrementReferenceCount;
   public static final Method undoDecrementReferenceCount;
   public static final Method attachArray;
   public static final Method detachArray;
   public static final Method undoAttachArray;
   public static final Method undoDetachArray;

   static
   {
      try
      {
         attachInterceptor = PojoUtil.class.getDeclaredMethod("attachInterceptor",
                                                              new Class[]{Object.class, InstanceAdvisor.class, Interceptor.class});
         detachInterceptor = PojoUtil.class.getDeclaredMethod("detachInterceptor",
                                                              new Class[]{InstanceAdvisor.class, Interceptor.class});
         detachCollectionInterceptor = PojoUtil.class.getDeclaredMethod("detachCollectionInterceptor",
               new Class[]{AbstractCollectionInterceptor.class});

         undoAttachInterceptor = PojoUtil.class.getDeclaredMethod("undoAttachInterceptor",
                                                                  new Class[]{Object.class, InstanceAdvisor.class, Interceptor.class});
         undoDetachInterceptor = PojoUtil.class.getDeclaredMethod("undoDetachInterceptor",
                                                                  new Class[]{InstanceAdvisor.class, Interceptor.class});
         undoDetachCollectionInterceptor = PojoUtil.class.getDeclaredMethod("undoDetachCollectionInterceptor",
               new Class[]{AbstractCollectionInterceptor.class});
         inMemorySubstitution = PojoUtil.class.getDeclaredMethod("inMemorySubstitution",
                                                                 new Class[]{Object.class, Field.class, Object.class});
         undoInMemorySubstitution = PojoUtil.class.getDeclaredMethod("undoInMemorySubstitution",
                                                                     new Class[]{Object.class, Field.class, Object.class});
         incrementReferenceCount = PojoUtil.class.getDeclaredMethod("incrementReferenceCount",
                                                                    new Class[]{Reference.class, int.class, Set.class});
         decrementReferenceCount = PojoUtil.class.getDeclaredMethod("decrementReferenceCount",
                                                                    new Class[]{Reference.class, int.class, Set.class});
         undoIncrementReferenceCount = PojoUtil.class.getDeclaredMethod("undoIncrementReferenceCount",
                                                                        new Class[]{Reference.class, int.class, Set.class});
         undoDecrementReferenceCount = PojoUtil.class.getDeclaredMethod("undoDecrementReferenceCount",
                                                                        new Class[]{Reference.class, int.class, Set.class});

         attachArray = PojoUtil.class.getDeclaredMethod("attachArray", new Class[]{Object.class, CachedArray.class});
         detachArray = PojoUtil.class.getDeclaredMethod("detachArray", new Class[]{Object.class, CachedArray.class});
         undoAttachArray = PojoUtil.class.getDeclaredMethod("undoAttachArray", new Class[]{Object.class, CachedArray.class});
         undoDetachArray = PojoUtil.class.getDeclaredMethod("undoDetachArray", new Class[]{Object.class, CachedArray.class});
      }
      catch (NoSuchMethodException e)
      {
         throw new ExceptionInInitializerError(e);
      }
   }
}
