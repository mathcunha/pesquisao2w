package br.unifor.onaga.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import br.unifor.onaga.occi.xml.Compute;
import br.unifor.onaga.occi.xml.Computes;
import br.unifor.onaga.occi.xml.Disk;
import br.unifor.onaga.occi.xml.Storage;

public class OCCIClient {
	
	protected static Logger log = Logger.getLogger(OCCIClient.class.getName());

	public static final String username = "oneadmin";
	public static final String password = "oneadmin";
	public static final String endpoint = "http://localhost:4567";

	public static Storage list_storage() {
		Storage storage = null;
		//<STORAGE>    <DISK href="http://localhost:4567/storage/1"/>    <DISK href="http://localhost:4567/storage/2"/></STORAGE>
		try {
			String command = "occi-storage --url " + endpoint + " --user " + username
			+ " --password " + password + " list";
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			storage = Storage.loadFromInputStream(process.getInputStream());

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} catch (SAXException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return storage;
	}
	
	public static Disk show_storage(String id) {
		Disk disk = null;		
		try {
			String command = "occi-storage --url " + endpoint + " --user " + username
			+ " --password " + password + " show "+id;
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			disk = Disk.loadFromInputStream(process.getInputStream());

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} catch (SAXException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return disk;
	}

	public static File createVMFile(String id) {
		File file = new File("/tmp/VM_" + id + "_" + System.currentTimeMillis()
				+ ".xml");

		log.log(Level.INFO, file.getAbsolutePath());
		
		try {
			FileWriter writer = new FileWriter(file);
			writer.write("	<COMPUTE> \n");
			writer.write("		<ID>0</ID> \n");
			writer.write("		<NAME>"+file.getName()+"</NAME> \n");
			writer.write("		<STATE>PENDING</STATE> \n");
			writer.write("		<INSTANCE_TYPE>small</INSTANCE_TYPE> \n");
			writer.write("		<STORAGE> \n");
			writer.write("			<DISK image=\""+id+"\" dev=\"sda2\"/> \n");

			writer.write("			<SWAP size=\"1024\" dev=\"sda1\"/> \n");
			writer.write("		</STORAGE> \n");
			writer.write("		<NETWORK> \n");
			
			writer.write("			<NIC network=\"0\" /> \n");

			writer.write("		</NETWORK> \n");
			writer.write("	</COMPUTE> \n");
			
			writer.flush();
			writer.close();

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return file;
	}

	public static String deploy_vm(String id) {
		String retorno = null;
		
		File file = createVMFile(id);

		try {
			
			String command = 
				"occi-compute --url " + endpoint + " --user " + username
				+ " --password " + password + " create "+file.getAbsolutePath();
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;

			StringBuffer out = new StringBuffer();
			while ((line = in.readLine()) != null) {
				out.append(line);
			}

			
			retorno = out.toString();
			
			log.log(Level.FINE, retorno);

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return retorno;
	}
	
	public static Computes list_compute() {
		Computes retorno = null;
		//<STORAGE>    <DISK href="http://localhost:4567/storage/1"/>    <DISK href="http://localhost:4567/storage/2"/></STORAGE>
		try {
			String command = "occi-compute --url " + endpoint + " --user " + username
			+ " --password " + password + " list";
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			retorno = Computes.loadFromInputStream(process.getInputStream());

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} catch (SAXException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return retorno;
	}
	
	public static Compute show_compute(String id) {
		Compute retorno = null;
		try {
			String command = "occi-compute --url " + endpoint + " --user " + username
			+ " --password " + password + " show "+id;
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			retorno = Compute.loadFromInputStream(process.getInputStream());

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} catch (SAXException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return retorno;
	}
	
	public static String delete_compute(String id) {
		String retorno = null;

		try {
			
			String command = 
				"occi-compute --url " + endpoint + " --user " + username
				+ " --password " + password + " delete "+id;
			
			log.log(Level.FINE, command);
			
			Process process = Runtime.getRuntime().exec(command);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;

			StringBuffer out = new StringBuffer();
			while ((line = in.readLine()) != null) {
				out.append(line);
			}

			
			retorno = out.toString();
			
			log.log(Level.FINE, retorno);

		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		return retorno;
	}

}
