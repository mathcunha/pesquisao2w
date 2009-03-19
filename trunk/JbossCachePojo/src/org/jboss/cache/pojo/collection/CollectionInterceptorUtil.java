/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.collection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.AspectManager;
import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.AdviceBinding;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.aop.pointcut.ast.ParseException;
import org.jboss.aop.proxy.ClassProxy;
import org.jboss.aop.proxy.ClassProxyFactory;
import org.jboss.aop.util.MethodHashing;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CachedListInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CachedMapInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CachedSetInterceptor;
import org.jboss.cache.pojo.util.AopUtil;

/**
 * CollectionInterceptorUtil contains helper methods for the interceptors of
 * the different collection types.
 *
 * @author <a href="mailto:harald@gliebe.de">Harald Gliebe</a>
 * @author Ben Wang
 */
public class CollectionInterceptorUtil
{
   private static Log log = LogFactory.getLog(CollectionInterceptorUtil.class.getName());

   private static ClassProxy createProxy(Class clazz, AbstractCollectionInterceptor interceptor)
           throws Exception
   {
      ClassProxy result = ClassProxyFactory.newInstance(clazz, null, true);
      InstanceAdvisor advisor = result._getInstanceAdvisor();
      advisor.appendInterceptor(interceptor);
      interceptor.setBoundProxy(result);

      return result;
   }

   public static ClassProxy createMapProxy(PojoCacheImpl cache, Fqn fqn, Class clazz, Map obj) throws Exception
   {
      return CollectionInterceptorUtil.createProxy(clazz, new CachedMapInterceptor(cache, fqn, clazz, obj));
   }

   public static ClassProxy createListProxy(PojoCacheImpl cache, Fqn fqn, Class clazz, List obj) throws Exception
   {
      return CollectionInterceptorUtil.createProxy(clazz, new CachedListInterceptor(cache, fqn, clazz, obj));
   }

   public static ClassProxy createSetProxy(PojoCacheImpl cache, Fqn fqn, Class clazz, Set obj) throws Exception
   {
      return CollectionInterceptorUtil.createProxy(clazz, new CachedSetInterceptor(cache, fqn, clazz, obj));
   }

   public static AbstractCollectionInterceptor getInterceptor(ClassProxy proxy)
   {
      InstanceAdvisor advisor = proxy._getInstanceAdvisor();
      return (AbstractCollectionInterceptor) AopUtil.findCollectionInterceptor(advisor);
   }

   public static Map getMethodMap(Class clazz)
   {
      Map result = ClassProxyFactory.getMethodMap(clazz.getName());
      if (result == null)
      {
         try
         {
            ClassProxyFactory.newInstance(clazz);
         }
         catch (Exception e)
         {
            throw new PojoCacheException(e);
         }
         result = ClassProxyFactory.getMethodMap(clazz.getName());
      }
      return result;
   }

   public static Map getManagedMethods(Class clazz)
   {
      Method tostring = null;
      try
      {
         tostring = Object.class.getDeclaredMethod("toString", new Class[0]);
      }
      catch (NoSuchMethodException e)
      {
         throw new PojoCacheException(e);
      }

      Map managedMethods = new HashMap();
      try
      {
         Method[] methods = clazz.getDeclaredMethods();
         for (int i = 0; i < methods.length; i++)
         {
            long hash = MethodHashing.methodHash(methods[i]);
            managedMethods.put(hash, methods[i]);
         }
         // Add toString to ManagedMethod
         long hash = MethodHashing.methodHash(tostring);
         managedMethods.put(hash, tostring);
      }
      catch (Exception ignored)
      {
         log.trace(ignored, ignored);
      }
      return managedMethods;
   }

   private static boolean skipVerify(Method method)
   {
      String name = method.getName();
      Class<?>[] types = method.getParameterTypes();
      if ("toString".equals(name) && types.length == 0)
         return true;

      return false;
   }

   public static Object invoke(Invocation invocation,
                               AbstractCollectionInterceptor interceptor,
                               Object impl,
                               Map methodMap, Map managedMethods)
           throws Throwable
   {

      try
      {
         if (invocation instanceof MethodInvocation)
         {
            MethodInvocation methodInvocation = (MethodInvocation) invocation;
            Long methodHash = methodInvocation.getMethodHash();
            Method method = (Method) managedMethods.get(methodHash);
            if (log.isTraceEnabled() && method != null)
            {
               log.trace("invoke(): method intercepted " + method.getName());
            }

            if (writeReplaceInvocation(methodInvocation)) {
               if (!skipVerify(methodInvocation.getMethod()))
                  interceptor.verifyAttached(impl);

               return interceptor.getSerializationCopy();
            }

            Object[] args = methodInvocation.getArguments();
            if (method != null)
            {
               if (!skipVerify(method))
                  interceptor.verifyAttached(impl);

               return method.invoke(impl, args);
            } else
            {
               method = methodInvocation.getMethod();
               if (method == null)
               {
                  method = (Method) methodMap.get(methodHash);
               }

               if (log.isTraceEnabled())
                  log.trace("invoke(): non-managed method: " + method.toString());
               Object target = methodInvocation.getTargetObject();
               if (target == null)
               {
                  throw new PojoCacheException("CollectionInterceptorUtil.invoke(): targetObject is null." +
                          " Can't invoke " + method.toString());
               }
               return method.invoke(target, args);
               //            return method.invoke(interceptor, args);
            }
         }
      }
      catch (InvocationTargetException e)
      {
         if (e.getCause() != null)
            throw e.getCause();
         else if (e.getTargetException() != null)
            throw e.getTargetException();
         throw e;
      }

      return invocation.invokeNext();
   }

   private static boolean writeReplaceInvocation(MethodInvocation methodInvocation)
   {
      Method method = methodInvocation.getMethod();
      return "writeReplace".equals(method.getName()) && method.getParameterTypes().length == 0;
   }

}
