package org.jboss.cache.pojo.collection;

/**
 * Cache of integers in String format from 0-99.
 */
public class IntegerCache
{

   private IntegerCache()
   {
   }

   private static final String values[] = new String[100];

   static
   {
      for (int i = 0; i < values.length; i++)
         values[i] = Integer.toString(i).intern();
   }

   public static String toString(int i)
   {
      if (i >= 0 && i < values.length)
         return values[i];
      return Integer.toString(i);
   }

}
