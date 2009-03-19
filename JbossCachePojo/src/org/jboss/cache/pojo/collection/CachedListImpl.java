/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.collection;

import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_OPERATION;

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.util.AopUtil;
import org.jboss.cache.pojo.util.CacheApiUtil;
import org.jboss.cache.pojo.util.Null;

/**
 * List implementation that uses cache as a backend store.
 *
 * @author Ben Wang
 * @author Scott Marlow
 */
public class CachedListImpl extends CachedListAbstract
{
   private static Log log_ = LogFactory.getLog(CachedListImpl.class.getName());
   private Cache<Object, Object> cache;
   private PojoCacheImpl pojoCache;
   private AbstractCollectionInterceptor interceptor;

   public CachedListImpl(PojoCacheImpl cache, AbstractCollectionInterceptor interceptor)
   {
      this.pojoCache = cache;
      this.cache = pojoCache.getCache();
      this.interceptor = interceptor;
   }

   private Fqn getFqn()
   {
      return interceptor.getFqn();
   }

   // implementation of the java.util.List interface

   private Set<Node> getNodeChildren()
   {
      return CacheApiUtil.getNodeChildren(cache, getFqn());
   }

   /**
    * Serialize a normal ArrayList
    */
   private Object XwriteReplace() throws ObjectStreamException
   {
      log_.warn("writeReplace(): this calss is not suppored to be serialized." +
              " Will substitue with a normal ArryList");

      ArrayList toSerialize = new ArrayList();
      toSerialize.addAll(this);
      return toSerialize;
   }

   public Object get(int index)
   {
      checkIndex();
      return Null.toNullValue(pojoCache.find(AopUtil.constructFqn(getFqn(), IntegerCache.toString(index))));
   }

   private static void checkIndex()
   {
      // TODO This is too expensive now to check it everytime from the cache (potentially twice).
      // It is showing up in the JProfiler. So I am disabling it now.
      return;
/*
      if(size() == 0) return; // No need to check here.
      if( i < 0 || i >= size() ) {
         throw new IndexOutOfBoundsException("Index out of bound at CachedListImpl(). Index is " +i
         + " but size is " +size());
      } */
   }

   public int size()
   {
      Set<Node> children = getNodeChildren();
      return children == null ? 0 : children.size();
   }

   public Object set(int index, Object element)
   {
      if (index != 0)
         checkIndex(); // Since index can be size().
      return Null.toNullValue(attach(index, element, "SET"));
   }

   public void add(int index, Object element)
   {
      if (index != 0)
         checkIndex(); // Since index can be size().
      for (int i = size(); i > index; i--)
      {
         Object obj = detach(i - 1);
         attach(i, obj);
      }
      attach(index, element, "ADD");
   }

   private Object attach(int i, Object obj)
   {
      return attach(i, obj, null);
   }

   private Object attach(int i, Object obj, String operation)
   {
      Fqn fqn = AopUtil.constructFqn(getFqn(), IntegerCache.toString(i));
      Object o = pojoCache.attach(fqn, Null.toNullObject(obj));
      if (operation != null)
         pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, operation);

      return o;
   }

   private Object detach(int i)
   {
      return detach(i, null);
   }

   private Object detach(int i, String operation)
   {
      Fqn fqn = AopUtil.constructFqn(getFqn(), IntegerCache.toString(i));
      if (operation != null)
         pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, operation);
      return pojoCache.detach(fqn);
   }

   public int indexOf(Object o)
   {
      int size = size();
      if (o == null)
      {
         for (int i = 0; i < size; i++)
         {
            if (null == get(i))
               return i;
         }
      } else
      {
         for (int i = 0; i < size; i++)
         {
            if (o.equals(get(i)))
               return i;
         }
      }
      return -1;
   }

   public int lastIndexOf(Object o)
   {
      if (o == null)
      {
         for (int i = size() - 1; i >= 0; i--)
         {
            if (null == get(i))
               return i;
         }
      } else
      {
         for (int i = size() - 1; i >= 0; i--)
         {
            if (o.equals(get(i)))
               return i;
         }
      }
      return -1;
   }

   public Object remove(int index)
   {
      checkIndex();
      // Object result = cache.removeObject(((Fqn) fqn.clone()).add(new Integer(index)));
      int size = size();
      Object result = Null.toNullValue(detach(index, "REMOVE"));
      if (size == (index + 1))
      {
         return result; // We are the last one.
      }
      for (int i = index; i < size - 1; i++)
      {
         Object obj = detach(i + 1);
         attach(i, obj);
      }
      return result;
   }

   public Iterator iterator()
   {
      // TODO: check for concurrent modification
      return new Iterator()
      {
         // Need to share this
         int current = -1;
         int size = size();

         public boolean hasNext()
         {
            if (size == 0) return false;
            if (current > size)
               throw new NoSuchElementException("CachedSetImpl.iterator.hasNext(). " +
                       " Cursor position " + current + " is greater than the size " + size());

            return current < size - 1;
         }

         public Object next()
         {
            if (current == size)
               throw new NoSuchElementException("CachedSetImpl.iterator.next(). " +
                       " Cursor position " + current + " is greater than the size " + size());

            return Null.toNullValue(pojoCache.find(AopUtil.constructFqn(getFqn(), IntegerCache.toString(++current))));
         }

         public void remove()
         {
            // TODO Need optimization here since set does not care about index
            if (size == 0) return;
            if (current == size)
               throw new IllegalStateException("CachedSetImpl.iterator.remove(). " +
                       " Cursor position " + current + " is greater than the size " + size);
            if (current < (size - 1))
            {
               // Need to reshuffle the items.
               Object last = detach(current, "REMOVE");
               for (int i = current + 1; i < size; i++)
               {
                  last = detach(i);
                  attach(i - 1, last);
               }
            } else
            { // we are the last index.
               // Need to move back the cursor.
               detach(current, "REMOVE");
            }
            current--;
            size--;
         }
      };
   }

   public List subList(int fromIndex, int toIndex)
   {
      if (fromIndex < 0)
         throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
      if (toIndex > size())
         throw new IndexOutOfBoundsException("toIndex = " + toIndex + " but size() =" + size());
      if (fromIndex > toIndex)
         throw new IllegalArgumentException("fromIndex (" + fromIndex + ") must be less than toIndex(" + toIndex + ")");
      if (fromIndex == toIndex)            // request for empty list?
         return new LinkedList();
      return new MyCachedSubListImpl(this, fromIndex, toIndex);
   }

   public ListIterator listIterator()
   {
      return new MyListIterator(this, 0);
   }

   public ListIterator listIterator(int index)
   {
      return new MyListIterator(this, index);
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer();
      int size = size();
      for (int i = 0; i < size; i++)
      {
         Object key = get(i);
         if (key == interceptor.getBoundProxy())
            key = "(this list)";
         buf.append("[").append(key).append("]");
         if (i <= size) buf.append(", ");
      }

      return buf.toString();
   }

   static class MyListIterator implements ListIterator
   {
      int index = 0;
      List list_;

      public MyListIterator(List list, int index)
      {
         list_ = list;
         if (index < 0 || index > list_.size())
         {
            throw new IndexOutOfBoundsException("CachedListImpl: MyListIterator construction. " +
                    " Index is out of bound : " + index);
         }
         this.index = index;
      }

      public int nextIndex()
      {
         return index;
      }

      public int previousIndex()
      {
         return index - 1;
      }

      public void remove()
      {
         int size = list_.size();
         if (size == 0) return;
         if (previousIndex() == size)
            throw new IllegalStateException("CachedSetImpl.MyListIterator.remove(). " +
                    " Cursor position " + index + " is greater than the size " + size);
         if (previousIndex() < (size))
         {
            list_.remove(previousIndex());
            index--;
         }
      }

      public boolean hasNext()
      {
         return (index < list_.size());
      }

      public boolean hasPrevious()
      {
         return (index != 0);
      }

      public Object next()
      {
         if (index == list_.size())
            throw new NoSuchElementException();

         index++;
         return list_.get(index - 1);  // pass zero relative index
      }

      public Object previous()
      {
         if (index == 0)
            throw new NoSuchElementException();

         index--;
         return list_.get(index);
      }

      public void add(Object o)
      {
         int size = list_.size();
         if (size == 0) return;

         if (previousIndex() == size)
            throw new IllegalStateException("CachedSetImpl.MyListIterator.add(). " +
                    " Cursor position " + index + " is greater than the size " + size);
         if (previousIndex() < (size))
         {
            list_.add(previousIndex(), o);
         }
      }

      public void set(Object o)
      {
         int size = list_.size();
         if (size == 0) return;

         if (previousIndex() == size)
            throw new IllegalStateException("CachedSetImpl.MyListIterator.set(). " +
                    " Cursor position " + index + " is greater than the size " + size);
         if (previousIndex() < (size))
         {
            list_.set(previousIndex(), o);
         }
      }
   }

   static public class MyCachedSubListImpl extends CachedListAbstract implements List
   {

      private List backStore_;
      private int fromIndex_;
      private int toIndex_;

      MyCachedSubListImpl(List backStore, int fromIndex, int toIndex)
      {
         backStore_ = backStore;
         fromIndex_ = fromIndex;
         toIndex_ = toIndex;
      }

      public int size()
      {
         int size = backStore_.size();
         if (size > toIndex_)
            size = toIndex_;
         size -= fromIndex_;     // subtract number of items ignored at the start of list
         return size;
      }

      public Iterator iterator()
      {
         // TODO: check for concurrent modification
         return new Iterator()
         {
            int current = -1;
            Iterator iter_ = initializeIter();

            private Iterator initializeIter()
            {
               Iterator iter = backStore_.iterator();
               for (int looper = 0; looper < fromIndex_; looper++)
                  if (iter.hasNext())      // skip past to where we need to start from
                     iter.next();
               return iter;
            }

            public boolean hasNext()
            {
               int size = size();
               if (size == 0) return false;
               if (current > size)
                  throw new IllegalStateException("CachedSetImpl.MyCachedSubListImpl.iterator.hasNext(). " +
                          " Cursor position " + current + " is greater than the size " + size());

               return current < size() - 1;
            }

            public Object next()
            {
               if (current == size())
                  throw new IllegalStateException("CachedSetImpl.MyCachedSubListImpl.iterator.next(). " +
                          " Cursor position " + current + " is greater than the size " + size());
               current++;
               return iter_.next();
            }

            public void remove()
            {
               iter_.remove();
               current--;
            }
         };

      }

      public Object get(int index)
      {
         checkIndex(index);
         return backStore_.get(index + fromIndex_);
      }

      public Object set(int index, Object element)
      {
         checkIndex(index);
         return backStore_.set(index + fromIndex_, element);
      }

      public void add(int index, Object element)
      {
         backStore_.add(index + fromIndex_, element);
      }

      public Object remove(int index)
      {
         return backStore_.remove(index + fromIndex_);
      }

      public int indexOf(Object o)
      {
         int index = backStore_.indexOf(o);
         if (index < fromIndex_ || index >= toIndex_)
            index = -1;
         else
            index -= fromIndex_;    // convert to be relative to our from/to range
         return index;
      }

      public int lastIndexOf(Object o)
      {
         int index = backStore_.lastIndexOf(o);
         if (index < fromIndex_ || index >= toIndex_)
            index = -1;
         else
            index -= fromIndex_;    // convert to be relative to our from/to range
         return index;
      }

      public ListIterator listIterator()
      {
         return new MyListIterator(this, 0);
      }

      public ListIterator listIterator(int index)
      {
         return new MyListIterator(this, index);
      }

      public List subList(int fromIndex, int toIndex)
      {
         if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
         if (toIndex > size())
            throw new IndexOutOfBoundsException("toIndex = " + toIndex + " but size() =" + size());
         if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex (" + fromIndex + ") must be less than toIndex(" + toIndex + ")");
         if (fromIndex == toIndex)            // request for empty list?
            return new LinkedList();
         return new MyCachedSubListImpl(this, fromIndex, toIndex);
      }

      private void checkIndex(int i)
      {
         if (size() == 0) return; // No need to check here.
         if (i < 0 || i >= size())
         {
            throw new IndexOutOfBoundsException("Index out of bound at CachedListImpl(). Index is " + i
                    + " but size is " + size());
         }
      }

   }

}
