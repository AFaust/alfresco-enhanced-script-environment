function convert(obj)
{
    var result, type, arr, idx, max, key, convertedKey, el, convertedEl, mapType = Java.type("java.util.Map"), listType = Java
            .type("java.util.List");

    if (obj === undefined || obj === null)
    {
        result = obj;
    }
    // TODO: remove null checks when 8u20 b16 ships
    else if (typeof obj === 'string'
            || (obj.trim !== undefined && obj.toUpperCase !== undefined && obj.trim !== null && obj.toUpperCase !== null))
    {
        type = Java.type("java.lang.String");
        result = new type(obj);
    }
    else if (typeof obj === 'number' && isFinite(obj))
    {
        // check for (representable) integer
        if (obj === obj | 0)
        {
            type = Java.type("java.lang.Integer");
        }
        else
        {
            type = Java.type("java.lang.Double");
        }
        result = type.valueOf(String(obj));
    }
    else if (typeof obj === 'boolean')
    {
        type = Java.type("java.lang.Boolean");
        result = type.valueOf(String(obj));
    }
    // TODO: remove null checks when 8u20 b16 ships
    else if (obj.getClass !== undefined && obj.getClass !== null)
    {
        if (obj instanceof listType)
        {
            for (idx = 0, max = obj.length; idx < max; idx++)
            {
                el = obj[idx];
                convertedEl = convert(el);

                if (el !== convertedEl)
                {
                    obj[idx] = convertedEl;
                }
            }
        }
        else if (obj instanceof mapType)
        {
            for (key in obj)
            {
                el = obj[key];
                convertedKey = convert(key);
                convertedEl = convert(el);

                if (convertedKey !== key || convertedEl !== el)
                {
                    delete obj[key];
                    obj[convertedKey] = convertedEl;
                }
            }
        }
        // return as is
        result = obj;
    }
    // TODO: remove null checks when 8u20 b16 ships
    else if (obj.length !== undefined && obj.length !== null)
    {
        // simple array
        arr = [];

        for (idx = 0, max = obj.length; idx < max; idx++)
        {
            el = obj[idx];
            convertedEl = convert(el);
            arr.push(convertedEl);
        }

        result = Java.to(arr, "java.util.List");
    }
    else if (typeof obj === 'object')
    {
        // native JS object - treat as hash
        type = Java.type("java.util.HashMap");
        result = new type();
        for (key in obj)
        {
            if (obj.hasOwnProperty(key))
            {
                el = obj[key];
                convertedKey = convert(key);
                convertedEl = convert(el);
                result.put(convertedKey, convertedEl);
            }
        }
    }
    else
    {
        throw new Error(obj + " is not supported");
    }

    return result;
}

javaObj = convert(nashornObj);