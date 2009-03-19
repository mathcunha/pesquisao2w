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

import static org.jboss.cache.notifications.event.Event.Type.TRANSACTION_COMPLETED;
import static org.jboss.cache.notifications.event.Event.Type.TRANSACTION_REGISTERED;
import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_OPERATION;
import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_STATUS;
import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_LOCK;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.annotation.TransactionCompleted;
import org.jboss.cache.notifications.annotation.TransactionRegistered;
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.TransactionalEvent;
import org.jboss.cache.notifications.event.NodeModifiedEvent.ModificationType;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.notification.NotificationContext;
import org.jboss.cache.pojo.notification.event.ArrayModifiedEvent;
import org.jboss.cache.pojo.notification.event.AttachedEvent;
import org.jboss.cache.pojo.notification.event.DetachedEvent;
import org.jboss.cache.pojo.notification.event.Event;
import org.jboss.cache.pojo.notification.event.FieldModifiedEvent;
import org.jboss.cache.pojo.notification.event.ListModifiedEvent;
import org.jboss.cache.pojo.notification.event.MapModifiedEvent;
import org.jboss.cache.pojo.notification.event.SetModifiedEvent;
import org.jboss.cache.pojo.notification.event.TransactionCompletedEvent;
import org.jboss.cache.pojo.notification.event.TransactionRegisteredEvent;

// $Id: CacheListenerAdaptor.java 6048 2008-06-26 02:21:26Z jason.greene@jboss.com $

/**
 * Adapts the core cache listener API into the POJO listener API.
 *
 * @author Jason T. Greene
 */
@CacheListener
public class CacheListenerAdaptor
{
   private static final HashSet<String> internalKeys = new HashSet<String>();
   private static final Log log = LogFactory.getLog(CacheListenerAdaptor.class);

   static
   {
      internalKeys.add(POJOCACHE_STATUS);
      internalKeys.add(POJOCACHE_OPERATION);
      internalKeys.add(PojoInstance.KEY);
      internalKeys.add(PojoReference.KEY);
      internalKeys.add(POJOCACHE_LOCK);
   }

   private PojoCacheImpl cache;
   private NotificationDispatcher dispatcher = new NotificationDispatcher();

   public CacheListenerAdaptor(PojoCacheImpl cache)
   {
      this.cache = cache;
   }

   public PojoCache getPojoCache()
   {
      return this.cache;
   }

   private FieldModifiedEvent createModifyEvent(NotificationContext context, Fqn fqn, String key, Object value, boolean local)
   {
      if (value instanceof PojoReference)
         value = cache.find(((PojoReference) value).getFqn().toString());

      Object o = cache.find(fqn.toString());
      // Detached
      if (o == null)
         return null;

      Field f;
      try
      {
         f = o.getClass().getDeclaredField(key);
      }
      catch (NoSuchFieldException e)
      {
         if (log.isWarnEnabled())
            log.warn("Could not get field " + key + " on class " + o.getClass());
         return null;
      }

      return new FieldModifiedEvent(context, o, f, value, local);
   }

   private NotificationContext createContext(final TransactionalEvent event)
   {
      return new NotificationContext()
      {

         public PojoCache getPojoCache()
         {
            return CacheListenerAdaptor.this.getPojoCache();
         }

         public Transaction getTransaction()
         {
            return event.getTransaction();
         }
      };
   }

   private void sendNotification(Event notification, Set<NotificationDispatcher.Entry> listeners)
   {
      if (notification == null)
         return;

      if (listeners == null)
         dispatcher.dispatch(notification);
      else
         dispatcher.dispatch(notification, listeners);
   }

   @TransactionCompleted
   @TransactionRegistered
   public void handleTransaction(TransactionalEvent event)
   {
      if (event.getType() == TRANSACTION_COMPLETED)
      {
         boolean successful = ((org.jboss.cache.notifications.event.TransactionCompletedEvent) event).isSuccessful();
         sendNotification(new TransactionCompletedEvent(createContext(event), successful, event.isOriginLocal()), null);
      }
      else if (event.getType() == TRANSACTION_REGISTERED)
      {
         sendNotification(new TransactionRegisteredEvent(createContext(event), event.isOriginLocal()), null);
      }
   }

   @NodeModified
   public void nodeModified(NodeModifiedEvent event)
   {
      Map<Object, Object> data = event.getData();
      boolean pre = event.isPre();
      Fqn fqn = event.getFqn();
      ModificationType modType = event.getModificationType();
      boolean isLocal = event.isOriginLocal();

      if (pre)
         return;

      //System.out.println(fqn + " " + modType + " " + data);

      // If we are filtering, don't load as much as possible
      Set<NotificationDispatcher.Entry> matched = null;
      if (dispatcher.hasFilters())
      {
         matched = matchListeners(fqn);
         if (matched != null && matched.isEmpty())
            return;
      }

      if (modType == ModificationType.PUT_DATA)
      {
         if ("ATTACHED".equals(data.get(POJOCACHE_STATUS)))
         {
            Object o = cache.find(fqn.toString());
            sendNotification(new AttachedEvent(createContext(event), o, isLocal), matched);
         }
         else if ("DETACHING".equals(data.get(POJOCACHE_STATUS)))
         {
            Object o = cache.find(fqn.toString());
            sendNotification(new DetachedEvent(createContext(event), o, isLocal), matched);
         }
         else if (data.containsKey(POJOCACHE_OPERATION))
         {
            Object collection = cache.find(fqn.getParent().toString());
            if (collection instanceof List)
            {
               int i = Integer.parseInt(fqn.getLastElementAsString());
               ListModifiedEvent.Operation operation = ListModifiedEvent.Operation.valueOf(data.get(POJOCACHE_OPERATION).toString());
               Object value = cache.find(fqn.toString());
               sendNotification(new ListModifiedEvent(createContext(event), (List) collection, operation, i, value, isLocal), matched);
            }
            else if (collection instanceof Set)
            {
               SetModifiedEvent.Operation operation = SetModifiedEvent.Operation.valueOf(data.get(POJOCACHE_OPERATION).toString());
               Object value = cache.find(fqn.toString());
               sendNotification(new SetModifiedEvent(createContext(event), (Set) collection, operation, value, isLocal), matched);
            }
            else if (collection instanceof Map)
            {
               MapModifiedEvent.Operation operation = MapModifiedEvent.Operation.valueOf(data.get(POJOCACHE_OPERATION).toString());
               Object value = cache.find(fqn.toString());
               sendNotification(new MapModifiedEvent(createContext(event), (Map) collection, operation, fqn.getLastElement(), value, isLocal), matched);
            }
            else if (collection instanceof Object[]) {
               int i = Integer.parseInt(fqn.getLastElementAsString());
               Object value = cache.find(fqn.toString());
               sendNotification(new ArrayModifiedEvent(createContext(event), collection, i, value, isLocal), matched);
            }
         }
         else if ("ATTACHED".equals(cache.getCache().get(fqn, POJOCACHE_STATUS)))
         {
            for (Map.Entry entry : data.entrySet())
            {
               String key = entry.getKey().toString();
               Object value = entry.getValue();

               if (internalKeys.contains(key))
                  continue;

               sendNotification(createModifyEvent(createContext(event), fqn, key, value, isLocal), matched);
            }
         }
      }
      else if (modType == ModificationType.REMOVE_DATA)
      {
         for (Map.Entry entry : data.entrySet())
         {
            String key = entry.getKey().toString();
            if (internalKeys.contains(key))
               continue;

            sendNotification(createModifyEvent(createContext(event), fqn, key, null, isLocal), matched);
         }
      }
   }

   private Set<NotificationDispatcher.Entry> matchListeners(Fqn fqn)
   {
      PojoInstance instance = (PojoInstance) cache.getCache().get(fqn, PojoInstance.KEY);
      if (instance != null)
         return dispatcher.getListenerEntries(instance.getReferences());

      return null;
   }

   public boolean isEmpty()
   {
      return dispatcher.isEmpty();
   }

   public Collection<Object> getListeners()
   {
      return dispatcher.getListeners();
   }

   public void addListener(Object listener)
   {
      dispatcher.add(listener);
   }

   public void addListener(Object listener, Pattern pattern)
   {
      if (pattern == null)
         dispatcher.add(listener);
      else
         dispatcher.add(listener, pattern);
   }

   public void removeListener(Object listener)
   {
      dispatcher.remove(listener);
   }
}