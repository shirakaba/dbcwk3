DROP TABLE IF EXISTS LikedPost;
DROP TABLE IF EXISTS LikedTopic;
DROP TABLE IF EXISTS FavouritedTopic;

CREATE TABLE LikedPost (
  PostId INTEGER REFERENCES Post(id),
  PersonId INTEGER REFERENCES Person(id),
  CONSTRAINT LikedPostUnique UNIQUE (PostId, PersonId) ON CONFLICT ABORT);

CREATE TABLE LikedTopic (
  TopicId INTEGER REFERENCES Topic(id),
  PersonId INTEGER REFERENCES Person(id),
  CONSTRAINT LikedTopicUnique UNIQUE (TopicId, PersonId) ON CONFLICT ABORT);


CREATE TABLE FavouritedTopic (
  TopicId INTEGER REFERENCES Topic(id),
  PersonId INTEGER REFERENCES Person(id),
  CONSTRAINT FavouritedTopic UNIQUE (TopicId, PersonId) ON CONFLICT ABORT);

