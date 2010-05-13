package br.unifor.onaga.rn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.unifor.onaga.ejb.entity.VirtualMachine;
import br.unifor.onaga.ejb.entity.WebContainerVM;

public class WebContainerRN {

	public WebContainerVM getNewWebContainerVM(WebContainerVM vm) {

		List<String> names = new ArrayList<String>();
		if (vm.getVirtualAppliance().getVirtualMachines() != null) {
			for (VirtualMachine lVm : vm.getVirtualAppliance()
					.getVirtualMachines()) {
				if (lVm instanceof WebContainerVM) {
					names.add(((WebContainerVM) lVm).getJk_route());
				}
			}
			Collections.sort(names);

			String name = vm.getVirtualAppliance().getName()+"_W_" + names.size();

			vm.setJk_route(name);

			vm.setName(name);
		}else{
			vm.setJk_route(vm.getVirtualAppliance().getName()+"_W_0");

			vm.setName(vm.getVirtualAppliance().getName()+"_W_0");
		}
		return vm;
	}
}
