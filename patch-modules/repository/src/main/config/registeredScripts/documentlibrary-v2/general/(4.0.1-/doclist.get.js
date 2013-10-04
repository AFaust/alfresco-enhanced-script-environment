importScript("registry", "evaluator.lib@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "filters.lib@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "parse-args.lib@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "doclist.lib@documentlibrary-v2", true, importVersionCondition);

/**
 * Document List Component: doclist
 */
model.doclist = doclist_main();