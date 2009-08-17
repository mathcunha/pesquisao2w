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
package org.jboss.cache.config;

import org.jboss.cache.config.parsing.XmlConfigHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A configuration component where the implementation class can be specified, and comes with its own set of properties.
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 2.2.0
 */
public abstract class PluggableConfigurationComponent extends ConfigurationComponent
{
   protected String className;
   protected Properties properties;

   public String getClassName()
   {
      return className;
   }

   public void setClassName(String className)
   {
      if (className == null || className.length() == 0) return;
      testImmutability("className");
      this.className = className;
   }

   public Properties getProperties()
   {
      return properties;
   }

   public void setProperties(Properties properties)
   {
      testImmutability("properties");
      this.properties = properties;
   }

   public void setProperties(String properties) throws IOException
   {
      if (properties == null) return;

      testImmutability("properties");
      // JBCACHE-531: escape all backslash characters
      // replace any "\" that is not preceded by a backslash with "\\"
      properties = XmlConfigHelper.escapeBackslashes(properties);
      ByteArrayInputStream is = new ByteArrayInputStream(properties.trim().getBytes("ISO8859_1"));
      this.properties = new Properties();
      this.properties.load(is);
      is.close();
   }

   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PluggableConfigurationComponent that = (PluggableConfigurationComponent) o;

      if (className != null ? !className.equals(that.className) : that.className != null) return false;
      if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;

      return true;
   }

   public int hashCode()
   {
      int result;
      result = (className != null ? className.hashCode() : 0);
      result = 31 * result + (properties != null ? properties.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + " {className = " + className +
            ", properties=" + properties + "}";
   }

   @Override
   public PluggableConfigurationComponent clone() throws CloneNotSupportedException
   {
      PluggableConfigurationComponent clone = (PluggableConfigurationComponent) super.clone();
      if (properties != null) clone.properties = (Properties) properties.clone();
      return clone;
   }
}
