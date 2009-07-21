package br.unifor.catalogo.test;

import java.util.List;

import br.unifor.catalogo.persistence.CatalogoTO;
import br.unifor.catalogo.persistence.manager.PersistenceDelegate;

public class TestCatalogo implements Test {

	@Override
	public boolean execute(PersistenceDelegate delegate,
			List<CatalogoTO> catalogos, boolean interativo) {

		delegate.newTest();

		for (CatalogoTO catalogo : catalogos) {
			delegate.insert(catalogo);
		}

		if (interativo) {
			try {
				Thread.currentThread().sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (CatalogoTO catalogo : catalogos) {
			delegate.findByPk(catalogo);
		}

		for (CatalogoTO catalogo : catalogos) {
			delegate.update(catalogo);
		}

		for (CatalogoTO catalogo : catalogos) {
			delegate.delete(catalogo);
		}

		return true;
	}

}
