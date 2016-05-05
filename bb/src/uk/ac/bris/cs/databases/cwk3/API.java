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

//            return Result.success(map);
        } catch (SQLException e) {
//            return Result.fatal(e.getMessage());
        }

        return Result.success(map); // TODO: ask whether we return the map even if it's empty, and what do when failing.
    }

    /**
     * Checks whether username is in the Person table.
     *
     * @param username - the username to check for the existence of in the Person table.
     * @return the corresponding Person.id if the username is registered. Otherwise, null.
     */
    private Long validateUsername(String username) throws SQLException {
        final String getUsername = "SELECT id, username FROM Person WHERE username = ?;";

        try (PreparedStatement p = c.prepareStatement(getUsername)) {
            p.setString(1, username);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // username doesn't exist

            return rs.getLong("id");
        }
    }

    // implemented by Alex [tested. Validation added by Jamie.]
    @Override
    public Result<PersonView> getPersonView(String username) {
        final String STMT = "SELECT name, username, studentId FROM Person WHERE username = ?;";

        if (username.equals("")) return Result.failure("Username was empty or null.");

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (validateUsername(username) == null) return Result.failure("Username did not exist.");

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

//            return Result.success(list);
        } catch (SQLException e) {
//            return Result.fatal(e.getMessage()); // TODO: need any exception handling here?
        }
        return Result.success(list); // like getLikers, we return successful even if the list is empty.
    }

    // implemented by Phan [seems to be working in SQLite]
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        int posts = 0;

        try {
            if(validateTopicId(topicId) == null) return Result.failure("topic didn't exist.");
            posts = countRowsOfTopicTable(topicId, CountRowsOfTableMode.POSTS);
        } catch (SQLException e) {
//            return Result.failure(e.getMessage()); // TODO: need any exception handling here?
        }
        return Result.success(posts);
    }


    /**
     * Checks whether topicId is in the Topic table.
     *
     * @param topicId - the id to check for existence of in the Topic table.
     * @return the corresponding ForumId if the topicId is registered. Otherwise, null.
     */
    private Long validateTopicId(long topicId) throws SQLException {
        final String getTopicId = "SELECT id, forumId FROM Topic WHERE id = ?;";

        try (PreparedStatement p = c.prepareStatement(getTopicId)) {
            p.setLong(1, topicId);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // topicId doesn't exist

            return rs.getLong("ForumId");
        }
    }

    // to Jamie [FINISHED, tested]
    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        List<PersonView> list = new ArrayList<>();

        final String STMT =
                "SELECT name, username, studentId FROM LikedTopic " +
                        "INNER JOIN Person ON PersonId = Person.id " +
                        "WHERE TopicId = ? " +
                        "ORDER BY name ASC;";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");

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
            if (validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");

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

    /**
     * Counts the number of entries (rows) in a table detailing the Posts Or Likes for a  given Topic.
     *
     * @param topicId - the id to count Posts/Likes for.
     * @param mode - counts Posts if CountRowsOfTableMode.POSTS; otherwise, counts LIKES.
     * @return the counted number of Posts/Likes.. Otherwise, null.
     */
    private int countRowsOfTopicTable(long topicId, CountRowsOfTableMode mode) throws SQLException {
        final String whichTable = mode.equals(CountRowsOfTableMode.POSTS) ? "Post" : "LikedTopic";

        final String getTopicId = String.format("SELECT count(*) AS count FROM %s WHERE TopicId = ?;", whichTable);

        try (PreparedStatement p = c.prepareStatement(getTopicId)) {
            p.setLong(1, topicId);

            ResultSet rs = p.executeQuery();
            return rs.getInt("count");
        }
    }

    /* 
     * Level 2 - standard difficulty. Most groups should get all of these.
     * They require a little bit more thought than the level 1 API though.
     */

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
        final String likesSTMT = "SELECT count(*) AS likes FROM LikedTopic WHERE TopicId = ?;";

        try (PreparedStatement latestPostP = c.prepareStatement(latestPostSTMT);
             PreparedStatement likesP = c.prepareStatement(likesSTMT)) {
            if (validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");

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

//    //TO ALEX - DONE [note: this had looping SQL queries, non-closing preparedStatements and isn't ordered by title]
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
//            return Result.success(ll);
        } catch (SQLException e) {
            e.printStackTrace(); // TODO: should we take any action in the event of exceptions being caught?
//            return Result.fatal(e.getMessage());
        }
        return Result.success(ll);
    }

    /*
     * Create a new forum.
     * @param title - the title of the forum. Must not be null or empty and
     * no forum with this name must exist yet.
     * @return success if the forum was created, failure if the title was
     * null, empty or such a forum already existed; fatal on other errors.
     */
    // TO Phan
    @Override
    public Result createForum(String title) {
        final String STMT = "SELECT title FROM Forum WHERE title = ?;";
        return Result.failure("not done");
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
        final String insertSTMT = "INSERT INTO Post (`date`, `text`, PersonId, TopicId) VALUES (?, ?, ?, ?);";
        if (text.equals("")) return Result.failure("Posts cannot have empty text.");

        try (PreparedStatement p1 = c.prepareStatement(insertSTMT)) {
            Long personId = validateUsername(username);
            if (personId == null) return Result.failure("Person id did not exist.");
            if (validateTopicId(topicId) == null) return Result.failure("Topic id did not exist.");

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

    // to Jamie [FINISHED, tested]
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        final String STMT = "INSERT INTO Person (username, name, studentId) VALUES (?, ?, ?)";
        if (name.equals("")) return Result.failure("name cannot have empty text.");
        if (username.equals("")) return Result.failure("username cannot have empty text.");
        if (studentId.equals("")) return Result.failure("studentId cannot have empty text.");

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

    /*
     * Corner case: needs to 'meet the specification' if one uses getForum on a forum that has no topics.
     * Get the detailed view of a single forum.
     * @param id - the id of the forum to get.
     * @return A view of this forum if it exists, otherwise failure.
     */
    // TO Phan
    @Override
    public Result<ForumView> getForum(long id) {
        final String STMT = "SELECT Topic.id AS topicId, Topic.title AS topicTitle "
                + "FROM Forum INNER JOIN Topic ON Forum.id = Topic.ForumId "
                + "WHERE forumId = ?;";
        List<SimpleTopicSummaryView> topics = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(STMT)) {
            String forumTitle = validateForumId(id);
            if (forumTitle == null) return Result.failure("Forum id did not exist, or forum has no topics under it (illegal).");

            p.setLong(1, (int) id);
            ResultSet rs = p.executeQuery();

            while (rs.next()) topics.add(new SimpleTopicSummaryView(rs.getLong("topicId"), id, rs.getString("topicTitle")));

            return Result.success(new ForumView(id, forumTitle, topics));
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


    /**
     * Checks whether sufficient Posts exist at the page offset to display a page worth of Posts.
     *
     * @param page    - the page of Posts to navigate to.
     * @param topicId - TopicId to check for the existence of in table.
     * @return false if too few Posts. True if enough Posts, or if page specified is zero (supporting any number of Posts).
     */
    private boolean validatePostCount(long topicId, int page) throws SQLException {
        if (page == 0) return true;

        final String STMT = "SELECT count(*) AS postCnt FROM Post JOIN Topic ON Post.TopicId = Topic.id WHERE TopicId = ?";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet rs = p.executeQuery();
            long postCnt = rs.getLong("postCnt");
            if (postCnt < 10 * page + 1) return false;

            return true;
        }
    }

    // To Jamie [FINISHED; tested]
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
            if (validateTopicId(topicId) == null) return Result.failure("Topic id did not exist.");
            p.setLong(1, topicId);

            // TODO: populate database with posts to test this.
            // TODO: Ask guidance on printing this error (the line of code is hit, but doesn't print).
            if (!validatePostCount(topicId, page))
                return Result.failure("Too few posts existed to span to requested page");

            ResultSet rs = p.executeQuery();

            long forumId = rs.getLong("fId");
            String forumName = rs.getString("forumName");
            String title = rs.getString("tTitle");

            for (int postNumber = 1; rs.next(); postNumber++) {
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

    /**
     * Used to switch the mode of the likeOrFavouriteNeedsChanging() method.
     *
     * */
    private enum LikeOrFavourite {
        LIKE,
        FAVOURITE
    }

    /**
     * Checks 1) whether a Topic has already been liked by a given person in the LikedTopic table,
     * or     2) whether a Topic has already been favourited by a given person in the FavouritedTopic table.
     *
     * @param TopicId      - TopicId to check for the existence of in table.
     * @param PersonId     - PersonId to check for the existence of in table.
     * @param intendToApply - true if intending to apply like/favourite; false if intending to remove like/favourite.
     * @param mode - LikeOrFavourite.LIKE for like; LikeOrFavourite.FAVOURITE for favourite.
     * @return Returns true/false concerning whether an input Person id has already liked an the input Topic id.
     */
    private boolean likeOrFavouriteNeedsChanging(long TopicId, long PersonId, boolean intendToApply, LikeOrFavourite mode) throws SQLException {
        boolean topicAlreadyActedUpon = false;
        final String whichTable = mode.equals(LikeOrFavourite.FAVOURITE) ? "FavouritedTopic" : "LikedTopic";
        final String STMT = String.format("SELECT TopicId, PersonId FROM %s WHERE TopicId = ? AND PersonId = ?;", whichTable);

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, TopicId);
            p.setLong(2, PersonId);

            ResultSet rs = p.executeQuery();
            if (rs.next()) topicAlreadyActedUpon = true;

            if (intendToApply) {
                if (topicAlreadyActedUpon) return false;
            }
            else {
                if (!topicAlreadyActedUpon) return false;
            }

            return true;
        }
    }

    // TO ALEX - DONE
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        final String STMT;
        if (like) STMT = "INSERT INTO LikedTopic (TopicId, PersonId) VALUES (?, ?);";
        else STMT = "DELETE FROM LikedTopic WHERE TopicId = ? AND PersonId = ?;";

        try (PreparedStatement p1 = c.prepareStatement(STMT)) {
            Long personId = validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
            if (!likeOrFavouriteNeedsChanging(topicId, personId, like, LikeOrFavourite.LIKE)) return Result.success();

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
     * Set or unset a topic as favourite. Same semantics as likeTopic.
     * @param username - the person setting the favourite topic (must exist).
     * @param topicId - the topic to set as favourite (must exist).
     * @param fav - true to set, false to unset as favourite.
     * @return success (even if it was a no-op), failure if the person or topic
     * does not exist and fatal in case of db errors.
     */
    // TO PHAN
    @Override
    public Result favouriteTopic(String username, long topicId, boolean favourite) {
       final String STMT;
        if (favourite) STMT = "INSERT INTO FavouriteTopic (FavouriteId, PersonId) VALUES (?, ?);";
        else STMT = "DELETE FROM FavouriteTopic WHERE FavouriteId = ? AND PersonId = ?;";

        try (PreparedStatement p1 = c.prepareStatement(STMT)) {
            Long personId = validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (validateTopicId(topicId) == null) return Result.failure("topicId did not exist.");
            if (!likeOrFavouriteNeedsChanging(topicId, personId, favourite, LikeOrFavourite.FAVOURITE)) return Result.success();

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


    /**
     * Checks whether forumId has been registered in the Forum table, and has at least one Topic associated with it.
     *
     * @param forumId - forumId to check for the existence of in table.
     * @return Returns corresponding title of forumId. Otherwise, returns null.
     */
    private String validateForumId(long forumId) throws SQLException {
        final String checkForumId = "SELECT Forum.id, Forum.title FROM " +
                "Forum JOIN Topic ON Forum.id = Topic.ForumId WHERE Forum.id = ? LIMIT 1;";

        try (PreparedStatement p = c.prepareStatement(checkForumId)) {
            p.setLong(1, forumId);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // forum id doesn't exist, or forum has no topic under it.

            return rs.getString("title");
        }
    }

    /*
     * Level 3 - more complex queries. Leave these until last.
     */
    // TO ALEX - "I'll give it a go" [Jamie - added validation, rollback, and closed preparedStatements. Untested.]
    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
        final String createTopicSTMT = "INSERT INTO Topic (title, ForumId) VALUES(?, ?);";
        final String getTopicIdSTMT = "SELECT id FROM Topic WHERE title = ?;";
        final String STMT = "INSERT INTO Post (`date`, `text`, PersonId, TopicId) VALUES (?, ?, ?, ?);";

        if (title.equals("")) return Result.failure("title cannot be empty.");
        if (text.equals("")) return Result.failure("text cannot be empty.");

        try (PreparedStatement p2 = c.prepareStatement(createTopicSTMT);
             PreparedStatement p3 = c.prepareStatement(getTopicIdSTMT);
             PreparedStatement p1 = c.prepareStatement(STMT)) {

            Long personId = validateUsername(username);
            if (personId == null) return Result.failure("username did not exist.");
            if (validateForumId(forumId) == null)
                return Result.failure("Forum id did not exist, or forum has no topics under it (illegal)."); // TODO: ask about failure messages not printing.

            p2.setString(1, title);
            p2.setLong(2, forumId);
            p2.execute();

            p3.setString(1, title);
            ResultSet rs3 = p3.executeQuery();
            long topicId = rs3.getLong(1);

            long dateInSecs = new Date().getTime() / MS_TO_SECONDS;
            p1.setLong(1, dateInSecs);
            p1.setString(2, text);
            p1.setLong(3, personId);
            p1.setLong(4, topicId);

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

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	//TO ALEX
  /*  public AdvancedPersonView(String name, String username, String studentId,
            int topicLikes, int postLikes, List<TopicSummaryView> favourites)*/

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        final String STMT = "SELECT name, username, studentId FROM Person WHERE username = ?;";

        if (username.equals("")) return Result.failure("Username was empty or null.");

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            if (validateUsername(username) == null) return Result.failure("Username did not exist.");

            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            return Result.success(new AdvancedPersonView(
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("studentId"),
		            1,//getPersonalLikedPostCount(username),
		            1,//getPersonalFavouritedTopicCount(username),
		            new ArrayList<TopicSummaryView>()));
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.fatal(e.getMessage());
        }
    }

    //expects prior validation of username :)
    private int getPersonalLikedPostCount(String username){
        final String STMT = "SELECT COUNT(*) FROM Person JOIN LikedPost ON id = PersonId WHERE username = ?;";
        try (PreparedStatement p = c.prepareStatement(STMT)) {

            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            return rs.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }       
    }

    //expects prior validation of username :)
    private int getPersonalFavouritedTopicCount(String username){
        final String STMT = "SELECT COUNT(*) FROM Person JOIN FavouritedTopic ON id = PersonId WHERE username = ?;";
        try (PreparedStatement p = c.prepareStatement(STMT)) {

            p.setString(1, username);

            ResultSet rs = p.executeQuery();

            return rs.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }       
    }

    // TODO: under construction.
    @Override
    public Result<AdvancedForumView> getAdvancedForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
//        final String STMT = "SELECT Topic.id AS topicId, Topic.title AS topicTitle "
//                + "FROM Forum INNER JOIN Topic ON Forum.id = Topic.ForumId "
//                + "WHERE forumId = ? ORDER BY Topic.title ASC;";
//        List<TopicSummaryView> topics = new ArrayList<>();
//        try (PreparedStatement p = c.prepareStatement(STMT)) {
//            String forumTitle = validateForumId(id);
//            if (forumTitle == null) return Result.failure("Forum id did not exist, or forum has no topics under it (illegal).");
//
//            p.setLong(1, (int) id);
//            ResultSet rs = p.executeQuery();
//
//            while (rs.next()) topics.add(new TopicSummaryView(
//                    rs.getLong("topicId"), id, rs.getString("topicTitle"),
//                    // int postCount, int created, int lastPostTime, String lastPostName, int likes, String creatorName, String creatorUserName
//                    )
//            );
//
//            return Result.success(new AdvancedForumView(id, forumTitle, topics));
//        } catch (SQLException e) {
//            return Result.fatal(e.getMessage());
//        }
    }

    // TODO: this shouldn't be at all hard - just reference likeTopic() and generalise the likeOrFavouriteNeedsChanging() private method.
    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
