package br.unifor.onaga.ejb.entity;

import java.io.Serializable;

import javax.persistence.Transient;

public abstract class OnagaEntityAB implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public abstract String getName();
	
	@Transient
	public abstract String getDefaultNamedQuery();

}
