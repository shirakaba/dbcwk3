package uk.ac.bris.cs.databases.cwk3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 *
 * @author Alex, Jamie & Phan
 */
public class ValidityTester {

    private final Connection c;

    /**
     * Used to switch the mode of the likeOrFavouriteNeedsChanging() method.
     *
     * */
    enum LikeOrFavourite {
        LIKE,
        FAVOURITE
    }

    ValidityTester(Connection c){
        this.c = c;
    }

    // TODO: make this compile
    /**
     * validate if a post exist by checking the post index against the total of post from a topic,
     * if the index > the total number => the post does not exist
     */
    boolean validatePost (int post, long topicId) throws SQLException {
        throw new UnsupportedOperationException("not implemented yet.");
//        final String STMT =  "SELECT count(Post.id) AS totalPost FROM Post LEFT JOIN Topic ON Post.TopicId = Topic.id WHERE Topic.id = ?;";
//        int count;
//
//        try (PreparedStatement p = c.prepareStatement(STMT)) {
//            p.setLong(1, topicId);
//            ResultSet rs = p.executeQuery();
//            if (rs.next()) {
//                count = rs.getInt("totalPost");
//                return post <= count;
//            }
//        }
//        return false; //TODO: assess this
    }

    /**
     * Checks whether username is in the Person table.
     *
     * @param username - the username to check for the existence of in the Person table.
     * @return the corresponding Person.id if the username is registered. Otherwise, null.
     */
    Long validateUsername(String username) throws SQLException {
        final String getUsername = "SELECT id, username FROM Person WHERE username = ?;";

        try (PreparedStatement p = c.prepareStatement(getUsername)) {
            p.setString(1, username);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // username doesn't exist

            return rs.getLong("id");
        }
    }	

    /**
     * Checks whether topicId is in the Topic table.
     *
     * @param topicId - the id to check for existence of in the Topic table.
     * @return the corresponding ForumId if the topicId is registered. Otherwise, null.
     */
    Long validateTopicId(long topicId) throws SQLException {
        final String getTopicId = "SELECT id, forumId FROM Topic WHERE id = ?;";

        try (PreparedStatement p = c.prepareStatement(getTopicId)) {
            p.setLong(1, topicId);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // topicId doesn't exist

            return rs.getLong("ForumId");
        }
    }

    /**
     * Checks whether forumId has been registered in the Forum table, and has at least one Topic associated with it.
     *
     * @param forumId - forumId to check for the existence of in table.
     * @return Returns corresponding title of forumId. Otherwise, returns null.
     */
    String validateForumId(long forumId) throws SQLException {
        final String checkForumId = "SELECT Forum.id, Forum.title FROM " +
                "Forum JOIN Topic ON Forum.id = Topic.ForumId WHERE Forum.id = ? LIMIT 1;";

        try (PreparedStatement p = c.prepareStatement(checkForumId)) {
            p.setLong(1, forumId);

            ResultSet rs = p.executeQuery();
            if (!rs.next()) return null; // forum id doesn't exist, or forum has no topic under it.

            return rs.getString("title");
        }
    }


    /**
     * Checks whether sufficient Posts exist at the page offset to display a page worth of Posts.
     *
     * @param page    - the page of Posts to navigate to.
     * @param topicId - TopicId to check for the existence of in table.
     * @return false if too few Posts. True if enough Posts, or if page specified is zero (supporting any number of Posts).
     */
    boolean validatePostCount(long topicId, int page) throws SQLException {
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
    boolean likeOrFavouriteNeedsChanging(long TopicId, long PersonId, boolean intendToApply, LikeOrFavourite mode) throws SQLException {
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

}

