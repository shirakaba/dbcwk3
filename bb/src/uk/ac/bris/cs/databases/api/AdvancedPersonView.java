package uk.ac.bris.cs.databases.api;

import java.util.List;
import uk.ac.bris.cs.databases.util.Params;

/**
 * Advanced view of a person including #likes and favourite topics.
 * 
 * @author csxdb
 */
public class AdvancedPersonView {
    
    /* The name of the person. */
    private final String name;
    
    /* The username of the person. */
    private final String username;
    
    /* The studentId of the person or the empty string if the person does not
     * have a student Id.
    */
    private final String studentId;
    
    /* The total number of likes that topics started by this person have
     * received.
     */
    private final int topicLikes;
    
    /* The total number of likes that posts by this person have received. */
    private final int postLikes;
    
    /* The list of this person's favourite topics, sorted alphabetically by
       topic name.
    */
    private final List<TopicSummaryView> favouriteTopics;

    public AdvancedPersonView(String name, String username, String studentId,
            int topicLikes, int postLikes, List<TopicSummaryView> favourites) {
        
        Params.cannotBeEmpty(name);
        Params.cannotBeEmpty(username);
        Params.cannotBeNull(studentId);
        Params.cannotBeNull(favourites);
        
        this.name = name;
        this.username = username;
        this.studentId = studentId;
        this.topicLikes = topicLikes;
        this.postLikes = postLikes;
        this.favouriteTopics = favourites;
    }
        
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the studentId
     */
    public String getStudentId() {
        return studentId;
    }


    /**
     * @return the topicLikes
     */
    public int getTopicLikes() {
        return topicLikes;
    }

    /**
     * @return the postLikes
     */
    public int getPostLikes() {
        return postLikes;
    }

    /**
     * @return the favouriteTopics
     */
    public List<TopicSummaryView> getFavouriteTopics() {
        return favouriteTopics;
    }
    
}
