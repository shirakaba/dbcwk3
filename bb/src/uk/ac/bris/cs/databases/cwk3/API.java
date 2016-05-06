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

import uk.ac.bris.cs.databases.api.*;
/**
 *
 * @author csxdb
 */
public class API implements APIProvider {
    private final static int MS_TO_SECONDS = 1000;
    private final Connection c;
    private final ValidityTester vt;

    public API(Connection c) {
        this.c = c;
        this.vt = new ValidityTester(c);
    }

    // implemented by Alex & Phan [tested]
    @Override
    public Result<Map<String, String>> getUsers() {
        final String STMT = "SELECT username, name FROM Person;";
        Map<String, String> map = new HashMap<>();

        try (PreparedStatement p = c.prepareStatement(STMT)) {

            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                map.put(rs.getString("username"), rs.getString("name"));
            }

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return Result.success(map);
    }


    // implemented by Alex [tested. Validation added by Jamie.]
    @Override
    public Result<PersonView> getPersonView(String username) {
        final String STMT = "SELECT name, username, studentId FROM Person WHERE username = ?;";

        if (username == null || username.isEmpty()) return Result.failure("Username was empty or null.");

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (vt.validateUsername(username) == null) return Result.failure("Username did not exist.");

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
        final String STMT = "SELECT id, title FROM Forum ORDER BY title COLLATE NOCASE ASC;";
        List<SimpleForumSummaryView> list = new ArrayList<>();

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                list.add(new SimpleForumSummaryView(rs.getLong("id"), rs.getString("title")));
            }

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        return Result.success(list);
    }

    
    // implemented by Phan
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        int posts = 0;

        try {
            if(vt.validateTopicId(topicId) == null) return Result.failure("topic didn't exist.");
            posts = countRowsOfTopicTable(topicId, CountRowsOfTableMode.POSTS);
        } catch (SQLException e) {
            return Result.failure(e.getMessage());
        }
        return Result.success(posts);
    }



    // implemented by Jamie
    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        List<PersonView> list = new ArrayList<>();

        final String STMT =
                "SELECT name, username, studentId FROM LikedTopic " +
                        "INNER JOIN Person ON PersonId = Person.id " +
                        "WHERE TopicId = ? " +
                        "ORDER BY name ASC;";
        
        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (vt.validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
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

    // implemented by Jamie
    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        List<SimplePostView> simplePostViews = new ArrayList<>();

        final String STMT =
                "SELECT title, `name`, `text`, `date` FROM Topic " +
                        "INNER JOIN Post ON Topic.id = Post.TopicId " +
                        "INNER JOIN Person ON Person.id = Post.PersonId " +
                        "WHERE TopicId = ? ORDER BY `date` ASC;";
        
        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (vt.validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
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

    /**
     * Switches the mode of countRowsOfTopicTable() to counting Likes (LIKES) or Posts (POSTS).
     */
    private enum CountRowsOfTableMode {
        LIKES,
        POSTS
    }

    // apparently requires validation of topicId.
    /**
     * Counts the number of entries (rows) in a table detailing the Posts Or Likes for a  given Topic.
     *
     * @param topicId - the id to count Posts/Likes for.
     * @param mode - counts Posts if CountRowsOfTableMode.POSTS; otherwise, counts LIKES.
     * @return the counted number of Posts/Likes.. Otherwise, null.
     */
    private int countRowsOfTopicTable(long topicId, CountRowsOfTableMode mode) throws SQLException {
        final String whichTable;

        switch (mode) {
            case LIKES:
                whichTable = "SELECT count(*) AS count FROM Topic JOIN LikedTopic ON Topic.id = LikedTopic.TopicId WHERE Topic.id = ?;";
                break;
            case POSTS:
                whichTable = "SELECT count(*) AS count FROM Topic JOIN Post ON Post.TopicId = Topic.id WHERE Topic.id = ?;";
                break;
            default:
                throw new UnsupportedOperationException("An unimplemented branch of the countRowsOfTable method was used.");
        }

        try (PreparedStatement p = c.prepareStatement(whichTable)) {
            p.setLong(1, topicId);

            ResultSet rs = p.executeQuery();

            return rs.getInt("count");
        }
    }

    /* 
     * Level 2 - standard difficulty. Most groups should get all of these.
     * They require a little bit more thought than the level 1 API though.
     */

    // Implemented by Jamie
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
        final String likesSTMT = "SELECT count(*) AS likes FROM LikedTopic WHERE TopicId = ?;";

        try (PreparedStatement latestPostP = c.prepareStatement(latestPostSTMT);
             PreparedStatement likesP = c.prepareStatement(likesSTMT)) {
            if (vt.validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");

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

    // Implemented by Alex
    @Override
    public Result<List<ForumSummaryView>> getForums() {
        List<ForumSummaryView> ll = new ArrayList<>();
        final String STMT =
                "SELECT Forum.id AS fId, Forum.title AS fTitle, Topic.id AS tId, Topic.title AS tTitle FROM Forum " +
                        "LEFT JOIN Topic ON Topic.ForumId = Forum.id " +
                        "LEFT JOIN Post ON Post.TopicId = Topic.id " +
                        "GROUP BY Forum.id " +
                        "ORDER BY Forum.title COLLATE NOCASE ASC, `date` DESC, Post.id DESC;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                String title = rs.getString("tTitle");
                if (title == null) {
                    ll.add(new ForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"), null));

                } else {
                    ll.add(new ForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"),
                    // This is the the topic most recently posted in.
                        new SimpleTopicSummaryView(rs.getLong("tId"), rs.getLong("fId"), rs.getString("tTitle"))
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
//            return Result.fatal(e.getMessage());
        }
        return Result.success(ll);
    }

    // Implemented by Phan
    @Override
    public Result createForum(String title) {
        final String selectSTMT = "SELECT title FROM Forum WHERE title = ?;";
        final String insertSTMT = "INSERT INTO Forum (title) VALUES (?);";

        if (title== null || title.isEmpty()) return Result.failure("Title provided must not be empty/null.");

        try (PreparedStatement p = c.prepareStatement(selectSTMT);
             PreparedStatement p1 = c.prepareStatement(insertSTMT)) {
            p.setString(1, title);
            ResultSet rs = p.executeQuery();

            if (rs.next()) return Result.failure("Title provided must be unique.");

            p1.setString(1, title);
            p1.execute();
            c.commit();

            return Result.success();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
    }


    //Implemented by ALEX
    @Override
    public Result createPost(long topicId, String username, String text) {
        final String insertSTMT = "INSERT INTO Post (`date`, `text`, PersonId, TopicId) VALUES (?, ?, ?, ?);";
        if (text.isEmpty()) return Result.failure("Posts cannot have empty text.");

        try (PreparedStatement p1 = c.prepareStatement(insertSTMT)) {
            Long personId = vt.validateUsername(username);
            if (personId == null) return Result.failure("Person id did not exist.");
            if (vt.validateTopicId(topicId) == null) return Result.failure("Topic id did not exist.");

            long dateInSecs = new Date().getTime() / MS_TO_SECONDS;
            p1.setLong(1, dateInSecs);
            p1.setString(2, text);
            p1.setLong(3, personId);
            p1.setLong(4, topicId);
            p1.execute();

            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.failure(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    // Implemented by Jamie
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        final String STMT = "INSERT INTO Person (username, name, studentId) VALUES (?, ?, ?)";
        if (name == null || name.isEmpty()) return Result.failure("name cannot have empty text nor be null.");
        if (username == null || username.isEmpty()) return Result.failure("username cannot have empty text nor be null.");
        if (studentId == null || studentId.isEmpty()) return Result.failure("studentId cannot have empty text.");

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1, username);
            p.setString(2, name);
            p.setString(3, studentId);

            p.execute();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.failure(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }

        return Result.success();
    }
    
    
    // Implemented by Phan
    @Override
    public Result<ForumView> getForum(long id) {
        final String STMT = "SELECT Forum.title AS forumTitle, Topic.id AS topicId, Topic.title AS topicTitle"
                + " FROM Forum LEFT JOIN Topic ON Forum.id = Topic.ForumId "
                + " WHERE forum.id = ?;";
        List<SimpleTopicSummaryView> topics = new ArrayList<>();
        String fT = null;
        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, (int) id);
            ResultSet rs = p.executeQuery();
            
            if (!rs.isBeforeFirst() ) return Result.failure("Forum id did not exist");
            
            while (rs.next()) {
                fT = rs.getString("forumTitle");
                String tt = rs.getString("topicTitle");
                if (tt == null) { //if there is no topics
                    return Result.success(new ForumView(id, fT, topics));
                }
                topics.add(new SimpleTopicSummaryView(rs.getLong("topicId"), id, tt));
            }
            return Result.success(new ForumView(id, fT, topics));
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }


    /* Design decision: not limiting the number of posts of a topic to fetch.
     * Also: displaying oldest post of topic first (ASC). */

    // Order of expense:
    // Lots of queries more expensive than one massive one due to communication ping
    // if there's an index on the id (ie. WHERE ____  = 1), then it's a binary search and is fast (TABLE SEARCH).
    // If there's no unique constraint, will have to check every single entry in the table (TABLE SCAN: once through the whole table).



    // Implemented by Jamie [FINISHED; tested]
    @Override
    public Result<TopicView> getTopic(long topicId, int page) {
        if (page < 0) return Result.failure("A negative number of pages worth of topics were requested.");
        List<PostView> posts = new ArrayList<>();
        final String limiter;

        if (page == 0) limiter = "";
        else limiter = String.format("LIMIT %d OFFSET %d", 10 * page, 10 * (page - 1) + 1);

        final String ascendingPostsOfTopicSTMT = String.format(
                "SELECT DISTINCT Post.id AS pId, Topic.Id AS tId, Forum.id AS fId, Forum.title AS forumName, " +
                        "Topic.title AS tTitle, name, username, `text`, `date`, count(Post.id) AS likes FROM Post " +
                        "JOIN Topic ON Post.TopicId = Topic.id JOIN Person ON Post.PersonId = Person.id JOIN Forum ON Topic.ForumId = Forum.id " +
                        "LEFT JOIN LikedPost ON LikedPost.PostId = Post.id " +
                        "WHERE TopicId = ? GROUP BY Post.id " +
                        "ORDER BY `date` ASC, Post.id ASC %s;", limiter);

        try (PreparedStatement p = c.prepareStatement(ascendingPostsOfTopicSTMT)) {
            if (vt.validateTopicId(topicId) == null) return Result.failure("Topic id did not exist.");
            p.setLong(1, topicId);

            // TODO: populate database with posts to test this.
            // TODO: Ask guidance on printing this error (the line of code is hit, but doesn't print).
            if (!vt.validatePostCount(topicId, page))
                return Result.failure("Too few posts existed to span to requested page");

            ResultSet rs = p.executeQuery();

            long forumId = rs.getLong("fId");
            String forumName = rs.getString("forumName");
            String title = rs.getString("tTitle");

            for (int postNumber = 1 + page * 10; rs.next(); postNumber++) {
                posts.add(new PostView(
                                forumId,
                                topicId,
                                postNumber,
                                rs.getString("name"),
                                rs.getString("username"),
                                rs.getString("text"),
                                rs.getInt("date"),
                                rs.getInt("likes")
                        )
                );

//                System.out.println(String.format("Adding PostView during getTopic()... \n" +
//                    "forumId = %d;\n topicId = %d;\n postNumber = %d;\n author = %s;\n username = %s;\n text = %s; postedAt = %d;\n likes = %d.",
//                    forumId, topicId, postNumber, rs.getString("name"), rs.getString("username"), rs.getString("text"), rs.getInt("date"), rs.getInt("likes")));
            }

            return Result.success(new TopicView(forumId, topicId, forumName, title, posts, page));
        } catch (SQLException e) {
            return Result.failure(e.getMessage());
        }

    }



    // TO ALEX - DONE
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        final String STMT;
        if (like) STMT = "INSERT INTO LikedTopic (TopicId, PersonId) VALUES (?, ?);";
        else STMT = "DELETE FROM LikedTopic WHERE TopicId = ? AND PersonId = ?;";

        try (PreparedStatement p1 = c.prepareStatement(STMT)) {
            Long personId = vt.validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (vt.validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
            if (!vt.likeOrFavouriteNeedsChanging(topicId, personId, like, ValidityTester.LikeOrFavourite.LIKE)) return Result.success();

            p1.setLong(1, topicId);
            p1.setLong(2, personId);
            System.out.println(p1);
            p1.execute();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
//            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
        return Result.success();

    }


    // Alex
    @Override
    public Result favouriteTopic(String username, long topicId, boolean favourite) {
       final String STMT;
        if (favourite) STMT = "INSERT INTO FavouritedTopic (TopicId, PersonId) VALUES (?, ?);";
        else STMT = "DELETE FROM FavouritedTopic WHERE TopicId = ? AND PersonId = ?;";

        try (PreparedStatement p1 = c.prepareStatement(STMT)) {
            Long personId = vt.validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (vt.validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
            if (!vt.likeOrFavouriteNeedsChanging(topicId, personId, favourite, ValidityTester.LikeOrFavourite.FAVOURITE)) return Result.success();

            p1.setLong(1, topicId);
            p1.setLong(2, personId);
            System.out.println(p1);
            p1.execute();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
//            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
        return Result.success();

    }



    /*
     * Level 3 - more complex queries. Leave these until last.
     */
    // TO ALEX - "I'll give it a go" [Jamie - added validation, rollback, and closed preparedStatements. Untested.]
    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
        final String createTopicSTMT = "INSERT INTO Topic (title, ForumId) VALUES(?, ?);";

        if (title.isEmpty()) return Result.failure("title cannot be empty.");
        if (text.isEmpty()) return Result.failure("text cannot be empty.");

        try (PreparedStatement p1 = c.prepareStatement(createTopicSTMT)) {

            Long personId = vt.validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (vt.validateForumId(forumId) == null)
                return Result.failure("Forum id did not exist, or forum has no topics under it (illegal)."); // TODO: ask about failure messages not printing.

            p1.setString(1, title);
            p1.setLong(2, forumId);
            p1.execute();

            c.commit();

            createPost(getTopicId(title), username, text);

            return Result.success();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
    }

    /**
     * returns the topicId using topic title
     *
     * @param title - the title of the topic
     * @return the topicId
     * @throws SQLException on failure
     */
    private long getTopicId(String title) throws SQLException{
        final String getTopicIdSTMT = "SELECT id FROM Topic WHERE title = ?;";

        try (PreparedStatement p2 = c.prepareStatement(getTopicIdSTMT)){
            p2.setString(1, title);
            ResultSet rs2 = p2.executeQuery();
            return rs2.getLong(1);
        }
    }

// 1) You can't override a function that doesn't exist in the abstract class.
//    //Phan
//    @Override
//    private Result getTimeCreatedTopic () {
//        return result.failure ("not written yet");
//    }

    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("This query never worked.");
//        final String STMT = "SELECT Forum.id AS fId, Forum.title AS fTitle, Topic.id AS tId, Topic.title AS tTitle, " +
//                            "count(Post.id) AS count, Post.`date` AS created, Post.date AS lastTime, " +
//                            "Person.name AS cName, Person.username AS cUserName " +
//                            "FROM Forum LEFT JOIN Topic ON Topic.ForumId = Forum.id " +
//                            "LEFT JOIN Post ON Post.TopicId = Topic.id " +
//                            "LEFT JOIN Person ON Person.id = Post.PersonId " +
//                            "GROUP BY Forum.id " +
//                            "ORDER BY fTitle COLLATE NOCASE ASC, `date` DESC, Post.id DESC " +
//                            "; " +
//
//                            "SELECT `date` AS lastTime, username, text, Forum.id AS forumId, count(*) AS postNumber FROM Topic " + //get latest post query
//                            "INNER JOIN Post ON Topic.id = Post.TopicId " +
//                            "INNER JOIN Person ON Person.id = Post.PersonId " +
//                            "INNER JOIN Forum ON Forum.id = Topic.ForumId " +
//                            "WHERE Post.TopicId = ? " +
//                            "ORDER BY lastTime DESC, Post.id DESC " + // orders first by date, then by size (newness) of id in case of same-day post
//                            "LIMIT 1;";
//
//        List<AdvancedForumSummaryView> list= new ArrayList<> ();
//
//        try (PreparedStatement p = c.prepareStatement(STMT)) {
//            ResultSet rs = p.executeQuery();
//            while (rs.next()) {
//                String title = rs.getString("tTitle");
//                if (title == null) {
//                    list.add(new AdvancedForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"), null));
//                } else {
//                    list.add (new AdvancedForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"),
//                                                       new TopicSummaryView(rs.getLong("tId"), rs.getLong("fId"), rs.getString("tTitle"),
//                                                                            rs.getInt("count"), rs.getInt("created"),
//                                                                            rs.getInt("lastTime"), rs.getString("lastName"), rs.getInt("likes"),
//                                                                            rs.getString("cName"), rs.getString("cUserName")
//                                                                            ) ));
//                }
//            }
//            return Result.success (list);
//        } catch (SQLException e) {
//            return Result.fatal(e.getMessage());
//        }

    }
    /*list.add (new AdvancedForumSummaryView(rs.getLong("fId"), rs.getString("fTitle"),
                                           new TopicSummaryView(rs.getLong("tId"), rs.getLong("fId"), rs.getString("tTitle"), rs.getInt("count"), rs.getInt("created"),
                                                                rs.getInt("lastTime"), rs.getString("lastName"), rs.getInt("likes"),
                                                                rs.getString("cName"), rs.getString("cUserName")
                                                                ) ));*/

	//TO ALEX
  /*  public AdvancedPersonView(String name, String username, String studentId,
            int topicLikes, int postLikes, List<TopicSummaryView> favourites)*/

    // TODO: test this
    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        final String STMT = "SELECT name, username, studentId FROM Person WHERE username = ?;";

        if (username.isEmpty()) return Result.failure("Username was empty or null.");

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (vt.validateUsername(username) == null) return Result.failure("Username did not exist.");

            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            ArrayList<TopicSummaryView> topicSummaryView = getTopicSummaryView(username);
            if (topicSummaryView == null) return Result.failure("failed to make topicSummaryView.");

            return Result.success(new AdvancedPersonView(
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("studentId"),
                    countRowsOfTable(username, CountRowsOfPersonMode.TOPIC_LIKES),
                    countRowsOfTable(username, CountRowsOfPersonMode.POST_LIKES),
                    topicSummaryView));
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
    }

    /**
     * Switches the mode of countRowsOfTable() to counting Likes (TOPIC_LIKES) or Favourites (POST_LIKES).
     */
    private enum CountRowsOfPersonMode {
        TOPIC_LIKES,
        POST_LIKES
    }

    /**
     * Counts the number of entries (rows) in a table detailing the number of LikedTopic or LikedPost by a username.
     * Expects prior validation of username.
     *
     * @param username - the username to count Favourites/Likes for.
     * @param mode - counts LikedPost if POST_LIKES; or LikedTopic if TOPIC_LIKES.
     * @return the counted number of Posts/Likes.. Otherwise, null.
     */
    private int countRowsOfTable(String username, CountRowsOfPersonMode mode) throws SQLException {
        final String getTopicId;

        switch (mode) {
            case TOPIC_LIKES:
                getTopicId = "SELECT count(*) AS count FROM Person JOIN LikedTopic ON id = PersonId WHERE username = ?;";
                break;
            case POST_LIKES:
                getTopicId = "SELECT count(*) AS count FROM Person JOIN LikedPost ON id = PersonId WHERE username = ?;";
                break;
            default:
                throw new UnsupportedOperationException("An unimplemented branch of the countRowsOfTable method was used.");
        }

        try (PreparedStatement p = c.prepareStatement(getTopicId)) {
            p.setString(1, username);

            ResultSet rs = p.executeQuery();
            return rs.getInt("count");
        }
    }

    /**
     * Gets the TopicSummaryView for a given username. Expects prior validation of the username.
     * @param username - the username to search for Topics interacted with by.
     * @return - a list of all the TopicSummaryViews which a Person has interacted with.
     * @throws SQLException
     */
    private ArrayList<TopicSummaryView> getTopicSummaryView(String username) throws SQLException{
        ArrayList<TopicSummaryView> list = new ArrayList<>();
        final String STMT = "SELECT Topic.Id AS topicId, Forum.Id AS forumId, " +
                            "Topic.title AS title, Person.name AS name, Person.username AS username " +
                            "FROM Person JOIN FavouritedTopic ON Person.id = FavouritedTopic.PersonId " +
                            "JOIN Topic ON Topic.id = FavouritedTopic.TopicId " +
                            "JOIN Forum ON Forum.id = ForumId WHERE username = ? " +
                            "GROUP BY Topic.id;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {

            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            //check if result set is empty
            if (!rs.isBeforeFirst() ) return new ArrayList<>();

            while (rs.next()) {
                long currentTopicId = rs.getLong("topicId");
                int topicLikedCount = countRowsOfTopicTable(currentTopicId, CountRowsOfTableMode.LIKES);
                int topicPostCount = countRowsOfTopicTable(currentTopicId, CountRowsOfTableMode.POSTS);

                ExtremePostView latestPostDatePerson = getExtremeDatePoster(currentTopicId, getExtremeDatePosterMode.NEWEST);
//                System.out.println(String.format("latest post date is %d", latestPostDatePerson.getDate()));
                if(latestPostDatePerson == null) return null;
                ExtremePostView firstPostDatePerson = getExtremeDatePoster(currentTopicId, getExtremeDatePosterMode.CREATION);
//                System.out.println(String.format("first post date is %d", firstPostDatePerson.getDate()));
                if(firstPostDatePerson == null) return null;

                list.add(new TopicSummaryView(currentTopicId,
                                              rs.getLong("forumId"),
                                              rs.getString("title"),
                                              topicPostCount,
                                              firstPostDatePerson.getDate(), // int created
                                              latestPostDatePerson.getDate(), // int lastPostTime
                                              latestPostDatePerson.getName(), // String lastPostName
                                              topicLikedCount,
                                              firstPostDatePerson.getName(), // String creatorName
                                              firstPostDatePerson.getUsername())); // String creatorUserName
            }

            return list;
        }
    }


    /**
     * Switches the mode of getExtremeDatePoster() to sorting by date ASC (CREATION) or DESC (NEWEST).
     */
    private enum getExtremeDatePosterMode {
        CREATION,
        NEWEST
    }

    // TODO: requires topic to be validated beforehand
    // TODO: protect against failure when returning.
    // we've been asked not to throw unusual Exceptions, and that line stops it from compiling anyway
    /**
     * Requires a valid topicId.
     * Gets the date, name, and username of which Person made the earliest or last Post in the specified Topic.
     * @param topicId - the Topic id to search for. Must be valid.
     * @param mode - CREATION: date of first Post in Topic (made at Topic creation time).  NEWEST: date of newest Post in Topic.
     * @return returns a String array. In slot 0, the date. In slot 1, the name. In slot 2, the username.
     *         Uncharacterised behaviour if failure.
     */
    private ExtremePostView getExtremeDatePoster(long topicId, getExtremeDatePosterMode mode) throws SQLException {
//        if (validateTopicId(topicId) == null) throw new Exception("TopicId did not exist.");

        final String sort;

        switch(mode){
            case CREATION:
                sort = "ASC";
                break;
            case NEWEST:
                sort = "DESC";
                break;
            default:
                throw new UnsupportedOperationException("unimplemented branch of getExtremeDatePoster reached");
        }

        final String STMT = String.format("SELECT `date`, Person.name AS name, Person.username AS username FROM Topic " +
                "JOIN Post ON Post.TopicId = Topic.id " +
                "JOIN Person ON PersonId = Person.id " +
                "WHERE Topic.id = ? " +
                "ORDER BY `date` %s LIMIT 1;", sort);


        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);

            ResultSet rs = p.executeQuery();

            if(!rs.next()) return null;

            return new ExtremePostView(rs.getInt("date"), rs.getString("name"), rs.getString("username"));
        }
    }


    // not worth doing. Can't figure out how to avoid looped SQL queries.
    @Override
    public Result<AdvancedForumView> getAdvancedForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    // THIS DOES NOT RUN.
    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");

//        final String STMT;
//
//        final String findPerson = "SELECT id AS pId FROM Person WHERE username = ?;";
//        int pId;
//        try (PreparedStatement q = c.prepareStatement(findPerson)){
//            if (vt.validateUsername (username) == null) {
//                return Result.failure ("username does not exist");
//            } else if (vt.validateTopicId(topicId) == null) {
//                return Result.failure ("the topic does not exist");
//            } else if (!validatePost(post, topicId)) {
//                return Result.failure ("the post does not exist");
//            }
//            q.setString(1, username);
//            ResultSet r = q.executeQuery();
//            if (r.next()) {
//                pId = r.getInt("pId");
//            }
//        }
//        if (like) {
//            String checkLike = "SELECT PostId, PersonId FROM LikedPost WHERE PostId = ?, PersonId = ?;";
//            try (PreparedStatement cL = c.prepareStatement(checkLike)) {
//                ResultSet r_cL;
//                cL.setInt(1, post);
//                cL.setInt(2, pId);
//                cL.executeQuery();
//                if (r_cL.next()!= null) {
//                    return Result.failure ("User already liked the topic");
//                }
//
//            }
//            STMT = "INSERT INTO LikedPost (PostId, PersonId) VALUES (?, ?);";
//        } else {
//            STMT = "DELETE FROM LikedPost WHERE PostId = ? AND PersonId = ?;";
//        }
//        try (PreparedStatement p = c.prepareStatement(STMT)) {
//            p.setInt(1, post);
//            p.setInt(2, pId);
//            p.execute();
//            c.commit();
//            return Result.success();
//        } catch (SQLException e) {
//            try {
//                c.rollback();
//            } catch (SQLException f) {
//                return Result.fatal (f.getMessage());
//            }
//            return Result.fatal (e.getMessage());
//        }
    }
}
