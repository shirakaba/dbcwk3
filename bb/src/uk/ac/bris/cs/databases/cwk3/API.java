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

    // TODO: Alex must implement getPersonView() for this to work.
    // to Jamie
    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        List <PersonView> list = new ArrayList<>();

        final String STMT = "SELECT username FROM LikedTopic "
                + "INNER JOIN Person ON PersonId = Person.id "
                + "WHERE TopicId = ? "
                + "ORDER BY username ASC;"; // ordering is required.
        
        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  String.valueOf(topicId));
            ResultSet rs = p.executeQuery();

            while(rs.next()){
                String username = rs.getString("username");
                // getValue() returns the value rather than just info about the result.
                list.add(getPersonView(username).getValue());

                System.out.println("Adding PersonView to List<PersonView>. " +
                        "username = " + username);
            }
            return Result.success(list);
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
    }

    // Design decision for report: the Post entity has no 'number within topic' attribute.
    // Pros: we don't have to repair that attribute for all Posts if one is deleted.
    // ... although actually, that function may not exist anyway.
    // Cons: we have to order by date every time and manually calculate the Post numbers, which
    // will take longer and longer as the Topic gets bigger and bigger.
    // to Jamie
    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        List<SimplePostView> simplePostViews = new ArrayList<>();

        final String STMT =
                "SELECT title, " + // topicTitle
                "username, " + // author (of Post)
                "text, " + // text (of Post)
                "\"date\" " + // postedAt (int date of Post submission)

                "FROM Topic " +
                "INNER JOIN Post ON Topic.id = Post.TopicId " +
                "INNER JOIN Person ON Person.id = Post.PersonId "
                + "WHERE TopicId = ? ORDER BY date ASC;";

        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  String.valueOf(topicId));
            ResultSet rs = p.executeQuery();

            String topicTitle = rs.getString("title");
            System.out.println(String.format("Identified %s as the topic's title.", topicTitle));

            // TODO: assess whether this method of counting posts is the best way to do it.
            int postNumber = 0;
            while(rs.next()){
                postNumber++;

                simplePostViews.add(new SimplePostView(
                        postNumber, // int postNumber
                        rs.getString("username"), // String author
                        rs.getString("text"), // String text
                        rs.getInt("date"))); // int postedAt

                System.out.println("Adding SimplePostView. " +
                        "postNumber = " + String.valueOf(postNumber) + "; " +
                        "author = " + rs.getString("username") + "; " +
                        "text = " + rs.getString("text") + "; " +
                        "postedAt = " + rs.getInt("date"));
            }
            return Result.success(new SimpleTopicView(topicId, topicTitle, simplePostViews));
        } catch(SQLException e){
            return Result.failure(e.getMessage());
        }
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
            c.commit(); // tells the db driver to end the transaction.
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
