package br.unifor.catalogo.persistence.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import br.unifor.catalogo.persistence.CatalogoTO;

public interface JbossCacheManager {
	
	void persist(CatalogoTO to);
	
	void insert(CatalogoTO to);
	
	void update(CatalogoTO to);
	
	CatalogoTO delete(CatalogoTO to);
	
	CatalogoTO findByPk(CatalogoTO to);
	
	Map findAll(CatalogoTO to);
	
	void config(String configurationFile) throws SecurityException, IOException ;
	
	void createCache(String configurationFile) ;
	
	Map getRoot();
	
	void newTest();
	
	void printResult(PrintWriter out);

}
