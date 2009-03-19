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

import java.util.EventObject;

import org.jboss.cache.pojo.notification.NotificationContext;

/**
 * Base class for all POJO Cache events.
 *
 * @author Jason T. Greene
 */
public abstract class Event extends EventObject
{
   private static final long serialVersionUID = -1981636493457934325L;

   private final NotificationContext context;
   private final boolean local;

   public Event(NotificationContext context, Object source, boolean local)
   {
      super(source);
      this.context = context;
      this.local = local;
   }

   /**
    * Determines if this event originated locally.
    *
    * @return true if this event originated locally, otherwise false.
    */
   public boolean isLocal()
   {
      return local;
   }

   /**
    * Obtain the context associated with this notification.
    *
    * @return the notification context
    */
   public NotificationContext getContext()
   {
      return context;
   }
}