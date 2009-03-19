/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

package org.jboss.cache.pojo;


/**
 * Represents the thread specific context for POJO Cache operations against a particular cache instance. This is
 * primarily used to change thread specific options. Once set, they remain for the entire lifetime of the thread.
 * 
 * Instances of this class can only be obtained by {@link PojoCache#getThreadContext}
 * 
 * @author Jason T. Greene
 * @since 2.1
 */
public interface PojoCacheThreadContext
{   
   /**
    * Returns whether or not this thread should trigger gravitation when a cache-miss occurs. The default is false.
    * 
    * @return true if gravitation should be triggered on cache-miss, false if gravitation should not be triggered
    */
   public boolean isGravitationEnabled();
   
   /**
    * Enables or disables gravitation on cache-miss
    * 
    * @param gravitate  true if gravitation should be triggered on cache-miss, false if gravitation should not be triggered
    */
   public void setGravitationEnabled(boolean gravitate);
   
   /**
    * Clears all thread settings stored on this context. After invoked, defaults will be returned. This will also reclaim
    * any memory used to store this thread's settings.
    */
   public void clear();
}