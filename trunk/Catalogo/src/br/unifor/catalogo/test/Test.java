package br.unifor.catalogo.test;

import java.util.List;

import br.unifor.catalogo.persistence.CatalogoTO;
import br.unifor.catalogo.persistence.manager.PersistenceDelegate;

public interface Test {
	
	boolean execute(PersistenceDelegate delegate, List<CatalogoTO> catalogos, boolean interativo);
	
}
