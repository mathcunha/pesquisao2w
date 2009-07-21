package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.Configuration;

import br.unifor.catalogo.persistence.CatalogoTO;
import br.unifor.catalogo.persistence.manager.test.TestManager;
import br.unifor.catalogo.persistence.manager.test.TestManager.Test;

public class JbossCachePersistenceManager implements JbossCacheManager{
	
	private Logger log = Logger.getLogger(getClass().getName());
	private TestManager testManager = new TestManager();
	private Test test = testManager.newTest();
	
	
	protected Cache cache;
	protected Configuration config ;
	public void config(String configurationFile) throws SecurityException, IOException {
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
	public void createCache(String configurationFile) {
		CacheFactory factory = new DefaultCacheFactory();
		
		cache = factory.createCache(configurationFile, false);
		 
		config = cache.getConfiguration();
		
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
		
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().getChild(fqn);
		no.put("identificador", to.getIdentificador());
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("update "+ inicial);
		test.sumOperation(inicial);
	}
	
	public void insert(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().addChild(fqn);
		no.put("identificador", to.getIdentificador());
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("insert "+inicial);
		test.sumOperation(inicial);
	}

	public CatalogoTO delete(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		
		Fqn fqn = Fqn.fromString(key);
		Boolean retorno = cache.getRoot().removeChild(fqn);
		
		inicial = System.currentTimeMillis() - inicial;
		log.info(retorno+" delete "+inicial);
		test.sumOperation(inicial);
		return to;
	}
	
	public CatalogoTO findByPk(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();	
		
		Fqn fqn = Fqn.fromString(key);
		Node no = cache.getRoot().getChild(fqn);
		
		CatalogoTO retorno =  new CatalogoTO();
		retorno.setIdentificador((Long)no.get("identificador"));
		
		inicial = System.currentTimeMillis() - inicial;
		log.info("findByPk "+inicial);
		test.sumOperation(inicial);
		return retorno;
	}
	
	public Map findAll(CatalogoTO to){
		Fqn fqn = Fqn.fromString("/catPojo/"+to.getClass().getName());
		return cache.getData(fqn);
	}
	
	private String getKey(CatalogoTO to) {
		return "/catPojo/"+to.getClass().getName()+"/"+to.getIdentificador();
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
