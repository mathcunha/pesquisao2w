package org.jboss.cache.pojo.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.cache.CacheException;

public class Instantiator
{
   private static abstract class Allocator
   {
      public Object allocate(Class<?> c) throws Exception
      {
         Constructor<?> constructor = c.getDeclaredConstructor();
         PrivilegedCode.setAccessible(constructor);
         return constructor.newInstance();
      }
   }

   private static final Allocator allocator;

   static
   {
      // Currently only Sun and Mac JVMs are suported
      Allocator a = createUnsafeAllocator();
      allocator = a != null ? a : createDefaultAllocator();
   }


   public static Object newInstance(Class<?> c) throws CacheException
   {
      try
      {
         return allocator.allocate(c);
      }
      catch (Exception e)
      {
         throw new CacheException("failed creating instance of " + c.getName(), e);
      }
   }

   private static Allocator createUnsafeAllocator()
   {
      Allocator allocator = null;
      try
      {
         final Class<?> clazz = Class.forName("sun.misc.Unsafe");
         final Field field = clazz.getDeclaredField("theUnsafe");
         PrivilegedCode.setAccessible(field);
         final Object object = field.get(null);
         final Method method = clazz.getMethod("allocateInstance", Class.class);

         allocator = new Allocator()
         {
            public Object allocate(Class<?> c) throws Exception
            {
               try
               {
                  return super.allocate(c);
               }
               catch (Exception e)
               {
               }
               return method.invoke(object, c);
            }
         };
      }
      catch (Exception e)
      {
      }

      return allocator;
   }

   private static Allocator createDefaultAllocator()
   {
      return new Allocator()
      {

         public Object allocate(Class<?> c) throws Exception
         {
            try
            {
               return super.allocate(c);
            }
            catch (Exception e)
            {
               throw new IllegalArgumentException("A noarg constructor is required on this JVM", e);
            }
         }
      };
   }
}
