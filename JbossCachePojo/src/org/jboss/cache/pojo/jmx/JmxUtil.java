/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.jmx;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;

/**
 * Various JMX related utilities
 *
 * @author Ben Wang
 * @version $Id: JmxUtil.java 3892 2007-05-23 10:28:59Z msurtani $
 */
public class JmxUtil extends org.jboss.cache.jmx.JmxUtil
{
   public static final String POJO_CACHE_DOMAIN = "jboss.pojocache";
   public static final String POJO_CACHE_TYPE = "PojoCache";

   public static void registerPojoCache(MBeanServer server, PojoCacheJmxWrapperMBean cache, String objectName)
         throws Exception
   {
      if (server == null || cache == null || objectName == null)
         return;
      ObjectName tmpObj = new ObjectName(objectName);
      if (!server.isRegistered(tmpObj))
         server.registerMBean(cache, tmpObj);

   }

   public static ObjectName getPlainCacheObjectName(ObjectName pojoCacheName)
         throws MalformedObjectNameException
   {
      String domain = pojoCacheName.getDomain();
      Hashtable attributes = new Hashtable(pojoCacheName.getKeyPropertyList());
      Object type = attributes.get(CACHE_TYPE_KEY);
      if (type == null || POJO_CACHE_TYPE.equals(type))
      {
         attributes.put(CACHE_TYPE_KEY, PLAIN_CACHE_TYPE);
      }
      else
      {
         attributes.put(UNIQUE_ID_KEY, String.valueOf(System.currentTimeMillis()));
      }
      return new ObjectName(domain, attributes);
   }

   public static void unregisterPojoCache(MBeanServer server, String objectName)
         throws Exception
   {
      if (server == null || objectName == null)
         return;

      ObjectName on = new ObjectName(objectName);
      if (server.isRegistered(on))
         server.unregisterMBean(on);

   }
}