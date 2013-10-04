var importVersionCondition =
{
    fallbackChain : [
    // look for version and edition specific match
    {
        version : DESCRIPTOR.VERSION
    },
    // look for version specific but edition independent match
    {
        version : DESCRIPTOR.VERSION,
        community : null
    } ]
};
importScript("registry", "doclist.get@documentlibrary-v2", true, importVersionCondition);