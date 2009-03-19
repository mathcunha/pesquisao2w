/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.jboss.aop.InstanceAdvisor;
import org.jboss.aop.advice.Interceptor;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.Reference;
import org.jboss.cache.pojo.collection.CachedArray;
import org.jboss.cache.pojo.collection.CachedArrayRegistry;
import org.jboss.cache.pojo.interceptors.PojoTxSynchronizationHandler;
import org.jboss.cache.pojo.interceptors.dynamic.AbstractCollectionInterceptor;
import org.jboss.cache.pojo.interceptors.dynamic.CacheFieldInterceptor;
import org.jboss.cache.pojo.util.MethodCall;

/**
 * Utility class for method wrappers that we are interested to rollback (i.e., rollback).
 *
 * @author Ben Wang
 * @version $Id: PojoUtil.java 6374 2008-07-23 05:01:02Z jason.greene@jboss.com $
 */
public class PojoUtil
{
   public void attachInterceptor(Object pojo, InstanceAdvisor advisor, Interceptor interceptor)
   {
      _attachInterceptor(pojo, advisor, interceptor);
      Method method = MethodDeclarations.undoAttachInterceptor;
      MethodCall mc = new MethodCall(method, new Object[] {pojo, advisor, interceptor}, this);
      addUndo(mc);
   }

   public void detachInterceptor(InstanceAdvisor advisor, Interceptor interceptor)
   {
      _detachInterceptor(advisor, interceptor);
      Method method = MethodDeclarations.undoDetachInterceptor;
      MethodCall mc = new MethodCall(method, new Object[] {advisor, interceptor}, this);
      addUndo(mc);
   }

   private void addUndo(MethodCall mc)
   {
      PojoTxSynchronizationHandler handler = PojoTxSynchronizationHandler.current();
      if (handler != null)
         handler.addToList(mc);
   }

   public void detachCollectionInterceptor(AbstractCollectionInterceptor interceptor) {
      interceptor.detach(true);
      Method method = MethodDeclarations.undoDetachCollectionInterceptor;
      MethodCall mc = new MethodCall(method, new Object[] {interceptor}, this);
      addUndo(mc);
   }

   public void attachArray(Object array, CachedArray cached) {
      CachedArrayRegistry.register(array, cached);
      Method method = MethodDeclarations.undoAttachArray;
      MethodCall mc = new MethodCall(method, new Object[] {array, cached}, this);
      addUndo(mc);
   }

   public void detachArray(Object array, CachedArray cached) {
      CachedArrayRegistry.unregister(array);
      Method method = MethodDeclarations.undoDetachArray;
      MethodCall mc = new MethodCall(method, new Object[] {array, cached}, this);
      addUndo(mc);
   }

   public void undoAttachInterceptor(Object pojo, InstanceAdvisor advisor, Interceptor interceptor)
   {
      _detachInterceptor(advisor, interceptor);
   }

   public void undoDetachInterceptor(InstanceAdvisor advisor, Interceptor interceptor)
   {
      Object pojo = ((CacheFieldInterceptor) interceptor).getAopInstance().get();
      if (pojo == null)
      {
         throw new PojoCacheException("PojoUtil.detachInterceptor(): null pojo");
      }

      _attachInterceptor(pojo, advisor, interceptor);
   }

   public void undoDetachCollectionInterceptor(AbstractCollectionInterceptor interceptor) {
      interceptor.attach(null, false);
   }

   public void undoAttachArray(Object array, CachedArray cached) {
      CachedArrayRegistry.unregister(array);
   }

   public void undoDetachArray(Object array, CachedArray cached) {
      CachedArrayRegistry.register(array, cached);
   }

   public void inMemorySubstitution(Object obj, Field field, Object newValue)
   {
      Method method = MethodDeclarations.undoInMemorySubstitution;
      Object[] args;
      try
      {
         args = new Object[]{obj, field, field.get(obj)};
      }
      catch (Throwable t)
      {
         throw new PojoCacheException("Severe error building undo list", t);
      }

      _inMemorySubstitution(obj, field, newValue);

      MethodCall mc = new MethodCall(method, args, this);
      addUndo(mc);
   }

   public void undoInMemorySubstitution(Object obj, Field field, Object oldValue)
   {
      _inMemorySubstitution(obj, field, oldValue);
   }

   private void _attachInterceptor(Object pojo, InstanceAdvisor advisor, Interceptor interceptor)
   {
      advisor.appendInterceptor(interceptor);
   }

   private void _inMemorySubstitution(Object obj, Field field, Object newValue)
   {
      try
      {
         field.set(obj, newValue);
      }
      catch (IllegalAccessException e)
      {
         throw new PojoCacheException(
               "PojoUtil.inMemorySubstitution(): Can't swap out the class of field \" " +
               "+field.getLastElementAsString()," + e);
      }
   }

   private void _detachInterceptor(InstanceAdvisor advisor, Interceptor interceptor)
   {
      advisor.removeInterceptor(interceptor.getName());
      // retrieve pojo
      Object pojo = ((CacheFieldInterceptor) interceptor).getAopInstance().get();

      if (pojo == null)
      {
         throw new PojoCacheException("PojoUtil.detachInterceptor(): null pojo");
      }
   }

   public int incrementReferenceCount(Reference reference, int count, Set<Reference> referencedBy_)
   {
      int ret = _incrementReferenceCount(reference, count, referencedBy_);
      Method method = MethodDeclarations.undoIncrementReferenceCount;
      Object[] args = new Object[]{reference, count, referencedBy_};
      MethodCall mc = new MethodCall(method, args, this);
      addUndo(mc);
      return ret;
   }

   public int undoIncrementReferenceCount(Reference reference, int count, Set<Reference> refList)
   {
      return _decrementReferenceCount(reference, count, refList);
   }

   private int _incrementReferenceCount(Reference reference, int count, Set<Reference> referencedBy_)
   {
      referencedBy_.add(reference);
      return count + 1;
   }

   public int decrementReferenceCount(Reference reference, int count, Set<Reference> referencedBy_)
   {
      int ret = _decrementReferenceCount(reference, count, referencedBy_);
      Method method = MethodDeclarations.undoDecrementReferenceCount;
      Object[] args = new Object[]{reference, count, referencedBy_};
      MethodCall mc = new MethodCall(method, args, this);
      addUndo(mc);
      return ret;
   }

   public int undoDecrementReferenceCount(Reference reference, int count, Set<Reference> refList)
   {
      return _incrementReferenceCount(reference, count, refList);
   }

   private int _decrementReferenceCount(Reference reference, int count, Set<Reference> referencedBy_)
   {
      referencedBy_.remove(reference);
      return count - 1;
   }
}
