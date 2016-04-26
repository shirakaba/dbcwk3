package uk.ac.bris.cs.databases.cwk3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.AdvancedForumSummaryView;
import uk.ac.bris.cs.databases.api.AdvancedForumView;
import uk.ac.bris.cs.databases.api.ForumSummaryView;
import uk.ac.bris.cs.databases.api.ForumView;
import uk.ac.bris.cs.databases.api.AdvancedPersonView;
import uk.ac.bris.cs.databases.api.PostView;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.api.SimpleForumSummaryView;
import uk.ac.bris.cs.databases.api.SimplePostView;
import uk.ac.bris.cs.databases.api.SimpleTopicView;
import uk.ac.bris.cs.databases.api.TopicView;

/**
 *
 * @author csxdb
 */
public class API implements APIProvider {

    private final Connection c;
    
    public API(Connection c) {
        this.c = c;
    }

    /*implemented by Alex & Phan*/
    @Override
    public Result<Map<String, String>> getUsers() {
        final String STMT = "SELECT username, name FROM Person;";
        Map <String, String> map = new HashMap<>();
        try(PreparedStatement p = c.prepareStatement(STMT)){
            ResultSet rs = p.executeQuery();
            while(rs.next()){
                map.put(rs.getString("username"), rs.getString("name"));
                System.out.println(rs.getString("username") + rs.getString("name"));
            }
            return Result.success(map);
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
    }
    
    // to Alex
    @Override
    public Result<PersonView> getPersonView(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // to Phan, but dependent on Alex populating db.
    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // to Phan
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    // to Jamie: gets all people who have liked a particular topic, ordering them alphabetically.
    // FINISHED but untested as it depends upon getPersonView.
    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        Long topicIDforStringification = topicId;
//        final String STMT = "SELECT PersonId FROM LikedTopic WHERE PersonId = ?;";
        // This gets the PersonIds, but would need to be joined to Person to get their usernames.
        final String STMT = "SELECT username FROM LikedTopic "
                + "INNER JOIN Person ON PersonId = Person.id "
                + "WHERE TopicId = ? " // replace this '?' with the input topicId
                + "ORDER BY username ASC;";
        List <PersonView> list = new ArrayList<>();
        PersonView currentPersonView;
        
        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  topicIDforStringification.toString());
            ResultSet rs = p.executeQuery();
            while(rs.next()){
                // May work!
                currentPersonView = getPersonView(rs.getString("username")).getValue();
                list.add(currentPersonView);
                System.out.println(currentPersonView);
            }
            return Result.success(list);
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
        
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    // to Jamie
    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
//      Long topicIDforStringification = topicId;
//      String title;
//      List<SimplePostView> posts = new ArrayList<>();
//      SimpleTopicView simpleTopicView = new SimpleTopicView(topicId, title, posts);
//
//
//
//      final String STMT = "SELECT title FROM Topic "
//              + "WHERE TopicId = ?;";
//      List <PersonView> list = new ArrayList<>();
//      SimplePostView simplePostView;
//
//      try(PreparedStatement p = c.prepareStatement(STMT)){
//          p.setString(1,  topicIDforStringification.toString());
//          ResultSet rs = p.executeQuery();
//          while(rs.next()){
//              simplePostView = new SimplePostView(0, STMT, STMT, 0); // note to self: fill in this constructor using SimplePostView.java
//              posts.add(simplePostView);
//              System.out.println(simplePostView);
//          }
//          return Result.success();
//      }catch(SQLException e){
//          return Result.failure(e.getMessage());
//      }

        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* 
     * Level 2 - standard difficulty. Most groups should get all of these.
     * They require a little bit more thought than the level 1 API though.
     */
    
    @Override
    public Result<PostView> getLatestPost(long topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createForum(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createPost(long topicId, String username, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* Implemented by Jamie */
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        
        final String STMT = "INSERT INTO Person (username, name, studentId) VALUES (?, ?, ?)";
        boolean myResult;
        
        
        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  username);
            p.setString(2, name);
            p.setString(3, studentId);
            
             myResult = p.execute();
             System.out.println("reaches execute");
        }catch(SQLException e){
            throw new UnsupportedOperationException("exception " + e);
        }
        
//      INSERT INTO Person (username, name, studentId) VALUES ("shirakaba", "Jamie", "jb15339");
        
        return Result.success();
        //Throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<ForumView> getForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<TopicView> getTopic(long topicId, int page) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result favouriteTopic(String username, long topicId, boolean fav) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedForumView> getAdvancedForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
   }
