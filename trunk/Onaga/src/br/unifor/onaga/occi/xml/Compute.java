package br.unifor.onaga.occi.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class Compute {
	private String id;
	private String name;
	private String state;
	private String instance_type;
	private String href;
	private Storage storage;

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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getInstance_type() {
		return instance_type;
	}

	public void setInstance_type(String instanceType) {
		instance_type = instanceType;
	}

	public Storage getStorage() {
		return storage;
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
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
	
	public static void config(Digester digester) {
		digester.addObjectCreate("COMPUTE", Compute.class);		
		digester.addBeanPropertySetter("COMPUTE/ID", "id");
		digester.addBeanPropertySetter("COMPUTE/NAME","name");
		digester.addBeanPropertySetter("COMPUTE/STATE","state");
		digester.addBeanPropertySetter("COMPUTE/INSTANCE_TYPE","instance_type");
		
		Storage.config(digester, "COMPUTE/");
		
		digester.addSetNext("COMPUTE/STORAGE", "setStorage");
		
	}

	public static Compute loadFromFile(File file) throws IOException, SAXException {
		Digester digester = new Digester();

		config(digester);

		return (Compute) digester.parse(file);
	}

	public static Compute loadFromInputStream(InputStream input)
			throws IOException, SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Compute) digester.parse(input);
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}
	
}
