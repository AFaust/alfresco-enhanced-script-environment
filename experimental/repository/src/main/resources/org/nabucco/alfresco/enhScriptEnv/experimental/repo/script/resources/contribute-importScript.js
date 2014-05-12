(function(importer, scopeParam)
{
    var importerDelegate = importer, scope = scopeParam, asObject = function(obj, allowJavaObj)
    {
        var result;

        if (obj !== undefined && obj !== null)
        {
            if (allowJavaObj === true && obj.getClass !== undefined)
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

    Object.defineProperty(this, "importScript",
    {
        configurable : false,
        enumerable : false,
        writable : false,
        value : function(locatorType, locationValue, failOnMissingScript, resolutionParams, executionScope)
        {
            var resolutionParamObj = asObject(resolutionParams, true), executionScopeObj = asObject(executionScope, false);

            return importerDelegate.importScript(locatorType, locationValue, failOnMissingScript === true, resolutionParamObj, scope,
                    executionScopeObj);
        }
    });
}(NashornImportScriptFunction, this));