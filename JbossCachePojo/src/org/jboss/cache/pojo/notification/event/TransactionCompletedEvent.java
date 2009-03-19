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

import javax.transaction.Transaction;

import org.jboss.cache.pojo.notification.NotificationContext;

/**
 * A notification that indicates a transaction completed.
 *
 * @author Jason T. Greene
 */
public final class TransactionCompletedEvent extends Event
{
   private static final long serialVersionUID = -1981636493457934325L;

   private final boolean successful;

   public TransactionCompletedEvent(NotificationContext context, boolean successful, boolean local)
   {
      super(context, context.getTransaction(), local);
      this.successful = successful;
   }

   @Override
   public Transaction getSource()
   {
      return (Transaction) super.getSource();
   }

   /**
    * Whether or not the transaction was successful.
    *
    * @return success boolean
    */
   public boolean isSuccessful()
   {
      return successful;
   }
}