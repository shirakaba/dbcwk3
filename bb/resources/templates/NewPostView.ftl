<#include "header.html">

<h1>Create new post</h1>

<div class="section">
<form method="post" action="/createpost">

<p><textarea rows="10" cols="80" name="text"></textarea></p>
<p>Post as:</p>

<p>
<select name="user">
<#list users as u>
<option value="${u}">${u}</option>
</#list>
</select>
</p>
<input type="hidden" name="topic" value="${topic}" />
<p><input type="submit" Value="Post"/></p>
</form>
</div>

<#include "footer.html">

