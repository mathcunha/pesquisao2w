package br.unifor.onaga.ejb.session;
import javax.ejb.Remote;

import br.unifor.onaga.ejb.entity.OnagaEntityAB;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.VirtualMachine;

@Remote
public interface RegisterSessionRemote {
	OnagaEntityAB add(OnagaEntityAB onagaEntity);
	
}
