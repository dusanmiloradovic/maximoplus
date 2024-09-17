package examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class FakeFilterResponse implements HttpServletResponse {
	private int status;
	private Map<String, String> headers = new HashMap<String, String>();

	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBufferSize(int i) {
		// TODO Auto-generated method stub

	}

	public void setCharacterEncoding(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setContentLength(int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setContentType(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLocale(Locale locale) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addCookie(Cookie cookie) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addDateHeader(String s, long l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addHeader(String name, String value) {
		// According to
		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
		// we separate multiple values with comma
		String existingHeaderValue = headers.get(name);
		if (existingHeaderValue == null) {
			headers.put(name, value);
		} else {
			headers.put(name, existingHeaderValue + "," + value);
		}

	}

	@Override
	public void addIntHeader(String s, int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean containsHeader(String s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String encodeRedirectURL(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeRedirectUrl(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeURL(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeUrl(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendError(int i) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendError(int i, String s) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendRedirect(String s) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDateHeader(String s, long l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHeader(String name, String value) {
		headers.put(name, value);

	}

	@Override
	public void setIntHeader(String s, int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(int status) {
		this.status = status;

	}

	@Override
	public void setStatus(int i, String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getStatus() {
		return status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

}
