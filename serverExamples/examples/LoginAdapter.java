package examples;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.spnego.SpnegoAuthenticator;
import net.sourceforge.spnego.SpnegoHttpFilter.Constants;
import net.sourceforge.spnego.SpnegoHttpServletResponse;
import net.sourceforge.spnego.SpnegoPrincipal;

import org.ietf.jgss.GSSException;

public class LoginAdapter {

	private SpnegoAuthenticator authenticator;
	private HttpServletRequest fakeServletRequest;
	private HttpServletResponse fakeServletResponse;
	private static final Logger LOGGER = Logger.getLogger("SPNEGOLogger");

	public LoginAdapter(HttpServletRequest fakeServletRequest, HttpServletResponse fakeServletResponse) {

		this.fakeServletRequest = fakeServletRequest;
		this.fakeServletResponse = fakeServletResponse;
		Map<String, String> kerberosConfig = new HashMap<String, String>();

		kerberosConfig.put("spnego.allow.basic", "false");
		kerberosConfig.put("spnego.allow.localhost", "true");
		kerberosConfig.put("spnego.allow.unsecure.basic", "true");
		kerberosConfig.put("spnego.login.client.module", "spnego-client");
		kerberosConfig.put("spnego.krb5.conf", "krb5.conf");
		kerberosConfig.put("spnego.login.conf", "login.conf");
		kerberosConfig.put("spnego.preauth.username", System.getProperty("spnego.preauth.username"));
		kerberosConfig.put("spnego.preauth.password", System.getProperty("spnego.preauth.password"));
		kerberosConfig.put("spnego.login.server.module", "spnego-server");
		kerberosConfig.put("spnego.prompt.ntlm", "false");
		kerberosConfig.put("spnego.logger.level", "1");
		kerberosConfig.put("spnego.allow.delegation", "false");

		try {
			authenticator = new SpnegoAuthenticator(kerberosConfig);
		} catch (LoginException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (GSSException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (PrivilegedActionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public String login() throws IOException {
		final SpnegoHttpServletResponse spnegoResponse = new SpnegoHttpServletResponse((HttpServletResponse) fakeServletResponse);
		try {
			SpnegoPrincipal principal = this.authenticator.authenticate(fakeServletRequest, spnegoResponse);
			if (spnegoResponse.isStatusSet()) {
				return null;// when the null is returned than it should go and
							// read the httpresponse and send it
			}
			if (null == principal) {
				LOGGER.severe("Principal was null.");
				spnegoResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, true);
				return null;
			}
			LOGGER.fine("principal=" + principal);
			String realm = principal.getRealm();
			String fullName = principal.getName();
			return fullName.substring(0, fullName.indexOf(realm) - 1);

		} catch (GSSException gsse) {
			LOGGER.severe("HTTP Authorization Header=" + fakeServletRequest.getHeader(Constants.AUTHZ_HEADER));
			throw new RuntimeException(gsse);
		}
	}
}
