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
package org.jboss.cache.loader.tcp;

/**
 * Statics that used to exist in the DelegatingCacheLoader class.
 *
 * @author <a href="mailto:manik AT jboss DOT org">Manik Surtani</a>
 * @since 2.0.0
 */
public interface TcpCacheOperations
{
   int GET_CHILDREN_NAMES = 1;
   int GET_KEY = 2;
   int GET = 3;
   int EXISTS = 4;
   int PUT_KEY_VAL = 5;
   int PUT = 6;
   int REMOVE_KEY = 7;
   int REMOVE = 8;
   int REMOVE_DATA = 9;
   int LOAD_ENTIRE_STATE = 10;
   int STORE_ENTIRE_STATE = 11;
   int PUT_LIST = 12;
}
