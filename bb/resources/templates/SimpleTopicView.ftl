<#include "header.html">

<h1>Topic: ${title}</h1>

<#list posts as p>
<div class="section">
<p>Post #${p.postNumber} by ${p.author} at ${(p.postedAt*1000)?number_to_datetime}</p>
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

