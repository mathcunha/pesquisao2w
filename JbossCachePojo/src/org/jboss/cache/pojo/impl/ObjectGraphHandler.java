/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;

/**
 * Handle the object graph management.
 *
 * @author Ben Wang
 *         Date: Aug 4, 2005
 * @version $Id: ObjectGraphHandler.java 7052 2008-11-02 18:13:25Z bstansberry@jboss.com $
 */
class ObjectGraphHandler extends AbstractHandler
{
   private PojoCacheImpl cache;
   private InternalHelper internal_;
   private final static Log log = LogFactory.getLog(ObjectGraphHandler.class);

   public ObjectGraphHandler(PojoCacheImpl cache, InternalHelper internal)
   {
      this.cache = cache;
      internal_ = internal;
   }

   protected Fqn<?> getFqn(Object obj)
   {
      return null;
   }

   protected boolean handles(Class<?> clazz)
   {
      return false;
   }

   @Override
   protected Object get(Fqn<?> fqn, Class<?> clazz, PojoInstance pojoInstance) throws CacheException
   {
      // Note this is actually the aliasFqn, not the real fqn!
      Object obj;

      obj = cache.find(fqn);
      if (obj == null)
         throw new PojoCacheException("ObjectGraphHandler.get(): null object from internal ref node." +
                                      " Internal ref node: " + fqn);

      return obj; // No need to set the instance under fqn. It is located in refFqn anyway.
   }

   @Override
   protected void put(Fqn<?> fqn, Reference reference, Object obj) throws CacheException
   {
      setupRefCounting(fqn, reference);
   }

   boolean isMultipleReferenced(Fqn<?> internalFqn)
   {
      // Note this is actually the aliasFqn, not the real fqn!
      PojoInstance pojoInstance = null;
      try
      {
         pojoInstance = internal_.getPojoInstance(internalFqn);
      }
      catch (CacheException e)
      {
         throw new PojoCacheException("Exception in isMultipleReferenced", e);
      }
      // check if this is a refernce
      return InternalHelper.isMultipleReferenced(pojoInstance);

   }

   @Override
   protected Object remove(Fqn<?> fqn, Reference reference, Object pojo)
         throws CacheException
   {
      if (log.isTraceEnabled())
      {
         log.trace("remove(): removing object fqn: " + reference
                   + " Will just de-reference it.");
      }
      removeFromReference(fqn, reference);

      return null;
   }

   /**
    * Remove the object from the the reference fqn, meaning just decrement the ref counter.
    */
   private void removeFromReference(Fqn<?> originalFqn, Reference reference) throws CacheException
   {
      if (decrementRefCount(originalFqn, reference) == PojoInstance.INITIAL_COUNTER_VALUE)
      {
         // No one is referring it so it is safe to remove
         // TODO we should make sure the parent nodes are also removed they are empty as well.
         cache.detach(originalFqn);
      }
   }

   /**
    * 1. increment reference counter
    * 2. put in refFqn so we can get it.
    *
    * @param fqn    The original fqn node
    * @param refFqn The new internal fqn node
    */
   private void setupRefCounting(Fqn<?> fqn, Reference reference) throws CacheException
   {
      // increment the reference counting
      incrementRefCount(fqn, reference);
   }

   private int incrementRefCount(Fqn<?> originalFqn, Reference reference) throws CacheException
   {
      return internal_.incrementRefCount(originalFqn, reference);
   }

   private int decrementRefCount(Fqn<?> originalFqn, Reference reference) throws CacheException
   {
      return internal_.decrementRefCount(originalFqn, reference);
   }
}
