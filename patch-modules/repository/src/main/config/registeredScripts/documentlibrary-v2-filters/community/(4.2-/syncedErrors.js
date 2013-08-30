var filterQuery = constructPathQuery(parsedArgs);
filterQuery += ' +ASPECT:"sync:syncFailed"';
query = filterQuery;