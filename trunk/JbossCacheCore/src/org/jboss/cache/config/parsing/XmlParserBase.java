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
package org.jboss.cache.config.parsing;

import org.jboss.cache.config.LegacyConfigurationException;
import org.jboss.util.StringPropertyReplacer;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Contains utility methods that might be useful to most of the parsers.
 *
 * @author Mircea.Markus@jboss.com
 * @since 3.0
 */
public abstract class XmlParserBase
{
   /**
    * @see Integer#parseInt(String)
    */
   protected int getInt(String intStr)
   {
      return Integer.parseInt(intStr);
   }

   /**
    * @see Long#parseLong(String)
    */
   protected long getLong(String longStr)
   {
      return Long.parseLong(longStr);
   }

   /**
    * @see Boolean#valueOf(String)
    */
   protected boolean getBoolean(String str)
   {
      return str == null ? false : Boolean.valueOf(str);
   }

   /**
    * @return true if the given value is not empty.
    */
   protected boolean existsAttribute(String attrValue)
   {
      return attrValue != null && attrValue.length() > 0;
   }

   /**
    * Convenient method for retrieving a single element with the give name.
    */
   protected Element getSingleElement(String namespace, String elementName, Element parent)
   {
      NodeList nodeList = parent.getElementsByTagNameNS(namespace, elementName);
      if (nodeList.getLength() == 0)
      {
         return null;
      }
      return (Element) nodeList.item(0);
   }

   /**
    * Convenient method for retrieving a single element with the give name.
    */
   protected Element getSingleElementInCoreNS(String elementName, Element parent)
   {
      return getSingleElement(RootElementBuilder.JBOSSCACHE_CORE_NS, elementName, parent);
   }

   /**
    * Beside querying the element for its attribute value, it will look into the value, if any, and replace the
    * jboss properties(e.g. ${someValue:defaultValue}.
    * <p/>
    * {@link org.jboss.util.StringPropertyReplacer#replaceProperties(String)}
    */
   protected String getAttributeValue(Element element, String attrName)
   {
      if (element == null || attrName == null) return null;
      String value = element.getAttribute(attrName);
      return value == null ? null : StringPropertyReplacer.replaceProperties(value);
   }

   protected void assertNotLegacyElement(Element e)
   {
      // is this a legacy element?!?
      String name = e.getNodeName();
      if ("config".equals(name)) throw new LegacyConfigurationException("Legacy element encountered when using parser " + getClass().getSimpleName());
   }
}
