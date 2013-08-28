var Filters =
{
    /**
     * Type map to filter required types. NOTE: "documents" filter also returns folders to show UI hint about hidden folders.
     */
    TYPE_MAP :
    {
        "documents" : '+(TYPE:"content" OR TYPE:"app:filelink" OR TYPE:"folder")',
        "folders" : '+(TYPE:"folder" OR TYPE:"app:folderlink")',
        "images" : '+@cm\\:content.mimetype:image/*'
    },

    /**
     * Types that we want to suppress from the resultset
     */
    IGNORED_TYPES : [ "cm:systemfolder", "fm:forums", "fm:forum", "fm:topic", "fm:post" ],
    
    /**
     * Aspects ignored from the canned query based resultset
     */
    // the following is only used in 4.1.2-
    IGNORED_ASPECTS:
    [
       "cm:checkedOut"
    ],

    /**
     * Encode a path with ISO9075 encoding
     * 
     * @method iso9075EncodePath
     * @param path
     *            {string} Path to be encoded
     * @return {string} Encoded path
     */
    iso9075EncodePath : function Filter_iso9075EncodePath(path)
    {
        var parts = path.split("/");
        for ( var i = 1, ii = parts.length; i < ii; i++)
        {
            parts[i] = "cm:" + search.ISO9075Encode(parts[i]);
        }
        return parts.join("/");
    },

    /**
     * Create filter parameters based on input parameters
     * 
     * @method getFilterParams
     * @param filter
     *            {string} Required filter
     * @param parsedArgs
     *            {object} Parsed arguments object literal
     * @param optional
     *            {object} Optional arguments depending on filter type
     * @return {object} Object literal containing parameters to be used in Lucene search
     */
    getFilterParams : function Filter_getFilterParams(filter, parsedArgs, optional)
    {
        var filterParams =
        {
            query : "+PATH:\"" + parsedArgs.pathNode.qnamePath + "/*\"",
            limitResults : null,
            sort : [
            {
                column : "@cm:name",
                ascending : true
            } ],
            language : "lucene",
            templates : null,
            variablePath : true,
            // the following applies to 4.0.1-
            ignoreTypes : Filters.IGNORED_TYPES,
            // the following applies to 4.1.2-
            ignoreAspects : Filters.IGNORED_ASPECTS
        };

        optional = optional || {};

        // Sorting parameters specified?
        var sortAscending = args.sortAsc, sortField = args.sortField;

        if (sortAscending == "false")
        {
            filterParams.sort[0].ascending = false;
        }
        if (sortField !== null)
        {
            filterParams.sort[0].column = (sortField.indexOf(":") != -1 ? "@" : "") + sortField;
        }

        // Max returned results specified?
        var argMax = args.max;
        if ((argMax !== null) && !isNaN(argMax))
        {
            filterParams.limitResults = argMax;
        }

        var favourites = optional.favourites;
        if (typeof favourites == "undefined")
        {
            favourites = [];
        }

        // Create query based on passed-in arguments
        var filterData = String(args.filterData), filterQuery = "";

        // Common types and aspects to filter from the UI - known subtypes of cm:content and cm:folder
        var filterQueryDefaults = ' -TYPE:"' + Filters.IGNORED_TYPES.join('" -TYPE:"') + '"';

        var filterContext =
        {
            /*
             * parameter properties // TODO how to automatically pass all interesting values without the sensitive ones? e.g. companyhome
             * ...
             */
            filterQueryDefaults : filterQueryDefaults,
            parsedArgs : parsedArgs,
            filter : filter,
            args : args,
            person : person,
            constructPathQuery : this.constructPathQuery,
            favourites : favourites,
            /*
             * result properties
             */
            limitResults : filterParams.limitResults,
            // clone to decouple from changes
            sort : [
            {
                column : filterParams.sort[0].column,
                ascending : filterParams.sort[0].ascending
            } ],
            query : filterParams.query,
            variablePath : filterParams.variablePath
        }, filterImportVersionCondition =
        {
            version : DESCRIPTOR.VERSION,
            // filters are simple enough that they should be shareable by Community and Enterprise
            community : null
        };

        var filterImported = importScript("registry", filter + "@documentlibrary-v2-filters", false, filterImportVersionCondition,
                filterContext);
        if (!filterImported)
        {
            // the requested filter might be edition specific, try again with edition specific lookup
            filterImportVersionCondition.community = DESCRIPTOR.IS_COMMUNITY;
            filterImported = importScript("registry", filter + "@documentlibrary-v2-filters", false, filterImportVersionCondition,
                    filterContext);

            if (!filterImported)
            {
                // default "path"
                filterParams.variablePath = false;
                filterQuery = '+PATH:"' + parsedArgs.pathNode.qnamePath + '/*"';
                filterParams.query = filterQuery + filterQueryDefaults;
            }
            else
            {
                filterParams.query = filterContext.query;
                filterParams.limitResults = filterContext.limitResults;
                filterParams.sort = filterContext.sort;
                filterParams.variablePath = filterContext.variablePath;
            }
        }
        else
        {
            filterParams.query = filterContext.query;
            filterParams.limitResults = filterContext.limitResults;
            filterParams.sort = filterContext.sort;
            filterParams.variablePath = filterContext.variablePath;
        }

        // Specialise by passed-in type
        if (filterParams.query !== "")
        {
            filterParams.query += " " + (Filters.TYPE_MAP[parsedArgs.type] || "");
        }

        return filterParams;
    },

    constructPathQuery : function constructPathQuery(parsedArgs)
    {
        var pathQuery = "";
        if (parsedArgs.nodeRef != "alfresco://company/home")
        {
            pathQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath;
            if (parsedArgs.nodeRef == "alfresco://sites/home")
            {
                pathQuery += "/*/cm:documentLibrary";
            }
            pathQuery += "//*\"";
        }
        return pathQuery;
    }
};
