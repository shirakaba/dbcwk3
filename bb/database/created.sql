SELECT date, Person.name, Person.username FROM Topic 
JOIN Post ON Post.TopicId = Topic.id  
JOIN Person ON PersonId = Person.id
WHERE Topic.id = 1
ORDER BY `date` ASC LIMIT 1;
