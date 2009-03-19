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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.notification.annotation.ArrayModified;
import org.jboss.cache.pojo.notification.annotation.Attached;
import org.jboss.cache.pojo.notification.annotation.Detached;
import org.jboss.cache.pojo.notification.annotation.FieldModified;
import org.jboss.cache.pojo.notification.annotation.ListModified;
import org.jboss.cache.pojo.notification.annotation.MapModified;
import org.jboss.cache.pojo.notification.annotation.PojoCacheListener;
import org.jboss.cache.pojo.notification.annotation.SetModified;
import org.jboss.cache.pojo.notification.annotation.TransactionCompleted;
import org.jboss.cache.pojo.notification.annotation.TransactionRegistered;
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

// $Id: NotificationDispatcher.java 6551 2008-08-09 03:21:50Z jason.greene@jboss.com $

/**
 * Dispatches notification events to POJO cache listeners.
 *
 * @author Jason T. Greene
 * @revision $Id: NotificationDispatcher.java 6551 2008-08-09 03:21:50Z jason.greene@jboss.com $
 */
class NotificationDispatcher
{
   private final static Map<Class<? extends Annotation>, Class<? extends Event>> annotations = new HashMap<Class<? extends Annotation>, Class<? extends Event>>();
   private final Set<Entry> listeners = new CopyOnWriteArraySet<Entry>();
   private Set<Object> filteredListeners = new HashSet<Object>();
   private volatile boolean hasFilters;

   static
   {
      annotations.put(Attached.class, AttachedEvent.class);
      annotations.put(Detached.class, DetachedEvent.class);
      annotations.put(FieldModified.class, FieldModifiedEvent.class);
      annotations.put(ListModified.class, ListModifiedEvent.class);
      annotations.put(MapModified.class, MapModifiedEvent.class);
      annotations.put(SetModified.class, SetModifiedEvent.class);
      annotations.put(ArrayModified.class, ArrayModifiedEvent.class);
      annotations.put(TransactionRegistered.class, TransactionRegisteredEvent.class);
      annotations.put(TransactionCompleted.class, TransactionCompletedEvent.class);
   }

   final static class Entry
   {
      private final Object listener;
      private final Pattern pattern;
      private final Map<Class<?>, List<Method>> notifiers;

      private Entry(Object listener, boolean build)
      {
         this(listener, build, null);
      }

      private Entry(Object listener, boolean build, Pattern pattern)
      {
         if (listener == null)
            throw new IllegalArgumentException("Listener can't be null");

         this.listener = listener;
         this.pattern = pattern;

         this.notifiers = build ? buildNotifiers(listener.getClass()) : null;
      }

      // equality is confined to listener
      public int hashCode()
      {
         return listener.hashCode();
      }

      // equality is confined to listener
      public boolean equals(Object o)
      {
         if (o == this)
            return true;
         if (! (o instanceof Entry))
            return false;

         // Must be the same instance
         return ((Entry)o).listener == this.listener;
      }

      private static Map<Class<?>, List<Method>> buildNotifiers(Class<?> clazz)
      {
         if (! Modifier.isPublic(clazz.getModifiers()))
            throw new IllegalArgumentException("Listener must be public! Class:" + clazz.getName());

         if (! clazz.isAnnotationPresent(PojoCacheListener.class))
            throw new IllegalArgumentException("Not a listener, class did not contain @PojoCacheListener. Class: " + clazz.getName());


         Map<Class<?>, List<Method>> notifiers = new HashMap<Class<?>, List<Method>>();
         for (Method method : clazz.getMethods())
         {
            for (Annotation annotation : method.getAnnotations())
            {
               Class<? extends Event> event = annotations.get(annotation.annotationType());
               if (event == null)
                  continue;

               Class<?>[] types = method.getParameterTypes();
               if (types.length != 1 || !types[0].isAssignableFrom(event))
               {
                  throw new IllegalArgumentException("Listener has invlaid method signature for annotation. " +
                        "Method: \"" + method.getName() + "\" " +
                        "Annotation: \"" + annotation.annotationType().getSimpleName() + "\" " +
                        "Expected Parameter: \"" + event.getSimpleName() + "\"");
               }

               List<Method> list = notifiers.get(event);
               if (list == null)
               {
                  list = new ArrayList<Method>();
                  notifiers.put(event, list);
               }

               list.add(method);
            }
         }

         return notifiers;
      }
   }

   void add(Object listener)
   {
      listeners.add(new Entry(listener, true));
   }

   // gaurds filteredListeners
   synchronized void add(Object listener, Pattern pattern)
   {
      listeners.add(new Entry(listener, true, pattern));
      filteredListeners.add(listener);
      hasFilters = true;
   }

   // gaurds filteredListeners
   synchronized void remove(Object listener)
   {
      filteredListeners.remove(listener);
      if (filteredListeners.size() == 0)
         hasFilters = false;
      listeners.remove(new Entry(listener, false));
   }

   boolean hasFilters()
   {
      return hasFilters;
   }

   Set<Object> getListeners()
   {
      Set<Object> set = new HashSet<Object>();
      for (Entry entry : listeners)
         set.add(entry.listener);

      return Collections.unmodifiableSet(set);
   }

   Set<Entry> getListenerEntries(Collection<Reference> references)
   {
      Set<Entry> set = new HashSet<Entry>();
      for (Entry entry : listeners)
      {
         if (entry.pattern == null)
         {
            set.add(entry);
            continue;
         }

         for (Reference reference : references)
         {
            if (entry.pattern.matcher(reference.getFqn().toString()).matches())
            {
               set.add(entry);
               break;
            }
         }
      }

      return set;
   }

   boolean isEmpty()
   {
      return listeners.size() == 0;
   }

   void dispatch(Event notification)
   {
      for (Entry entry : listeners)
      {
         // Prevent dispatch to filtered entries
         if (entry.pattern == null)
            dispatch(notification, entry);
      }
   }

   void dispatch(Event notification, Set<Entry> listeners)
   {
      for (Entry listener : listeners)
         dispatch(notification, listener);
   }

   private void dispatch(Event notification, Entry entry)
   {
      List<Method> methods = entry.notifiers.get(notification.getClass());
      if (methods == null)
         return;

      try
      {
         for (Method method : methods)
            method.invoke(entry.listener, notification);
      }
      catch (Exception e)
      {
         throw new PojoCacheException(e);
      }
   }
}
