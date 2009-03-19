/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo;

import org.w3c.dom.Element;


/**
 * MBean interface for PojoCache.
 *
 * @author Ben Wang
 * @since 1.4
 */
public interface PojoCacheMBean extends PojoCache
{
   /**
    * Inject the config element that is specific to PojoCache.
    *
    * @param config
    */
   public void setPojoCacheConfig(Element config);

   public Element getPojoCacheConfig();

}
