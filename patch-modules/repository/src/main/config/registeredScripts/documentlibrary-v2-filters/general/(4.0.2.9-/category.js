var filterQuery = constructPathQuery(parsedArgs);

// Remove any trailing "/" character
if (filterData.charAt(filterData.length - 1) == "/")
{
    filterData = filterData.slice(0, -1);
}

query = filterQuery + ' +PATH:"/cm:generalclassifiable' + iso9075EncodePath(filterData) + '/member"';