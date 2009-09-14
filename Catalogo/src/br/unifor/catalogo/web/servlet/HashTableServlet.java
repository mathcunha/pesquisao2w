package br.unifor.catalogo.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.catalogo.persistence.EntryTO;
import br.unifor.catalogo.persistence.manager.JbossCacheHashTable;
import br.unifor.catalogo.persistence.manager.PersistenceDelegate;

/**
 * Servlet implementation class HashTableServlet
 */
public class HashTableServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final JbossCacheHashTable jbossCache = new JbossCacheHashTable();  
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HashTableServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}
	
	protected void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String strAcao = request.getParameter("acao");
		
		Byte chave = request.getParameter("identificador") == null ? -1 : new Byte(request.getParameter("identificador"));
		Byte valor = request.getParameter("valor") == null ? -1 : new Byte(request.getParameter("valor"));
		Byte ini = request.getParameter("ini") == null ? -1 : new Byte(request.getParameter("ini"));
		Byte fim = request.getParameter("fim") == null ? -1 : new Byte(request.getParameter("fim"));
		Integer tamanho = request.getParameter("tamanho") == null ? null : new Integer(request.getParameter("tamanho"));
		
		if(strAcao == null){
			
		}else if("teste".equals(strAcao)){
			popular();
			listar(request, response);
		}else if("update".equals(strAcao)){
			jbossCache.newTest();
			if(tamanho != null){
				update( ini, fim, tamanho);
			}else{
				update( ini, fim, valor);
			}
			
		}else if("exibir".equals(strAcao)){
			jbossCache.findByPk(new EntryTO(chave, null));
			listar(request, response);
		}else if("resultado".equals(strAcao)){
			resultado(request, response);
		}
		
	}
	
	private void update (Byte ini, Byte fim, Byte valor){
		
		for (byte i = ini; i < fim; i++) {
			EntryTO entry = new EntryTO(i,valor);
			jbossCache.update(entry);
		}
	}
	
	private void update (Byte ini, Byte fim, Integer tamanho){
		String valor = gerarString(tamanho);
		for (byte i = ini; i < fim; i++) {
			EntryTO entry = new EntryTO(i,valor);
			jbossCache.update(entry);
		}
	}
	
	private String gerarString(Integer tamanho){
		String retorno = "N";
		while (retorno.getBytes().length < tamanho) {
			retorno += retorno;			
		}
		
		return retorno;
	}
	
	private void listar(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Set mapaByte = jbossCache.findAll(new EntryTO((byte)1,(byte)1));
		
		List lista = new ArrayList( 129);
		
		for (Object object : mapaByte) {
			lista.add(object);
		}
		
		request.setAttribute("itens", lista);
		
		request.getRequestDispatcher("hash/listar.jsp").forward(request, response);
	}
	
	protected void popular(){
		int ini = 128;
		int fim = ini;//(byte)(ini *2);

		for (int i = 0; i < ini; i++) {
			jbossCache.insert(new EntryTO((byte)i,(byte)i));
		}
		
		for (int i = ini; i < fim; i++) {
			jbossCache.insert(new EntryTO((byte)i,"S"+i));
		}
	}
	/*
	public class ByteEntry implements Map.Entry<Byte, Byte>, Comparable<Entry>, Serializable{
		private Byte key, value;
		
		public ByteEntry(Byte key, Byte value){
			this.key = key;
			this.value = value;
		}

		@Override
		public Byte getKey() {
			return key;
		}

		@Override
		public Byte getValue() {
			return value;
		}

		@Override
		public Byte setValue(Byte value) {
			Byte aux = this.value;
			this.value = value;
			return aux;
		}

		@Override
		public int compareTo(Entry o) {
			return key.compareTo((Byte)o.getKey());
		}
		
	}
	
	public class StringEntry implements Map.Entry<Byte, String>, Comparable<Entry>, Serializable{
		private Byte key; 
		private String value;
		
		public StringEntry(Byte key, String value){
			this.key = key;
			this.value = value;
		}

		@Override
		public Byte getKey() {
			return key;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String setValue(String value) {
			String aux = this.value;
			this.value = value;
			return aux;
		}
		
		@Override
		public int compareTo(Entry o) {
			return key.compareTo((Byte)o.getKey());
		}

		
	}*/

	protected void resultado(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		jbossCache.printResult(response.getWriter());
	}
	

}
