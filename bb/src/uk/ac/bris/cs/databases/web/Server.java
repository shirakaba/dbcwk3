/*
 * Mini implementation forum server and UI. 
 */
package uk.ac.bris.cs.databases.web;

import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.util.ServerRunner;
import freemarker.template.Configuration;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.cwk3.API;

/**
 * @author csxdb
 */
public class Server extends RouterNanoHTTPD {
    
    private static final String DATABASE = "jdbc:sqlite:database/database.sqlite3";

    public Server() {
        super(8000);
        addMappings();
    }
    
    @Override public void addMappings() {
        super.addMappings();
        addRoute("/person/:id", PersonHandler.class);
        addRoute("/person2/:id", AdvancedPersonHandler.class);
        addRoute("/people", PeopleHandler.class);
        addRoute("/newtopic", NewTopicHandler.class);
        addRoute("/forums0", SimpleForumsHandler.class);
        addRoute("/forums", ForumsHandler.class);
        addRoute("/forums2", AdvancedForumsHandler.class);
        addRoute("/forum/:id", ForumHandler.class);
        addRoute("/forum2/:id", AdvancedForumHandler.class);
        addRoute("/topic/:id", TopicHandler.class);
        addRoute("/topic0/:id", SimpleTopicHandler.class);
        
        addRoute("/newforum", NewForumHandler.class);
        addRoute("/createforum", CreateForumHandler.class);
        
        addRoute("/newtopic/:id", NewTopicHandler.class);
        addRoute("/createtopic", CreateTopicHandler.class);
        
        addRoute("/newpost/:id", NewPostHandler.class);
        addRoute("/createpost", CreatePostHandler.class);
        
        addRoute("/newperson", NewPersonHandler.class);
        addRoute("/createperson", CreatePersonHandler.class);

        addRoute("/login", LoginHandler.class);
        addRoute("/login/:id", LoginHandler.class);
        
        addRoute("/styles.css", StyleHandler.class, "resources/styles.css");
        addRoute("/gridlex.css", StyleHandler.class, "resources/gridlex.css");
    }
    
    public static void main(String[] args) throws Exception {
        
        ApplicationContext c = ApplicationContext.getInstance();

        // database //
        
        Connection conn;
        try {
            conn = DriverManager.getConnection(DATABASE);
            conn.setAutoCommit(false);
            APIProvider api = new API(conn);
            c.setApi(api);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // templating //
        
        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setDirectoryForTemplateLoading(new File("resources/templates"));
        cfg.setDefaultEncoding("UTF-8");
        c.setTemplateConfiguration(cfg);
        
        // server //
        
        Server server = new Server();
        ServerRunner.run(Server.class);
    }
}
