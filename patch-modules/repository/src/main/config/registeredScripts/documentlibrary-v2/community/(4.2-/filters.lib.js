// We can reuse the script from Enterprise 4.1.4 because they are identical
importScript("registry", "filters.lib@documentlibrary-v2", true,
{
    version : "4.0.2.9",
    // have to override the implicit community flag
    community : false
});

// apply patch
Filters.constructPathQuery = function constructPathQuery(parsedArgs)
{
    var pathQuery = "";
    if (parsedArgs.libraryRoot != companyhome || parsedArgs.nodeRef != "alfresco://company/home")
    {
        if (parsedArgs.nodeRef == "alfresco://sites/home")
        {
            // all sites query - better with //cm:*
            pathQuery = '+PATH:"' + parsedArgs.rootNode.qnamePath + '//cm:*"';
        }
        else
        {
            // site specific query - better with //*
            pathQuery = '+PATH:"' + parsedArgs.rootNode.qnamePath + '//*"';
        }
    }
    return pathQuery;
};
