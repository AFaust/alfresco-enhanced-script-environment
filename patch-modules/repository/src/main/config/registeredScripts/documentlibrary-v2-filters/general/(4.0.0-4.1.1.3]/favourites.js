var foundOne = false, favourite, filterQuery = "";

for (favourite in favourites)
{
    if (foundOne)
    {
        filterQuery += " OR ";
    }

    foundOne = true;
    filterQuery += 'ID:"' + favourite + '"';
}

if (filterQuery.length > 0)
{
    filterQuery = "+(" + filterQuery + ") " + constructPathQuery(parsedArgs);
}
else
{
    // empty favourites query
    filterQuery = '+ID:""';
}

query = filterQuery;