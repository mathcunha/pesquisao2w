package br.unifor.onaga.config.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import br.unifor.onaga.ejb.session.RegisterSessionRemote;

public class Util {

	public static final String JNDI_REGISTER = "RegisterSession";

	public static RegisterSessionRemote registerSession;

	public static RegisterSessionRemote getRegisterSession()
			throws NamingException {
		if (registerSession != null) {
			return registerSession;
		} else {

			Context initialContext;

			initialContext = new InitialContext();
			registerSession = (RegisterSessionRemote) initialContext
					.lookup(Util.JNDI_REGISTER);
			return registerSession;
		}
	}
}
