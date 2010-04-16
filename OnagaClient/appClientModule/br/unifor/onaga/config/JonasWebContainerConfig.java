package br.unifor.onaga.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.unifor.onaga.config.SimpleOnagaConfig.SimpleConfigInfo;
import br.unifor.onaga.config.util.Util;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.WebContainerVM;

public class JonasWebContainerConfig implements Runnable {

	protected final String JONAS_HOME;
	public static final String FILE_NAME = "tomcat6-server.xml";
	protected SimpleOnagaConfig simpleConfig;
	protected WebContainerVM vm;
	protected String virtualAppliance;
	protected Logger log = Logger.getLogger(JonasWebContainerConfig.class
			.getName());

	public JonasWebContainerConfig(String jonasHome, String virtualAppliance) {
		JONAS_HOME = jonasHome;
		simpleConfig = new SimpleOnagaConfig(JONAS_HOME + File.separator
				+ "conf" + File.separator + FILE_NAME);
		
		this.virtualAppliance = virtualAppliance;
	}

	@Override
	public void run() {
		config(virtualAppliance);
		simpleConfig.run();
		simpleConfig.setConfigInfo(new JonasWebContainerConfigInfo(vm));
	}

	public void config(String virtualAppliance) {
		VirtualAppliance appliance = new VirtualAppliance();
		appliance.setName(virtualAppliance);
		
		

		WebContainerVM novaVM = new WebContainerVM();
		novaVM.setVirtualAppliance(appliance);
		try {
			novaVM.setIp(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			log.log(Level.SEVERE, "ip nao disponivel", e);
		}
		novaVM.setJmxUrl("http://"+novaVM.getIp()+":1099/");
		
		novaVM = Util.getRegisterSession().add(novaVM);
		this.vm = novaVM;
	}

	public class JonasWebContainerConfigInfo implements SimpleConfigInfo {
		WebContainerVM vm;

		public JonasWebContainerConfigInfo(WebContainerVM vm) {
			this.vm = vm;
		}

		@Override
		public String getConfInfo() {
			String config = "<Engine name=\""
					+ vm.getVirtualAppliance().getName()
					+ "\" defaultHost=\"localhost\" jvmRoute=\""
					+ vm.getJk_route() + "\">" + "\n" + "</Engine>";
			// TODO Auto-generated method stub
			return config;
		}

	}
	public static void main(String[] args){
		JonasWebContainerConfig lJonasWebContainerConfig = new JonasWebContainerConfig(args[0], args[1]);
		lJonasWebContainerConfig.run();		
	}
}
