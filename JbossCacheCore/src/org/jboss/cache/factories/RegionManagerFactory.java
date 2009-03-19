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
package org.jboss.cache.factories;

import org.jboss.cache.LegacyRegionManagerImpl;
import org.jboss.cache.RegionManager;
import org.jboss.cache.RegionManagerImpl;
import org.jboss.cache.factories.annotations.DefaultFactoryFor;

/**
 * Creates region managers
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 3.0
 */
@DefaultFactoryFor(classes = RegionManager.class)
public class RegionManagerFactory extends ComponentFactory
{
   @SuppressWarnings("deprecation")
   protected <T> T construct(Class<T> componentType)
   {
      switch (configuration.getNodeLockingScheme())
      {
         case MVCC:
            return componentType.cast(new RegionManagerImpl());
         default:
            return componentType.cast(new LegacyRegionManagerImpl());
      }
   }
}