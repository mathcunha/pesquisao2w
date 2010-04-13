package br.unifor.onaga.ejb.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name="WebContext")
@Table(name = "WC_WEB_CONTEXT")
@NamedQueries({
  @NamedQuery(name="findWebContextByName",
              query="SELECT o " +
                    "FROM WebContext o " +
                    "WHERE o.name = :name "),
                    
  @NamedQuery(name="findAllWebContext",
              query="SELECT o " +
                    "FROM WebContext o ")
})
public class WebContext extends OnagaEntityAB {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private Long id;
	private List<WebContainerVM> webVMs;
	private List<ApacheVM> apacheVMs;
	
	@Transient
	public String getDefaultNamedQuery() {
		return "findWebContextByName";
	}

	@Column(name = "WC_NAME", length = 255, nullable = false, unique=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Id
    @SequenceGenerator(name = "WC_ID", sequenceName = "SEQ_WC_WEB_CONTEXT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "WC_ID")
    @Column(name = "WC_ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setWebVMs(List<WebContainerVM> webVMs) {
		this.webVMs = webVMs;
	}

	@ManyToMany(mappedBy="contexts")
	public List<WebContainerVM> getWebVMs() {
		return webVMs;
	}

	public void setApacheVMs(List<ApacheVM> apacheVMs) {
		this.apacheVMs = apacheVMs;
	}
	
	@ManyToMany(mappedBy="contexts")
	public List<ApacheVM> getApacheVMs() {
		return apacheVMs;
	}

}
