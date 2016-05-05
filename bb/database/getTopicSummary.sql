SELECT * FROM Person
JOIN FavouritedTopic ON Person.id = PersonId
JOIN Topic ON Topic.id = TopicId
WHERE username = 'aaaaa';

