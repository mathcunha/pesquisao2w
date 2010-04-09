package br.unifor.onaga.ejb.session;
import javax.ejb.Remote;

import br.unifor.onaga.ejb.entity.VirtualMachine;

@Remote
public interface RegisterSessionRemote {
	VirtualMachine addVirtualMachine(String name, String ip, String info);
}
