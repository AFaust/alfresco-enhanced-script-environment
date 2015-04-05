package org.nabucco.alfresco.enhScriptEnv.common.script.aop;

public enum ListMethodName
{
    GET, ADD, ADDALL, SET, REMOVE, REMOVEALL, RETAINALL, INDEXOF, LASTINDEXOF, CONTAINS, CONTAINSALL, SIZE, ITERATOR, TOARRAY, SUBLIST, ISEMPTY, CLEAR, LISTITERATOR, UNKNOWN;

    protected static ListMethodName methodLiteralOf(final String methodName)
    {
        ListMethodName value = UNKNOWN;

        for (final ListMethodName literal : values())
        {
            if (literal.name().equalsIgnoreCase(methodName))
            {
                value = literal;
                break;
            }
        }

        return value;
    }
}