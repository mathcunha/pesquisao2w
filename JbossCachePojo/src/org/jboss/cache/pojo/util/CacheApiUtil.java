/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.util;

import java.util.Set;

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;

/**
 * Utility for the 2.0 Cache API
 *
 * @author Ben Wang
 * @version $Id: CacheApiUtil.java 6048 2008-06-26 02:21:26Z jason.greene@jboss.com $
 */
public class CacheApiUtil
{
   public static Set<Node> getNodeChildren(Cache<Object, Object> cache, Fqn fqn)
   {
      Node n = cache.getRoot().getChild(fqn);
      return n != null ? n.getChildren() : null;
   }
   
   
}
