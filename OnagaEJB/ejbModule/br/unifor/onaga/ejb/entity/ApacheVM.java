package br.unifor.onaga.ejb.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name="ApacheVM")
@Table(name = "VA_APACHE_VM")
@NamedQueries({
  @NamedQuery(name="findApacheVMByName",
              query="SELECT o " +
                    "FROM ApacheVM o " +
                    "WHERE o.name = :name "),
                    
  @NamedQuery(name="findAllApacheVM",
              query="SELECT o " +
                    "FROM ApacheVM o ")
})
public class ApacheVM extends VirtualMachine {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<WebContext> contexts;
	
	public void setContexts(List<WebContext> contexts) {
		this.contexts = contexts;
	}
	
	@ManyToMany
	  @JoinTable(
	      name="VA_WC_RELACAO",
	      joinColumns={@JoinColumn(name="VA_WC_VM_ID", referencedColumnName="VM_ID")},
	      inverseJoinColumns={@JoinColumn(name="VA_WC_WC_ID", referencedColumnName="WC_ID")})
	public List<WebContext> getContexts() {
		return contexts;
	}
	

	@Transient
	public String getDefaultNamedQuery() {
		return "findApacheVMByName";
	}
}
