/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.cache.pojo.notification.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.notification.event.ArrayModifiedEvent;

// $Id: ArrayModified.java 4101 2007-07-03 01:44:37Z jgreene $

/**
 * <p>
 * Indicates that the marked method should be called when an Array is modified.
 * <b>NOTE: This is event not currently implemented yet</b>
 *
 * <p>
 * The method can have any name, but should take a single parameter which is
 * either a {@link ArrayModifiedEvent} or any superclass. Otherwise an
 * {@link IllegalArgumentException} will be thrown by
 * {@link PojoCache#addListener(java.lang.Object)}
 *
 * <p>
 * Example:
 *
 * <pre>
 *    &#064;PojoCacheListener
 *    public class MyListener()
 *    {
 *       &#064;ArrayModified
 *       public void handleArray(ArrayModifiedEvent event)
 *       {
 *          System.out.println(&quot;Object = &quot; + even.getSource());
 *       }
 *    }
 * </pre>
 *
 * @author Jason T. Greene
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ArrayModified
{
}