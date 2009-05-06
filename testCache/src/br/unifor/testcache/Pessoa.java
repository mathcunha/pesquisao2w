package br.unifor.testcache;

import java.io.Serializable;

public class Pessoa implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Endereco endereco;
	private String nome;

	public Endereco getEndereco() {
		return endereco;
	}

	public void setEndereco(Endereco endereco) {
		this.endereco = endereco;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}
	
	/*
	 
	 matheus = new br.unifor.testcache.Pessoa();
	 matheus.setNome("Matheus Cunha");
	 endereco = new br.unifor.testcache.Endereco();
	 endereco.setRua("Marcos Macêdo"); 
	 matheus.setEndereco(endereco);
	 cache.attach("pojo/matheus", matheus);
	 cache.attach("pojo/endereco", endereco);
	 
	 */
	 

}
