var onlySelf = (filter.indexOf("ByMe")) > 0 ? true : false, dateField = (filter.indexOf("Modified") > 0) ? "modified" : "created", ownerField = (dateField === "created") ? "creator"
        : "modifier",
// Default to 7 days - can be overridden using "days" argument
dayCount = 7, argDays = args.days, date, toQuery, fromQuery, filterQuery;

if ((argDays !== null) && !isNaN(argDays))
{
    dayCount = argDays;
}

// Default limit to 50 documents - can be overridden using "max" argument
if (limitResults === null)
{
    limitResults = 50;
}

date = new Date();
toQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();
date.setDate(date.getDate() - dayCount);
fromQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();

filterQuery = constructPathQuery(parsedArgs);
filterQuery += " +@cm\\:" + dateField + ":[" + fromQuery + "T00\\:00\\:00.000 TO " + toQuery + "T23\\:59\\:59.999]";
if (onlySelf)
{
    filterQuery += " +@cm\\:" + ownerField + ":\"" + person.properties.userName + '"';
}
filterQuery += ' +TYPE:"cm:content"';

sort = [
{
    column : "@cm:" + dateField,
    ascending : false
} ];

query = filterQuery + filterQueryDefaults;