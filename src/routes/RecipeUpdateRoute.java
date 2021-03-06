package routes;

import java.io.IOException;
import java.util.List;

import sql.wrappers.RecipeUpdateWrapper;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.sun.net.httpserver.HttpExchange;

import core.ResponseCode;
import core.Server;
import core.json.UpdateItemJSON;

public class RecipeUpdateRoute extends Route {
	@Override
	public void handle(HttpExchange xchg) throws IOException {
		if (!"post".equalsIgnoreCase(xchg.getRequestMethod())) {respond(xchg, 404); return;}
		int householdID = (int)xchg.getAttribute("householdID");
		int userID = Server.sessionTable().authenticate(getToken(xchg));
		if (userID == -2) {error(xchg, ResponseCode.TOKEN_EXPIRED); return;}
		else if (userID == -1) {error(xchg, ResponseCode.INVALID_TOKEN); return;}

		int recipeID = (int)xchg.getAttribute("recipeID");
		String request = getRequest(xchg.getRequestBody());
		RecipeUpdateJSON ruj = null;
		try {
			ruj = gson.fromJson(request, RecipeUpdateJSON.class);
		} catch (JsonSyntaxException e) {
			error(xchg, ResponseCode.INVALID_PAYLOAD); return;
		}
		if (ruj == null || !ruj.valid()) { error(xchg, ResponseCode.INVALID_PAYLOAD); return;}
		RecipeUpdateWrapper ruw = new RecipeUpdateWrapper(userID, householdID, recipeID, ruj);
		ResponseCode result = ruw.update();
		if (!result.success()) 
			error(xchg, result);
		else {
			xchg.getResponseHeaders().set("ETag",
					"\"" + ruw.getVersion() + "\"");
			respond(xchg, 200, gson.toJson(ruw, RecipeUpdateWrapper.class));
		}
	}

	public static class RecipeUpdateJSON {
		@Expose(deserialize = true)
		public long version;
		@Expose(deserialize = true)
		public List<UpdateItemJSON> ingredients;
		@Expose(deserialize = true)
		public List<String> instructions;
		@Expose( deserialize = true)
		public String recipeName;
		@Expose( deserialize = true)
		public String recipeDescription;

		public boolean valid() {
			if (version < 0) return false;
			if (recipeName == null || recipeName.length() > 40) return false;
			if (recipeDescription == null || recipeDescription.length() > 128) return false;
			for (UpdateItemJSON i : ingredients) {
				if (!i.valid()) return false;
			}
			for (String s : instructions) {
				if (s == null || s.length() > 128) return false;
			}
			return true;
		}
	}

}
