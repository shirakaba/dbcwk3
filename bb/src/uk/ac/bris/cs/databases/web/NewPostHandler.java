package uk.ac.bris.cs.databases.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.Result;

/**
 *
 * @author csxdb
 */
public class NewPostHandler extends SimpleHandler {

    @Override
    RenderPair simpleRender(String p) throws RenderException {
        APIProvider api = ApplicationContext.getInstance().getApi();
        Result<Map<String, String>> userMap = api.getUsers();
        if (!userMap.isSuccess()) {
            throw new RenderException(500, "Failed to get users.");
        }
        List<String> unames = new ArrayList<>(userMap.getValue().keySet());
        
        Map<String,Object> data = new HashMap<>();
        data.put("topic", p);
        data.put("users", unames);
        
        return new RenderPair("NewPostView.ftl", Result.success(data));
    }    
}
