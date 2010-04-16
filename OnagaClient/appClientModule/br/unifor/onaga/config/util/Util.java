package br.unifor.onaga.config.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import br.unifor.onaga.ejb.session.RegisterSessionRemote;

public class Util {

	public static final String JNDI_REGISTER = "RegisterSession";
	protected static Logger log = Logger.getLogger(Util.class
			.getName());
	public static RegisterSessionRemote registerSession;

	public static RegisterSessionRemote getRegisterSession() {
		if (registerSession != null) {
			return registerSession;
		} else {

			Context initialContext;

			try {
				initialContext = new InitialContext();
				registerSession = (RegisterSessionRemote) initialContext
				.lookup(Util.JNDI_REGISTER);
			} catch (NamingException e) {
				log.log(Level.SEVERE, "register session nao disponivel", e);
			}
			
			return registerSession;
		}
	}
}
