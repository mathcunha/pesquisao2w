package br.unifor.onaga.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.unifor.onaga.config.JonasWebContainerConfig.JonasWebContainerConfigInfo;
import br.unifor.onaga.config.SimpleOnagaConfig.SimpleConfigInfo;
import br.unifor.onaga.config.util.Util;
import br.unifor.onaga.ejb.entity.ApacheVM;
import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.WebContainerVM;
import br.unifor.onaga.ejb.entity.WebContext;

public class ApacheConfig implements Runnable {

	public static final String FILE_NAME = "workers.properties";
	public final String CONF_DIR;
	protected SimpleOnagaConfig simpleConfig;
	protected ApacheVM vm;
	protected Logger log = Logger.getLogger(ApacheConfig.class.getName());
	protected ResourceBundle settingsResource;

	public ApacheConfig() {

		settingsResource = ResourceBundle.getBundle("vm_onaga");
		CONF_DIR = settingsResource.getString("vm.web.home");

		simpleConfig = new SimpleOnagaConfig(CONF_DIR + File.separator
				+ FILE_NAME, "##Onaga Begin##", "##Onaga End##");
	}

	public void config() {
		VirtualAppliance appliance = new VirtualAppliance();
		appliance.setName(settingsResource.getString("vm.virtualappliance"));

		ApacheVM novaVM = new ApacheVM();
		novaVM.setVirtualAppliance(appliance);
		novaVM.setName(settingsResource.getString("vm.name"));
		String ip = settingsResource.getString("vm.ip");
		if (ip == null) {
			try {
				novaVM.setIp(InetAddress.getLocalHost().getHostAddress());
			} catch (UnknownHostException e) {
				log.log(Level.SEVERE, "ip nao disponivel", e);
			}
		} else {
			novaVM.setIp(ip);
		}

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

	@Override
	public void run() {
		config();
		simpleConfig.setConfigInfo(new ApacheConfigInfo(vm));
		simpleConfig.run();

	}

	public class ApacheConfigInfo implements SimpleConfigInfo {
		ApacheVM vm;

		public ApacheConfigInfo(ApacheVM vm) {
			this.vm = vm;
		}

		@Override
		public String getConfInfo() {
			String config = "";
			String worker_list = "#----------------------- \n# List the workers name\n#-----------------------\nworker.list=";
			String[] worker_balance = null;
			List<String> worker = new ArrayList<String>();

			if (vm.getContexts() != null && vm.getContexts().size() > 0) {
				worker_balance = new String[vm.getContexts().size()];

				for (int i = 0; i < worker_balance.length; i++) {
					if (i == 0) {
						worker_balance[i] = "#-----------------------\n# Load Balancer worker\n#-----------------------\n";
					} else {
						worker_balance[i] = "\n";
					}
					worker_balance[i] += "worker."
							+ vm.getContexts().get(i).getName() + ".type=lb\n";

					String vms = "";

					if (vm.getContexts().get(i).getWebVMs() != null
							&& vm.getContexts().get(i).getWebVMs().size() > 0) {

						worker_list += vm.getContexts().get(i).getName() + ",";

						for (WebContainerVM webVM : vm.getContexts().get(i)
								.getWebVMs()) {
							String lWorker = "#-----------------------\n# "
									+ webVM.getName()
									+ "\n#-----------------------\n"
									+ "worker." + webVM.getName() + ".port="
									+ webVM.getAjp_port() + "\n" + "worker."
									+ webVM.getName() + ".host="
									+ webVM.getIp() + "\n" + "worker."
									+ webVM.getName() + ".type=ajp13 \n"
									+ "worker." + webVM.getName()
									+ ".lbfactor=1 \n";

							worker.add(lWorker);
							vms += webVM.getName() + ",";
						}

						if (vms.length() > 0) {
							vms = vms.substring(0, vms.length() - 1);
						}

						worker_balance[i] += "worker."
								+ vm.getContexts().get(i).getName()
								+ ".balance_workers=" + vms + "\n";
						vms = "";
					} else {
						worker_balance[i] = "";
					}
				}

				for (String string : worker) {
					config += string;
				}
				for (String string : worker_balance) {
					config += string;
				}
			}

			worker_list += "jkstatus\n";

			config = worker_list + config + "worker.jkstatus.type=status \n";

			return config;
		}

	}

}
