package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.UnversionedNode;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.mvcc.RepeatableReadNode;

import br.unifor.catalogo.persistence.EntryTO;
import br.unifor.catalogo.persistence.manager.test.TestManager;
import br.unifor.catalogo.persistence.manager.test.TestManager.Test;

public class JbossCacheHashTable {

	private Logger log = Logger.getLogger(getClass().getName());
	private TestManager testManager = new TestManager();
	private Test test = testManager.newTest();
	
	
	protected Cache cache;
	protected Configuration config ;
	
	public JbossCacheHashTable(){
		try {
			config("/replSync-service.xml");
			createCache("/replSync-service.xml");
		} catch (SecurityException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
	}
	
	public void config(String configurationFile) throws SecurityException,
			IOException {
		if (configurationFile == null) {
			 throw new FileNotFoundException("Configuration file cannot be null, please specify with the -config parameter when starting!");
		}
	}

	
	public void createCache(String configurationFile) {
		CacheFactory factory = new DefaultCacheFactory();
		
		cache = factory.createCache(configurationFile, false);
		 
		config = cache.getConfiguration();
		
		//config.setClusterName("Catalogo");
		
		// Have to create and start cache before using it
		cache.create();
		cache.start();
		
	}

	
	public Entry<Object, Object> delete(Entry<Object, Object> par) {
		String key = getKey(par);
		long inicial = System.currentTimeMillis();
		
		Fqn fqn = Fqn.fromString(key);
		Boolean retorno = cache.getRoot().removeChild(fqn);
		
		inicial = System.currentTimeMillis() - inicial;
		log.info(retorno+" delete "+inicial);
		test.sumOperation(inicial);
		return par;
	}

	
	public Set findAll(Entry par) {
		Fqn fqn = Fqn.fromString("/catPojo/"+par.getKey().getClass().getName());
		Node no = cache.getNode(fqn);
		
		
		Object[] vetor = no.getChildren().toArray();
		Set retorno = new HashSet<Map.Entry>(vetor.length);
		for (Object object : vetor) {
			RepeatableReadNode lNo = (RepeatableReadNode) object;
			EntryTO entry = new EntryTO(lNo.get("k"), lNo.get("v"));
			retorno.add(entry);
		}
		
		return retorno;
	}

	
	public Entry findByPk(Entry par) {
		String key = getKey(par.getKey());
		long inicial = System.currentTimeMillis();	
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().getChild(fqn);
		
		Entry retorno =  new EntryTO(no.get("k"), no.get("v"));		
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("findByPk "+inicial);
		test.sumOperation(inicial);
		return retorno;
	}

	public void insert(Entry par) {
		String key = getKey(par.getKey());
		long inicial = System.currentTimeMillis();
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().addChild(fqn);
		no.put("k", par.getKey());
		
		if(par.getValue() != null){
			no.put("v", par.getValue());
		}
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("insert "+inicial);
		test.sumOperation(inicial);
		
	}

	
	public void newTest() {
		test = testManager.newTest();
		
	}

	
	public void persist(Entry<Object, Object> par) {
		update(par);
		
	}

	public void update(Entry par) {
		String key = getKey(par.getKey());
		long inicial = System.currentTimeMillis();
		
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().getChild(fqn);
		no.put("v", par.getValue());
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("update "+ inicial);
		test.sumOperation(inicial);
	}
	
	private String getKey(Object obj) {
		return "/catPojo/"+obj.getClass().getName()+"/"+obj;
	}
	
	public Map getRoot(){
		return cache.getData(Fqn.fromString("/"));
	}
	
	public void printResult(PrintWriter out){
		for (Test test : testManager.tests) {
			out.write(test.id+", "+test.operations+", "+test.time+"\n");
		}
	}

}
