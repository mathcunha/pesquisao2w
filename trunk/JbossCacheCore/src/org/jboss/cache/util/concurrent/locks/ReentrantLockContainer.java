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

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A LockContainer that holds ReentrantLocks
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @see org.jboss.cache.util.concurrent.locks.OwnableReentrantLockContainer
 * @since 3.0
 */
@ThreadSafe
public class ReentrantLockContainer<E> extends LockContainer<E>
{
   ReentrantLock[] sharedLocks;

   /**
    * Creates a new LockContainer which uses a certain number of shared locks across all elements that need to be locked.
    *
    * @param concurrencyLevel concurrency level for number of stripes to create.  Stripes are created in powers of two, with a minimum of concurrencyLevel created.
    */
   public ReentrantLockContainer(int concurrencyLevel)
   {
      initLocks(calculateNumberOfSegments(concurrencyLevel));
   }

   protected void initLocks(int numLocks)
   {
      sharedLocks = new ReentrantLock[numLocks];
      for (int i = 0; i < numLocks; i++) sharedLocks[i] = new ReentrantLock();
   }

   public final ReentrantLock getLock(E object)
   {
      return sharedLocks[hashToIndex(object)];
   }

   public final int getNumLocksHeld()
   {
      int i = 0;
      for (ReentrantLock l : sharedLocks) if (l.isLocked()) i++;
      return i;
   }

   public final boolean ownsLock(E object, Object owner)
   {
      ReentrantLock lock = getLock(object);
      return lock.isHeldByCurrentThread();
   }

   public final boolean isLocked(E object)
   {
      ReentrantLock lock = getLock(object);
      return lock.isLocked();
   }

   public String toString()
   {
      return "ReentrantLockContainer{" +
            "sharedLocks=" + (sharedLocks == null ? null : Arrays.asList(sharedLocks)) +
            '}';
   }

   public void reset()
   {
      initLocks(sharedLocks.length);
   }

   public int size()
   {
      return sharedLocks.length;
   }
}
