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

import org.jboss.cache.config.ConfigurationException;
import org.jboss.cache.factories.annotations.DefaultFactoryFor;
import org.jboss.cache.transaction.BatchModeTransactionManager;
import org.jboss.cache.transaction.TransactionManagerLookup;

import javax.transaction.TransactionManager;

/**
 * Uses a number of mechanisms to retrieve a transaction manager.
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 2.1.0
 */
@DefaultFactoryFor(classes = {TransactionManager.class})
public class TransactionManagerFactory extends ComponentFactory
{
   protected <T> T construct(Class<T> componentType)
   {
      // See if we had a TransactionManager injected into our config
      TransactionManager transactionManager = configuration.getRuntimeConfig().getTransactionManager();
      TransactionManagerLookup lookup = null;

      if (transactionManager == null)
      {
         // Nope. See if we can look it up from JNDI
         if (configuration.getTransactionManagerLookupClass() != null)
         {
            try
            {
               ClassLoader cl = Thread.currentThread().getContextClassLoader();
               if (cl == null) cl = getClass().getClassLoader();
               Class clazz = cl.loadClass(configuration.getTransactionManagerLookupClass());
               lookup = (TransactionManagerLookup) clazz.newInstance();
            }
            catch (Exception e)
            {
               throw new ConfigurationException("Problems looking up transaction manager", e);
            }
         }

         try
         {
            if (lookup != null)
            {
               transactionManager = lookup.getTransactionManager();
               configuration.getRuntimeConfig().setTransactionManager(transactionManager);
            }
         }
         catch (Exception e)
         {
            log.info("failed looking up TransactionManager, will not use transactions", e);
         }
      }

      if (transactionManager == null && configuration.isInvocationBatchingEnabled())
      {
         log.info("Using a batchMode transaction manager");
         transactionManager = BatchModeTransactionManager.getInstance();
      }
      return componentType.cast(transactionManager);
   }
}
