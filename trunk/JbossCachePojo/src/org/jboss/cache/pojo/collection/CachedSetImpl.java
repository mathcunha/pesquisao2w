/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.collection;

import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_OPERATION;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.util.AopUtil;
import org.jboss.cache.pojo.util.CacheApiUtil;
import org.jboss.cache.pojo.util.Null;

/**
 * Set implementation that uses a cache as an underlying backend store.
 * Set data is stored in children nodes named based on the attached Object's hash code
 * in hexidecimal string form.
 * If there are conflicts between two Objects with the same hash code, then a
 * counter (upper 32 bits of 64) is used.
 *
 * @author Ben Wang
 * @author Scott Marlow
 * @author Jussi Pyorre
 */
public class CachedSetImpl extends AbstractSet
{
   private PojoCacheImpl pojoCache;
   private Cache<Object, Object> cache;
   private AbstractCollectionInterceptor interceptor;

   public CachedSetImpl(PojoCacheImpl cache, AbstractCollectionInterceptor interceptor)
   {
      this.pojoCache = cache;
      this.cache = pojoCache.getCache();
      this.interceptor = interceptor;
   }

   private Set<Node> getNodeChildren()
   {
      return CacheApiUtil.getNodeChildren(cache, getFqn());
   }

   private Fqn getFqn()
   {
      // Cannot cache this as this can be reset
      return interceptor.getFqn();
   }

   // java.util.Set implementation

   public boolean add(Object o)
   {
      o = Null.toNullObject(o);
      int hashCode = o.hashCode();
      int size = size();
      for (int i = 0; i < size + 1; i++)
      {
         Object key = toLong(hashCode, i);
         Object o2 = getNoUnmask(key);
         if (o2 == null)
         {
            attach(key, o, true);
            return true;
         }
         if (o.equals(o2))
         {
            return false;
         }
      }
      // should never reach here
      throw new CacheException();
   }

   public void clear()
   {
      Set<Node> children = getNodeChildren();
      for (Node n : children)
      {
         pojoCache.detach(n.getFqn());
      }
   }

   public boolean contains(Object o)
   {
      o = Null.toNullObject(o);
      int hashCode = o.hashCode();
      int size = size();
      for (int i = 0; i < size; i++)
      {
         Object key = toLong(hashCode, i);
         Object o2 = getNoUnmask(key);
         if (o2 == null)
         {
            return false;
         }
         if (o.equals(o2))
         {
            return true;
         }
      }
      return false;
   }

   public Iterator<Object> iterator()
   {
      Node<Object, Object> node = cache.getRoot().getChild(getFqn());
      if (node == null)
      {
         return Collections.EMPTY_SET.iterator();
      }
      return new IteratorImpl(node);
   }

   public boolean remove(Object o)
   {
      o = Null.toNullObject(o);
      int hashCode = o.hashCode();
      int size = size();
      boolean removed = false;
      Object oldkey = null;
      for (int i = 0; i < size; i++)
      {
         Object key = toLong(hashCode, i);
         Object o2 = getNoUnmask(key);
         if (o2 == null)
         {
            break;
         }
         if (removed)
         {
            // move o2 to old key
            detach(key);
            attach(oldkey, o2);
         }
         if (o.equals(o2))
         {
            detach(key, true);
            removed = true;
         }
         oldkey = key;
      }
      return removed;
   }

   public int size()
   {
      Set<Node> children = getNodeChildren();
      return (children == null) ? 0 : children.size();
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer();
      for (Iterator it = iterator(); it.hasNext();)
      {
         Object key = it.next();
         if (key == interceptor.getBoundProxy())
            key = "(this set)";
         buf.append("[").append(key).append("]");
         if (it.hasNext()) buf.append(", ");
      }

      return buf.toString();
   }

   /**
    * Return a long with "count" as the high 32 bits.
    * TODO should be able to use java.lang.Long, but some CacheLoader don't
    * support non-String keys
    */
   private String toLong(long hashCode, long count)
   {
      long key = (hashCode & 0xFFFFL) | (count << 32);
      return Long.toHexString(key);
   }


   private Object attach(Object key, Object pojo)
   {
      return attach(key, pojo, false);
   }

   private Object attach(Object key, Object pojo, boolean add)
   {
      Fqn fqn = AopUtil.constructFqn(getFqn(), key);
      Object o = pojoCache.attach(fqn, pojo);
      if (add)
         pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, "ADD");

      return o;
   }

   private Object detach(Object key)
   {
      return detach(key, false);
   }

   private Object detach(Object key, boolean remove)
   {
      Fqn fqn = AopUtil.constructFqn(getFqn(), key);
      if (remove)
         pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, "REMOVE");

      return pojoCache.detach(fqn);
   }

   private Object getNoUnmask(Object key)
   {
      return pojoCache.find(AopUtil.constructFqn(getFqn(), key));
   }

   public int hashCode()
   {
      int result = super.hashCode();
      result = 29 * result + cache.hashCode();
      result = 29 * result + interceptor.hashCode();
      return result;
   }

   public boolean equals(Object o)
   {
      if (o == this)
      {
         return true;
      }

      try
      {
         return super.equals(o);
      }
      catch (ClassCastException e)
      {
         return false;
      }
      catch (NullPointerException unused)
      {
         return false;
      }
   }

   private class IteratorImpl implements Iterator<Object>
   {
      private Iterator<Node<Object, Object>> iterator;

      private Node<Object, Object> node;
      private Object o;

      private IteratorImpl(Node<Object, Object> node)
      {
         Collection<Node<Object, Object>> children = new HashSet<Node<Object, Object>>(node.getChildren());
         iterator = children.iterator();
      }

      public boolean hasNext()
      {
         return iterator.hasNext();
      }

      public Object next()
      {
         node = iterator.next();
         o = Null.toNullValue(pojoCache.find(node.getFqn()));
         return o;
      }

      public void remove() throws IllegalStateException
      {
         if (node == null)
         {
            throw new IllegalStateException();
         }
         CachedSetImpl.this.remove(o);
      }

   }
}
