package uk.ac.bris.cs.databases.cwk3;

import uk.ac.bris.cs.databases.util.Params;

/**
 * View of a Post made by a Person that is either first or last in a Topic.
 */
public class ExtremePostView {

    /* The name of the person. */
    private final String name;

    /* The username of the person. */
    private final String username;

    /* The studentId of the person or the empty string if the person does not
     * have a student Id.
    */
    private final int date;

    /**
     *
     * @param date - of Post creation or last Post in Topic.
     * @param name - of Person who made the Post.
     * @param username - of Person who made the Post.
     */
    public ExtremePostView(int date, String name, String username) {

        Params.cannotBeEmpty(name);
        Params.cannotBeEmpty(username);
        Params.cannotBeNull(date);

        this.name = name;
        this.username = username;
        this.date = date;
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
     * @return the date
     */
    public int getDate() {
        return date;
    }
}
