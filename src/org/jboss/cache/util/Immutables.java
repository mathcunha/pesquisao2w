/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.cache.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Factory for generating immutable type wrappers.
 *
 * @author Jason T. Greene
 */
public class Immutables
{
   /**
    * Whether or not this collection type is immutable
    *
    * @param o a Collection, Set, List, or Map
    * @return true if immutable, false if not
    */
   public static boolean isImmutable(Object o)
   {
      return o instanceof Immutable;
   }

   /**
    * Converts a Collection to an immutable List by copying it.
    *
    * @param source the collection to convert
    * @return a copied/converted immutable list
    */
   public static <T> List<T> immutableListConvert(Collection<? extends T> source)
   {
      return new ImmutableListCopy<T>(source);
   }

   /**
    * Creates an immutable copy of the list.
    *
    * @param list the list to copy
    * @return the immutable copy
    */
   public static <T> List<T> immutableListCopy(List<? extends T> list)
   {
      return new ImmutableListCopy<T>(list);
   }

   /**
    * Wraps an array with an immutable list. There is no copying involved.
    *
    * @param <T>
    * @param array the array to wrap
    * @return a list containing the array
    */
   public static <T> List<T> immutableListWrap(T... array)
   {
      return new ImmutableListCopy<T>(array);
   }

   /**
    * Creates a new immutable list containing the union (combined entries) of both lists.
    *
    * @param list1 contains the first elements of the new list
    * @param list2 contains the successor elements of the new list
    * @return a new immutable merged copy of list1 and list2
    */
   public static <T> List<T> immutableListMerge(List<? extends T> list1, List<? extends T> list2)
   {
      return new ImmutableListCopy<T>(list1, list2);
   }

   /**
    * Converts a Collections into an immutable Set by copying it.
    *
    * @param collection the collection to convert/copy
    * @return a new immutable set containing the elements in collection
    */
   public static <T> Set<T> immutableSetConvert(Collection<? extends T> collection)
   {
      return immutableSetWrap(new HashSet<T>(collection));
   }

   /**
    * Wraps a set with an immutable set. There is no copying involved.
    *
    * @param set the set to wrap
    * @return an immutable set wrapper that delegates to the original set
    */
   public static <T> Set<T> immutableSetWrap(Set<? extends T> set)
   {
      return new ImmutableSetWrapper<T>(set);
   }

   /**
    * Creates an immutable copy of the specified set.
    *
    * @param set the set to copy from
    * @return an immutable set copy
    */
   public static <T> Set<T> immutableSetCopy(Set<? extends T> set)
   {
      Set<? extends T> copy = attemptKnownSetCopy(set);
      if (copy == null)
         attemptClone(set);
      if (copy == null)
         // Set uses Collection copy-ctor
         copy = attemptCopyConstructor(set, Collection.class);
      if (copy == null)
         copy = new HashSet<T>(set);

      return new ImmutableSetWrapper<T>(copy);
   }


   /**
    * Wraps a map with an immutable map. There is no copying involved.
    *
    * @param map the map to wrap
    * @return an immutable map wrapper that delegates to the original map
    */
   public static <K, V> Map<K, V> immutableMapWrap(Map<? extends K, ? extends V> map)
   {
      return new ImmutableMapWrapper<K, V>(map);
   }

   /**
    * Creates an immutable copy of the specified map.
    *
    * @param map the map to copy from
    * @return an immutable map copy
    */
   public static <K, V> Map<K, V> immutableMapCopy(Map<? extends K, ? extends V> map)
   {
      Map<? extends K, ? extends V> copy = attemptKnownMapCopy(map);

      if (copy == null)
         attemptClone(map);
      if (copy == null)
         copy = attemptCopyConstructor(map, Map.class);
      if (copy == null)
         copy = new HashMap<K, V>(map);

      return new ImmutableMapWrapper<K, V>(copy);
   }

   /**
    * Creates a new immutable copy of the specified Collection.
    *
    * @param collection the collection to copy
    * @return an immutable copy
    */
   public static <T> Collection<T> immutableCollectionCopy(Collection<? extends T> collection)
   {
      Collection<? extends T> copy = attemptKnownSetCopy(collection);
      if (copy == null)
         copy = attemptClone(collection);
      if (copy == null)
         copy = attemptCopyConstructor(collection, Collection.class);
      if (copy == null)
         copy = new ArrayList<T>(collection);

      return new ImmutableCollectionWrapper<T>(copy);
   }

   @SuppressWarnings("unchecked")
   private static <T extends Map> T attemptKnownMapCopy(T map)
   {
      if (map instanceof FastCopyHashMap)
         return (T) ((FastCopyHashMap) map).clone();
      if (map instanceof HashMap)
         return (T) ((HashMap) map).clone();
      if (map instanceof LinkedHashMap)
         return (T) ((LinkedHashMap) map).clone();
      if (map instanceof TreeMap)
         return (T) ((TreeMap) map).clone();

      return null;
   }

   @SuppressWarnings("unchecked")
   private static <T extends Collection> T attemptKnownSetCopy(T set)
   {
      if (set instanceof HashSet)
         return (T) ((HashSet) set).clone();
      if (set instanceof LinkedHashSet)
         return (T) ((LinkedHashSet) set).clone();
      if (set instanceof TreeSet)
         return (T) ((TreeSet) set).clone();

      return null;
   }

   @SuppressWarnings("unchecked")
   private static <T> T attemptClone(T source)
   {
      if (source instanceof Cloneable)
      {
         try
         {
            return (T) source.getClass().getMethod("clone").invoke(source);
         }
         catch (Exception e)
         {
         }
      }

      return null;
   }

   @SuppressWarnings("unchecked")
   private static <T> T attemptCopyConstructor(T source, Class<? super T> clazz)
   {
      try
      {
         return (T) source.getClass().getConstructor(clazz).newInstance(source);
      }
      catch (Exception e)
      {
      }

      return null;
   }


   public interface Immutable
   {
   }

   /*
    * Immutable wrapper types.
    *
    * We have to re-implement Collections.unmodifiableXXX, since it is not
    * simple to detect them (the class names are JDK dependent).
    */

   private static class ImmutableIteratorWrapper<E> implements Iterator<E>
   {
      private Iterator<? extends E> iterator;

      public ImmutableIteratorWrapper(Iterator<? extends E> iterator)
      {
         this.iterator = iterator;
      }

      public boolean hasNext()
      {
         return iterator.hasNext();
      }

      public E next()
      {
         return iterator.next();
      }

      public void remove()
      {
         throw new UnsupportedOperationException();
      }
   }

   private static class ImmutableCollectionWrapper<E> implements Collection<E>, Serializable, Immutable
   {
      private static final long serialVersionUID = 6777564328198393535L;

      Collection<? extends E> collection;

      public ImmutableCollectionWrapper(Collection<? extends E> collection)
      {
         this.collection = collection;
      }

      public boolean add(E o)
      {
         throw new UnsupportedOperationException();
      }

      public boolean addAll(Collection<? extends E> c)
      {
         throw new UnsupportedOperationException();
      }

      public void clear()
      {
         throw new UnsupportedOperationException();
      }

      public boolean contains(Object o)
      {
         return collection.contains(o);
      }

      public boolean containsAll(Collection<?> c)
      {
         return collection.containsAll(c);
      }

      public boolean equals(Object o)
      {
         return collection.equals(o);
      }

      public int hashCode()
      {
         return collection.hashCode();
      }

      public boolean isEmpty()
      {
         return collection.isEmpty();
      }

      public Iterator<E> iterator()
      {
         return new ImmutableIteratorWrapper<E>(collection.iterator());
      }

      public boolean remove(Object o)
      {
         throw new UnsupportedOperationException();
      }

      public boolean removeAll(Collection<?> c)
      {
         throw new UnsupportedOperationException();
      }

      public boolean retainAll(Collection<?> c)
      {
         throw new UnsupportedOperationException();
      }

      public int size()
      {
         return collection.size();
      }

      public Object[] toArray()
      {
         return collection.toArray();
      }

      public <T> T[] toArray(T[] a)
      {
         return collection.toArray(a);
      }

      public String toString()
      {
         return collection.toString();
      }
   }


   private static class ImmutableSetWrapper<E> extends ImmutableCollectionWrapper<E> implements Set<E>, Serializable, Immutable
   {
      private static final long serialVersionUID = 7991492805176142615L;

      public ImmutableSetWrapper(Set<? extends E> set)
      {
         super(set);
      }
   }


   static class ImmutableEntry<K, V> implements Map.Entry<K, V>
   {
      private K key;
      private V value;
      private int hash;

      ImmutableEntry(Map.Entry<? extends K, ? extends V> entry)
      {
         this.key = entry.getKey();
         this.value = entry.getValue();
         this.hash = entry.hashCode();
      }

      public K getKey()
      {
         return key;
      }

      public V getValue()
      {
         return value;
      }

      public V setValue(V value)
      {
         throw new UnsupportedOperationException();
      }

      private static boolean eq(Object o1, Object o2)
      {
         return o1 == o2 || (o1 != null && o1.equals(o2));
      }

      @SuppressWarnings("unchecked")
      public boolean equals(Object o)
      {
         if (!(o instanceof Map.Entry))
            return false;

         Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
         return eq(entry.getKey(), key) && eq(entry.getValue(), value);
      }

      public int hashCode()
      {
         return hash;
      }

      public String toString()
      {
         return getKey() + "=" + getValue();
      }
   }

   private static class ImmutableEntrySetWrapper<K, V> extends ImmutableSetWrapper<Map.Entry<K, V>>
   {
      private static final long serialVersionUID = 6378667653889667692L;

      @SuppressWarnings("unchecked")
      public ImmutableEntrySetWrapper(Set<? extends Map.Entry<? extends K, ? extends V>> set)
      {
         super((Set<Entry<K, V>>) set);
      }

      public Object[] toArray()
      {
         Object[] array = new Object[collection.size()];
         int i = 0;
         for (Map.Entry<K, V> entry : this)
            array[i++] = entry;
         return array;
      }

      @SuppressWarnings("unchecked")
      public <T> T[] toArray(T[] array)
      {
         int size = collection.size();
         if (array.length < size)
            array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);

         int i = 0;
         Object[] result = array;
         for (Map.Entry<K, V> entry : this)
            result[i++] = entry;

         return array;
      }

      public Iterator<Map.Entry<K, V>> iterator()
      {
         return new ImmutableIteratorWrapper<Entry<K, V>>(collection.iterator())
         {
            public Entry<K, V> next()
            {
               return new ImmutableEntry<K, V>(super.next());
            }
         };
      }
   }

   private static class ImmutableMapWrapper<K, V> implements Map<K, V>, Serializable, Immutable
   {
      private static final long serialVersionUID = 708144227046742221L;

      private Map<? extends K, ? extends V> map;

      public ImmutableMapWrapper(Map<? extends K, ? extends V> map)
      {
         this.map = map;
      }

      public void clear()
      {
         throw new UnsupportedOperationException();
      }

      public boolean containsKey(Object key)
      {
         return map.containsKey(key);
      }

      public boolean containsValue(Object value)
      {
         return map.containsValue(value);
      }

      public Set<Entry<K, V>> entrySet()
      {
         return new ImmutableEntrySetWrapper<K, V>(map.entrySet());
      }

      public boolean equals(Object o)
      {
         return map.equals(o);
      }

      public V get(Object key)
      {
         return map.get(key);
      }

      public int hashCode()
      {
         return map.hashCode();
      }

      public boolean isEmpty()
      {
         return map.isEmpty();
      }

      public Set<K> keySet()
      {
         return new ImmutableSetWrapper<K>(map.keySet());
      }

      public V put(K key, V value)
      {
         throw new UnsupportedOperationException();
      }

      public void putAll(Map<? extends K, ? extends V> t)
      {
         throw new UnsupportedOperationException();
      }

      public V remove(Object key)
      {
         throw new UnsupportedOperationException();
      }

      public int size()
      {
         return map.size();
      }

      public Collection<V> values()
      {
         return new ImmutableCollectionWrapper<V>(map.values());
      }

      public String toString()
      {
         return map.toString();
      }
   }
}
