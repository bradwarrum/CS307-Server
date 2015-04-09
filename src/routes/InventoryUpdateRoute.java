package routes;

import java.io.IOException;
import java.util.List;

import sql.wrappers.InventoryUpdateWrapper;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.sun.net.httpserver.HttpExchange;

import core.ResponseCode;
import core.Server;

public class InventoryUpdateRoute extends Route {
	@Override
	public void handle(HttpExchange xchg) throws IOException {
		if (!"post".equalsIgnoreCase(xchg.getRequestMethod())) {respond(xchg, 404); return;}
		
		int userID = Server.sessionTable().authenticate(getToken(xchg));
		if (userID == -2) {error(xchg, ResponseCode.TOKEN_EXPIRED); return;}
		else if (userID == -1) {error(xchg, ResponseCode.INVALID_TOKEN); return;}
		
		int householdID = (int)xchg.getAttribute("householdID");
		String request = getRequest(xchg.getRequestBody());
		InventoryUpdateJSON luj = null;
		try {
			luj = gson.fromJson(request, InventoryUpdateJSON.class);
		} catch (JsonSyntaxException e) {
			error(xchg, ResponseCode.INVALID_PAYLOAD); return;
		}
		if (luj == null || !luj.valid()) { error(xchg, ResponseCode.INVALID_PAYLOAD); return;}
		
		InventoryUpdateWrapper iuw = new InventoryUpdateWrapper(userID, householdID, luj.version, luj.items);
		ResponseCode result = iuw.update();
		if (!result.success()) 
			error(xchg, result);
		else {
			xchg.getResponseHeaders().set("ETag",
					"\"" + iuw.getVersion() + "\"");
			respond(xchg, 200, gson.toJson(iuw, InventoryUpdateWrapper.class));
		}
	}
	
	public static class InventoryUpdateJSON {
		@Expose(deserialize = true)
		public long version;
		@Expose(deserialize = true)
		public List<InventoryUpdateItemJSON> items;
		
		public boolean valid() {
			if (version < 0) return false;
			if (items == null) return false;
			for (InventoryUpdateItemJSON l : items) {
				if (!l.valid()) return false;
			}
			return true;
		}
	}
	public static class InventoryUpdateItemJSON {
		@Expose(deserialize = true)
		public String UPC;
		@Expose(deserialize = true)
		public int quantity;
		@Expose(deserialize = true)
		public int fractional = 0;
		
		public boolean valid() {
			if (UPC == null || UPC.length() > 13 ) return false;
			if (quantity < 0) return false;
			if (fractional <0 || fractional > 99) return false;
			return true;
		}
	}
}