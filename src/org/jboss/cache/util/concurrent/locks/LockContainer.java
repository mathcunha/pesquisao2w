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
package org.jboss.cache.util.concurrent.locks;

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.locks.Lock;

/**
 * A container for locks.  Used with lock striping.
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 3.0
 */
@ThreadSafe
public abstract class LockContainer<E>
{
   private int lockSegmentMask;
   private int lockSegmentShift;


   protected int calculateNumberOfSegments(int concurrencyLevel)
   {
      int tempLockSegShift = 0;
      int numLocks = 1;
      while (numLocks < concurrencyLevel)
      {
         ++tempLockSegShift;
         numLocks <<= 1;
      }
      lockSegmentShift = 32 - tempLockSegShift;
      lockSegmentMask = numLocks - 1;
      return numLocks;
   }

   public final int hashToIndex(E object)
   {
      return (hash(object) >>> lockSegmentShift) & lockSegmentMask;
   }

   /**
    * Returns a hash code for non-null Object x.
    * Uses the same hash code spreader as most other java.util hash tables, except that this uses the string representation
    * of the object passed in.
    *
    * @param object the object serving as a key
    * @return the hash code
    */
   final int hash(E object)
   {
      // Spread bits to regularize both segment and index locations,
      // using variant of single-word Wang/Jenkins hash.
      int h = object.hashCode();
      h += (h << 15) ^ 0xffffcd7d;
      h ^= (h >>> 10);
      h += (h << 3);
      h ^= (h >>> 6);
      h += (h << 2) + (h << 14);
      h = h ^ (h >>> 16);
      return h;
   }

   protected abstract void initLocks(int numLocks);

   /**
    * Tests if a give owner owns a lock on a specified object.
    *
    * @param object object to check
    * @param owner  owner to test
    * @return true if owner owns lock, false otherwise
    */
   public abstract boolean ownsLock(E object, Object owner);

   /**
    * @param object object
    * @return true if an object is locked, false otherwise
    */
   public abstract boolean isLocked(E object);

   /**
    * @param object object
    * @return the lock for a specific object
    */
   public abstract Lock getLock(E object);

   /**
    * @return number of locks held
    */
   public abstract int getNumLocksHeld();

   /**
    * Clears all locks held and re-initialises stripes.
    */
   public abstract void reset();

   public abstract int size();
}
