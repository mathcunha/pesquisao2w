import java.util.ResourceBundle;

import javax.ejb.EJB;

import br.unifor.onaga.config.ApacheConfig;
import br.unifor.onaga.config.JonasWebContainerConfig;
import br.unifor.onaga.ejb.session.RegisterSessionRemote;


public class Main {
	
	public static void main(String[] args) {
		String type = ResourceBundle.getBundle("vm_onaga").getString("vm.type");
		if("JonasWebContainer".equals(type)){
			JonasWebContainerConfig.main(args);
		}else if("Apache".equals(type)){
			ApacheConfig config = new ApacheConfig();
			config.run();
		}
	}
}