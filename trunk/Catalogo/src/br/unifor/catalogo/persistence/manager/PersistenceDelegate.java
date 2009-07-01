package br.unifor.catalogo.persistence.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import br.unifor.catalogo.persistence.CatalogoTO;

public class PersistenceDelegate {
	
	private final JbossCachePersistenceManager manager ;
	
	private static PersistenceDelegate myself; 
	
	private PersistenceDelegate() throws SecurityException, IOException{
		manager = new JbossCachePersistenceManager();		
		manager.config("C:/Users/Administrator/workspace/Catalogo/Catalogo/g2cl-conf/replSync-service.xml");
		manager.createCache("C:/Users/Administrator/workspace/Catalogo/Catalogo/g2cl-conf/replSync-service.xml");
	}
	
	public static PersistenceDelegate getInstance() throws SecurityException, IOException{
		if(myself == null){
			myself = new PersistenceDelegate();
		}
		return myself;
	}	
	
	public void persist(CatalogoTO to){
		manager.persist(to);
	}
	
	public void update(CatalogoTO to){
		manager.update(to);
	}
	
	public void insert(CatalogoTO to){
		if(to.getIdentificador() == null){
			to.setIdentificador(System.currentTimeMillis());
		}
		manager.insert(to);
	}

	public CatalogoTO delete(CatalogoTO to){
		return manager.delete(to);
	}
	
	public CatalogoTO findByPk(CatalogoTO to){
		return manager.findByPk(to);
	}
	
	public Map getRoot(){
		return manager.getRoot();
	}	
	
	public Map findAll(CatalogoTO to){
		return manager.findAll(to);
	}	
	
	public void newTest(){
		manager.newTest();
	}
	
	public void printResult(PrintWriter out){
		manager.printResult(out);
	}
	
}
