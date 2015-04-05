var favourite, filterQuery = "";

for (favourite in favourites)
{
    if (filterQuery)
    {
        filterQuery += " OR ";
    }

    filterQuery += 'ID:"' + favourite + '"';
}

if (filterQuery.length !== 0)
{
    filterQuery = "+(" + filterQuery + ")";
    // no need to specify path here for all sites - IDs are exact matches
    if (parsedArgs.nodeRef != "alfresco://sites/home" && parsedArgs.nodeRef != "alfresco://company/home")
    {
        filterQuery += ' +PATH:"' + parsedArgs.rootNode.qnamePath + '//*"';
    }
}
else
{
    // empty favourites query
    filterQuery = '+ID:""';
}

query = filterQuery;