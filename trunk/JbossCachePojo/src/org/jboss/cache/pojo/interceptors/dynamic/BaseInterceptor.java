/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.interceptors.dynamic;

import org.jboss.aop.advice.Interceptor;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.impl.PojoInstance;


/**
 * Base cache interceptor
 *
 * @author Ben Wang
 */

public interface BaseInterceptor
      extends Interceptor, Cloneable
{
   /**
    * Get the original fqn that is associated with this interceptor (or advisor).
    */
   Fqn getFqn();

   void setFqn(Fqn fqn);

   PojoInstance getAopInstance();

   void setAopInstance(PojoInstance pojoInstance);

   void setInterceptor(Interceptor interceptor);
}
