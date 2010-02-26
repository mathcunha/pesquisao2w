package br.unifor.onaga.occi.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
	
	public String getIdFromHref(){
		int last = getHref().lastIndexOf("/");		
		String str = getHref().substring(last+1, getHref().length()).trim();
		String retorno = "";
        
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))){            	
            	retorno = retorno + str.charAt(i);
            }
        }
		
		return retorno;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public static void config(Digester digester) {
		digester.addObjectCreate("DISK", Disk.class);
		//digester.addBeanPropertySetter("DISK/id");
		digester.addBeanPropertySetter("DISK/ID", "id");

		digester.addBeanPropertySetter("DISK/NAME","name");
		digester.addBeanPropertySetter("DISK/SIZE","size");
		digester.addBeanPropertySetter("DISK/URL","url");
		digester.addBeanPropertySetter("DISK/href");
	}

	public static Disk loadFromFile(File file) throws IOException, SAXException {
		Digester digester = new Digester();

		config(digester);

		return (Disk) digester.parse(file);
	}

	public static Disk loadFromInputStream(InputStream input)
			throws IOException, SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Disk) digester.parse(input);
	}

}
