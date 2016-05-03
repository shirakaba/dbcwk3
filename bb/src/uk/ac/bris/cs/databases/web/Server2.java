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
import java.sql.Statement;

import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.cwk3.API;

/**
 * @author csxdb
 */
public class Server2 extends RouterNanoHTTPD {
    
    private static final String DATABASE = "jdbc:sqlite:database/database.sqlite3";

    public Server2() {
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
        addRoute("/forums", ForumsHandler.class); //getSimpleForums
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
        ;
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
        
        //Server server = new Server();
        //ServerRunner.run(Server.class);

        try (Statement s = conn.createStatement()) {
            s.executeQuery("select * from post");
        } catch (SQLException e) {
            throw new RuntimeException("database not up - " + e);
        }

		APIProvider api = c.getApi();
		Result rs;
    		rs = api.createTopic(1, "shirakaba2", "NewTopic", "hello, I am information :)");
		rs = api.likeTopic("shirakaba2", 1, false);
//		rs = api.getForums();
//		rs = api.addNewPerson("Jamie2", "shirakaba2", "jb153392");
//		rs = api.getLatestPost(1);

//        if (!rs.isSuccess()) {
//            System.out.println(rs.getMessage());
//        }
//		rs = api.createPost(6, "username2", "Gladiators is quite possibly... the greatest film every.");
//      rs = api.getSimpleForums();
        rs = api.countPostsInTopic(1);

//		rs.close();
		conn.close();
    }
}
