package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jboss.cache.config.Configuration;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheFactory;

import br.unifor.catalogo.persistence.CatalogoTO;
import br.unifor.catalogo.persistence.manager.test.TestManager;
import br.unifor.catalogo.persistence.manager.test.TestManager.Test;

public class JbossCachePojoPersistenceManager {
	
	private Logger log = Logger.getLogger(getClass().getName());
	private TestManager testManager = new TestManager();
	private Test test = testManager.newTest();
	
	
	protected PojoCache cache;
	protected Configuration config ;
	public void config(String configurationFile) throws SecurityException, IOException {
		//FileHandler handler = new FileHandler(getClass().getName()+".txt");
		//handler.setFormatter(new SimpleFormatter());
		//log.addHandler(handler);
		if (configurationFile == null) {
			 throw new FileNotFoundException("Configuration file cannot be null, please specify with the -config parameter when starting!");
		}		
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		stop();
	}

	public void stop(){
		cache.stop();
		cache.destroy();
	}
	
	public void newTest(){
		test = testManager.newTest();
	}

	/**
	 * Factory method that creates the cache model delegate instance for this
	 * demo
	 * 
	 * @return instance of CacheModelDelegate
	 * @throws Exception
	 */
	protected void createCache(String configurationFile) {
		
		cache = PojoCacheFactory.createCache(configurationFile, false);
		 
		config = cache.getCache().getConfiguration();
		
		//config.setClusterName("Catalogo");
		
		// Have to create and start cache before using it
		cache.create();
		cache.start();
	}
	
	public void persist(CatalogoTO to){
		update(to);
	}
	
	public void update(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		cache.attach(key, to);
		inicial = System.currentTimeMillis() - inicial;
		log.info("update "+ inicial);
		test.sumOperation(inicial);
	}
	
	public void insert(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		cache.attach(key, to);
		inicial = System.currentTimeMillis() - inicial;
		log.info("insert "+inicial);
		test.sumOperation(inicial);
	}

	public CatalogoTO delete(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		CatalogoTO retorno =  (CatalogoTO) cache.detach(key);
		inicial = System.currentTimeMillis() - inicial;
		log.info("delete "+inicial);
		test.sumOperation(inicial);
		return retorno;
	}
	
	public CatalogoTO findByPk(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();	
		CatalogoTO retorno =  (CatalogoTO) cache.find(key);
		inicial = System.currentTimeMillis() - inicial;
		log.info("findByPk "+inicial);
		test.sumOperation(inicial);
		return retorno;
	}
	
	public Map findAll(CatalogoTO to){
		return cache.findAll("/catPojo/"+to.getClass().getName());
	}
	
	private String getKey(CatalogoTO to) {
		return "/catPojo/"+to.getClass().getName()+"/"+to.getIdentificador();
	}
	
	public Map getRoot(){
		return cache.findAll("/");
	}
	
	public void printResult(PrintWriter out){
		for (Test test : testManager.tests) {
			out.write(test.id+", "+test.operations+", "+test.time+"\n");
		}
	}

}
