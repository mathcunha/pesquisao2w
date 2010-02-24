package br.unifor.onaga.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OCCIClient {
	
	protected static Logger log = Logger.getLogger(OCCIClient.class.getName());

	public static final String username = "oneadmin";
	public static final String password = "oneadmin";
	public static final String endpoint = "http://localhost:4567";

	public static String list_storage() {
		String retorno = null;

		try {
			String command = "occi-storage --url " + endpoint + " --user " + username
			+ " --password " + password + " list";
			
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retorno;
	}

	public static File createVMFile(String id) {
		File file = new File("/tmp/VM_" + id + "_" + System.currentTimeMillis()
				+ ".xml");

		log.log(Level.INFO, file.getAbsolutePath());
		
		try {
			FileWriter writer = new FileWriter(file);
			writer.write("	<COMPUTE>");
			writer.write("		<ID>0</ID>");
			writer.write("		<NAME>"+file.getName()+"</NAME>");
			writer.write("		<STATE>PENDING</STATE>");
			writer.write("		<INSTANCE_TYPE>small</INSTANCE_TYPE>");
			writer.write("		<STORAGE>");
			writer.write("			<DISK image=\""+id+"\" dev=\"sda1\"/>");

			writer.write("			<SWAP size=\"1024\" dev=\"sda2\"/>");
			writer.write("		</STORAGE>");
			writer.write("		<NETWORK>");
			
			writer.write("			<NIC network=\"0\" />");

			writer.write("		</NETWORK>");
			writer.write("	</COMPUTE>");
			
			writer.flush();
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retorno;
	}

}
