package org.jboss.cache.pojo.util;

import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Performs privileged actions
 *
 * @author Jason T. Greene
 */
public class PrivilegedCode
{
   public static void setAccessible(final AccessibleObject object)
   {
      if (object.isAccessible())
         return;

      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            object.setAccessible(true);
            return null;
         }
      });
   }
}
