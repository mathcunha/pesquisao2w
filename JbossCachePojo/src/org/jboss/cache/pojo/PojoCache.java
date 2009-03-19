/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.cache.pojo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.annotation.Replicable;
import org.jboss.cache.pojo.notification.annotation.PojoCacheListener;

/**
 * <p>Main PojoCache APIs. PojoCache is an in-memory, transactional, fine-grained, and
 * object-oriented POJO (plain old Java object) distributed cache system. It
 * differs from the traditional generic distributed cache library by operating on the
 * POJO level directly without requiring that object to be serializable. It can preserve
 * object graph relationship during replication or persistency. It also track the
 * replication via fine-grained maner, i.e., only modified fields are replicated.</p>
 *
 * @author Ben Wang
 * @author Jason T. Greene
 * @since 2.0
 */
public interface PojoCache
{
   /**
    * <p>Attach a POJO into PojoCache. It will also recursively put any
    * sub-POJO into the cache system. A POJO can be the following and have the
    * consqeuences when attached:</p> <p/> <li>it is Replicable, that is, it
    * has been annotated with {@link Replicable} annotation (or via XML),
    * and has
    * been "instrumented" either compile- or load-time. The POJO will be mapped
    * recursively to the system and fine-grained replication will be
    * performed.</li> <li>It is Serializable. The POJO will still be stored in
    * the cache system. However, it is treated as an "opaque" object per se.
    * That is, the POJO will neither be intercepted
    * (for fine-grained operation) or object relantionship will be
    * maintained.</li>
    * <li>Neither of above. In this case, a user can specify whether it wants
    * this POJO to be stored (e.g., replicated or persistent). If not, a
    * PojoCacheException will be thrown.</li>
    *
    * @param id   An id String to identify the object in the cache. To promote
    *             concurrency, we recommend the use of hierarchical String separating by a
    *             designated separator. Default is "/" but it can be set differently via a
    *             System property, jbosscache.separator in the future release. E.g., "/ben",
    *             or "/student/joe", etc.
    * @param pojo object to be inerted into the cache. If null, it will nullify
    *             the fqn node.
    * @return Existing POJO or null if there is none.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object attach(String id, Object pojo) throws PojoCacheException;

   /**
    * <p>Attach a POJO into PojoCache. It will also recursively put any
    * sub-POJO into the cache system. A POJO can be the following and have the
    * consequences when attached:</p> <p/> <li>it is Replicable, that is, it
    * has been annotated with {@link Replicable} annotation (or via XML),
    * and has
    * been "instrumented" either compile- or load-time. The POJO will be mapped
    * recursively to the system and fine-grained replication will be
    * performed.</li> <li>It is Serializable. The POJO will still be stored in
    * the cache system. However, it is treated as an "opaque" object per se.
    * That is, the POJO will neither be intercepted
    * (for fine-grained operation) or object relationship will be
    * maintained.</li>
    * <li>Neither of above. In this case, a user can specify whether it wants
    * this POJO to be stored (e.g., replicated or persistent). If not, a
    * PojoCacheException will be thrown.</li>
    *
    * @since 2.1
    * @param id   the Fqn that specifies the location in the cache to attach the object
    * @param pojo object to be inserted into the cache. If null, it will nullify
    *             the fqn node.
    * @return Existing POJO or null if there is none.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object attach(Fqn<?> id, Object pojo) throws PojoCacheException;

   /**
    * Remove POJO object from the cache.
    *
    * @param id Is string that associates with this node.
    * @return Original value object from this node.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object detach(String id) throws PojoCacheException;

   /**
    * Remove POJO object from the cache.
    *
    * @since 2.1
    * @param id location of the object to remove
    * @return Original value object from this node.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object detach(Fqn<?> id) throws PojoCacheException;

  /**
   * Return the <code>Fqn</code> of the internal node containing the data of this attached object.
   * <p>
   * Note that if the object is not attached to the cache system, this method will simply return
   * null. Same for any object of an immediate type (primitive wrapper types, String, or Class) or
   * of any <code>Serializable</code> types.
   * </p>
   *
   * @param object Any object.
   * @return <code>Fqn</code> of the internal data node. <code>null</code> if the object is
   *         immediate, serializable, or not in the cache.
   */
  Fqn<?> getInternalFqn(Object object);

  /**
   * Return a list of the references from attached objects to this object. For each reference it
   * returns a {@link Reference} object containing the <code>Fqn</code> of the referrer object and
   * the name of the field that contains the reference.
   * <p>
   * If the node is not attached to the cache, this method will return an empty list. Same for any
   * object of an immediate type (primitive wrapper types, String, or Class) or of any
   * <code>Serializable</code> types.
   * </p>
   * <p>
   * For external references (i.e. when the object was directly attached to the cache by user code)
   * the <code>Reference.fieldName</code> property is <code>null</code>. Otherwise it is the name
   * of the field that contains the reference to this object.
   * </p>
   *
   * @param object Any object.
   * @return Collection of internal references to the given object. Empty collection if the object is
   *         immediate, serializable, or not in the cache.
   */
   Collection<Reference> getReferences(Object object);

   /**
    * Determines if an object is attached at a particular location. This is somewhat less expensive
    * than find() because an object is not created, and internal reference links are not traversed.
    *
    * @since 2.1
    * @param id the location in the cache to examine
    * @return true if an attached object exists, false if not
    */
   boolean exists(Fqn<?> id);

   /**
    * Retrieve POJO from the cache system. Return null if object does not exist in the cache.
    * Note that this operation is fast if there is already a POJO instance attached to the cache.
    *
    * @param id that associates with this node.
    * @return Current content value. Null if does not exist.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object find(String id) throws PojoCacheException;

   /**
    * Retrieve POJO from the cache system. Return null if object does not exist in the cache.
    * Note that this operation is fast if there is already a POJO instance attached to the cache.
    *
    * @since 2.1
    * @param id that associates with this node.
    * @return Current content value. Null if does not exist.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Object find(Fqn<?> id) throws PojoCacheException;

   /**
    * Query all managed POJO objects under the id recursively. Note that this will not return
    * the sub-object POJOs, e.g., if <em>Person</em> has a sub-object of <em>Address</em>, it
    * won't return <em>Address</em> pojo. Also note also that this operation is not thread-safe
    * now. In addition, it assumes that once a POJO is found with a id, no more POJO is stored
    * under the children of the id. That is, we don't mix the id with different POJOs.
    *
    * @param id The starting place to find all POJOs.
    * @return Map of all POJOs found with (id, POJO) pair. Return size of 0, if not found.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Map<Fqn<?>, Object> findAll(String id) throws PojoCacheException;

   /**
    * Query all managed POJO objects under the id recursively. Note that this will not return
    * the sub-object POJOs, e.g., if <em>Person</em> has a sub-object of <em>Address</em>, it
    * won't return <em>Address</em> pojo. Also note also that this operation is not thread-safe
    * now. In addition, it assumes that once a POJO is found with a id, no more POJO is stored
    * under the children of the id. That is, we don't mix the id with different POJOs.
    *
    * @since 2.1
    * @param id The starting place to find all POJOs.
    * @return Map of all POJOs found with (id, POJO) pair. Return size of 0, if not found.
    * @throws PojoCacheException Throws if there is an error related to the cache operation.
    */
   Map<Fqn<?>, Object> findAll(Fqn<?> id) throws PojoCacheException;

   /**
    * Lifecycle method to start PojoCache.
    *
    * @throws PojoCacheException
    */
   void create() throws PojoCacheException;

   /**
    * Lifecycle method to start PojoCache.
    *
    * @throws PojoCacheException
    */
   void start() throws PojoCacheException;

   /**
    * Lifecycle method to stop PojoCache. Note that PojoCache can be stopped and started
    * repeatedly.
    *
    * @throws PojoCacheException
    */
   void stop() throws PojoCacheException;

   /**
    * Lifecycle method to destroy PojoCache.
    *
    * @throws PojoCacheException
    */
   void destroy() throws PojoCacheException;

   /**
    * <p>
    * Add a PojoCache listener. A given listener instance can only be added once.
    * To have a duplicate listener simply create a new instance.
    *
    * <p>
    * The listener must be annotated with the {@link PojoCacheListener} annotation, and
    * all callback methods need to be annotated with the respective event annotations.
    * Otherwise, an exception will be thrown.
    *
    * @param listener the listener instance to register
    * @throws IllegalArgumentException if listener does not conform to annotation requirements
    * @see PojoCacheListener for examples
    */
   void addListener(Object listener);

   /**
    * <p>
    * Add a PojoCache listener that will only monitor a specific ID(FQN) pattern.
    * A given listener instance can only be added once, whether or not there is
    * a pattern. To have a duplicate listener simply create a new instance.
    *
    * <p>
    * The listener must be annotated with the {@link PojoCacheListener} annotation, and
    * all callback methods need to be annotated with the respective event annotations.
    * Otherwise, an exception will be thrown.
    *
    * @param listener the listener instance to register
    * @param pattern the ID pattern for notifications of interest
    * @throws IllegalArgumentException if listener does not conform to annotation requirements
    * @see PojoCacheListener for examples
    */
   void addListener(Object listener, Pattern pattern);

   /**
    * Retrieve a read-only list of listeners.
    */
   Collection<Object> getListeners();

   /**
    * Remove the specific listener.
    *
    * @param listener the listener to remove
    */
   void removeListener(Object listener);
   
   /**
    * Get's the thread context for all POJO Cache operations.
    * 
    * @return the current thread's context
    * @since 2.1
    */
   PojoCacheThreadContext getThreadContext();

   /**
    * Obtain the underlying generic cache system. Use this for non-POJO cache operation, e.g.
    */
   Cache<Object, Object> getCache();
}
