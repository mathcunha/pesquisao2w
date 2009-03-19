/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo;

/**
 * Generic PojoCacheException.
 *
 * @author Ben Wang
 * @version $Id: PojoCacheException.java 3851 2007-05-22 02:31:46Z bstansberry $
 */
public class PojoCacheException extends RuntimeException
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -2879024229641921582L;

   public PojoCacheException()
   {
      super();
   }

   public PojoCacheException(String err, Throwable e)
   {
      super(err, e);
   }

   public PojoCacheException(String err)
   {
      super(err);
   }

   public PojoCacheException(Throwable e)
   {
      super(e);
   }
}
