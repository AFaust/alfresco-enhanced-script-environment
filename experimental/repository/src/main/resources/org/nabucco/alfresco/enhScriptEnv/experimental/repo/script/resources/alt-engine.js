(function(JavaParam, javaParam, PackagesParam)
{
    var Java = JavaParam, java = javaParam, Packages = PackagesParam, print;

    /**
     * This script file is executed by script engine at the construction of the every new Global object. The functions here assume global
     * variables "context" of type javax.script.ScriptContext and "engine" of the type jdk.nashorn.api.scripting.NashornScriptEngine.
     */

    Object.defineProperty(this, "__noSuchProperty__",
    {
        configurable : true,
        enumerable : false,
        writable : true,
        value : function(name)
        {
            'use strict';
            return engine.__noSuchProperty__(this, context, name);
        }
    });

    print = function()
    {
        var writer = context != null ? context.writer : engine.context.writer;
        if (!(writer instanceof java.io.PrintWriter))
        {
            writer = new java.io.PrintWriter(writer);
        }

        var buf = new java.lang.StringBuilder();
        for (var i = 0; i < arguments.length; i++)
        {
            if (i != 0)
            {
                buf.append(' ');
            }
            buf.append(String(arguments[i]));
        }
        writer.println(buf.toString());
    };

    Object.defineProperty(this, "print",
    {
        configurable : true,
        enumerable : false,
        writable : true,
        value : print
    });

    /**
     * This is C-like printf
     * 
     * @param format
     *            string to format the rest of the print items
     * @param args
     *            variadic argument list
     */
    Object.defineProperty(this, "printf",
    {
        configurable : true,
        enumerable : false,
        writable : true,
        value : function(format, args/* , more args */)
        {
            print(sprintf.apply(this, arguments));
        }
    });

    /**
     * This is C-like sprintf
     * 
     * @param format
     *            string to format the rest of the print items
     * @param args
     *            variadic argument list
     */
    Object.defineProperty(this, "sprintf",
    {
        configurable : true,
        enumerable : false,
        writable : true,
        value : function(format, args/* , more args */)
        {
            var len = arguments.length - 1;
            var array = [];

            if (len < 0)
            {
                return "";
            }

            for (var i = 0; i < len; i++)
            {
                if (arguments[i + 1] instanceof Date)
                {
                    array[i] = arguments[i + 1].getTime();
                }
                else
                {
                    array[i] = arguments[i + 1];
                }
            }

            array = Java.to(array);
            return Packages.jdk.nashorn.api.scripting.ScriptUtils.format(format, array);
        }
    });
}).call(this, Java, java, Packages);