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
package org.jboss.cache.pojo.interceptors.dynamic;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.array.ArrayElementReadInvocation;
import org.jboss.aop.array.ArrayElementWriteInvocation;
import org.jboss.aop.array.ArrayRegistry;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.cache.pojo.collection.CachedArray;
import org.jboss.cache.pojo.collection.CachedArrayRegistry;

/**
 * AOP interceptor which delegates to POJO Cache.
 *
 * @author Jason T. Greene
 */
public class ArrayInterceptor implements Interceptor
{

   /* (non-Javadoc)
    * @see org.jboss.aop.advice.Interceptor#getName()
    */
   public String getName()
   {
      // TODO Auto-generated method stub
      return this.getClass().getName();
   }

   /* (non-Javadoc)
    * @see org.jboss.aop.advice.Interceptor#invoke(org.jboss.aop.joinpoint.Invocation)
    */
   public Object invoke(Invocation invocation) throws Throwable
   {
      if (invocation instanceof ArrayElementReadInvocation)
      {
         ArrayElementReadInvocation read = (ArrayElementReadInvocation)invocation;
         Object array = read.getTargetObject();
         CachedArray cached = CachedArrayRegistry.lookup(array);
         if (cached != null)
         {
            int index = read.getIndex();
            Object element = cached.get(index);
            
            // AOP only registers on write, work around for now
            if (element != null && element.getClass().isArray())
               ArrayRegistry.getInstance().addElementReference(array, index, element);
            
            return element;
         }
      }
      else if (invocation instanceof ArrayElementWriteInvocation)
      {
         ArrayElementWriteInvocation write = (ArrayElementWriteInvocation) invocation;
         Object array = write.getTargetObject();
         CachedArray cached = CachedArrayRegistry.lookup(array);
         if (cached != null)
         {
            cached.set(write.getIndex(), write.getValue());
            return null;
         }
      }
      
      return invocation.invokeNext();
   }

}
