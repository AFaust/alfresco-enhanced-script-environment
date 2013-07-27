importScript("registry", "evaluator.lib.js@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "filters.lib.js@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "parse-args.lib.js@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "doclist.lib.js@documentlibrary-v2", true, importVersionCondition);

/**
 * Document List Component: doclist
 */
model.doclist = doclist_main();