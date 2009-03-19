/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.collection;

import static org.jboss.cache.pojo.impl.InternalConstant.POJOCACHE_OPERATION;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.aop.Advised;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.impl.PojoCacheImpl;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.util.CacheApiUtil;
import org.jboss.cache.pojo.util.Null;

/**
 * Map that uses cache as a backend store.
 *
 * @author Ben Wang
 * @author Scott Marlow
 */
public class CachedMapImpl implements Map
{
   private PojoCacheImpl pojoCache;
   private Cache<Object, Object> cache;
   private AbstractCollectionInterceptor interceptor;

   public CachedMapImpl(PojoCacheImpl pCache, AbstractCollectionInterceptor interceptor)
   {
      this.pojoCache = pCache;
      this.cache = pojoCache.getCache();
      this.interceptor = interceptor;
   }

   private static Fqn constructFqn(Fqn baseFqn, Object relative)
   {
      if (!(relative instanceof Serializable) && !(relative instanceof Advised))
      {
         throw new PojoCacheException("Non-serializable for " + relative.getClass().getName());
      }

      return new Fqn(baseFqn, relative);
   }

   private Fqn getFqn()
   {
      return interceptor.getFqn();
   }

   private Object attach(Object key, Object value)
   {
      Fqn fqn = constructFqn(getFqn(), Null.toNullKeyObject(key));
      Object o = pojoCache.attach(fqn, Null.toNullObject(value));
      pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, "PUT");

      return o;
   }

   private Object detach(Object key)
   {
      Fqn fqn = constructFqn(getFqn(), Null.toNullKeyObject(key));
      pojoCache.getCache().put(fqn, POJOCACHE_OPERATION, "REMOVE");

      return pojoCache.detach(fqn);
   }

   // implementation of the java.util.Map interface

   private Set<Node> getNodeChildren()
   {
      return CacheApiUtil.getNodeChildren(cache, getFqn());
   }

   public Object get(Object key)
   {
      return Null.toNullValue(pojoCache.find(constructFqn(getFqn(), Null.toNullKeyObject(key))));
   }

   public Object put(Object key, Object value)
   {
      return attach(key, value);
   }

   public void putAll(Map map)
   {
      for (Iterator i = map.entrySet().iterator(); i.hasNext();)
      {
         Map.Entry entry = (Map.Entry) i.next();
         put(entry.getKey(), entry.getValue());
      }
   }

   public Object remove(Object key)
   {
      return detach(key);
   }

   public void clear()
   {
      // Need to clone first to avoid CME
      ArrayList list = new ArrayList(keySet());
      for (int i = 0; i < list.size(); i++)
      {
         remove(list.get(i));
      }
   }

   public int size()
   {
      Set<Node> children = getNodeChildren();
      return children == null ? 0 : children.size();
   }

   public boolean isEmpty()
   {
      return size() == 0;
   }

   public boolean containsKey(Object object)
   {
      Set<Node> children = getNodeChildren();
      if (children == null) return false;
      for (Object n : children)
      {
         if (((Node) n).getFqn().getLastElement().equals(Null.toNullKeyObject(object))) return true;
      }

      return false;
   }

   public boolean containsValue(Object object)
   {
      return values().contains(Null.toNullObject(object));
   }

   public Set entrySet()
   {
      final CachedMapImpl map = this;

      return new AbstractSet()
      {

         public int size()
         {
            Set<Node> children = getNodeChildren();
            return children == null ? 0 : children.size();
         }

         public Iterator iterator()
         {
            Set<Node> children = getNodeChildren();
            final Iterator i =
                    children == null
                            ? Collections.EMPTY_LIST.iterator()
                            : children.iterator();
            return new Iterator()
            {
               Object lastKey; // for remove

               public boolean hasNext()
               {
                  return i.hasNext();
               }

               public Object next()
               {
                  return new Entry(lastKey = ((Node) i.next()).getFqn().getLastElement());
               }

               public void remove()
               {
                  map.remove(lastKey);
               }
            };
         }
      };
   }

   public Collection values()
   {
      final CachedMapImpl map = this;

      return new AbstractCollection()
      {

         public int size()
         {
            Set<Node> children = getNodeChildren();
            return children == null ? 0 : children.size();
         }

         public void clear()
         {
            map.clear();
         }

         public Iterator iterator()
         {
            Set<Node> children = getNodeChildren();
            final Iterator i =
                    children == null
                            ? Collections.EMPTY_LIST.iterator()
                            : children.iterator();

            return new Iterator()
            {
               Object lastKey; // for remove

               public boolean hasNext()
               {
                  return i.hasNext();
               }

               public Object next()
               {
                  Fqn f = ((Node) i.next()).getFqn();
                  lastKey = f.getLastElement();
                  return Null.toNullValue(pojoCache.find(f));
               }

               public void remove()
               {
                  Object key = lastKey;
                  if (key != null)  // convert from internal Null form to actual null if needed
                     key = Null.toNullKeyValue(key);
                  map.remove(key);
               }
            };
         }
      };
   }

   public Set keySet()
   {
      final CachedMapImpl map = this;

      return new AbstractSet()
      {

         public int size()
         {
            Set<Node> children = getNodeChildren();
            return children == null ? 0 : children.size();
         }

         public Iterator iterator()
         {
            Set<Node> children = getNodeChildren();
            final Iterator i =
                    children == null
                            ? Collections.EMPTY_LIST.iterator()
                            : children.iterator();

            return new Iterator()
            {
               Object lastKey; // for remove

               public boolean hasNext()
               {
                  return i.hasNext();
               }

               public Object next()
               {
                  lastKey = ((Node) i.next()).getFqn().getLastElement();
                  return Null.toNullKeyValue(lastKey);

               }

               public void remove()
               {
                  Object key = lastKey;
                  if (key != null)  // convert from internal Null form to actual null if needed
                     key = Null.toNullKeyValue(key);
                  map.remove(key);
               }
            };

         }
      };
   }

   public int hashCode()
   {
      int result = 0;
      for (Iterator i = entrySet().iterator(); i.hasNext();)
      {
         result += i.next().hashCode();
      }
      return result;
   }

   public boolean equals(Object object)
   {
      if (object == this)
         return true;
      if (object == null || !(object instanceof Map))
         return false;
      Map map = (Map) object;
      if (size() != map.size())
         return false;
      for (Iterator i = entrySet().iterator(); i.hasNext();)
      {
         Entry entry = (Entry) i.next();
         Object value = entry.getValue();
         Object key = entry.getKey();
         if (value == null)
         {
            if (!(map.get(key) == null && map.containsKey(key)))
            {
               return false;
            }
         } else
         {
            if (!value.equals(map.get(key)))
               return false;
         }
      }
      return true;
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer();
      Set set = keySet();
      for (Iterator it = set.iterator(); it.hasNext();)
      {
         Object key = it.next();
         Object value = get(key);
         if (key == interceptor.getBoundProxy())
            key = "(this map)";
         if (value == interceptor.getBoundProxy())
            value = "(this map)";

         buf.append("[").append(key).append(", ").append(value).append("]");
         if (it.hasNext()) buf.append(", ");
      }

      return buf.toString();
   }

   class Entry implements Map.Entry
   {

      Object key;

      public Entry(Object key)
      {
         this.key = key;
      }

      public Object getKey()
      {
         return Null.toNullValue(key);
      }

      public Object getValue()
      {
         return Null.toNullValue(pojoCache.find(constructFqn(getFqn(), key)));
      }

      public Object setValue(Object value)
      {
         return attach(key, value);
      }

      public int hashCode()
      {
         Object value = getValue();
         return ((key == null) ? 0 : key.hashCode())
                 ^ ((value == null) ? 0 : value.hashCode());
      }

      public boolean equals(Object obj)
      {
         if (!(obj instanceof Entry))
            return false;
         Entry entry = (Entry) obj;
         Object value = getValue();
         return (
                 key == null
                         ? entry.getKey() == null
                         : key.equals(entry.getKey()))
                 && (value == null
                 ? entry.getValue() == null
                 : value.equals(entry.getValue()));
      }
   }


}
