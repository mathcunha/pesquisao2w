package br.unifor.onaga.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;

import br.unifor.onaga.config.util.Util;
import br.unifor.onaga.ejb.session.RegisterSessionRemote;

public class OnagaConfig implements Runnable {
	public static final String ONAGA_END = "<!--Onaga End-->";
	protected RegisterSessionRemote register;
	protected final String JONAS_HOME;
	protected static final String FILE_NAME = "matheus.txt";

	protected static Logger log = Logger
			.getLogger(OnagaConfig.class.getName());

	public OnagaConfig(RegisterSessionRemote businessItf,
			String jonasHome) {
		register = businessItf;
		JONAS_HOME = jonasHome;
	}

	

	@Override
	public void run() {
		try {
			FileInputStream fileInput = new FileInputStream(JONAS_HOME
					+ File.separator + "conf" + File.separator + FILE_NAME);
			FileChannel inChannel = fileInput.getChannel();

			FileOutputStream fileOutput = new FileOutputStream(JONAS_HOME
					+ File.separator + "conf" + File.separator + FILE_NAME,
					true);
			FileChannel outChannel = fileOutput.getChannel();
			
			File tmpFileOut = new File("tmp_file");
			FileWriter tmpWriter = new FileWriter(tmpFileOut,false);

			char[] onagaTokenIni = "<!--Onaga Begin-->".toCharArray();
			int index_onaga_ini = 0;
			
			char[] onagaTokenFim = ONAGA_END.toCharArray();
			int index_onaga_fim = 0;

			int len = 4;
			ByteBuffer buf = ByteBuffer.allocate(len);
			
			long position_ini = 0;
			long position_fim = 0;

			String encoding = System.getProperty("file.encoding");
			int current;
			while ((current = inChannel.read(buf)) != -1) {
				buf.flip();
				CharBuffer lido = Charset.forName(encoding).decode(buf);

				for (int i = 0; i < current; i++) {
					if (lido.get(i) == onagaTokenIni[index_onaga_ini]) {
						index_onaga_ini++;
						if (index_onaga_ini == onagaTokenIni.length) {
							index_onaga_ini = 0;

							position_ini = inChannel.position() - current + i + 1;
							
							fileOutput.flush();
							
						}
					} else {
						index_onaga_ini = 0;
					}
					
					if (lido.get(i) == onagaTokenFim[index_onaga_fim]) {
						index_onaga_fim++;
						if (index_onaga_fim == onagaTokenFim.length) {
							index_onaga_fim = 0;

							position_fim = inChannel.position() - current + i + 1;
							
							
							tmpWriter.write("\n"+getNewConf()+"\n"+ONAGA_END);
							
							System.out.println(tmpFileOut.getAbsolutePath());
							continue;
							
						}
					} else {
						index_onaga_fim = 0;
					}
					
					if(position_fim != 0){
						tmpWriter.write(lido.get(i));
					}
				}

				buf.clear();
			}
			
			tmpWriter.close();
			
			mergeConfFile(outChannel, tmpFileOut, buf, position_ini,
					position_fim);
			
			
			fileInput.close();
			fileOutput.close();
			

		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "arquivo não encontrado", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "erro ao ler o arquivo de configuração", e);
		}

	}

	private void mergeConfFile(FileChannel outChannel, File tmpFileOut,
			ByteBuffer buf, long position_ini, long position_fim)
			throws FileNotFoundException, IOException {
		int current;
		long cursor = position_ini;
		if(position_fim != 0 && position_ini != 0){
			
			FileInputStream tmpReader = new FileInputStream(tmpFileOut);
			FileChannel inTmpChannel = tmpReader.getChannel();
			
			outChannel.position(position_ini);
			
			while ((current = inTmpChannel.read(buf)) != -1){
				buf.flip();
				outChannel.write(buf, cursor);
				cursor += current;
				buf.flip();
			}
			
			inTmpChannel.close();
			tmpReader.close();
		}
	}

	private String getNewConf() {
		return "Some text";
	}

	public void lineRead(String linha) {
		String token = "<!--Onaga Begin-->";
		if (token.equals(linha)) {
			System.out.println(linha);
		} else {
			System.out.println("(" + linha + ")");
		}
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			OnagaConfig lJonasWebContainerConfig = new OnagaConfig(
					null, "C:\\jonas-full-5.1.1");
			lJonasWebContainerConfig.run();
		} else {
			try {
				OnagaConfig lJonasWebContainerConfig = new OnagaConfig(
						Util.getRegisterSession(), args[0]);
				lJonasWebContainerConfig.run();
			} catch (NamingException e) {
				log.log(Level.SEVERE, "erro ao obter o registersession", e);
			}
		}

	}

}
