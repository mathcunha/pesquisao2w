package br.unifor.catalogo.persistence;

public class FilmeTO extends CatalogoTO {

	private String nome;
	private Long quantidade;
	
	public FilmeTO (){
		
	}
	
	public FilmeTO (Long identificador, String nome, Long quantidade){
		this.identificador = identificador;
		this.nome = nome;
		this.quantidade = quantidade;
	}
	
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public Long getQuantidade() {
		return quantidade;
	}
	public void setQuantidade(Long quantidade) {
		this.quantidade = quantidade;
	}
	
}
