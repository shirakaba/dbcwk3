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

<p><a href="/person2/${username}">advanced view</a></p>
</div>

<#include "footer.html">
