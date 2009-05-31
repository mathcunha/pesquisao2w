package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.util.Map;

import org.jboss.cache.config.Configuration;
import org.jboss.cache.pojo.PojoCache;
import org.jboss.cache.pojo.PojoCacheFactory;

import br.unifor.catalogo.persistence.CatalogoTO;

public class JbossCachePersistenceManager {
	protected PojoCache cache;
	protected Configuration config ;
	public void config(String configurationFile) throws FileNotFoundException {
		
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
		cache.attach(getKey(to), to);
	}
	
	public void insert(CatalogoTO to){
		cache.attach(getKey(to), to);
	}

	public CatalogoTO delete(CatalogoTO to){
		return (CatalogoTO) cache.detach(getKey(to));
	}
	
	public CatalogoTO findByPk(CatalogoTO to){
		return (CatalogoTO) cache.find(getKey(to));
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
