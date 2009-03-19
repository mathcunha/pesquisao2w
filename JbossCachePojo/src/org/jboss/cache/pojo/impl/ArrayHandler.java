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

import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.collection.CachedArray;
import org.jboss.cache.pojo.collection.CachedArrayRegistry;

/**
 * Handles array types.
 *
 * @author Jason T. Greene
 */
public class ArrayHandler extends AbstractHandler
{
   private final PojoCacheImpl cache;
   private final PojoUtil util = new PojoUtil();

   ArrayHandler(PojoCacheImpl cache)
   {
      this.cache = cache;
   }

   protected Fqn<?> getFqn(Object array)
   {
      CachedArray cached = CachedArrayRegistry.lookup(array);
      return cached != null ? cached.getFqn() : null;
   }

   @Override
   protected void put(Fqn<?> fqn, Reference reference, Object obj)
   {
      // Always initialize the ref count so that we can mark this as an AopNode.
      PojoInstance pojoInstance = InternalHelper.initializeAopInstance(reference);
      pojoInstance.set(obj);
      pojoInstance.setPojoClass(obj.getClass());
      cache.getCache().put(fqn, PojoInstance.KEY, pojoInstance);

      CachedArray cached = CachedArray.create(fqn, cache, obj);
      util.attachArray(obj, cached);
   }

   @Override
   protected Object get(Fqn<?> fqn, Class<?> clazz, PojoInstance pojo)
   {
      CachedArray cached = CachedArray.load(fqn, cache, clazz);
      Object array = cached.toArray();
      CachedArrayRegistry.register(array, cached);

      return array;
   }

   @Override
   protected Object remove(Fqn<?> fqn, Reference referencingFqn, Object obj)
   {
      CachedArray cached = CachedArrayRegistry.lookup(obj);
      if (cached != null) {
         util.detachArray(obj, cached);
         cached.destroy();
      }

      return obj;
   }

   @Override
   protected boolean handles(Class<?> clazz)
   {
      return clazz.isArray();
   }

}
