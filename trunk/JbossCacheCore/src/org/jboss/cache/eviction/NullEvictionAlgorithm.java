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
package org.jboss.cache.eviction;

import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionAlgorithmConfig;
import org.jboss.cache.eviction.EvictionEvent.Type;

import java.util.concurrent.BlockingQueue;

/**
 * An eviction algorithm that does nothing - a no-op for everything.
 *
 * @author Brian Stansberry
 */
public class NullEvictionAlgorithm implements EvictionAlgorithm
{
   /**
    * Singleton instance of this class.
    */
   private static final NullEvictionAlgorithm INSTANCE = new NullEvictionAlgorithm();

   /**
    * Constructs a new NullEvictionAlgorithm.
    */
   private NullEvictionAlgorithm()
   {
   }

   public static NullEvictionAlgorithm getInstance()
   {
      return INSTANCE;
   }

   /**
    * Returns {@link NullEvictionQueue#INSTANCE}.
    */
   public EvictionQueue getEvictionQueue()
   {
      return NullEvictionQueue.INSTANCE;
   }

   public void setEvictionActionPolicy(EvictionActionPolicy evictionActionPolicy)
   {
      // no-op
   }

   public void assignToRegion(Fqn fqn, CacheSPI<?, ?> cache, EvictionAlgorithmConfig evictionAlgorithmConfig, Configuration configuration)
   {
      // no-op
   }

   public void process(BlockingQueue<EvictionEvent> queue) throws EvictionException
   {
      // no-op
   }

   public void resetEvictionQueue()
   {
      // no-op
   }

   public boolean canIgnoreEvent(Type eventType)
   {
      return true; // always ignore everything!
   }

   public void initialize()
   {
      // no-op
   }

   public Class<? extends EvictionAlgorithmConfig> getConfigurationClass()
   {
      return NullEvictionAlgorithmConfig.class;
   }
}
