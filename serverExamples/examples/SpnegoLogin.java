package examples;

import java.io.IOException;
import java.util.Map;

import maximoplus.Login;
import maximoplus.LoginResponse;

public class SpnegoLogin implements Login {

	@Override
	public LoginResponse login(Map headers, Map credentials) {
		LoginResponse ret = new LoginResponse();
		FakeFilterRequest req = new FakeFilterRequest(headers);
		FakeFilterResponse resp = new FakeFilterResponse();
		LoginAdapter adapter = new LoginAdapter(req, resp);
		try {
			String login = adapter.login();
			if (login != null) {
				ret.setUserName(login);
				return ret;
			}

			ret.setHttpHeaders(resp.getHeaders());
			ret.setStatus(resp.getStatus());
			// ret.setBody(resp.get)
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}

}
