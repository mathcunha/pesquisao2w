package br.unifor.onaga.ejb.session;

import java.util.List;

import javax.ejb.Remote;

import br.unifor.onaga.ejb.entity.ApacheVM;
import br.unifor.onaga.ejb.entity.OnagaEntityAB;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.VirtualMachine;
import br.unifor.onaga.ejb.entity.WebContainerVM;

@Remote
public interface RegisterSessionRemote {
	OnagaEntityAB add(OnagaEntityAB onagaEntity);

	WebContainerVM add(WebContainerVM onagaEntity);

	ApacheVM add(ApacheVM onagaEntity);

	List<OnagaEntityAB> findAll(VirtualAppliance virtual);

	OnagaEntityAB getOrInsert(OnagaEntityAB onagaEntity);
	
	VirtualMachine get(VirtualMachine onagaEntity);
}
