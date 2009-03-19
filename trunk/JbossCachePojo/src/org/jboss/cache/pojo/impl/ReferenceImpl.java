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

package org.jboss.cache.pojo.impl;

import java.io.Serializable;

import net.jcip.annotations.Immutable;

import org.jboss.cache.Fqn;
import org.jboss.cache.pojo.Reference;

/**
 * A reference from an attached object to another attached object. This class also contains the name
 * of the field that contains the reference.
 *
 * @author Dan Berindei <dan.berindei@gmail.com>
 */
@Immutable
public final class ReferenceImpl implements Reference, Serializable
{
   private static final long serialVersionUID = 2647262858847953704L;

   private Fqn<?> fqn;
   private String key;

   public ReferenceImpl(Fqn<?> fqn)
   {
      this(fqn, null);
   }

   /**
    * @param fqn <code>Fqn</code> of the referring node. Cannot be <code>null</code>.
    * @param key Name of the field, index in the field or key in the collection that is containing the reference.
    */
   public ReferenceImpl(Fqn<?> fqn, String key)
   {
      if (fqn == null)
         throw new IllegalArgumentException("Fqn can not be null!!");

      this.fqn = fqn;
      this.key = key;
   }

   public String getKey()
   {
      return key;
   }

   public Fqn<?> getFqn()
   {
      return fqn;
   }

   private boolean equals(Object o1, Object o2)
   {
      if (o1 == o2)
         return true;

      if (o1 != null && o1.equals(o2))
         return true;

      return false;
   }

   @Override
   public int hashCode()
   {
      int result = 629 * fqn.hashCode();

      if (key != null)
         result = 37 * result + key.hashCode();

      return result;
   }

   @Override
   public boolean equals(Object o)
   {
      if (o instanceof Reference)
         return equals(((ReferenceImpl) o).fqn, fqn) && equals(((ReferenceImpl) o).key, key);

      return false;
   }

   @Override
   public String toString()
   {
      return "Reference[fqn=" + fqn + " field=" + key + "]";
   }
}