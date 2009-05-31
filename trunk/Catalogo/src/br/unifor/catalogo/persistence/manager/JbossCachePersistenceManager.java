package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jboss.cache.config.Configuration;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheFactory;

import br.unifor.catalogo.persistence.CatalogoTO;

public class JbossCachePersistenceManager {
	
	private Logger log = Logger.getLogger(getClass().getName());
	
	
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
		log.info("update "+(System.currentTimeMillis() - inicial));
	}
	
	public void insert(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		cache.attach(key, to);
		log.info("insert "+(System.currentTimeMillis() - inicial));
	}

	public CatalogoTO delete(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();
		CatalogoTO retorno =  (CatalogoTO) cache.detach(key);
		log.info("delete "+(System.currentTimeMillis() - inicial));
		return retorno;
	}
	
	public CatalogoTO findByPk(CatalogoTO to){
		String key = getKey(to);
		long inicial = System.currentTimeMillis();	
		CatalogoTO retorno =  (CatalogoTO) cache.find(key);
		log.info("findByPk "+(System.currentTimeMillis() - inicial));
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

}
