package uk.ac.bris.cs.databases.cwk3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

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
import uk.ac.bris.cs.databases.api.SimpleTopicSummaryView;
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
    
    // implemented by the Alex
    @Override
    public Result<PersonView> getPersonView(String username) {
        final String STMT = "SELECT * FROM Person WHERE username = ?;";
        PersonView pv;

        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  username);

            ResultSet rs = p.executeQuery();

            pv = new PersonView(rs.getString("name"),
				rs.getString("username"),
				rs.getString("studentId"));
            /*System.out.println(pv.getName() + " "
				+ pv.getUsername() + " "
				+ pv.getStudentId());*/

            return Result.success(pv);
        }catch(SQLException e){
	    e.printStackTrace();
            return Result.failure(e.getMessage());
        }
    }

    // to Phan, but dependent on Alex populating db. (db has been populated)
    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        final String STMT = "SELECT id FROM Forum ORDER BY title ACS";
        List <SimpleForumSummaryView> list = new ArrayList <>();
        try(PreparedStatement p = c.prepareStatement(STMT)){
            ResultSet rs = p.executeQuery();
            while(rs.next()){
                SimpleForumSummaryView fv = new SimpleForumSummaryView(rs.getLong("id"), rs.getString("title"));
            }
            return Result.success(list);
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
    }

    // to Phan
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        final String STMT = "SELECT count(*) AS count FROM Topic" + " WHERE ForumId = ?";
        int count = 0;
        try(PreparedStatement p = c.prepareStatement(STMT)){
            ResultSet rs = p.executeQuery();
            while(rs.next()){
                count = rs.getInt("count");
                System.out.println(count);
            }
            return Result.success(count);
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
        
    }

    // TODO: Alex must implement getPersonView() for this to work.(getPersonView done)
    // to Jamie [FINISHED, untested]
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

    /* Design decision for report: the Post entity has no 'number within topic' attribute.
     * Pros: we don't have to repair that attribute for all Posts if one is deleted.
     * ... although actually, that function may not exist anyway.
     * Cons: we have to order by date every time and manually calculate the Post numbers, which
     * will take longer and longer as the Topic gets bigger and bigger. */

    // to Jamie [FINISHED, untested]
    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        List<SimplePostView> simplePostViews = new ArrayList<>();

        final String STMT =
                "SELECT title, " + // topicTitle
                "`name`, " + // author (of Post)
                "text, " + // text (of Post)
                "`date` " + // postedAt (int date of Post submission)

                "FROM Topic " +
                "INNER JOIN Post ON Topic.id = Post.TopicId " +
                "INNER JOIN Person ON Person.id = Post.PersonId " +
                "WHERE TopicId = ? ORDER BY `date` ASC;";

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
                        rs.getString("name"), // String author
                        rs.getString("text"), // String text
                        rs.getInt("date"))); // int postedAt

                System.out.println("Adding SimplePostView. " +
                        "postNumber = " + String.valueOf(postNumber) + "; " +
                        "author = " + rs.getString("name") + "; " +
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

    // To Jamie [FINISHED, untested]
    @Override
    public Result<PostView> getLatestPost(long topicId) {
        // gets the latest Post, along with info about who posted it.
        final String latestPostSTMT =
                "SELECT " +
                "`date`, " + // postedAt (int date of Post submission)
                "`name`, " + // authorName (of Post)
                "username, " + // authorUserName (of Post)
                "text " + // text (of Post)

                "FROM Topic " +
                "INNER JOIN Post ON Topic.id = Post.TopicId " +
                "INNER JOIN Person ON Person.id = Post.PersonId " +
                "WHERE Post.TopicId = ? " +
                "ORDER BY `date`, Post.id DESC " + // orders first by date, then by recentness of id in case of same-day post
                "LIMIT 1;";

        // gets the Forum id for the Topic in question.
        final String forumIdSTMT =
                "SELECT Forum.id AS forumId " + // forumId
                "FROM Topic " +
                "INNER JOIN Forum ON Forum.id = Topic.ForumId " +
                "WHERE Topic.id = ? " +
                "LIMIT 1;";

        // counts the number of likes for the Topic in question.
        final String likesSTMT =
                "SELECT count(TopicId) as c " +
                "FROM LikedTopic " +
                "WHERE TopicId = ?;";

        // determines the number of the latest Post by counting all Posts in a Topic.
        final String postNumberSTMT =
                "SELECT count(Post.id) AS postNumber " +
                "FROM POST " +
                "INNER JOIN Topic ON Topic.id = Post.TopicId " +
                "WHERE Post.TopicId = ?;";

        // tries communicating with the database.
        try(PreparedStatement latestPostP = c.prepareStatement(latestPostSTMT);
            PreparedStatement forumIdP = c.prepareStatement(forumIdSTMT);
            PreparedStatement likesP = c.prepareStatement(likesSTMT);
            PreparedStatement postNumberP = c.prepareStatement(postNumberSTMT)) {

            // sets all the '?' to be the topicId.
            latestPostP.setString(1, String.valueOf(topicId));
            forumIdP.setString(1, String.valueOf(topicId));
            likesP.setString(1, String.valueOf(topicId));
            postNumberP.setString(1, String.valueOf(topicId));

            // catches all the ResultSets of each executed query.
            ResultSet latestPostRS = latestPostP.executeQuery(),
                      forumIdRS = forumIdP.executeQuery(),
                      likesRS = likesP.executeQuery(),
                      postNumberRS = postNumberP.executeQuery();


            // gets the ints or Strings out of the ResultSets.
            long forumId = forumIdRS.getLong("forumId");
            int postNumber = postNumberRS.getInt("postNumber");
            String authorName = latestPostRS.getString("name");
            String authorUserName = latestPostRS.getString("username");
            String text = latestPostRS.getString("text");
            int postedAt = latestPostRS.getInt("date");
            int likes = likesRS.getInt("c");

            // just for debug.;
            System.out.println(String.format("Getting LatestPost...\n" +
                    "forumId = %d; \n" +
                    "topicId = %d; \n" +
                    "postNumber = %d; \n" +
                    "authorName = %s; \n" +
                    "authorUserName = %s; \n" +
                    "text = %s; \n" +
                    "postedAt = %d; \n" +
                    "likes = %d.",
                    forumId, topicId, postNumber, authorName, authorUserName, text, postedAt, likes
            ));

            return Result.success(new PostView(forumId, topicId, postNumber, authorName, authorUserName, text, postedAt, likes));
        } catch(SQLException e){
            return Result.failure(e.getMessage());
        }
    }

	//TO ALEX - DONE
    @Override
    public Result<List<ForumSummaryView>> getForums() {
        final String STMT = "SELECT * FROM Forum;";
        List<ForumSummaryView> ll = new ArrayList<>();
	SimpleTopicSummaryView stsv;
	
        try(PreparedStatement p = c.prepareStatement(STMT)){
            ResultSet rs = p.executeQuery();
	    String latestTopicIdSTMT;

            while(rs.next()){
		long currForumId = rs.getInt(1);
		latestTopicIdSTMT = "SELECT Topic.id, Topic.title From Topic" +
				    " JOIN Post ON Post.TopicId = Topic.id " +
				    " WHERE ForumId = " + currForumId + " ORDER BY date LIMIT 1;";

		PreparedStatement p1 = c.prepareStatement(latestTopicIdSTMT);
		ResultSet rs1 = p1.executeQuery();
		stsv = new SimpleTopicSummaryView(rs1.getLong(1), currForumId, rs1.getString("title"));
		/*System.out.println(rs.getLong(1) + " " + rs.getString("title") + " " + stsv.getTitle());*/
						   	    
		ForumSummaryView fsv = new ForumSummaryView(rs.getLong(1),
							    rs.getString("title"),
						   	    stsv);
                ll.add(fsv);
            }
            return Result.success(ll);
        }catch(SQLException e){
	    e.printStackTrace();
            return Result.failure(e.getMessage());
        }	
    }

    // REALLY EASY UNALLOCATED ONE HERE
    @Override
    public Result createForum(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	//TO ALEX - DONE
    @Override
    public Result createPost(long topicId, String username, String text) {

	long dateInSecs = new Date().getTime() / 1000;

	final String getPersonIdSTMT = "SELECT id FROM Person WHERE username = ?";
	long personId;
        try(PreparedStatement p = c.prepareStatement(getPersonIdSTMT)){
            p.setString(1, username);
            ResultSet rs = p.executeQuery();
	    personId = rs.getLong(1);


            final String STMT = "INSERT INTO Post (date,text,PersonId,TopicId) VALUES (?, ?, ?, ?);";

            PreparedStatement p1 = c.prepareStatement(STMT);

            p1.setLong(1, dateInSecs);
            p1.setString(2, text);
            p1.setLong(3, personId);
	    p1.setLong(4, topicId);
            
            p1.execute();
            c.commit(); 
        }catch(SQLException e){
            return Result.failure(e.getMessage());
        }
        return Result.success();
    }

    // to Jamie [FINISHED, untested]
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        final String STMT = "INSERT INTO Person (username, name, studentId) VALUES (?, ?, ?)";
        
        try(PreparedStatement p = c.prepareStatement(STMT)){
            p.setString(1,  username);
            p.setString(2, name);
            p.setString(3, studentId);
            
            p.execute();
            c.commit(); // tells the db driver to end the transaction.
        }catch(SQLException e){
//          throw new UnsupportedOperationException("exception " + e); // this was originally here, but prob. better to remove.
            // c.rollback(); // TODO: "Whenever you have done a write but not yet committed, make all exception handlers do an explicit rollback()."
                            // Assess whether this is the right scenario and place to use this. This would also need to be wrapped in a try-catch.
            return Result.failure(e.getMessage());
        }
        
        return Result.success();
    }

    @Override
    public Result<ForumView> getForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* Design decision: not limiting the number of posts of a topic to fetch. */

    // To Jamie
    @Override
    public Result<TopicView> getTopic(long topicId, int page) {
        final String forumIdSTMT =
                "SELECT Forum.id, " +
                        "Forum.name " + // forumId
                        "FROM Topic " +
                        "INNER JOIN Forum ON Forum.id = Topic.ForumId " +
                        "WHERE Topic.id = ? " +
                        "LIMIT 1;";

        final String postsOfTopicSTMT =
                "SELECT " +
                "Topic.title, " + // authorUserName (of Post)
                "text " + // text (of Post)

                "FROM POST " +
                "INNER JOIN Topic ON Topic.id = Post.TopicId " +
                "WHERE Post.TopicId = ? " +
                ";";

        // tries communicating with the database.
        try(PreparedStatement postsOfTopicP = c.prepareStatement(postsOfTopicSTMT);
            PreparedStatement forumIdP = c.prepareStatement(forumIdSTMT)) {
            // sets all the '?' to be the topicId.
            postsOfTopicP.setString(1, String.valueOf(topicId));
            forumIdP.setString(1, String.valueOf(topicId));

            // catches all the ResultSets of each executed query.
            ResultSet postsOfTopicRS = postsOfTopicP.executeQuery();
            ResultSet forumIdRS = forumIdP.executeQuery();

            // gets the ints or Strings out of the ResultSets.
            String forumName = forumIdRS.getString("Forum.name");
            String title = postsOfTopicRS.getString("Topic.title");
            long forumId = forumIdRS.getLong("Forum.id");

            // just for debug.;
//            System.out.println(String.format("Getting LatestPost...\n" +
//                            "forumId = %d; \n" +
//                            "topicId = %d; \n" +
//                            "postNumber = %d; \n" +
//                            "authorName = %s; \n" +
//                            "authorUserName = %s; \n" +
//                            "text = %s; \n" +
//                            "postedAt = %d; \n" +
//                            "likes = %d.",
//                    forumId, topicId, postNumber, authorName, authorUserName, text, postedAt, likes
//            ));

                    throw new UnsupportedOperationException("Not supported yet.");
//            return Result.success(new TopicView(forumId, topicId, forumName, title, posts, page));
        } catch(SQLException e){
            return Result.failure(e.getMessage());
        }

//        throw new UnsupportedOperationException("Not supported yet.");
    }

    //TO ALEX
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result favouriteTopic(String username, long topicId, boolean fav) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    /*
     * Level 3 - more complex queries. Leave these until last.
     */

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
