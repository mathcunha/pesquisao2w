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

package org.jboss.cache.pojo.notification.event;

import java.util.List;

import org.jboss.cache.pojo.notification.NotificationContext;

/**
 * A notification that indicates an attached List was modified.
 *
 * @author Jason T. Greene
 */
public final class ListModifiedEvent extends Event
{
   private static final long serialVersionUID = -1981636493457934325L;

   public enum Operation {ADD, SET, REMOVE}
   private final Operation operation;
   private final int index;
   private final Object value;

   public ListModifiedEvent(NotificationContext context, List source, Operation operation, int index, Object value, boolean local)
   {
      super(context, source, local);
      this.operation = operation;
      this.index = index;
      this.value = value;
   }

   /**
    * Gets the index of the change. For an add operation this may be equal to the current size (append).
    *
    * @return the index of this change
    */
   public int getIndex()
   {
      return index;
   }

   /**
    * Gets the new value for this change. Returns null on a remove operation.
    *
    * @return the new value, or null if remove
    */
   public Object getValue()
   {
      return value;
   }

   /**
    * Gets the operation of this change.
    *
    * @return the operation.
    */
   public Operation getOperation()
   {
      return operation;
   }

   @Override
   public List getSource()
   {
      return (List) super.getSource();
   }
}