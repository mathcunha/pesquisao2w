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

import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.impl.ArrayInterceptable;
import org.jboss.cache.pojo.impl.InternalConstant;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.util.AopUtil;
import org.jboss.cache.pojo.util.Null;

/**
 * A CachedObjectArray is used to back arrays with a component type that extends Object.
 * It currently maps each array element to a cache Node, to support fine-grained locking.
 *
 * @author Jason T. Greene
 */
public class CachedObjectArray extends CachedArray
{
   // Used to indicate that the source of the element is an interceptable array
   // so that multi-dimensional arrays can be handled properly
   private static ArrayInterceptable arraySource = new ArrayInterceptable() {};

   protected CachedObjectArray(Fqn<?> fqn, Class<?> type, PojoCacheImpl cache)
   {
      super(fqn, type, cache);
   }

   public void set(int index, Object element)
   {
      Fqn<?> fqn = AopUtil.constructFqn(this.fqn, IntegerCache.toString(index));

      cache.attach(fqn, Null.toNullObject(element), null, arraySource);
      cache.getCache().put(fqn, InternalConstant.POJOCACHE_OPERATION, "SET");
   }

   public Object get(int index)
   {
      Fqn<?> fqn = AopUtil.constructFqn(this.fqn, IntegerCache.toString(index));

      return Null.toNullValue(cache.find(fqn, null, arraySource));
   }

   public void destroy()
   {
      // Detach all children to ensure reference cleanup
      for (int i = 0; i < length(); i++)
         cache.detach(AopUtil.constructFqn(this.fqn, IntegerCache.toString(i)), null, arraySource);

      super.destroy();
   }
}
