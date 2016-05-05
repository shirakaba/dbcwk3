SELECT date, Person.name FROM Topic 
JOIN Post ON Post.TopicId = Topic.id  
JOIN Person ON PersonId = Person.id
WHERE Topic.id = 1
ORDER BY `date` DESC, Post.id DESC LIMIT 1;
