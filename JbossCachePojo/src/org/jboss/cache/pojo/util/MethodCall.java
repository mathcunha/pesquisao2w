package org.jboss.cache.pojo.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A special MethodCall object to wrap around the rollback method call. No Serializable is required.
 *
 * @author Ben Wang
 * @version $Revision: 3411 $
 */
public class MethodCall
{

   /**
    * The name of the method, case sensitive.
    */
   protected String method_name = null;

   /**
    * The arguments of the method.
    */
   protected Object[] args = null;

   /**
    * The class types, e.g., new Class[]{String.class, int.class}.
    */
   protected Class[] types = null;

   /**
    * The signature, e.g., new String[]{String.class.getLastElementAsString(), int.class.getLastElementAsString()}.
    */
   protected String[] signature = null;

   /**
    * The Method of the call.
    */
   protected Method method = null;

   /*
   * The target object to invoke upon.
   */
   protected Object target = null;

   protected static final Log log = LogFactory.getLog(MethodCall.class);

   public MethodCall(Method method, Object[] arguments, Object target)
   {
      init(method);
      if (arguments != null) args = arguments;
      this.target = target;
   }

   private void init(Method method)
   {
      this.method = method;
      method_name = method.getName();
   }

   public String getName()
   {
      return method_name;
   }

   public void setName(String n)
   {
      method_name = n;
   }

   public Object[] getArgs()
   {
      return args;
   }

   public void setArgs(Object[] args)
   {
      if (args != null)
         this.args = args;
   }

   public Method getMethod()
   {
      return method;
   }

   public void setMethod(Method m)
   {
      init(m);
   }

   Method findMethod(Class target_class) throws Exception
   {
      int len = args != null ? args.length : 0;
      Method m;

      Method[] methods = getAllMethods(target_class);
      for (int i = 0; i < methods.length; i++)
      {
         m = methods[i];
         if (m.getName().equals(method_name))
         {
            if (m.getParameterTypes().length == len)
               return m;
         }
      }

      return null;
   }

   Method[] getAllMethods(Class target)
   {

      Class superclass = target;
      List methods = new ArrayList();
      int size = 0;

      while (superclass != null)
      {
         Method[] m = superclass.getDeclaredMethods();
         methods.add(m);
         size += m.length;
         superclass = superclass.getSuperclass();
      }

      Method[] result = new Method[size];
      int index = 0;
      for (Iterator i = methods.iterator(); i.hasNext();)
      {
         Method[] m = (Method[]) i.next();
         System.arraycopy(m, 0, result, index, m.length);
         index += m.length;
      }
      return result;
   }

   /**
    * Returns the first method that matches the specified name and parameter types. The overriding
    * methods have priority. The method is chosen from all the methods of the current class and all
    * its superclasses and superinterfaces.
    *
    * @return the matching method or null if no mathching method has been found.
    */
   Method getMethod(Class target, String methodName, Class[] types)
   {

      if (types == null)
      {
         types = new Class[0];
      }

      Method[] methods = getAllMethods(target);
      methods:
      for (int i = 0; i < methods.length; i++)
      {
         Method m = methods[i];
         if (!methodName.equals(m.getName()))
         {
            continue;
         }
         Class[] parameters = m.getParameterTypes();
         if (types.length != parameters.length)
         {
            continue;
         }
         for (int j = 0; j < types.length; j++)
         {
            if (!types[j].equals(parameters[j]))
            {
               continue methods;
            }
         }
         return m;
      }
      return null;
   }

   public Object invoke() throws Throwable
   {
      return this.invoke(this.target);
   }

   /**
    * Invokes the method with the supplied arguments against the target object.
    * If a method lookup is provided, it will be used. Otherwise, the default
    * method lookup will be used.
    *
    * @param target - the object that you want to invoke the method on
    * @return an object
    */
   protected Object invoke(Object target) throws Throwable
   {
      Class cl;
      Method meth = null;
      Object retval = null;


      if (method_name == null || target == null)
      {
         if (log.isErrorEnabled()) log.error("method name or target is null");
         return null;
      }
      cl = target.getClass();
      try
      {
         if (this.method != null)
            meth = this.method;

         if (meth != null)
         {
            retval = meth.invoke(target, args);
         }
         else
         {
            throw new NoSuchMethodException(method_name);
         }
         return retval;
      }
      catch (InvocationTargetException inv_ex)
      {
         throw inv_ex.getTargetException();
      }
      catch (NoSuchMethodException no)
      {
         StringBuffer sb = new StringBuffer();
         sb.append("found no method called ").append(method_name).append(" in class ");
         sb.append(cl.getName()).append(" with (");
         if (args != null)
         {
            for (int i = 0; i < args.length; i++)
            {
               if (i > 0)
                  sb.append(", ");
               sb.append((args[i] != null) ? args[i].getClass().getName() : "null");
            }
         }
         sb.append(") formal parameters");
         log.error(sb.toString());
         throw no;
      }
      catch (Throwable e)
      {
         // e.printStackTrace(System.err);
         if (log.isErrorEnabled()) log.error("exception in invoke()", e);
         throw e;
      }
   }

   public Object invoke(Object target, Object[] args) throws Throwable
   {
      if (args != null)
         this.args = args;
      return invoke(target);
   }


   Class[] getTypesFromString(Class cl, String[] signature) throws Exception
   {
      String name;
      Class parameter;
      Class[] mytypes = new Class[signature.length];

      for (int i = 0; i < signature.length; i++)
      {
         name = signature[i];
         if ("long".equals(name))
            parameter = long.class;
         else if ("int".equals(name))
            parameter = int.class;
         else if ("short".equals(name))
            parameter = short.class;
         else if ("char".equals(name))
            parameter = char.class;
         else if ("byte".equals(name))
            parameter = byte.class;
         else if ("float".equals(name))
            parameter = float.class;
         else if ("double".equals(name))
            parameter = double.class;
         else if ("boolean".equals(name))
            parameter = boolean.class;
         else
            parameter = Class.forName(name, false, cl.getClassLoader());
         mytypes[i] = parameter;
      }
      return mytypes;
   }


   public String toString()
   {
      StringBuffer ret = new StringBuffer();
      boolean first = true;
      if (method_name != null)
         ret.append(method_name);
      ret.append('(');
      if (args != null)
      {
         for (int i = 0; i < args.length; i++)
         {
            if (first)
               first = false;
            else
               ret.append(", ");
            ret.append(args[i]);
         }
      }
      ret.append(')');
      return ret.toString();
   }

   public String toStringDetails()
   {
      StringBuffer ret = new StringBuffer();
      ret.append("MethodCall ");
      if (method_name != null)
         ret.append("name=").append(method_name);
      ret.append(", number of args=").append((args != null ? args.length : 0)).append(')');
      if (args != null)
      {
         ret.append("\nArgs:");
         for (int i = 0; i < args.length; i++)
         {
            ret.append("\n[").append(args[i]).append(" (").
                    append((args[i] != null ? args[i].getClass().getName() : "null")).append(")]");
         }
      }
      return ret.toString();
   }

}

