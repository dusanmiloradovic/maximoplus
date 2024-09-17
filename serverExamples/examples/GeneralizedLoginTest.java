package examples;

import java.util.Map;

import maximoplus.Login;
import maximoplus.LoginResponse;

public class GeneralizedLoginTest implements Login {

	@Override
	public LoginResponse login(Map requestHTTPHeaders, Map credentials) {
		System.out.println("Calling the generalized login for username " + credentials.get("username"));
		LoginResponse ret = new LoginResponse();
		ret.setUserName((String) credentials.get("username"));
		return ret;
	}

}
