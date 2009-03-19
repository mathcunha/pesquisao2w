/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo;

/**
 * Thrown when the POJO has already detached from the cache store by the remote side, but user
 * is still trying to access it via the cache interceptor.
 *
 * @author Ben Wang
 * @version $Id: PojoCacheAlreadyDetachedException.java 3852 2007-05-22 04:16:07Z bstansberry $
 */
public class PojoCacheAlreadyDetachedException extends PojoCacheException
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -7994594319185959431L;

   public PojoCacheAlreadyDetachedException()
   {
      super();
   }

   public PojoCacheAlreadyDetachedException(String err, Throwable e)
   {
      super(err, e);
   }

   public PojoCacheAlreadyDetachedException(String err)
   {
      super(err);
   }

   public PojoCacheAlreadyDetachedException(Throwable e)
   {
      super(e);
   }
}
