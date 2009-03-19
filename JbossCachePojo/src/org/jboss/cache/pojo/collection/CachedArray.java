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
package org.jboss.cache.pojo.collection;

import java.lang.reflect.Array;

import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.impl.CachedType;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

/**
 * A CachedArray is the base class for cache backed array access. It replicates the Java array contract.
 *
 * @author Jason T. Greene
 */
public abstract class CachedArray
{
   private static final String LENGTH = "ARRAY.LENGTH";
   protected PojoCacheImpl cache;
   protected Fqn<?> fqn;
   private int length = -1;
   private Class<?> type;

   public static CachedArray load(Fqn<?> fqn, PojoCacheImpl cache, Class<?> type)
   {
      boolean primitive = CachedType.isImmediate(type.getComponentType());
      CachedArray array = primitive ? new CachedPrimitiveArray(fqn, type, cache) : new CachedObjectArray(fqn, type, cache);
      return array;
   }

   public static CachedArray create(Fqn<?> fqn, PojoCacheImpl cache, Object originalArray)
   {
      Class<?> type = originalArray.getClass();
      assert type.isArray();

      Class<?> component = type.getComponentType();
      boolean primitive = CachedType.isImmediate(component);
      CachedArray array = primitive ? new CachedPrimitiveArray(fqn, type, cache) : new CachedObjectArray(fqn, type, cache);

      int length = Array.getLength(originalArray);
      for (int c = 0; c < length; c++)
         array.set(c, Array.get(originalArray, c));

      array.length = length;
      array.writeInfo();

      return array;
   }

   protected CachedArray(Fqn<?> fqn, Class<?> type, PojoCacheImpl cache)
   {
      this.fqn = fqn;
      this.type = type;
      this.cache = cache;
   }

   public Fqn<?> getFqn()
   {
      return fqn;
   }

   public abstract void set(int index, Object element);

   public abstract Object get(int index);

   protected void writeInfo()
   {
      cache.getCache().put(fqn, LENGTH, length);
   }

   public void destroy()
   {
      cache.getCache().removeNode(fqn);
      length = -1;
   }

   public int length()
   {
      if (length == -1)
      {
         Integer i = (Integer)cache.getCache().get(fqn, LENGTH);
         length = i != null ? i.intValue() : 0;
      }

      return length;
   }

   public Object toArray()
   {
      try
      {
         int len = length();
         Object array = Array.newInstance(type.getComponentType(), len);
         for (int i = 0; i < len; i++)
            Array.set(array, i, get(i));

         return array;
      }
      catch (Exception e)
      {
         throw new CacheException("Could not construct array " + type);
      }
   }
}