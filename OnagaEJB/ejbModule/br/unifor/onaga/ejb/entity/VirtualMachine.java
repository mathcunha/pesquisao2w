package br.unifor.onaga.ejb.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name = "VirtualMachine")
@Table(name = "VM_VIRTUAL_MACHINE")
@NamedQueries({
  @NamedQuery(name="findVirtualMachineByName",
              query="SELECT o " +
                    "FROM VirtualMachine o " +
                    "WHERE o.name = :name "),
                    
  @NamedQuery(name="findAllVirtualMachine",
              query="SELECT o " +
                    "FROM VirtualMachine o ")
})
public class VirtualMachine extends OnagaEntityAB {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String ip;
	private Long id;
	private VirtualAppliance virtualAppliance;

	@Column(name = "VM_NAME", length = 255, nullable = false, unique=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "VM_IP", length = 15, nullable = false, unique=true)
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Id
    @SequenceGenerator(name = "VM_ID", sequenceName = "SEQ_VM_VIRTUAL_MACHINE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VM_ID")
    @Column(name = "VM_ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "VirtualMachine [id=" + id + ", ip=" + ip + ", name=" + name
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualMachine other = (VirtualMachine) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public void setVirtualAppliance(VirtualAppliance virtualAppliance) {
		this.virtualAppliance = virtualAppliance;
	}

	@ManyToOne(optional = false)
    @JoinColumn(name = "VM_VA_ID", nullable = false, updatable = false)
	public VirtualAppliance getVirtualAppliance() {
		return virtualAppliance;
	}

	@Transient
	public String getDefaultNamedQuery() {
		// TODO Auto-generated method stub
		return null;
	}

}
