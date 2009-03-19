package org.jboss.cache.pojo.impl;

import org.jboss.cache.pojo.PojoCacheThreadContext;

public class PojoCacheThreadContextImpl implements PojoCacheThreadContext
{
   private static final int GRAVITATE = 0;
   private static final Boolean GRAVITATE_DEFAULT = false;
   
   // Every cache instance gets it's own configuration
   // An array is used to conserve memory usage since reclamation is slow with TLs, and prevent CL leaks
   // To further reduce leaks, allocation should be lazily initialized
   // In the future, if we get multiple booleans, use bitwise operations on an integer 
   // as the first entry
   private final ThreadLocal<Object[]> state = new ThreadLocal<Object[]>()
   {
      @Override
      protected Object[] initialValue()
      {
         return null;
      }
   };
   
   PojoCacheThreadContextImpl() 
   {
   }

   /**
    * Returns whether or not this thread should trigger gravitation when a cache-miss occurs. The default is false.
    * 
    * @return true if gravitation should be triggered on cache-miss, false if gravitation should not be triggered
    */
   public boolean isGravitationEnabled()
   {
      Object[] values = state.get();
      return values == null ? GRAVITATE_DEFAULT : (Boolean) values[GRAVITATE];
   }
   
   /**
    * Enables or disables gravitation on cache-miss
    * 
    * @param gravitate  true if gravitation should be triggered on cache-miss, false if gravitation should not be triggered
    */
   public void setGravitationEnabled(boolean gravitate)
   {  
      Object[] values = state.get();
      if (values == null)
      {
         // Don't initialize if this is the default
         if (gravitate == GRAVITATE_DEFAULT)
            return;
      
         state.set(new Object[] {gravitate});
      }
      else
      {
         values[GRAVITATE] = gravitate;
      }
   }
   
   public void clear()
   {
      state.remove();
   }
}