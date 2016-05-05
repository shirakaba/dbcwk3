SELECT Topic.Id, Forum.Id, Topic.title, Person.name, Person.username FROM Person 
                            JOIN FavouritedTopic ON Person.id = FavouritedTopic.PersonId 
                            JOIN Post ON Post.PersonId = Person.id 
                            JOIN Topic ON Topic.id = Post.TopicId 
                            JOIN Forum ON Forum.id = ForumId 
                            WHERE username = 'aaaaa';
