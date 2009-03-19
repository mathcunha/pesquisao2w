/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo;

import org.jboss.cache.config.Configuration;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

/**
 * Factory method to create a PojoCache instance.
 *
 * @version $Id: PojoCacheFactory.java 3429 2007-01-18 16:55:20Z msurtani $
 */
public class PojoCacheFactory
{
   /**
    * Create a PojoCache instance. Note that this will start the cache life cycle automatically.
    *
    * @param config A configuration string that represents the file name that is used to
    *               setCache the underlying Cache instance.
    * @return PojoCache
    */
   public static PojoCache createCache(String config)
   {
      return new PojoCacheImpl(config, true);
   }

   /**
    * Create a PojoCache instance.
    *
    * @param config A configuration string that represents the file name that is used to
    *               setCache the underlying Cache instance.
    * @param start  If true, it will start the cache life cycle.
    * @return PojoCache
    */
   public static PojoCache createCache(String config, boolean start)
   {
      return new PojoCacheImpl(config, start);
   }

   /**
    * Create a PojoCache instance.
    *
    * @param config A configuration object that is used to setCache the underlying Cache instance.
    * @param start  If true, it will start the cache life cycle.
    * @return PojoCache
    */
   public static PojoCache createCache(Configuration config, boolean start)
   {
      return new PojoCacheImpl(config, start);
   }
}
