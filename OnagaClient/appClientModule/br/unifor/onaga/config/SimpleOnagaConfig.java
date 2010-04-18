package br.unifor.onaga.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleOnagaConfig implements Runnable {
	public final String ONAGA_END ;
	public final String ONAGA_BEGIN ;
	protected final String FILE_NAME;
	protected SimpleConfigInfo configInfo;

	public void setConfigInfo(SimpleConfigInfo configInfo) {
		this.configInfo = configInfo;
	}

	protected static Logger log = Logger.getLogger(SimpleOnagaConfig.class
			.getName());

	public SimpleOnagaConfig(String fileName, String patternBegin, String patternEnd) {
		FILE_NAME = fileName;
		ONAGA_END = patternEnd;
		ONAGA_BEGIN = patternBegin;
	}

	@Override
	public void run() {
		try {
			FileInputStream fileInput = new FileInputStream(FILE_NAME);
			FileChannel inChannel = fileInput.getChannel();

			FileOutputStream fileOutput = new FileOutputStream(FILE_NAME, true);
			FileChannel outChannel = fileOutput.getChannel();

			File tmpFileOut = new File("tmp_file");
			FileWriter tmpWriter = new FileWriter(tmpFileOut, false);

			char[] onagaTokenIni = ONAGA_BEGIN.toCharArray();
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

							position_ini = inChannel.position() - current + i
									+ 1;

							fileOutput.flush();

						}
					} else {
						index_onaga_ini = 0;
					}

					if (lido.get(i) == onagaTokenFim[index_onaga_fim]) {
						index_onaga_fim++;
						if (index_onaga_fim == onagaTokenFim.length) {
							index_onaga_fim = 0;

							position_fim = inChannel.position() - current + i
									+ 1;

							String newText = "\n" + getNewConf() + "\n" + ONAGA_END;
							tmpWriter.write(newText);
							
							int diff = (new Long(position_fim - position_ini - newText.getBytes().length)).intValue();
							if(diff > 0){
								char[] buffer = new char[diff];
								
								for (int k = 0; k < buffer.length; k++) {
									buffer[k] = ' ';
									
								}
								System.out.println(diff+" tem diferença! "+buffer.length); 
								
								tmpWriter.write(buffer);
								
							}

							System.out.println(tmpFileOut.getAbsolutePath());
							continue;

						}
					} else {
						index_onaga_fim = 0;
					}

					if (position_fim != 0) {
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
			log.log(Level.SEVERE, "arquivo nao encontrado", e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "erro ao ler o arquivo de configuracao", e);
		}

	}

	private void mergeConfFile(FileChannel outChannel, File tmpFileOut,
			ByteBuffer buf, long position_ini, long position_fim)
			throws FileNotFoundException, IOException {
		int current;
		long cursor = position_ini;
		if (position_fim != 0 && position_ini != 0) {

			FileInputStream tmpReader = new FileInputStream(tmpFileOut);
			FileChannel inTmpChannel = tmpReader.getChannel();

			outChannel.position(position_ini);

			while ((current = inTmpChannel.read(buf)) != -1) {
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
		if (configInfo != null) {
			return configInfo.getConfInfo();
		}
		return "SimpleConfigInfo nao informado";
	}
	
	public static interface SimpleConfigInfo {
		String getConfInfo();
	}

}
