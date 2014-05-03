function convert(obj)
{
    var result, type, idx, max, cls, clsName, key;

    if (obj === undefined || obj === null)
    {
        result = obj;
    }
    else if (typeof obj === 'string' || (obj.trim !== undefined && obj.toUpperCase !== undefined))
    {
        type = Java.type("java.lang.String");
        result = new type(obj);
    }
    else if (obj.length !== undefined)
    {
        type = Java.type("org.mozilla.javascript.NativeArray");
        result = new type(obj.length);

        for (idx = 0, max = obj.length; idx < max; idx++)
        {
            result.put(idx, result, convert(obj[idx]));
        }
    }
    else
    {
        if (obj.getClass !== undefined)
        {
            cls = obj.getClass();
            clsName = cls.getName();
            if (clsName.startsWith("java.util.") && clsName.endsWith("Map"))
            {
                type = Java.type("org.mozilla.javascript.NativeObject");
                result = new type();
                for (key in obj)
                {
                    result.put(key, result, convert(obj[key]));
                }
            }
            // TODO: Do other types of potential Java objects need to be mapped?
            else
            {
                // return as-is
                result = obj;
            }
        }
        else if (typeof obj === 'object')
        {
            type = Java.type("org.mozilla.javascript.NativeObject");
            result = new type();
            for (key in obj)
            {
                result.put(key, result, convert(obj[key]));
            }
        }
        else if (typeof obj === 'number' && isFinite(obj))
        {
            type = Java.type("java.lang.Double");
            result = type.valueOf(String(obj));
        }
        else if (typeof obj === 'boolean')
        {
            type = Java.type("java.lang.Boolean");
            result = type.valueOf(String(obj));
        }
        else
        {
            throw new Error(obj + " is not supported");
        }
    }

    return result;
}

rhinoObj = convert(nashornObj);
