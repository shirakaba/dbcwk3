<#include "header.html">

<h1>Topic: ${title}</h1>

<div class="section">
<p>In forum: <a href="/forum/${forumId}">${forumName}</a></p>
<#if (page > 0)>
<p>Page ${page}</p>
</#if>
</div>

<#list posts as p>
<div class="section">
<p>Post #${p.postNumber} (${p.likes} likes)
by: <a href="/person/${p.authorUserName}">${p.authorName} [${p.authorUserName}]</a>
    at ${(p.postedAt*1000)?number_to_datetime}</p>
<pre>
${p.text}
</pre>
</div>
</#list>

<div class="section alt">
<p>
<a href="/newpost/${topicId}">Reply</a>
</p>
</div>

<#include "footer.html">

