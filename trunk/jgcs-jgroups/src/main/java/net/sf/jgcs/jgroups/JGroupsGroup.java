
/*
 *
 * JGroups implementation of JGCS - Group Communication Service
 * Copyright (C) 2006 Nuno Carvalho, Universidade de Lisboa
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 * 
 */
 
package net.sf.jgcs.jgroups;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.sf.jgcs.GroupConfiguration;


public class JGroupsGroup implements GroupConfiguration {

	private File configFile;
	private String groupName;
	
	/**
	 * 
	 * 
	 * @param cfgFile the config file name.
	 */
	public JGroupsGroup(String props_file) {
	      // Load properties
	      Properties properties = new Properties();
	      try {
//	    	  System.out.println("PROPS: "+props_file);
//			InputStream is = JGroupsGroup.class.getResource(props_file).openStream();
//			  properties.load(is);
//			  is.close();
	    	  // XXX alterado
	    	  InputStream is = new FileInputStream(props_file);
			  properties.load(new FileInputStream(props_file));
			  is.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		configFile = new File(properties.getProperty("net.sf.jgcs.protocol.jgroups.configFile"));
		groupName = properties.getProperty("net.sf.jgcs.protocol.jgroups.groupName");
	}

	@Override
	public int hashCode(){
		return groupName.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof JGroupsGroup) {
			JGroupsGroup ag = (JGroupsGroup) o;
			return ag.groupName.equals(this.groupName);
		}
		else
			return false;
	}

	public String getGroupName() {
		return groupName;
	}

	public File getConfigFile() {
		return configFile;
	}
	
	public String toString() {
		return getGroupName();
	}

}
