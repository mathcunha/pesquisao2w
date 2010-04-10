package br.unifor.onaga.ejb.session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import br.unifor.onaga.ejb.entity.OnagaEntityAB;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.VirtualMachine;

/**
 * Session Bean implementation class RegisterSession
 */
@Stateless(mappedName = "RegisterSession")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RegisterSession implements RegisterSessionRemote,
		RegisterSessionLocal {

	@PersistenceContext(unitName = "onaga")
	private EntityManager em;

	@PostConstruct
	public void initialize() {

	}

	@PreDestroy
	public void destroyBean() {

	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public VirtualMachine addVirtualMachine(VirtualMachine virtualMachine) {

		em.persist(virtualMachine);
		System.out.println(virtualMachine.toString());
		return virtualMachine;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public VirtualAppliance addVirtualAppliance(
			VirtualAppliance virtualAppliance) {

		em.persist(virtualAppliance);
		System.out.println(virtualAppliance.toString());
		return virtualAppliance;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public OnagaEntityAB add(OnagaEntityAB onagaEntity) {
		if (onagaEntity instanceof VirtualMachine) {
			return addVirtualMachine((VirtualMachine) onagaEntity);
		} else if (onagaEntity instanceof VirtualAppliance) {
			return addVirtualAppliance((VirtualAppliance) onagaEntity);
		}
		em.flush();
		return null;
	}
}
