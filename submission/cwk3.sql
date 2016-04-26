DROP TABLE IF EXISTS Person;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Forum;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS LikedPost;
DROP TABLE IF EXISTS LikedTopic;
DROP TABLE IF EXISTS FavouritedTopic;


CREATE TABLE Person ( -- This schema is set by David
  id INTEGER PRIMARY KEY,
  username VARCHAR(10) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  studentId VARCHAR(10) NULL
);

CREATE TABLE Topic (
  id INTEGER PRIMARY KEY,
  title TEXT NOT NULL,
  ForumId INTEGER REFERENCES Forum(id)
);

CREATE TABLE Forum (
  id INTEGER PRIMARY KEY,
  title TEXT NOT NULL UNIQUE,
);

CREATE TABLE Post (
  id INTEGER PRIMARY KEY,
  date DATE NOT NULL,
  text TEXT NOT NULL,
  PersonId INTEGER REFERENCES Person(id),
  TopicId INTEGER REFERENCES Topic(id),
);

CREATE TABLE LikedPost (
  PostId INTEGER REFERENCES Post(id),
  PersonId INTEGER REFERENCES Person(id)
);

CREATE TABLE LikedTopic (
  TopicId INTEGER REFERENCES Topic(id),
  PersonId INTEGER REFERENCES Person(id)
);

CREATE TABLE FavouritedTopic (
  TopicId INTEGER REFERENCES Topic(id),
  PersonId INTEGER REFERENCES Person(id)
);
