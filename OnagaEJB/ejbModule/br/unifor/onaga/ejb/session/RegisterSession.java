package br.unifor.onaga.ejb.session;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

	public List<OnagaEntityAB> findAll(VirtualAppliance virtual){
		return em.createNamedQuery("findAllVirtualAppliance").getResultList();
	}
	
	public OnagaEntityAB getOrInsert(OnagaEntityAB entity) {
		OnagaEntityAB retorno = entity;
		
        try {
            retorno = (OnagaEntityAB) em.createNamedQuery(entity.getDefaultNamedQuery()).setParameter("name", entity.getName()).getSingleResult();            
        } catch (NoResultException e) {
            add(entity);
        }

        return retorno;
    }

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public OnagaEntityAB add(OnagaEntityAB onagaEntity) {
		
		em.persist(onagaEntity);
		System.out.println(onagaEntity.toString());
		return onagaEntity;
	}
}
