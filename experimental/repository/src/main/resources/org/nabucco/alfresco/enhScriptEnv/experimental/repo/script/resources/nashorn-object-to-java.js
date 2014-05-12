function convert(obj)
{
    var result, type, arr, idx, max, key;

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
        arr = obj.length;

        for (idx = 0, max = obj.length; idx < max; idx++)
        {
            arr[idx] = convert(obj[idx]);
        }

        result = Java.to(arr, "java.util.List");
    }
    else
    {
        if (obj.getClass !== undefined)
        {
            // return as is
            result = obj;
        }
        else if (typeof obj === 'object')
        {
            type = Java.type("java.util.HashMap");
            result = new type();
            for (key in obj)
            {
                if (obj.hasOwnProperty(key))
                {
                    result.put(key, convert(obj[key]));
                }
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

javaObj = convert(nashornObj);
