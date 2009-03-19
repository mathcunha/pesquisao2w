/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annoataion that declares a field is transient (i.e., no-replicatable).
 *
 * @author Ben Wang
 *         Date: Jan 22, 2006
 * @version $Id: Transient.java 3411 2007-01-13 15:56:22Z bwang $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Transient
{
}
