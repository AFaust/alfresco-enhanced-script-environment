// We can reuse the script from Enterprise 4.0.1 because they are fully identical
importScript("registry", "doclist.get.js@documentlibrary-v2", true,
{
    version : "4.0.1",
    // have to override the implicit community flag
    community : false
});