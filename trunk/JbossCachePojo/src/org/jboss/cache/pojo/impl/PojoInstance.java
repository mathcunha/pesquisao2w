/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.cache.pojo.Reference;

/**
 * POJO class metadata information.
 * When an object is looked up or put in PojoCache, this object will be advised with a CacheFieldInterceptor.
 * The underlying cache stores a reference to this object (for example to update the instance variables, etc.).
 * Since this reference need to be transactional but never replicated (the reference is only valid
 * within the VM this reference is thus stored into an PojoReference (as a transient field).
 * In addition, this instance also serves as a metadata for PojoCache. E.g., it has a reference counting for
 * multiple references and reference FQN.
 *
 * @author Ben Wang
 */
public class PojoInstance implements Serializable // Externalizable is no more efficient
{
   //    protected static Log log=LogFactory.getLog(PojoReference.class.getLastElementAsString());
   public static final String KEY = InternalConstant.POJOCACHE_KEY_PREFIX + "PojoInstance";
   public static final int INITIAL_COUNTER_VALUE = -1;

   static final long serialVersionUID = 6492134565825613209L;

   // The instance is transient to avoid replication outside the VM
   private transient Object instance_;

   // Reference counting. THis will get replicated as well. This keep track of number of
   // other instances that referenced this fqn.
   private int refCount_ = INITIAL_COUNTER_VALUE;

   // List of fqns that reference this fqn.
   private final Set<Reference> referencedBy_ = new HashSet<Reference>(4);
   private Class<?> clazz_ = null;
   private transient PojoUtil util_ = new PojoUtil();

   public PojoInstance()
   {
   }

   public PojoInstance(Object instance)
   {
      set(instance);
   }

   public void setPojoClass(Class clazz)
   {
      clazz_ = clazz;
   }

   public Class getPojoClass()
   {
      return clazz_;
   }

   public Object get()
   {
      return instance_;
   }

   public void set(Object instance)
   {
      instance_ = instance;
   }

   synchronized public int incrementRefCount(Reference reference)
   {
      if (reference == null || reference.getFqn() == null)
      {
         throw new IllegalStateException("PojoInstance.incrementRefCount(): null sourceFqn");
      }

      if (util_ == null) util_ = new PojoUtil();
      refCount_ = util_.incrementReferenceCount(reference, refCount_, referencedBy_);
      return refCount_;
   }

   synchronized public int decrementRefCount(Reference reference)
   {
      if (reference == null || reference.getFqn() == null)
      {
         throw new IllegalStateException("PojoInstance.incrementRefCount(): null sourceFqn");
      }

      if (!referencedBy_.contains(reference))
         throw new IllegalStateException("PojoReference.decrementRefCount(): reference: " +
                                         reference + " is not present.");

      if (util_ == null) util_ = new PojoUtil();
      refCount_ = util_.decrementReferenceCount(reference, refCount_, referencedBy_);
      return refCount_;
   }

   synchronized public int getRefCount()
   {
      return refCount_;
   }

   public synchronized Collection<Reference> getReferences()
   {
      return Collections.unmodifiableCollection(new HashSet<Reference>(referencedBy_));
   }

   public String toString()
   {
      return "PI[ref=" + refCount_ + " class=" + clazz_.getName() + "]";
   }
}
