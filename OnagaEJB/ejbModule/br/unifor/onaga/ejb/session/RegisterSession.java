package br.unifor.onaga.ejb.session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import br.unifor.onaga.ejb.entity.VirtualMachine;

/**
 * Session Bean implementation class RegisterSession
 */
@Stateless(mappedName = "RegisterSession")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
	public VirtualMachine addVirtualMachine(String name, String ip, String info){
		return null;
	}
}
