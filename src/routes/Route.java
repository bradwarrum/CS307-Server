package routes;

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import core.ResponseCode;
import core.SessionToken;

public class Route implements HttpHandler{
	protected Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().excludeFieldsWithoutExposeAnnotation().create();
	@Override
	public void handle(HttpExchange xchg) throws IOException {
		xchg.sendResponseHeaders(404, -1);
		xchg.close();
	}
	
	protected String getRequest(InputStream stream) {
		byte[] chunk = new byte[512];
		StringBuffer request = new StringBuffer();
		int ofs = 0;
		int read = 0;
		try {
		do {
			read = stream.read(chunk, 0, 512);
			if (read > 0) {
				request.append(new String(chunk, 0, read));
				ofs += read;
				//Cut off chunking at 20KB
				if (ofs > 20000) return "";
			}
		}while (read == 512);
		}catch (IOException e) {
			System.out.println("WARNING: REQUEST BUFFER FAILURE");
			return "";
		}
		
		return request.toString();
	}
	
	/**
	 * Responds to the HttpRequest with the specified HttpStatus code. <p>
	 * This method does not write a response body.
	 * @param xchg The HttpExchange object for this request
	 * @param code The Http status code for the response
	 * @throws IOException
	 */
	protected void respond(HttpExchange xchg, int code) throws IOException {
		xchg.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		xchg.sendResponseHeaders(code, -1L);
		xchg.close();
	}
	
	/**
	 * Responds to the HttpExchange with a pre-formed, enumerated error code
	 * @param xchg The HttpExchange being responded to
	 * @param code The ErrorCode enumeration object used to create the response
	 * @throws IOException
	 */
	protected void error(HttpExchange xchg, ResponseCode code) throws IOException{
		xchg.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		xchg.getResponseHeaders().add("Content-Type", "application/json");
		String jsonError = code.toString();
		byte [] json = jsonError.getBytes();
		xchg.sendResponseHeaders(code.getHttpCode(), json.length);
		xchg.getResponseBody().write(json);
		xchg.close();
	}
	
	/**
	 * Responds to the HttpRequest with a general, pre-formed JSON payload
	 * @param xchg The HttpExchange object for this request
	 * @param code The Http status code for the response
	 * @param JSONresponse The JSON payload for this response
	 */
	protected void respond(HttpExchange xchg, int code, String JSONresponse) throws IOException {
		xchg.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		xchg.getResponseHeaders().add("Content-Type", "application/json");
		byte [] json = JSONresponse.getBytes();
		xchg.sendResponseHeaders(code, json.length);
		xchg.getResponseBody().write(json);
		xchg.close();
	}
	
	/**
	 * Returns the token passed in the URL query string.<p>
	 * @param xchg
	 * @return The token if the token exists, or null if it does not exist
	 */
	protected SessionToken getToken(HttpExchange xchg) {
		if (xchg.getRequestURI().getQuery() == null) return null;
		for (String p : xchg.getRequestURI().getQuery().split("&")) {
			if (p.startsWith("token=")) return SessionToken.fromString(p.substring(6));
		}
		return null;
	}

}
