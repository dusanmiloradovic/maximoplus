package maximoplus;

import java.util.Map;

public interface Login {
    /* login method should return the username in Maximo */
    public LoginResponse login(Map headers, Map credentials);

    // For the ordinary login, the userName is enough. If multiple roundtrips
    // are required, than the other attributes of LoginResponse are filled(for
    // example Negotiate HTTP headers for the SPNEGO SSO login. In case multiple
    // round trips are required,
    // username should be set to null, and then the headers set in the
    // LoginResponse will be sent to the user, which will initiate additional
    // call from
    // the user browser with different HTTP headers.
}
