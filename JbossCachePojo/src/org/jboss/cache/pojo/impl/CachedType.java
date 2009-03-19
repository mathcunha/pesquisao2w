package org.jboss.cache.pojo.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jboss.aop.Advisor;
import org.jboss.aop.joinpoint.FieldInvocation;
import org.jboss.cache.pojo.memory.FieldPersistentReference;
import org.jboss.cache.pojo.memory.PersistentReference;
import org.jboss.cache.pojo.util.PrivilegedCode;

/**
 * Represent a cached object type, e.g., whether it is <b>primitive</b> or not.
 * Note: need to pay special attention not to leak classloader.
 *
 * @author <a href="mailto:harald@gliebe.de">Harald Gliebe</a>
 * @author Ben Wang
 */

public class CachedType
{
   // Types that are considered "primitive".
   private static final Set<Object> immediates =
           new HashSet<Object>(Arrays.asList(new Object[]{
                   String.class,
                   Boolean.class,
                   Double.class,
                   Float.class,
                   Integer.class,
                   Long.class,
                   Short.class,
                   Character.class,
                   Byte.class,
                   Boolean.TYPE,
                   Double.TYPE,
                   Float.TYPE,
                   Integer.TYPE,
                   Long.TYPE,
                   Short.TYPE,
                   Character.TYPE,
                   Byte.TYPE,
                   Class.class}));

   private WeakReference<Class> type;
   private boolean immutable;
   private boolean immediate;

   // Java fields . Will use special FieldPersistentReference to prevent classloader leakage.
   private List<FieldPersistentReference> fields = new ArrayList<FieldPersistentReference>();
   private List<FieldPersistentReference> finalFields = new ArrayList<FieldPersistentReference>();
   private Map<String, FieldPersistentReference> fieldMap = new HashMap<String, FieldPersistentReference>();// Name -> CachedAttribute

   public CachedType()
   {
   }

   public CachedType(Class type)
   {
      this.type = new WeakReference<Class>(type);
      analyze();
   }

   public Class getType()
   {
      return type.get();
   }

   // determines if the object should be stored in the Nodes map or as a subnode
   public boolean isImmediate()
   {
      return immediate;
   }

   public static boolean isImmediate(Class clazz)
   {
      // Treat enums as a simple type since they serialize to a simple string
      return immediates.contains(clazz) || Enum.class.isAssignableFrom(clazz);
   }

   public boolean isImmutable()
   {
      return immutable;
   }

   public List<FieldPersistentReference> getFields()
   {
      return fields;
   }
   
   public List<FieldPersistentReference> getFinalFields()
   {
      return finalFields;
   }

   public Field getField(String name)
   {
      FieldPersistentReference ref = fieldMap.get(name);
      if (ref == null) return null;
      return (Field) ref.get();
   }

   /*
   public List getAttributes()
   {
      return attributes;
   }

   public CachedAttribute getAttribute(Method m)
   {
      return (CachedAttribute) attributeMap.get(m);
   }

   protected void setAttributes(List attributes)
   {
      this.attributes = attributes;

      attributeMap.clear();

      // TODO: is a class with no set methods immutable ?
      this.immutable = true;

      for (Iterator i = attributes.iterator(); i.hasNext();) {
         CachedAttribute attribute = (CachedAttribute) i.next();
         if (attribute.getGet() != null) {
            attributeMap.put(attribute.getGet(), attribute);
         }
         if (attribute.getSet() != null) {
            attributeMap.put(attribute.getSet(), attribute);
            immutable = false;
         }
      }
   }
   */

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append(getType().getName()).append(" {\n");
      /*
      for (Iterator i = attributes.iterator(); i.hasNext();) {
         CachedAttribute attr = (CachedAttribute) i.next();
         sb
               .append("\t")
               .append(attr.getType().getLastElementAsString())
               .append(" ")
               .append(attr.getLastElementAsString())
               .append(" [")
               .append(attr.getGet() == null
               ? "<no get>"
               : attr.getGet().getLastElementAsString())
               .append(", ")
               .append(attr.getSet() == null
               ? "<no set>"
               : attr.getSet().getLastElementAsString())
               .append("]\n");
      }
      */
      sb.append("}, immutable =").append(immutable);
      return sb.toString();
   }

   /* ---------------------------------------- */

   private void analyze()
   {

      /*
      // We intercept all fields now (instead of setter methods) so there is no need to
      // track the individual fields.
      HashMap attributes = new HashMap();
      Method[] methods = type.getMethods();
      for (int i = 0; i < methods.length; i++) {
         Method method = methods[i];
         if (isGet(method)) {
            CachedAttribute attribute =
                  getAttribute(method, attributes, true);
            attribute.setGet(method);
            attribute.setType(method.getReturnType());
         } else if (isSet(method)) {
            CachedAttribute attribute =
                  getAttribute(method, attributes, true);
            attribute.setSet(method);
            attribute.setType(method.getParameterTypes()[0]);
         }
      }
      this.setAttributes(new ArrayList(attributes.values()));
      */
      analyzeFields(getType());

      immediate = isImmediate(getType());

   }

   private void analyzeFields(Class clazz)
   {
      if (clazz == null)
      {
         return;
      }

      analyzeFields(clazz.getSuperclass());

      Field[] classFields = clazz.getDeclaredFields();
      for (int i = 0; i < classFields.length; i++)
      {
         Field f = classFields[i];
         if (isNonReplicable(f)) continue;

         PrivilegedCode.setAccessible(f);

         FieldPersistentReference persistentRef = new FieldPersistentReference(f, PersistentReference.REFERENCE_SOFT);

         fields.add(persistentRef);
         fieldMap.put(f.getName(), persistentRef);
       
         if (Modifier.isFinal(f.getModifiers()))
            finalFields.add(persistentRef);
      }
   }

   public static boolean isNonReplicable(Field field)
   {
      int mods = field.getModifiers();
      /**
       * The following modifiers are ignored in the cache, i.e., they will not be stored in the cache.
       * Whenever, user trying to access these fields, it will be accessed from the in-memory version.
       */
      return Modifier.isStatic(mods) || Modifier.isTransient(mods) ||
         field.isAnnotationPresent(org.jboss.cache.pojo.annotation.Transient.class);
   }

   public static boolean isSimpleAttribute(Field field)
   {
      return isImmediate(field.getType()) || field.isAnnotationPresent(org.jboss.cache.pojo.annotation.Serializable.class);
   }

   /*
    * converts a get/set method to an attribute name
    */
   protected static String attributeName(String methodName)
   {
      return methodName.substring(3, 4).toLowerCase()
              + methodName.substring(4);
   }

   protected static boolean isGet(Method method)
   {
      return method.getName().startsWith("get")
              && method.getParameterTypes().length == 0
              && method.getReturnType() != Void.TYPE;
   }

   protected static boolean isSet(Method method)
   {
      return method.getName().startsWith("set")
              && method.getParameterTypes().length == 1
              && method.getReturnType() == Void.TYPE;
   }

}// CachedType
