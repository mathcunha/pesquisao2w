/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.cache.config;

/**
 * Thrown if an eviction configuration that cannot be translated to the new 3.x eviction interfaces is used.
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 3.0
 */
public class UnsupportedEvictionImplException extends ConfigurationException
{
   public UnsupportedEvictionImplException(Exception e)
   {
      super(e);
   }

   public UnsupportedEvictionImplException(String string)
   {
      super(string);
   }

   public UnsupportedEvictionImplException(String string, String erroneousAttribute)
   {
      super(string, erroneousAttribute);
   }

   public UnsupportedEvictionImplException(String string, Throwable throwable)
   {
      super(string, throwable);
   }
}
