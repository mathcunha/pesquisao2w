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

package org.jboss.cache.pojo;

import org.jboss.cache.Fqn;

/**
 * A reference to an attached object. This class represents both normal Fqn aliases, and
 * references from other attached objects.
 *
 * @author Dan Berindei <dan.berindei@gmail.com>
 */
public interface Reference
{
   /**
    * Returns the Fqn of the referring node. Cannot be <code>null</code>.
    *
    * @return <code>Fqn</code> of the referring node.
    */
   public Fqn<?> getFqn();

   /**
    * Returns the name of the node key which references the attached object, or null
    * if the Fqn is a normal alias to the internal node. If there is a key, then this is
    * typically a field name or collection index.
    *
    * @return Name of the field or key/index in the collection that is containing the reference.
    */
   public String getKey();
}