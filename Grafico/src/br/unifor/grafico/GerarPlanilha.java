package br.unifor.grafico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
	private Integer[] numClientesObject;

	protected static final Logger log = Logger.getLogger(GerarPlanilha.class
			.getName());

	public GerarPlanilha(File diretorio) {
		this(diretorio, new int[] { 50, 60, 70, 80, 90, 100 }, new int[] { 10,
				100, 1000, 10000 }, 60, new String[] { "g2cl_jgroups", /*"g2cl-spread","g2cl-appia","jgroups"*/ }, new int[] { 2,
				4, 6 });
	}

	public GerarPlanilha(File diretorio, int[] numClientes, int[] tamMensagem,
			int repeticao, String[] camada, int[] numMaquinas) {
		this.numClientesObject = new Integer[numClientes.length];
		
		for (int x = 0; x < numClientes.length; x++) {
			this.numClientesObject[x] = numClientes[x];
		}
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
			
			for (int nm : numMaquinas) {
				for (int tm : tamMensagems) {
					criarResumo(workbook, nm, tm);
				}
			}
			
			
			/*
			FileOutputStream outPut2 = new FileOutputStream(new File(
					diretorio.getAbsolutePath() + File.separatorChar
							+ "planilha.xls"));
			workbook.write(outPut2);
			System.exit(0);*/
			
			
			for (String camada : camadas) {
				for (int numMaquina : numMaquinas) {
					int rownum = 1;
					HSSFSheet sheet = newSheet(workbook, camada, numMaquina, 0,
							0, 0);
					for (int numCliente : numClientes) {

						for (int tamMensagem : tamMensagems) {
							Double estat[]  = new Double[]{0d,0d,0d,0d,0d,0d};
							
							double tempototal[] = new double[repeticao];

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

								List<Info> list = carregarArquivo(buffer, numCliente);

								buffer.close();
								reader.close();
								
								Double lEstat[] = gerarEstatisticas(list);
								int k = 0;
								estat[k] += lEstat[k++];
								estat[k] += lEstat[k++];
								estat[k] += lEstat[k++];
								estat[k] += lEstat[k++];
								
								tempototal[i-1] = new Double(list.get(list.size()-1).getTtime());

							}
							
							{
								int k = 0;
								estat[k] = estat[k++]/repeticao;
								estat[k] = estat[k++]/repeticao;
								estat[k] = estat[k++]/repeticao;
								estat[k] = estat[k++]/repeticao;
							}

							

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
							cell.setCellValue(estat[0]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(estat[1]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(estat[2]);

							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(estat[3]);
							
							///////////////////////////////////////////
							
//							Double soma = 0d ;
//							for (Double valor : tempototal) {
//								soma += valor;
//							}
							double media = mediaCortandoOutliers(tempototal);
							
							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(media); // media
							System.out.println("Escrevendo média: " + media +
									"/ camada: " + camada
									+ " / numCliente: " + numCliente
									+ " / numMaquina: " + numMaquina
									+ " / tamMensagem: " + tamMensagem);
							
							writeItemResumo(workbook, camada, numCliente, numMaquina, tamMensagem, media);
							
							StandardDeviation desvio = new StandardDeviation();
							cell = row.createCell(columnnum++,
									HSSFCell.CELL_TYPE_NUMERIC);
							cell.setCellValue(desvio.evaluate(tempototal));
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
		
		System.out.println("FIM!!");
	}

	private void writeItemResumo(HSSFWorkbook workbook, String camada, int numCliente, int numMaquina,
			int tamMensagem, double media) {
		HSSFSheet sheet = workbook.getSheet("resumo_cluster" + numMaquina + "_msg" + tamMensagem);
		int row = findPosInArray(this.numClientesObject, new Integer(numCliente)) + 1;
		int column = findPosInArray(this.camadas, camada) + 1;
		writeCell(sheet, row, column, media, HSSFCell.CELL_TYPE_NUMERIC);
	}
	


	private int findPosInArray(Object[] array, Object camada) {
		int found = -1;
		for (int x = 0; x < array.length; x++) {
			if (camada.equals(array[x])) {
				found = x;
				break;
			}
		}
		// TEM QUE FUNFAR.. não quero tratar lá :) 
		assert found >= 0;
		return found;
	}

	private Double[] gerarEstatisticas(List<Info> list) {
		Double[] retorno = new Double[4];
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

		retorno[0] = new Double(menor);
		retorno[1] = new Double(maior);
		retorno[2] = new Double(soma / (list.size()));
		retorno[3] = new Double(desvio.evaluate(array));

		return retorno;
	}

	private HSSFSheet criarResumo(HSSFWorkbook workbook, int numMaquinas, int tamMensagens) {
		HSSFSheet sheet = workbook.createSheet("resumo_cluster" + numMaquinas + "_msg" + tamMensagens);
		writeCell(sheet, 0, 0, "CLIENTES", HSSFCell.CELL_TYPE_STRING);
		for (int x = 0; x < this.camadas.length; x++) {
			writeCell(sheet, 0, (x+1), this.camadas[x], HSSFCell.CELL_TYPE_STRING);
		}
		
		for (int x = 0; x < this.numClientes.length; x++) {
			writeCell(sheet, (x+1), 0, ""+numClientes[x], HSSFCell.CELL_TYPE_STRING);
		}
		return sheet;
	}
	

	private void writeCell(HSSFSheet sheet, int row, int column, Object value, int tipo) {
		HSSFRow hrow = sheet.getRow(row);
		if (hrow == null) {
			hrow = sheet.createRow(row);
		}
		HSSFCell cell = hrow.getCell(column);
		if (cell == null) {
			cell = hrow.createCell(column, tipo);
		}
		if (tipo == HSSFCell.CELL_TYPE_STRING) 
			cell.setCellValue((String) value);
		if (tipo == HSSFCell.CELL_TYPE_NUMERIC) {
			cell.setCellValue((Double) value);
		}
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
		cell.setCellValue("MEDIA_MENOR_TEMPO_REQUEST");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MEDIA_MAIOR_TEMPO_REQUEST");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MEDIA_POR_REQUEST");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("DESVIO_POR_REQUEST");
		
		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("MEDIA_TEMPO_TOTAL");

		cell = row.createCell(i++, HSSFCell.CELL_TYPE_STRING);
		cell.setCellValue("DESVIO_TEMPO_TOTAL");

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
			}

		}
		return list;
	}
	
	

	public static void main(String[] args) {
		String file = "/home/henrique/resultados/";
		GerarPlanilha lGerarPlanilha = new GerarPlanilha(new File(file));
		Thread thread = new Thread(lGerarPlanilha);
		thread.start();
	}

	
	/**
	 * método boxplot baseado no código do garotinho.
	 * 
	 * @param dados
	 * @return
	 */
	private static double mediaCortandoOutliers(double[] dados) {
		List<Double> l = new ArrayList<Double>();
		for (double d : dados) {
			l.add(d);
		}
		return mediaCortandoOutliers(l);
	}
	
	private static double mediaCortandoOutliers(List<Double> dados) {
		Collections.sort(dados);
		BigDecimal somatorio = new BigDecimal(0);
		int numAmostrasValidas = 0;
		
		int tamanhoAmostra = dados.size();
		int porcao = new BigDecimal(tamanhoAmostra).divide(new BigDecimal(4),
				0, BigDecimal.ROUND_HALF_EVEN).intValue();
		
		if (porcao > 0) {
			Double q1 = dados.get(porcao - 1);
			Double q3 = dados.get(3 * porcao - 1);
			Double d = new BigDecimal(q3).subtract(new BigDecimal(q1))
					.doubleValue();

			Double limiteInferior = new BigDecimal(q1).subtract(
					new BigDecimal(d)).doubleValue();
			Double limiteSuperior = new BigDecimal(q3).add(new BigDecimal(d))
					.doubleValue();

			for (int ii = 0; ii < dados.size(); ii++) {
				boolean outlier = false;
				Double dado = dados.get(ii);
				if (dado < limiteInferior || dado > limiteSuperior) {
					outlier = true;
				}
				
				System.out.printf("DADO: %f / oulier: %s%n", dado, outlier);
				if (!outlier) {
					somatorio = somatorio.add(new BigDecimal(dado));
					numAmostrasValidas++;
				}
			}
		}
		
		return somatorio.divide(new BigDecimal(numAmostrasValidas),
				0, BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}
	
	
	
	private static void testBoxPlot() {
		List<Double> teste = new ArrayList<Double>();
		teste.add(10.2);
		teste.add(12.2);
		teste.add(15.3);
		teste.add(13.3);
		teste.add(11.3);
		teste.add(17.3);
		teste.add(12.3);
		teste.add(3.3);
		teste.add(4.3);
		teste.add(0.3);
		teste.add(14.3);
		teste.add(16.3);
		teste.add(11.3);
		teste.add(11.3);
		teste.add(13.1);
		teste.add(11.3);
		teste.add(13.7);
		teste.add(11.5);
		teste.add(13.1);
		teste.add(13.3);
		teste.add(18.3);
		teste.add(30.3);
		teste.add(60.3);
		teste.add(10.3);
		System.out.println(mediaCortandoOutliers(teste));
	}

}
