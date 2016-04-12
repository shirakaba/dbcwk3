<#include "header.html">

<h1>Person: ${name}</h1>

<div class="section">
<h2>Information</h2>
<div class="grid">
    <div class="col-2"></div>
    <div class="col-4">Name</div>
    <div class="col-4">${name}</div>
    <div class="col-2"></div>
</div>
<div class="grid">
    <div class="col-2"></div>
    <div class="col-4">Username</div>
    <div class="col-4">${username}</div>
    <div class="col-2"></div>
</div>
<div class="grid">
    <div class="col-2"></div>
    <div class="col-4">Student id</div>
    <div class="col-4">${studentId}</div>
    <div class="col-2"></div>
</div>

<p><a href="/person/${username}">simple view</a></p>
</div>

<div class="section">
<h2>Likes</h2>
<p>
${name} has liked ${topicLikes} topics and ${postLikes} posts.
</p>
</div>

<div class="section">
<h2>Favourites</h2>
<#list favouriteTopics as topic>
<div class="subsection">
<p><a href="/topic/${topic.topicId}"><b>${topic.title}</b></a>
    (created by ${topic.creatorName} [${topic.creatorUserName}]
    at ${topic.created?number_to_datetime})</p>
<p>${topic.postCount} posts, last one by ${topic.lastPostName}
    at ${topic.lastPostTime?number_to_datetime}. ${topic.likes} likes.</p>
</div>
</#list>
</div>

<#include "footer.html">

