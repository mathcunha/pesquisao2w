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
package org.jboss.cache.config.parsing.element;

import org.jboss.cache.buddyreplication.NextMemberBuddyLocator;
import org.jboss.cache.config.BuddyReplicationConfig;
import org.jboss.cache.config.parsing.XmlConfigHelper;
import org.jboss.cache.config.parsing.XmlParserBase;
import org.w3c.dom.Element;

import java.util.Properties;

/**
 * Utility class for parsing 'buddy' element in the .xml configuration file.
 * <pre>
 * Note: class does not rely on element position in the configuration file.
 *       It does not rely on element's name either.
 * </pre>
 *
 * @author Mircea.Markus@jboss.com
 * @since 3.0
 */
public class BuddyElementParser extends XmlParserBase
{
   public BuddyReplicationConfig parseBuddyElement(Element element)
   {
      assertNotLegacyElement(element);

      BuddyReplicationConfig brc = new BuddyReplicationConfig();
      String enabled = getAttributeValue(element, "enabled");
      brc.setEnabled(getBoolean(enabled));
      String buddyPoolName = getAttributeValue(element, "poolName");
      if (existsAttribute(buddyPoolName)) brc.setBuddyPoolName(buddyPoolName);
      String buddyCommunicationTimeout = getAttributeValue(element, "communicationTimeout");
      if (existsAttribute(buddyCommunicationTimeout))
         brc.setBuddyCommunicationTimeout(getInt(buddyCommunicationTimeout));

      parseDataGravitationElement(getSingleElementInCoreNS("dataGravitation", element), brc);
      BuddyReplicationConfig.BuddyLocatorConfig blc = parseBuddyLocatorConfig(getSingleElementInCoreNS("locator", element));
      brc.setBuddyLocatorConfig(blc);
      return brc;
   }

   private BuddyReplicationConfig.BuddyLocatorConfig parseBuddyLocatorConfig(Element element)
   {
      if (element == null) return defaultBuddyLocatorConfig();
      BuddyReplicationConfig.BuddyLocatorConfig result = new BuddyReplicationConfig.BuddyLocatorConfig();
      String buddyLocatorClass = getAttributeValue(element, "class");
      if (existsAttribute(buddyLocatorClass)) result.setBuddyLocatorClass(buddyLocatorClass);
      Properties existing = new Properties();
      Properties configured = XmlConfigHelper.readPropertiesContents(element, "properties");
      existing.putAll(configured);
      result.setBuddyLocatorClass(buddyLocatorClass);
      result.setBuddyLocatorProperties(existing);
      return result;
   }

   private BuddyReplicationConfig.BuddyLocatorConfig defaultBuddyLocatorConfig()
   {
      BuddyReplicationConfig.BuddyLocatorConfig result = new BuddyReplicationConfig.BuddyLocatorConfig();
      result.setBuddyLocatorClass(NextMemberBuddyLocator.class.getName());
      Properties props = new Properties();
      result.setBuddyLocatorProperties(props);
      return result;
   }

   private void parseDataGravitationElement(Element element, BuddyReplicationConfig brc)
   {
      if (element == null) return;
      String auto = getAttributeValue(element, "auto");
      if (existsAttribute(auto)) brc.setAutoDataGravitation(getBoolean(auto));
      String removeOnFind = getAttributeValue(element, "removeOnFind");
      if (existsAttribute(removeOnFind)) brc.setDataGravitationRemoveOnFind(getBoolean(removeOnFind));
      String searchBackupTrees = getAttributeValue(element, "searchBackupTrees");
      if (existsAttribute(searchBackupTrees)) brc.setDataGravitationSearchBackupTrees(getBoolean(searchBackupTrees));
   }
}
