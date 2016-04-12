<#include "header.html">

<h1>Forum: ${title}</h1>

<#list topics as t>
<div class="section">
<p><b><a href="/topic0/${t.topicId}">${t.title}</a></b></p>
</div>
</#list>

<p>default view <a href="/forum2/${id}">advanced view</a></p>

<div class="section alt">
<p><a href="/newtopic/${id}">Create new topic</a></p>
</div>

<#include "footer.html">

