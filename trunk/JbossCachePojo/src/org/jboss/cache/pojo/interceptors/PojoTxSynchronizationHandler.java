/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.cache.pojo.interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.pojo.PojoCacheException;
import org.jboss.cache.pojo.util.MethodCall;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.ArrayList;
import java.util.List;

/**
 * Handling the rollback operation for PojoCache level, specifically interceptor add/remove, etc.
 *
 * @author Ben Wang
 * @version $Id: PojoTxSynchronizationHandler.java 6048 2008-06-26 02:21:26Z jason.greene@jboss.com $
 */

public class PojoTxSynchronizationHandler implements Synchronization
{
   private static Log log = LogFactory.getLog(PojoTxSynchronizationHandler.class.getName());
   private List undoList_ = new ArrayList();

   private static ThreadLocal<PojoTxSynchronizationHandler> handler = new ThreadLocal<PojoTxSynchronizationHandler>();

   PojoTxSynchronizationHandler()
   {
   }

   public static PojoTxSynchronizationHandler current()
   {
      return handler.get();
   }

   public static PojoTxSynchronizationHandler create()
   {
      PojoTxSynchronizationHandler current = handler.get();
      if (current == null)
      {
         current = new PojoTxSynchronizationHandler();
         handler.set(current);
      }

      return current;
   }

   public void beforeCompletion()
   {
      // Not interested
   }

   public void afterCompletion(int status)
   {
      try
      {
         switch (status)
         {
            case Status.STATUS_COMMITTED:
               break;
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_ROLLEDBACK:
               log.debug("Running Pojo rollback phase");
               runRollbackPhase();
               log.debug("Finished rollback phase");
               break;

            default:
               throw new IllegalStateException("illegal status: " + status);
         }
      }
      finally
      {
         resetUndoOp();
      }
   }

   private void runRollbackPhase()
   {
      // Rollback the pojo interceptor add/remove
      for (int i = (undoList_.size() - 1); i >= 0; i--)
      {
         MethodCall mc = (MethodCall) undoList_.get(i);
         try
         {
            mc.invoke();
         }
         catch (Throwable t)
         {
            throw new PojoCacheException(
                  "PojoTxSynchronizationHandler.runRollbackPhase(): error: " + t, t);
         }
      }
   }

   public void addToList(MethodCall mc)
   {
      undoList_.add(mc);
   }

   public void resetUndoOp()
   {
      undoList_.clear();
      handler.set(null);
      //PojoTxUndoSynchronizationInterceptor.reset();
   }
}

