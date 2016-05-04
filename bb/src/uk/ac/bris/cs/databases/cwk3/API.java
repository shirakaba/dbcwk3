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
    private final static int MS_TO_SECONDS = 1000;
    private final Connection c;

    public API(Connection c) {
        this.c = c;
    }

    /* implemented by Alex & Phan [tested] */
    @Override
    public Result<Map<String, String>> getUsers() {
        final String STMT = "SELECT username, name FROM Person;";
        Map<String, String> map = new HashMap<>();

        try (PreparedStatement p = c.prepareStatement(STMT)) {

            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                map.put(rs.getString("username"), rs.getString("name"));
            }

            return Result.success(map);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    // implemented by Alex [tested]
    @Override
    public Result<PersonView> getPersonView(String username) {
        final String STMT = "SELECT name, username, studentId FROM Person WHERE username = ?;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            return Result.success(new PersonView(
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("studentId")));
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
    }

    // implemented by Phan [tested]
    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        final String STMT = "SELECT id, title FROM Forum ORDER BY title DESC;";
        List<SimpleForumSummaryView> list = new ArrayList<>();

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                list.add(new SimpleForumSummaryView(rs.getLong("id"), rs.getString("title")));
            }

            return Result.success(list);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    // implemented by Phan [seems to be working in SQLite]
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        final String STMT = "SELECT count(*) FROM Post WHERE TopicId = ?;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet rs = p.executeQuery();

            return Result.success(rs.getInt("count"));
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    // TODO: topicId "must exist", but is that handled by the schema alone? long is primitive type, so can't be null.
    // TODO: Should we return Result.fatal() if topicId is found to not exist?
    // to Jamie [FINISHED, tested]
    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        List<PersonView> list = new ArrayList<>();

        final String STMT = "SELECT name, username, studentId FROM LikedTopic "
                + "INNER JOIN Person ON PersonId = Person.id "
                + "WHERE TopicId = ? "
                + "ORDER BY name ASC;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                list.add(new PersonView(rs.getString("name"), rs.getString("username"), rs.getString("studentId")));
            }

            return Result.success(list);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    /* Design decision for report: the Post entity has no 'number within topic' attribute.
     * Pros: we don't have to repair that attribute for all Posts if one is deleted.
     * ... although actually, that function may not exist anyway.
     * Cons: we have to order by date every time and manually calculate the Post numbers, which
     * will take longer and longer as the Topic gets bigger and bigger. */

    // to Jamie [FINISHED, tested]
    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        List<SimplePostView> simplePostViews = new ArrayList<>();

        final String STMT =
                "SELECT title, `name`, `text`, `date` FROM Topic " +
                "INNER JOIN Post ON Topic.id = Post.TopicId " +
                "INNER JOIN Person ON Person.id = Post.PersonId " +
                "WHERE TopicId = ? ORDER BY `date` ASC;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1, String.valueOf(topicId));
            ResultSet rs = p.executeQuery();

            String topicTitle = rs.getString("title");
            System.out.println(String.format("Identified %s as the topic's title.", topicTitle));

            for (int postNumber = 1; rs.next(); postNumber++) {
                simplePostViews.add(new SimplePostView(
                        postNumber,
                        rs.getString("name"),
                        rs.getString("text"),
                        rs.getInt("date")));

                System.out.println(String.format("Adding SimplePostView. " +
                        "postNumber = %s; author = %s; text = %s; postedAt = %d",
                        String.valueOf(postNumber), rs.getString("name"), rs.getString("text"), rs.getInt("date")));
            }

            return Result.success(new SimpleTopicView(topicId, topicTitle, simplePostViews));
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    /* 
     * Level 2 - standard difficulty. Most groups should get all of these.
     * They require a little bit more thought than the level 1 API though.
     */

    // TODO: what if topicId doesn't exist?
    // To Jamie [FINISHED, tested]
    @Override
    public Result<PostView> getLatestPost(long topicId) {
        final String latestPostSTMT =
                "SELECT `date`, `name`, username, text, Forum.id AS forumId, count(*) AS postNumber FROM Topic " +
                "INNER JOIN Post ON Topic.id = Post.TopicId " +
                "INNER JOIN Person ON Person.id = Post.PersonId " +
                "INNER JOIN Forum ON Forum.id = Topic.ForumId " +
                "WHERE Post.TopicId = ? " +
                "ORDER BY `date` DESC, Post.id DESC " + // orders first by date, then by size (newness) of id in case of same-day post
                "LIMIT 1;";

        // TODO: generalise this count statement for re-use in countPostsInTopic().
        final String likesSTMT = "SELECT count(*) as likes FROM LikedTopic WHERE TopicId = ?;";

        try (PreparedStatement latestPostP = c.prepareStatement(latestPostSTMT);
             PreparedStatement likesP = c.prepareStatement(likesSTMT)) {
            latestPostP.setLong(1, topicId);
            likesP.setLong(1, topicId);

            ResultSet latestPostRS = latestPostP.executeQuery(), likesRS = likesP.executeQuery();

            return Result.success(new PostView(
                    latestPostRS.getInt("forumId"), topicId, latestPostRS.getInt("postNumber"),
                    latestPostRS.getString("name"), latestPostRS.getString("username"),
                    latestPostRS.getString("text"), latestPostRS.getInt("date"), likesRS.getInt("likes")));
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

//    //TO ALEX - DONE [note: this has looping SQL queries, non-closing preparedStatements and isn't ordered by title]
//    @Override
//    public Result<List<ForumSummaryView>> getForums() {
//        final String STMT = "SELECT * FROM Forum;";
//        List<ForumSummaryView> ll = new ArrayList<>();
//        SimpleTopicSummaryView stsv;
//
//        try (PreparedStatement p = c.prepareStatement(STMT)) {
//            ResultSet rs = p.executeQuery();
//            String latestTopicIdSTMT;
//            while (rs.next()) {
//                long currForumId = rs.getInt(1);
//                latestTopicIdSTMT = "SELECT Topic.id, Topic.title From Topic" +
//                        " JOIN Post ON Post.TopicId = Topic.id " +
//                        " WHERE ForumId = " + currForumId + " ORDER BY date LIMIT 1;";
//
//                PreparedStatement p1 = c.prepareStatement(latestTopicIdSTMT);
//                ResultSet rs1 = p1.executeQuery();
//                stsv = new SimpleTopicSummaryView(rs1.getLong(1), currForumId, rs1.getString("title"));
//		/*System.out.println(rs.getLong(1) + " " + rs.getString("title") + " " + stsv.getTitle());*/
//
//                ForumSummaryView fsv = new ForumSummaryView(rs.getLong(1),
//                        rs.getString("title"),
//                        stsv);
//                ll.add(fsv);
//            }
//            return Result.success(ll);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return Result.fatal(e.getMessage());
//        }
//    }

    //TO ALEX - DONE [fixed liberally by Jamie (sorry)]
    @Override
    public Result<List<ForumSummaryView>> getForums() {
        List<ForumSummaryView> ll = new ArrayList<>();
        final String STMT =
                "SELECT Forum.id AS fId, Forum.title AS fTitle, Topic.id AS tId, Topic.title AS tTitle FROM Forum " +
                "JOIN Topic ON Topic.ForumId = Forum.id " +
                "JOIN Post ON Post.TopicId = Topic.id " +
                "GROUP BY Forum.id " +
                "ORDER BY Forum.title ASC, `date` DESC, Post.id DESC;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                ll.add(new ForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"),
                        // This is the the topic most recently posted in.
                        new SimpleTopicSummaryView(rs.getLong("tId"), rs.getLong("fId"), rs.getString("tTitle"))
                ));
            }
            return Result.success(ll);
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
    }

    // TO Phan
    @Override
    public Result createForum(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//
//    //TO ALEX - DONE
//    @Override
//    public Result createPost(long topicId, String username, String text) {
//
//        long dateInSecs = new Date().getTime() / MS_TO_SECONDS;
//
//        final String getPersonIdSTMT = "SELECT id FROM Person WHERE username = ?;";
//        long personId;
//        try (PreparedStatement p = c.prepareStatement(getPersonIdSTMT)) {
//            p.setString(1, username);
//            ResultSet rs = p.executeQuery();
//            personId = rs.getLong(1);
//
//
//            final String STMT = "INSERT INTO Post (date,text,PersonId,TopicId) VALUES (?, ?, ?, ?);";
//
//            PreparedStatement p1 = c.prepareStatement(STMT);
//
//            p1.setLong(1, dateInSecs);
//            p1.setString(2, text);
//            p1.setLong(3, personId);
//            p1.setLong(4, topicId);
//
//            p1.execute();
//            c.commit();
//        } catch (SQLException e) {
//            return Result.fatal(e.getMessage());
//        }
//        return Result.success();
//    }


    //TO ALEX - DONE [closed second preparedStatement and - Jamie; tested]
    @Override
    public Result createPost(long topicId, String username, String text) {
        // TODO: assess whether this first statement is reusable code elsewhere
        final String getPersonIdSTMT = "SELECT id AS personId FROM Person WHERE username = ?;";
        final String insertSTMT = "INSERT INTO Post (`date`, `text`, PersonId, TopicId) VALUES (?, ?, ?, ?);";

        try (PreparedStatement p = c.prepareStatement(getPersonIdSTMT);
             PreparedStatement p1 = c.prepareStatement(insertSTMT)) {
            p.setString(1, username);
            ResultSet rs = p.executeQuery();

            long dateInSecs = new Date().getTime() / MS_TO_SECONDS;
            p1.setLong(1, dateInSecs);
            p1.setString(2, text);
            p1.setLong(3, rs.getLong("personId"));
            p1.setLong(4, topicId);
            p1.execute();

            c.commit();
        } catch (SQLException e) {
            // TODO: do we rollback here?
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    // to Jamie [FINISHED, tested]
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        final String STMT = "INSERT INTO Person (username, name, studentId) VALUES (?, ?, ?)";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1, username);
            p.setString(2, name);
            p.setString(3, studentId);

            p.execute();
            c.commit(); // tells the db driver to end the transaction.
        } catch (SQLException e) {
//          throw new UnsupportedOperationException("exception " + e); // this was originally here, but prob. better to remove.
            // c.rollback(); // TODO: "Whenever you have done a write but not yet committed, make all exception handlers do an explicit rollback()."
            // Assess whether this is the right scenario and place to use this. This would also need to be wrapped in a try-catch.
            return Result.fatal(e.getMessage());
        }

        return Result.success();
    }

    // TO Phan
    @Override
    public Result<ForumView> getForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    /* Design decision: not limiting the number of posts of a topic to fetch.
     * Also: displaying oldest post of topic first (ASC). */

    // Order of expense:
    // Lots of queries more expensive than one massive one due to communication ping
    // if there's an index on the id (ie. WHERE ____  = 1), then it's a binary search and is fast (TABLE SEARCH).
    // If there's no unique constraint, will have to check every single entry in the table (TABLE SCAN: once through the whole table).

    // To Jamie [FINISHED]
    @Override
    public Result<TopicView> getTopic(long topicId, int page) {
        List<PostView> posts = new ArrayList<>();

        final String topicInfoSTMT =
//                "SELECT count(Post.id) AS postNumber, " + // postNumber not actually needed, I think.
                "SELECT Forum.id AS forumId, Forum.title AS forumName, Topic.title AS title FROM Topic INNER JOIN Forum ON Forum.id = Topic.ForumId INNER JOIN Post ON Post.TopicId = Topic.id WHERE Topic.id = ? LIMIT 1;";
        // SELECT Forum.id AS forumId, Forum.title AS forumName, Topic.title AS title FROM Topic INNER JOIN Forum ON Forum.id = Topic.ForumId INNER JOIN Post ON Post.TopicId = Topic.id WHERE Topic.id = 1 LIMIT 1;

        // we need all the likes for each post
        final String ascendingPostsOfTopicSTMT =
                "SELECT count(PostId) AS likes, `text`, name, username, `date` FROM Post " +
                "LEFT JOIN LikedPost ON PostId = Post.id LEFT JOIN Topic ON Topic.id = Post.TopicId LEFT JOIN Person ON Post.PersonId = Person.id " +
                "WHERE TopicId = ? GROUP BY PostId ORDER BY `date` DESC, Post.id ASC;";
//        "SELECT count(PostId) AS likes, `text`, name, username, `date`, Forum.id AS forumId, Forum.title AS forumName, Topic.title AS tTitle FROM Post LEFT JOIN LikedPost ON PostId = Post.id LEFT JOIN Topic ON Topic.id = Post.TopicId LEFT JOIN Person ON Post.PersonId = Person.id JOIN Forum ON Topic.ForumId = Forum.id WHERE TopicId = ? GROUP BY TopicId ORDER BY `date`, Post.id ASC;";
        // SELECT count(PostId) AS likes, text, name, username FROM Post LEFT JOIN LikedPost ON PostId = Post.id JOIN Topic ON Topic.id = Post.TopicId JOIN Person ON Post.PersonId = Person.id WHERE TopicId = 1 GROUP BY TopicId ORDER BY `date`, Post.id ASC;

        // tries communicating with the database.
        try (PreparedStatement ascendingPostsOfTopicP = c.prepareStatement(ascendingPostsOfTopicSTMT);
             PreparedStatement forumIdP = c.prepareStatement(topicInfoSTMT)) {
            // sets all the '?' to be the topicId.
            forumIdP.setLong(1, topicId);
            ascendingPostsOfTopicP.setLong(1, topicId);

            // catches all the ResultSets of each executed query.
            ResultSet forumIdRS = forumIdP.executeQuery();
            ResultSet ascendingPostsOfTopicRS = ascendingPostsOfTopicP.executeQuery();

            // gets the ints or Strings out of the ResultSets.
            long forumId = forumIdRS.getLong("forumId");
            String forumName = forumIdRS.getString("forumName");
            String title = forumIdRS.getString("title");


            for(int postNumber = 1; ascendingPostsOfTopicRS.next(); postNumber++) {
                System.out.println("ENTERING FOR LOOP");

                posts.add(new PostView(
                        forumId,
                        topicId,
                        postNumber, // int postNumber
                        ascendingPostsOfTopicRS.getString("name"), // String authorName
                        ascendingPostsOfTopicRS.getString("username"), // String authorUserName
                        ascendingPostsOfTopicRS.getString("text"), // String text
                        ascendingPostsOfTopicRS.getInt("date"),
                        ascendingPostsOfTopicRS.getInt("likes") // likes of Post
                        )
                );

//                System.out.println("Adding PostView during getTopic()... " +
//                        "postNumber = " + String.valueOf(postNumber) + "; " +
//                        "author = " + rs.getString("name") + "; " +
//                        "text = " + rs.getString("text") + "; " +
//                        "postedAt = " + rs.getInt("date"));
            }

            return Result.success(new TopicView(forumId, topicId, forumName, title, posts, page));
        } catch (SQLException e) {
            return Result.failure(e.getMessage());
        }

    }

    //TO ALEX - DONE
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {

        final String getPersonIdSTMT = "SELECT id FROM Person WHERE username = ?;";
        long personId;
        try (PreparedStatement p = c.prepareStatement(getPersonIdSTMT)) {
            p.setString(1, username);
            ResultSet rs = p.executeQuery();
            personId = rs.getLong(1);

            final String STMT;
            if (like) {
                STMT = "INSERT INTO LikedTopic (TopicId, PersonId) VALUES (?, ?);";
            } else {
                STMT = "DELETE FROM LikedTopic WHERE TopicId = ? AND PersonId = ?;";
            }

            PreparedStatement p1 = c.prepareStatement(STMT);

            p1.setLong(1, topicId);
            p1.setLong(2, personId);
            System.out.println(p1);
            p1.execute();
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.failure(e.getMessage());
        }
        return Result.success();

    }

    @Override
    public Result favouriteTopic(String username, long topicId, boolean fav) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    /*
     * Level 3 - more complex queries. Leave these until last.
     */
    // TO ALEX - "I'll give it ago"
    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
	long dateInSecs = new Date().getTime() / 1000;

	final String getPersonIdSTMT = "SELECT id FROM Person WHERE username = ?;";

	final String createTopicSTMT = "INSERT INTO Topic (title, ForumId) VALUES(?, ?);";

	long personId, topicId;
        try(PreparedStatement p = c.prepareStatement(getPersonIdSTMT)){
            p.setString(1, username);
            ResultSet rs = p.executeQuery();
	    personId = rs.getLong(1);

	    PreparedStatement p2 = c.prepareStatement(createTopicSTMT);

	    p2.setString(1,title);
	    p2.setLong(2,forumId);

            p2.execute();
            c.commit(); 

	    final String getTopicIdSTMT = "SELECT id FROM Topic WHERE title = ?;";

	    PreparedStatement p3 = c.prepareStatement(getTopicIdSTMT);		
	    p3.setString(1,title);
            ResultSet rs3 = p3.executeQuery();
	    topicId = rs3.getLong(1);

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
