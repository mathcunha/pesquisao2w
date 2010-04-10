import javax.ejb.EJB;

import br.unifor.onaga.ejb.session.RegisterSessionRemote;


public class Main {
	@EJB
	RegisterSessionRemote session;
	public static void main(String[] args) {
		Main nada = new Main();
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