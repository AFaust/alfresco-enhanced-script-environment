package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ValueConverter extends org.nabucco.alfresco.enhScriptEnv.common.script.ValueConverter
{
    /**
     * Converts the provided object into a Nashorn-compatible value.
     *
     * @param value
     *            the object to convert
     * @return the converted value instance
     */
    Object convertValueForNashorn(Object value);

    /**
     * Converts the provided object into a Nashorn-compatible value.
     *
     * @param value
     *            the object to convert
     * @param expectedClass
     *            the interface / class expectation the result object has to fulfill
     * @return the converted value
     */
    <T> T convertValueForNashorn(Object value, Class<T> expectedClass);
}