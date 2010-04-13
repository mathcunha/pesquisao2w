package br.unifor.onaga.ejb.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity(name="WebContainerVM")
@Table(name = "VMW_WEB_VM")
public class WebContainerVM extends VirtualMachine {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String jk_route;
	
	private String jmxUrl;

	@Column(name = "VMW_JK_ROUTE", length = 15, nullable = false, unique=true)
	public String getJk_route() {
		return jk_route;
	}

	public void setJk_route(String jkRoute) {
		jk_route = jkRoute;
	}

	@Column(name = "VMW_JMX_URL", length = 255, nullable = false, unique=true)
	public String getJmxUrl() {
		return jmxUrl;
	}

	public void setJmxUrl(String jmxUrl) {
		this.jmxUrl = jmxUrl;
	}
}
