(function contributeImportScript(importer)
{
    var importerDelegate = importer, existingImportScriptProp, asObject = function(obj, allowJavaObj)
    {
        var result;

        if (obj !== undefined && obj !== null)
        {
            if (allowJavaObj === true && obj.getClass !== undefined && obj.hasOwnProperty === undefined)
            {
                result = obj;
            }
            else if (typeof obj === 'object')
            {
                result = obj;
            }
            else
            {
                result = null;
            }
        }
        else
        {
            result = null;
        }

        return result;
    };

    // might have been defined already
    existingImportScriptProp = Object.getOwnPropertyDescriptor(this, "importScript");
    if (existingImportScriptProp === undefined || existingImportScriptProp.configurable === true)
    {
        Object.defineProperty(this, "importScript",
        {
            configurable : false,
            enumerable : false,
            writable : false,
            value : function importScript(locatorType, locationValue, failOnMissingScript, resolutionParams, executionScope)
            {
                var resolutionParamObj = asObject(resolutionParams, true), executionScopeObj = asObject(executionScope, false);

                return importerDelegate.importScript(locatorType, locationValue, failOnMissingScript === true, resolutionParamObj, this,
                        executionScopeObj);
            }
        }, true);
    }
}).call(this, NashornImportScriptFunction);