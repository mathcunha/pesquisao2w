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

package org.jboss.cache.pojo.memory;

import java.lang.reflect.Field;

import org.jboss.cache.pojo.util.PrivilegedCode;

/**
 * Creates a persistentReference for Fields
 *
 * @author csuconic
 */
public class FieldPersistentReference extends PersistentReference
{

   private String name;

   public FieldPersistentReference(Field field, int referenceType)
   {
      super(field != null ? field.getDeclaringClass() : null, field, referenceType);
      if (field != null)
         this.name = field.getName();
   }

   public synchronized Object rebuildReference() throws Exception
   {
      // A reference to guarantee the value is not being GCed during while the value is being rebuilt
      Object returnValue = null;
      if ((returnValue = internalGet()) != null) return returnValue;

      Field field = getMappedClass().getDeclaredField(name);
      PrivilegedCode.setAccessible(field);
      buildReference(field);
      return field;
   }

   public Field getField()
   {
      return (Field) get();
   }
}

