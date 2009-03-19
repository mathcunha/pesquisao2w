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

import org.jboss.cache.factories.annotations.DefaultFactoryFor;
import org.jboss.cache.statetransfer.DefaultStateTransferGenerator;
import org.jboss.cache.statetransfer.DefaultStateTransferIntegrator;
import org.jboss.cache.statetransfer.LegacyStateTransferGenerator;
import org.jboss.cache.statetransfer.LegacyStateTransferIntegrator;
import org.jboss.cache.statetransfer.StateTransferGenerator;
import org.jboss.cache.statetransfer.StateTransferIntegrator;

/**
 * Factory class able to create {@link org.jboss.cache.statetransfer.StateTransferGenerator} and
 * {@link org.jboss.cache.statetransfer.StateTransferIntegrator} instances.
 * <p/>
 * Updated in 3.0.0 to extend ComponentFactory, etc.
 * <p/>
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @author Manik Surtani
 * @version $Revision: 7168 $
 */
@DefaultFactoryFor(classes = {StateTransferGenerator.class, StateTransferIntegrator.class})
public class StateTransferFactory extends ComponentFactory
{
   @SuppressWarnings("deprecation")
   protected <T> T construct(Class<T> componentType)
   {
      if (componentType.equals(StateTransferIntegrator.class))
      {
         switch (configuration.getNodeLockingScheme())
         {
            case MVCC:
               return componentType.cast(new DefaultStateTransferIntegrator());
            default:
               return componentType.cast(new LegacyStateTransferIntegrator());
         }
      }
      else
      {
         switch (configuration.getNodeLockingScheme())
         {
            case MVCC:
               return componentType.cast(new DefaultStateTransferGenerator());
            default:
               return componentType.cast(new LegacyStateTransferGenerator());
         }
      }
   }
}
