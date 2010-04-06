package br.unifor.onaga.ejb.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name="VirtualAppliance")
@Table(name = "VA_VIRTUAL_APPLIANCE")
public class VirtualAppliance extends OnagaEntityAB {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private Long id;
	private List<VirtualMachine> virtualMachines;

	@Column(name = "VA_NAME", length = 255, nullable = false, unique=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
    @SequenceGenerator(name = "VA_ID", sequenceName = "SEQ_VA_VIRTUAL_APPLIANCE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VA_ID")
    @Column(name = "VA_ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@OneToMany(fetch=FetchType.EAGER, mappedBy="virtualAppliance")
	public List<VirtualMachine> getVirtualMachines() {
		return virtualMachines;
	}

	public void setVirtualMachines(List<VirtualMachine> virtualMachines) {
		this.virtualMachines = virtualMachines;
	}

	@Override
	public String toString() {
		return "VirtualAppliance [id=" + id + ", name=" + name
				+ ", virtualMachines=" + virtualMachines + "]";
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
		VirtualAppliance other = (VirtualAppliance) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
