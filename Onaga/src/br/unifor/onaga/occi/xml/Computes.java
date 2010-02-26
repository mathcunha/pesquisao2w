package br.unifor.onaga.occi.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class Computes {
	public List<Compute> computes;

	public List<Compute> getComputes() {
		return computes;
	}

	public void setComputes(List<Compute> computes) {
		this.computes = computes;
	}

	public void addCompute(Compute compute) {
		if (computes == null) {
			computes = new ArrayList<Compute>();
		}
		computes.add(compute);
	}

	public static void config(Digester digester) {
		digester.addObjectCreate("COMPUTES", Computes.class);
		digester.addObjectCreate("COMPUTES/COMPUTE", Compute.class);		
		digester.addSetProperties("COMPUTES/COMPUTE","href","href");
		digester.addSetNext("COMPUTES/COMPUTE", "addCompute");
	}

	public static Computes loadFromFile(File file) throws IOException,
			SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Computes) digester.parse(file);
	}

	public static Computes loadFromInputStream(InputStream input)
			throws IOException, SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Computes) digester.parse(input);
	}
}
