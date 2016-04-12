<#include "header.html">

<h1>Forum: ${title}</h1>

<#list topics as t>
<div class="section">
<p><b><a href="/topic/${t.topicId}">${t.title}</a></b>
    (created by <a href="/person/${t.creatorUserName}">${t.creatorName} [${t.creatorUserName}]</a>
    at ${(t.created*1000)?number_to_datetime})</p>
<p><b>${t.postCount} posts</b>, last one by ${t.lastPostName}
    at ${(t.lastPostTime*1000)?number_to_datetime}. ${t.likes} likes.</p>
</div>
</#list>

<p><a href="/forum/${id}">default view</a> advanced view</p>

<div class="section alt">
<p><a href="/newtopic/${id}">Create new topic</a></p>
</div>

<#include "footer.html">

