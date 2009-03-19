/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.impl;

import org.jboss.cache.Fqn;

import java.io.Serializable;

/**
 * POJO reference that contains the information to point to the real POJO storage.
 *
 * @author Ben Wang
 */
public class PojoReference implements Serializable // Externalizable is no more efficient
{
   //    protected static Log log=LogFactory.getLog(PojoReference.class.getLastElementAsString());
   public static final String KEY = InternalConstant.POJOCACHE_KEY_PREFIX + "PojoReference";
   static final long serialVersionUID = 6492134565825613209L;
   // If not null, it signifies that this is a reference that points to this fqn.
   // Note that this will get replicated.
   private Fqn internalFqn_ = null;
   private Class clazz_ = null;

   public PojoReference()
   {
   }

   public void setFqn(Fqn fqn)
   {
      internalFqn_ = fqn;
   }

   public Fqn getFqn()
   {
      return internalFqn_;
   }

   public void setPojoClass(Class clazz)
   {
      clazz_ = clazz;
   }

   public Class getPojoClass()
   {
      return clazz_;
   }

   public String toString()
   {
      StringBuffer buf = new StringBuffer();
      buf.append("Internal Fqn --> ").append(internalFqn_);
      return buf.toString();
   }
}
