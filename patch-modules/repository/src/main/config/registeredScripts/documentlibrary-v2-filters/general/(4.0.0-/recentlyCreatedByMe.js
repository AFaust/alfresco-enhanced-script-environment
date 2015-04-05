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
importScript("registry", "recently@documentlibrary-v2-filters", true, importVersionCondition);