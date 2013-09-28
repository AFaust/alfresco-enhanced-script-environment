// We can reuse the script from Enterprise 4.1.4 because they are identical except for some comment formatting
importScript("registry", "doclist.lib@documentlibrary-v2", true,
{
    version : "4.1.4",
    // have to override the implicit community flag
    community : false
});