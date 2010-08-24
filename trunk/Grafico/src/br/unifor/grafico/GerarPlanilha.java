package br.unifor.grafico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class GerarPlanilha implements Runnable {

	public final int[] numClientes;
	public final int[] numMaquinas;
	public final int[] tamMensagems;
	public final String[] camadas;
	public final int repeticao;
	public final File diretorio;

	protected static final Logger log = Logger.getLogger(GerarPlanilha.class
			.getName());

	public GerarPlanilha(File diretorio) {
		this(diretorio, new int[] { 50, 60, 70, 80, 90, 100 }, new int[] { 10,
				100, 1000, 10000 }, 60, new String[] { "g2cl" }, new int[] { 2,
				4, 6 });
	}

	public GerarPlanilha(File diretorio, int[] numClientes, int[] tamMensagem,
			int repeticao, String[] camada, int[] numMaquinas) {
		this.numClientes = numClientes;
		this.tamMensagems = tamMensagem;
		this.repeticao = repeticao;
		this.diretorio = diretorio;
		this.camadas = camada;
		this.numMaquinas = numMaquinas;
	}

	public void run() {
		String nome = "";
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			for (String camada : camadas) {
				for (int numMaquina : numMaquinas) {
					int rownum = 1;
					HSSFSheet sheet = newSheet(workbook, camada, numMaquina, 0,
							0, 0);
					for (int numCliente : numClientes) {

						for (int tamMensagem : tamMensagems) {
							List<Info> list = new ArrayList<Info>(numCliente
									* repeticao);

							for (int i = 1; i <= repeticao; i++) {
								nome = diretorio.getAbsolutePath()
										+ File.separatorChar + camada
										+ File.separatorChar + numMaquina
										+ File.separatorChar + "results_"
										+ numCliente + "_" + tamMensagem + "_"
										+ i + ".txt";
								File file = new File(nome);
								FileReader reader = new FileReader(file);
								BufferedReader buffer = new BufferedReader(
										reader);

								list.addAll(carregarArquivo(buffer, numCliente));

								buffer.close();
								reader.close();

							}

							Object estat[] = gerarEstatisticas(list);

							log.info(sheet.getSheetName() + " " + rownum + " "
									+ numCliente + " " + tamMensagem + " "
									+ estat[0] + " " + estat[1] + " "
									+ estat[2] + " " + estat[3] + " ");

							HSSFRow row = sheet.createRow(rownum++);
							int columnnum = 0;

							HSSFCell cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(numCliente);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(tamMensagem);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue((Integer) estat[0]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue((Integer) estat[1]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue((Double) estat[2]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue((Double) estat[3]);
						}
					}
				}
			}
			FileOutputStream outPut = new FileOutputStream(new File(
					diretorio.getAbsolutePath() + File.separatorChar
							+ "planilha.xls"));
			workbook.write(outPut);

		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, nome, e);
		} catch (IOException e) {
			log.log(Level.SEVERE, nome, e);
		}
	}

	private Object[] gerarEstatisticas(List<Info> list) {
		Object[] retorno = new Object[4];
		Integer menor = Integer.MAX_VALUE;
		Integer maior = Integer.MIN_VALUE;
		double array[] = new double[list.size()];
		Long soma = 0l;
		int i = 0;
		for (Info info : list) {
			soma += info.getTime();

			if (menor > info.getTime()) {
				menor = info.getTime();
			}

			if (maior < info.getTime()) {
				maior = info.getTime();
			}

			array[i++] = info.getTime();
		}

		StandardDeviation desvio = new StandardDeviation();

		retorno[0] = menor;
		retorno[1] = maior;
		retorno[2] = new Double(soma / (list.size()));
		retorno[3] = new Double(desvio.evaluate(array));

		return retorno;
	}

	private HSSFSheet newSheet(HSSFWorkbook workbook, String camada,
			int numMaquina, int numCliente, int tamMensagem, int repeticao) {
		HSSFSheet sheet = workbook.createSheet(camada + "_" + numMaquina);
		HSSFRow row = sheet.createRow((short) 0);
		int i = 0;

		HSSFCell cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("CLIENTES");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("TAM_MENSAGEM");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MENOR");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MAIOR");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MEDIA");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("DESVIO");

		/*
		 * HSSFCell cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("id");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("starttime");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("seconds");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("ctime");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("dtime");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("ttime");
		 * 
		 * cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		 * cell.setCellValue("wait");
		 */

		return sheet;
	}

	public List<Info> carregarArquivo(BufferedReader buffer, int j)
			throws IOException {
		String linha = null;
		int i = 0;
		List<Info> list = new ArrayList<Info>(j);
		while ((linha = buffer.readLine()) != null) {

			if (i == 0) {
				i++;
			} else {

				Info info = new Info(linha.split("\t"));
				list.add(info);
				if (list.size() > 1) {
					Info ant = list.get(list.size() - 2);
					info.setTime(info.getTtime() - ant.getTtime());
				} else {
					info.setTime(info.getTtime());
				}

				info.setId(i++);
				/*
				 * int k = 0; HSSFCell cell = row.createCell(k++,
				 * HSSFCell.CELL_TYPE_NUMERIC); cell.setCellValue(info.getId());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_STRING);
				 * cell.setCellValue(info.getStarttime());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_NUMERIC);
				 * cell.setCellValue(info.getSeconds());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_NUMERIC);
				 * cell.setCellValue(info.getCtime());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_NUMERIC);
				 * cell.setCellValue(info.getDtime());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_NUMERIC);
				 * cell.setCellValue(info.getTtime());
				 * 
				 * cell = row.createCell(k++, HSSFCell.CELL_TYPE_NUMERIC);
				 * cell.setCellValue(info.getWait());
				 */

			}

		}
		return list;
	}

	public static void main(String[] args) {
		GerarPlanilha lGerarPlanilha = new GerarPlanilha(new File(args[0]));
		Thread thread = new Thread(lGerarPlanilha);
		thread.start();
	}

}
