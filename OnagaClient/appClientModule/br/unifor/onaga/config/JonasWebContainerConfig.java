package br.unifor.onaga.config;

import java.io.File;

import javax.naming.NamingException;

import br.unifor.onaga.config.SimpleOnagaConfig.SimpleConfigInfo;
import br.unifor.onaga.config.util.Util;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.WebContainerVM;

public class JonasWebContainerConfig implements Runnable{
	
	protected final String JONAS_HOME;
	public static final String FILE_NAME = "tomcat6-server.xml";
	protected SimpleOnagaConfig simpleConfig;
	protected WebContainerVM vm;
	
	public JonasWebContainerConfig(String jonasHome, WebContainerVM vm){
		JONAS_HOME = jonasHome;
		simpleConfig = new SimpleOnagaConfig(JONAS_HOME+ File.separator + "conf" + File.separator + FILE_NAME);
		this.vm = vm;
	}

	@Override
	public void run() {
		simpleConfig.run();
		simpleConfig.setConfigInfo(new JonasWebContainerConfigInfo(vm));
	}
	
	public void config(String virtualAppliance){
		try {
			
			VirtualAppliance appliance = new VirtualAppliance();
	        appliance.setName(virtualAppliance);
	        
	        appliance = (VirtualAppliance) Util.getRegisterSession().getOrInsert(appliance);
			
			JonasWebContainerConfig lJonasWebContainerConfig = new JonasWebContainerConfig(null, null);
			lJonasWebContainerConfig.run();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	public class JonasWebContainerConfigInfo implements SimpleConfigInfo{
		WebContainerVM vm;
		public JonasWebContainerConfigInfo(WebContainerVM vm){
			this.vm = vm;
		}

		@Override
		public String getConfInfo() {
			String config = "<Engine name=\""+vm.getVirtualAppliance().getName()+"\" defaultHost=\"localhost\" jvmRoute=\""+vm.getJk_route()+"\">"+"\n"+
		    "</Engine>";
			// TODO Auto-generated method stub
			return config;
		}
		
	}

}
