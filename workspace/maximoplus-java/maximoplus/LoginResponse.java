package maximoplus;

import java.io.InputStream;
import java.util.Map;

public class LoginResponse {
    private Map httpHeaders;
    private int status;
    private InputStream body;
    private String userName;

    public Map getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Map httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public InputStream getBody() {
        return body;
    }

    public void setBody(InputStream body) {
        this.body = body;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
