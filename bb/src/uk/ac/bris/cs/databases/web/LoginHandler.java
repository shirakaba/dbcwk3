package uk.ac.bris.cs.databases.web;

import fi.iki.elonen.NanoHTTPD;
import java.util.Map;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.Result;

/**
 *
 * @author David
 */
public class LoginHandler extends SimpleHandler {

    String username = "";

    @Override
    void handleCookies(NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.CookieHandler h = session.getCookies();
        if (username.equals("")) {
            h.delete("user");
        } else {
            h.set("user", username + ";Path=/", 1);
        }
    }

    @Override
    boolean needsParameter() {
        return false;
    }
    
    @Override
    RenderPair simpleRender(String p) throws RenderException {
        if (p == null || p.equals("")) {
            username = "";
            return new RenderPair("Success.ftl", Result.success(new ValueHolder(
                "Logged out.")));
        } else {
            APIProvider api = ApplicationContext.getInstance().getApi();
            Result<Map<String, String>> r = api.getUsers();
            if (!r.isSuccess()) {
                return new RenderPair(null, Result.fatal("API call failed."));
            }
            Map<String, String> users = r.getValue();
            String name = users.get(p);
            
            if (name == null) {
                return new RenderPair(null, Result.failure("No such user."));
            }
            
            username = p;
            return new RenderPair("Success.ftl", Result.success(new ValueHolder(
                "Logged in as " + name)));
        }
    }

    
    
}
