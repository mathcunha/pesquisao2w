package br.unifor.onaga.ejb.session;

import java.util.ArrayList;
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
import javax.persistence.Query;

import br.unifor.onaga.ejb.entity.ApacheVM;
import br.unifor.onaga.ejb.entity.OnagaEntityAB;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.VirtualMachine;
import br.unifor.onaga.ejb.entity.WebContainerVM;
import br.unifor.onaga.ejb.entity.WebContext;
import br.unifor.onaga.rn.WebContainerRN;

/**
 * Session Bean implementation class RegisterSession
 */
@Stateless(mappedName = "RegisterSession")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RegisterSession implements RegisterSessionRemote,
		RegisterSessionLocal {

	private WebContainerRN webContainerRn = new WebContainerRN();

	@PersistenceContext(unitName = "onaga")
	private EntityManager em;

	@PostConstruct
	public void initialize() {

	}

	@PreDestroy
	public void destroyBean() {

	}

	public List<OnagaEntityAB> findAll(VirtualAppliance virtual) {
		return em.createNamedQuery("findAllVirtualAppliance").getResultList();
	}
	
	public WebContext getOrInsert(WebContext entity) {
		WebContext retorno = entity;

		try {
			Query query = em.createNamedQuery(entity.getDefaultNamedQuery());
			query.setParameter("name",entity.getName());
			query.setParameter("id",entity.getVirtualAppliance().getId());
			retorno = (WebContext) query.getSingleResult();
		} catch (NoResultException e) {
			retorno = (WebContext) add(entity);
		}

		return retorno;
	}

	public OnagaEntityAB getOrInsert(OnagaEntityAB entity) {
		OnagaEntityAB retorno = entity;

		try {
			retorno = (OnagaEntityAB) em.createNamedQuery(
					entity.getDefaultNamedQuery()).setParameter("name",
					entity.getName()).getSingleResult();
		} catch (NoResultException e) {
			retorno = add(entity);
		}

		return retorno;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public OnagaEntityAB add(OnagaEntityAB onagaEntity) {
		em.persist(onagaEntity);
		return onagaEntity;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public WebContainerVM add(WebContainerVM onagaEntity) {
		onagaEntity
				.setVirtualAppliance((VirtualAppliance) getOrInsert(onagaEntity
						.getVirtualAppliance()));

		List<WebContext> contexts = new ArrayList<WebContext>();
		for (WebContext webContext : onagaEntity.getContexts()) {
			webContext.setVirtualAppliance(onagaEntity.getVirtualAppliance());
			contexts.add((WebContext) getOrInsert(webContext));
		}
		onagaEntity = webContainerRn.getNewWebContainerVM(onagaEntity);
		onagaEntity.setContexts(contexts);

		em.persist(onagaEntity);

		return onagaEntity;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ApacheVM add(ApacheVM onagaEntity) {
		try {
			ApacheVM retorno = (ApacheVM) em.createNamedQuery(
					onagaEntity.getDefaultNamedQuery()).setParameter("name",
					onagaEntity.getName()).getSingleResult();
			retorno.getContexts().size();
			
			onagaEntity = retorno;
			return retorno;
		} catch (NoResultException e) {
			onagaEntity
					.setVirtualAppliance((VirtualAppliance) getOrInsert(onagaEntity
							.getVirtualAppliance()));

			List<WebContext> contexts = new ArrayList<WebContext>();
			for (WebContext webContext : onagaEntity.getContexts()) {
				webContext.setVirtualAppliance(onagaEntity.getVirtualAppliance());
				contexts.add((WebContext) getOrInsert(webContext));
			}

			onagaEntity.setContexts(contexts);

			em.persist(onagaEntity);

			return onagaEntity;
		}

	}

	@Override
	public VirtualMachine get(VirtualMachine onagaEntity) {
		
		VirtualMachine lVirtual = em.find(VirtualMachine.class, onagaEntity.getId());
		if(ApacheVM.TYPE.equals(lVirtual.getType())){
			return em.find(ApacheVM.class, onagaEntity.getId());
		}else if(WebContainerVM.TYPE.equals(lVirtual.getType())){
			return em.find(WebContainerVM.class, onagaEntity.getId());
		}else{
			return lVirtual;
		}
	}
}
