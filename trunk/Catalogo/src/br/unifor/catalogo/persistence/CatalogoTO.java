package br.unifor.catalogo.persistence;

import java.io.Serializable;

public class CatalogoTO implements Serializable, Comparable<CatalogoTO>{

	protected Long identificador; 
	
	public Long getIdentificador() {
		return identificador;
	}

	public void setIdentificador(Long identificador) {
		this.identificador = identificador;
	}

	@Override
	public int hashCode() {		
		return identificador.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CatalogoTO other = (CatalogoTO) obj;
		if (identificador == null) {
			if (other.identificador != null)
				return false;
		} else if (!identificador.equals(other.identificador))
			return false;
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	
	public void update(CatalogoTO catalogo){
		this.setIdentificador(catalogo.getIdentificador());
	}

	@Override
	public int compareTo(CatalogoTO o) {
		
		return identificador.compareTo(o.getIdentificador());
	}
	
}
