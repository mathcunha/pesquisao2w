/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.impl;

import org.jboss.cache.Fqn;

/**
 * Internal helper class to handle internal cache sotre, that is, the portion that is not part of
 * user's data.
 *
 * @author Ben Wang
 */
public class InternalConstant
{
   public static final String CLASS_INTERNAL = "__jboss:internal:class__";
   public static final String SERIALIZED = "__SERIALIZED__";
   public static final String JBOSS_INTERNAL_STRING = "__JBossInternal__";
   public static final String JBOSS_INTERNAL_ID_SEP_STRING = "_ID_";
   public static final Fqn<String> JBOSS_INTERNAL = new Fqn<String>(JBOSS_INTERNAL_STRING);
   public static final Fqn<String> JBOSS_INTERNAL_ID_SEP = new Fqn<String>(JBOSS_INTERNAL_ID_SEP_STRING);
   public static final Fqn<String> JBOSS_INTERNAL_MAP = new Fqn<String>(InternalConstant.JBOSS_INTERNAL, "__RefMap__");
   public static final String JBOSS_INTERNAL_STATIC = "__jboss:static__";
   public static final String POJOCACHE_KEY_PREFIX = "POJOCache.";
   public static final String POJOCACHE_STATUS = POJOCACHE_KEY_PREFIX + "Status";
   public static final String POJOCACHE_OPERATION = POJOCACHE_KEY_PREFIX + "Operation";
   public static final String POJOCACHE_LOCK = POJOCACHE_KEY_PREFIX + "LOCK";
}
