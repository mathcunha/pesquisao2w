import java.util.ResourceBundle;

import javax.ejb.EJB;

import br.unifor.onaga.config.JonasWebContainerConfig;
import br.unifor.onaga.ejb.session.RegisterSessionRemote;


public class Main {
	
	public static void main(String[] args) {
		String type = ResourceBundle.getBundle("vm_onaga").getString("vm.type");
		if("JonasWebContainer".equals(type)){
			JonasWebContainerConfig.main(args);
		}		
	}

	/* (non-Java-doc)
	 * @see java.lang.Object#Object()
	 */
	public Main() {
		super();
		System.out.println("Matheus Cunha!");
		//session.addVirtualMachine("matheus", "ip", "info");
	}

}