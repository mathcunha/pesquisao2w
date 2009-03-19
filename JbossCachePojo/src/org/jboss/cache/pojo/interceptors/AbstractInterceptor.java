/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.InvocationContext;
import org.jboss.cache.pojo.impl.PojoCacheImpl;

/**
 * Base interceptor class for PojoCache interceptor stack.
 *
 * @author Ben Wang
 * @version $Id: AbstractInterceptor.java 6747 2008-09-17 23:24:20Z jason.greene@jboss.com $
 */
public abstract class AbstractInterceptor implements Interceptor
{
   protected final Log log = LogFactory.getLog(AbstractInterceptor.this.getClass());

   protected InvocationContext getInvocationContext(MethodInvocation in)
   {
      return ((PojoCacheImpl) in.getTargetObject()).getCacheSPI().getInvocationContext();
   }

   protected CacheSPI getCache(MethodInvocation in)
   {
      return ((PojoCacheImpl) in.getTargetObject()).getCacheSPI();
   }

   public String getName()
   {
      return this.getClass().getName();
   }
}
