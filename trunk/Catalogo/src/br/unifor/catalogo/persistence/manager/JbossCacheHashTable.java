package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.Configuration;

import br.unifor.catalogo.persistence.EntryTO;
import br.unifor.catalogo.persistence.manager.test.TestManager;
import br.unifor.catalogo.persistence.manager.test.TestManager.Test;

public class JbossCacheHashTable {

	private Logger log = Logger.getLogger(getClass().getName());
	private TestManager testManager = new TestManager();
	private Test test = testManager.newTest();
	
	
	protected Cache cache;
	protected Configuration config ;
	
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

	
	public Map findAll(Entry<Object, Object> par) {
		Fqn fqn = Fqn.fromString("/catPojo/"+par.getKey().getClass().getName());
		return cache.getData(fqn);
	}

	
	public Entry<Object, Object> findByPk(Entry<Object, Object> par) {
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

	public void insert(Entry<Object, Object> par) {
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

	public void update(Entry<Object, Object> par) {
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
