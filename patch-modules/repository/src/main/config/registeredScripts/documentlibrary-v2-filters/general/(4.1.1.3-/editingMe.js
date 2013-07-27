var filterQuery = constructPathQuery(parsedArgs);
filterQuery += ' +((+@cm\\:workingCopyOwner:"' + person.properties.userName + '")';
filterQuery += ' OR (+@cm\\:lockOwner:"' + person.properties.userName + '"';
filterQuery += ' +@cm\\:lockType:"WRITE_LOCK"))';
query = filterQuery;