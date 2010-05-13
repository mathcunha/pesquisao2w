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
	public static final String TYPE = "WEBTI";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String jk_route;
	private String ajp_port;
	private String jmxUrl;
	private List<WebContext> contexts;
	
	public WebContainerVM(){
		setType(TYPE);
	}

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

	public void setAjp_port(String ajp_port) {
		this.ajp_port = ajp_port;
	}

	@Column(name = "VW_AJP_PORT", length = 6, nullable = false, unique=false)
	public String getAjp_port() {
		return ajp_port;
	}
}
