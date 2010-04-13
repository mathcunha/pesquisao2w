import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;

import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.entity.VirtualMachine;
import br.unifor.onaga.ejb.entity.WebContainerVM;
import br.unifor.onaga.ejb.session.RegisterSessionRemote;


public class Main_JNDI {

	private static final String JNDI_NAME = "br.unifor.onaga.ejb.session.RegisterSession"+ "_" + RegisterSessionRemote.class.getName() + "@Remote";
	private static final String JNDI_NAME_PEQUENO = "RegisterSession";
	
	/**
     * Utility class. No public constructor
     */
    private Main_JNDI() {
    }

    /**
     * Main method.
     * @param args the arguments (not required)
     * @throws Exception if exception is found.
     */
    public static void main(final String[] args) throws Exception {
        Context initialContext = new InitialContext();

        RegisterSessionRemote businessItf =
           (RegisterSessionRemote) initialContext.lookup(JNDI_NAME_PEQUENO);

        VirtualAppliance appliance = new VirtualAppliance();
        appliance.setName(args[0]);
        
        appliance = (VirtualAppliance) businessItf.getOrInsert(appliance);
        
        
        
        
        VirtualMachine machine = new VirtualMachine();
        machine.setName(args[1]); 
        machine.setIp(args[2]);
        machine.setVirtualAppliance(appliance);
        businessItf.add(machine);
        
        WebContainerVM vm = new WebContainerVM(); 
        vm.setIp(machine.getIp()+"12");
        vm.setJmxUrl(JNDI_NAME+(new Date()));
        vm.setVirtualAppliance(appliance);
        businessItf.add(vm);
    }


}