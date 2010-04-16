package br.unifor.onaga.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.unifor.onaga.config.SimpleOnagaConfig.SimpleConfigInfo;
import br.unifor.onaga.config.util.Util;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.WebContainerVM;
import br.unifor.onaga.ejb.entity.WebContext;

public class JonasWebContainerConfig implements Runnable {

	protected final String JONAS_HOME;
	public static final String FILE_NAME = "tomcat6-server.xml";
	protected SimpleOnagaConfig simpleConfig;
	protected WebContainerVM vm;

	protected Logger log = Logger.getLogger(JonasWebContainerConfig.class
			.getName());
	protected ResourceBundle settingsResource;

	public JonasWebContainerConfig() {
		settingsResource = ResourceBundle.getBundle("vm_onaga");

		JONAS_HOME = settingsResource.getString("vm.web.home");

		simpleConfig = new SimpleOnagaConfig(JONAS_HOME + File.separator
				+ "conf" + File.separator + FILE_NAME, "<!--Onaga Begin-->",
				"<!--Onaga End-->");
	}

	@Override
	public void run() {
		config(settingsResource.getString("vm.virtualappliance"));
		simpleConfig.setConfigInfo(new JonasWebContainerConfigInfo(vm));
		simpleConfig.run();
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
		novaVM.setJmxUrl("http://" + novaVM.getIp()
				+ settingsResource.getString("vm.web.jmx_port"));

		String[] contextos = settingsResource.getString("vm.web.context")
				.split(",");
		novaVM.setContexts(new ArrayList<WebContext>());

		for (String contexto : contextos) {
			WebContext webContext = new WebContext();
			webContext.setName(contexto);
			novaVM.getContexts().add(webContext);
		}

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
			String connector = "<Connector port=\""+vm.getAjp_port()+"\" redirectPort=\"9043\" protocol=\"AJP/1.3\"/>";
			String config = "<Engine name=\""
					+ vm.getVirtualAppliance().getName()
					+ "\" defaultHost=\"localhost\" jvmRoute=\""
					+ vm.getJk_route() + "\">" + "\n" + "</Engine>";
			return connector +"\n"+config;
		}

	}

	public static void main(String[] args) {
		JonasWebContainerConfig lJonasWebContainerConfig = new JonasWebContainerConfig();
		lJonasWebContainerConfig.run();
	}
}
