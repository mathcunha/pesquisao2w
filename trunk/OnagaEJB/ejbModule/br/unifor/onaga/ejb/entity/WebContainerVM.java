package br.unifor.onaga.ejb.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity(name="WebContainerVM")
@Table(name = "VW_WEB_VM")
public class WebContainerVM extends VirtualMachine {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String jk_route;
	private String webContext;
	private String jmxUrl;
	private List<WebContext> contexts;

	@Column(name = "VW_JK_ROUTE", length = 15, nullable = false, unique=true)
	public String getJk_route() {
		return jk_route;
	}

	public void setJk_route(String jkRoute) {
		jk_route = jkRoute;
	}

	@Column(name = "VW_JMX_URL", length = 255, nullable = false, unique=true)
	public String getJmxUrl() {
		return jmxUrl;
	}

	public void setJmxUrl(String jmxUrl) {
		this.jmxUrl = jmxUrl;
	}

	public void setWebContext(String webContext) {
		this.webContext = webContext;
	}

	@Column(name = "VW_CONTEXT", length = 30, nullable = false)
	public String getWebContext() {
		return webContext;
	}

	public void setContexts(List<WebContext> contexts) {
		this.contexts = contexts;
	}

	@ManyToMany
	  @JoinTable(
	      name="VW_WC_RELACAO",
	      joinColumns={@JoinColumn(name="VW_WC_VM_ID", referencedColumnName="VM_ID")},
	      inverseJoinColumns={@JoinColumn(name="VW_WC_WC_ID", referencedColumnName="WC_ID")})
	public List<WebContext> getContexts() {
		return contexts;
	}
}
