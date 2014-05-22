function convert(obj)
{
    var result, type, arr, idx, max, key, convertedKey, el, convertedEl, mapType = Java.type("java.util.Map"), listType = Java
            .type("java.util.List");

    print(obj);

    if (obj === undefined || obj === null)
    {
        result = obj;
    }
    else if (typeof obj === 'string' || (obj.trim !== undefined && obj.toUpperCase !== undefined))
    {
        type = Java.type("java.lang.String");
        result = new type(obj);
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
    else if (obj.getClass !== undefined)
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
                if (obj.hasOwnProperty(key))
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
        }
        // return as is
        result = obj;
    }
    else if (obj.length !== undefined)
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