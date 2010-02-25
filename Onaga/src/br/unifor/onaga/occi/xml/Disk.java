package br.unifor.onaga.occi.xml;

import java.io.File;
import java.io.IOException;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class Disk {

	private String id;
	private String name;
	private String size;
	private String url;
	private String href;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
	
	public static Disk loadFromFile(File file) throws IOException, SAXException{
		Digester digester = new Digester();
		digester.addObjectCreate("DISK", Disk.class);
		digester.addBeanPropertySetter("DISK/ID");
		digester.addBeanPropertySetter("DISK/NAME");
		digester.addBeanPropertySetter("DISK/SIZE");
		digester.addBeanPropertySetter("DISK/URL");
		digester.addBeanPropertySetter("DISK/href");
		
		
		return (Disk)digester.parse(file);
	}

}
