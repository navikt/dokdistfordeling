package no.nav.dokdistfordeling.config.cxf;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

/**
 * Sets the password for the Usernametoken WS-Security
 *
 * @author Joakim Bjørnstad, Visma Consulting
 */
public class SystemuserPasswordCallback implements CallbackHandler {

	private final String systemUserPassword;

	public SystemuserPasswordCallback(String systemUserPassword) {
		this.systemUserPassword = systemUserPassword;
	}

	@Override
	public void handle(Callback[] callbacks) {
		WSPasswordCallback wsPasswordCallback = (WSPasswordCallback) callbacks[0];

		wsPasswordCallback.setPassword(systemUserPassword);
	}
}
