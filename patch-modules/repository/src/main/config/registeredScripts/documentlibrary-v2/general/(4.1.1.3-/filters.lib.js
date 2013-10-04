// load script for previous version
importScript("registry", "filters.lib@documentlibrary-v2", true,
{
    // get the script from the previous version
    version : "4.0.2.9",
    community : null
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
