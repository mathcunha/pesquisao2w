package br.unifor.onaga.rn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.unifor.onaga.ejb.entity.VirtualMachine;
import br.unifor.onaga.ejb.entity.WebContainerVM;

public class WebContainerRN {

	public WebContainerVM getNewWebContainerVM(WebContainerVM vm){
		
		List<String> names = new ArrayList<String>();
		for (VirtualMachine lVm : vm.getVirtualAppliance().getVirtualMachines()) {
			if(lVm instanceof WebContainerVM){
				names.add(((WebContainerVM) lVm).getJk_route());
			}
			Collections.sort(names);
			
			String name = "W_"+names.size();
			
			vm.setJk_route(name);
			
			vm.setName(name);
			
		}
		
		return vm;
	}
}