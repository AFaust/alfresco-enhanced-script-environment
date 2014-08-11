/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.common.script.functions;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractLogFunction implements InitializingBean, ScopeContributor
{

    protected static class LoggerData
    {
        private Collection<Logger> loggers;
        private String explicitLogger;
        private boolean inheritLoggerContext = true;
        private Object scriptLogger;

        public LoggerData()
        {
            // NO-OP
        }

        /**
         * @return the loggers
         */
        public final Collection<Logger> getLoggers()
        {
            return this.loggers;
        }

        /**
         * @param loggers
         *            the loggers to set
         */
        public final void setLoggers(final Collection<Logger> loggers)
        {
            this.loggers = loggers;
        }

        /**
         * @return the explicitLogger
         */
        public final String getExplicitLogger()
        {
            return this.explicitLogger;
        }

        /**
         * @param explicitLogger
         *            the explicitLogger to set
         */
        public final void setExplicitLogger(final String explicitLogger)
        {
            this.explicitLogger = explicitLogger;
        }

        /**
         * @return the inheritLoggerContext
         */
        public final boolean isInheritLoggerContext()
        {
            return this.inheritLoggerContext;
        }

        /**
         * @param inheritLoggerContext
         *            the inheritLoggerContext to set
         */
        public final void setInheritLoggerContext(final boolean inheritLoggerContext)
        {
            this.inheritLoggerContext = inheritLoggerContext;
        }

        /**
         * @return the scriptLogger
         */
        public final Object getScriptLogger()
        {
            return this.scriptLogger;
        }

        /**
         * @param scriptLogger
         *            the legacyLogger to set
         */
        public final void setScriptLogger(final Object scriptLogger)
        {
            this.scriptLogger = scriptLogger;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLogFunction.class);

    private static final Object DUMMY_SCOPE = new Object();

    /**
     * Logger data map for state management and logger caching
     */
    protected final Map<Object, Map<ReferenceScript, LoggerData>> scopeLoggerData = new WeakHashMap<Object, Map<ReferenceScript, LoggerData>>();

    /**
     * Parent scope registry for bottom-up lookup and including context script in which parent-child relation was registered
     */
    protected final Map<Object, Pair<WeakReference<Object>, ReferenceScript>> scopeParents = new WeakHashMap<Object, Pair<WeakReference<Object>, ReferenceScript>>();

    protected final ReadWriteLock scopeLoggerDataLock = new ReentrantReadWriteLock(true);

    protected final ReadWriteLock scopeParentLock = new ReentrantReadWriteLock(true);

    protected EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor;

    protected ValueConverter valueConverter;

    protected String defaultLoggerPrefix;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
        PropertyCheck.mandatory(this, "defaultLoggerPrefix", this.defaultLoggerPrefix);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     * @param defaultLoggerPrefix
     *            the defaultLoggerPrefix to set
     */
    public final void setDefaultLoggerPrefix(final String defaultLoggerPrefix)
    {
        this.defaultLoggerPrefix = defaultLoggerPrefix;
    }

    /**
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    protected Logger getLogger()
    {
        return LOGGER;
    }

    protected void handleOut(final String message)
    {
        final ReferenceScript contextScriptLocation = this.scriptProcessor.getContextScriptLocation();
        this.getLogger().warn("Script {} logging to System.out: {}", contextScriptLocation, message);
        System.out.println(message);
    }

    protected void handleRegisterChildScope(final Object parentScope, final Object childScope)
    {
        ParameterCheck.mandatory("parentScope", parentScope);
        ParameterCheck.mandatory("childScope", childScope);

        final ReferenceScript script = this.scriptProcessor.getContextScriptLocation();
        this.scopeParentLock.writeLock().lock();
        try
        {
            this.scopeParents.put(childScope, new Pair<WeakReference<Object>, ReferenceScript>(new WeakReference<Object>(parentScope),
                    script));
        }
        finally
        {
            this.scopeParentLock.writeLock().unlock();
        }
    }

    protected void handleSetLoggerInheritance(final Object scope, final boolean inheritLoggerContext)
    {
        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();
        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, true);
        loggerData.setInheritLoggerContext(inheritLoggerContext);
    }

    protected void handleSetLogger(final Object scope, final String explicitLoggerName)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatoryString("explicitLoggerName", explicitLoggerName);

        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();
        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, true);

        if (loggerData.getLoggers() == null && loggerData.getExplicitLogger() == null)
        {
            loggerData.setExplicitLogger(explicitLoggerName);
        }
        else
        {
            if (loggerData.getExplicitLogger() != null)
            {
                throw new IllegalStateException("Explicit logger already set");
            }
            else if (loggerData.getLoggers() != null)
            {
                throw new IllegalStateException("Loggers already initialized");
            }
        }
    }

    protected Object getScriptLogger()
    {
        final ReferenceScript topLevelScript = this.scriptProcessor.getScriptCallChain().get(0);
        final LoggerData globalLoggerData = this.getLoggerData(DUMMY_SCOPE, topLevelScript, false);
        final Object scriptLogger = globalLoggerData != null ? globalLoggerData.getScriptLogger() : null;
        return scriptLogger;
    }

    protected void handleSetScriptLogger(final Object scriptLogger)
    {
        ParameterCheck.mandatory("scriptLogger", scriptLogger);

        // ScriptLogger is always treated as a top-level context data object, so retrieve the top-level logger data
        final ReferenceScript topLevelScript = this.scriptProcessor.getScriptCallChain().get(0);
        final LoggerData loggerData = this.getLoggerData(DUMMY_SCOPE, topLevelScript, true);
        final Object setScriptLogger = loggerData.getScriptLogger();
        if (setScriptLogger != scriptLogger && setScriptLogger != null)
        {
            if (!this.isUnreplaceableScriptLogger(setScriptLogger))
            {
                throw new IllegalStateException("ScriptLogger has already been set - not allowed to replace it once set");
            }
        }
        loggerData.setScriptLogger(scriptLogger);
    }

    /**
     * Checks if the provided script logger is a script logger that may not be replaced with a custom script logger. This is to ensure that
     * only the default Alfresco / Surf script logger instances may be replaced by a custom script logger, but any request to replace an
     * already set custom script logger fails.
     *
     * @param scriptLogger
     *            the script logger to test
     * @return {@code true} if the script logger may not be replaced, {@code false} otherwise
     */
    abstract protected boolean isUnreplaceableScriptLogger(Object scriptLogger);

    protected Collection<Logger> getParentLoggers(final Object scope, final ReferenceScript script)
    {
        // determine parent script from call chain
        final List<? extends ReferenceScript> scriptCallChain = this.scriptProcessor.getScriptCallChain();
        final int scriptIndex = scriptCallChain.indexOf(script);
        final ReferenceScript parentScript = scriptIndex > 0 ? scriptCallChain.get(scriptIndex - 1) : null;

        // determine parent scope from explicit registration
        final Pair<WeakReference<Object>, ReferenceScript> scopeParentPair;

        this.scopeParentLock.readLock().lock();
        try
        {
            scopeParentPair = this.scopeParents.get(scope);
        }
        finally
        {
            this.scopeParentLock.readLock().unlock();
        }
        // use parent scope only if one has been registered and the script it was registered for is the identical script retrieved from the
        // call chain
        final Object parentScope = scopeParentPair == null || (scopeParentPair.getSecond() != parentScript) ? scope : scopeParentPair
                .getFirst().get();

        final LoggerData parentLoggerData = parentScope != null && parentScript != null ? this.getLoggerData(parentScope, parentScript,
                false) : null;
        final Collection<Logger> loggers;
        if (parentLoggerData == null || parentLoggerData.isInheritLoggerContext())
        {
            loggers = this.getLoggers(parentScope, parentScript, parentLoggerData);
        }
        else
        {
            loggers = Collections.emptySet();
        }

        return loggers;
    }

    protected Collection<Logger> getLoggers(final Object scope, final boolean createIfNull)
    {
        final Collection<Logger> loggers;

        // TODO: what about logging in functions defined in script A but called from script B (due to scope access or explicit passing)?
        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();

        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, createIfNull);
        loggers = this.getLoggers(scope, referenceScript, loggerData);

        return loggers;
    }

    protected Collection<Logger> getLoggers(final Object scope, final ReferenceScript referenceScript, final LoggerData loggerData)
    {
        final Collection<Logger> loggers;
        if (loggerData == null || loggerData.getLoggers() == null)
        {
            loggers = new HashSet<Logger>();
            if (referenceScript != null)
            {
                loggers.addAll(this.getParentLoggers(scope, referenceScript));
            }

            if (loggerData != null && loggerData.getExplicitLogger() != null)
            {
                loggers.add(LoggerFactory.getLogger(loggerData.getExplicitLogger()));
            }
            else if (!this.isParentLoggerExplicit(scope, referenceScript))
            {
                // Note: We previously included the legacy script logger explicitly here, but this is now handled separately

                if (referenceScript != null)
                {
                    final Collection<ReferencePathType> supportedReferencePathTypes = referenceScript.getSupportedReferencePathTypes();
                    for (final ReferencePathType referencePathType : supportedReferencePathTypes)
                    {
                        final String referencePath = referenceScript.getReferencePath(referencePathType);
                        if (referencePath != null)
                        {
                            final String loggerSuffix = referencePath.replace('.', '_').replace('/', '.');
                            final String loggerName = MessageFormat.format("{0}.{1}.{2}", this.defaultLoggerPrefix, referencePathType,
                                    loggerSuffix);
                            loggers.add(LoggerFactory.getLogger(loggerName));
                        }
                    }
                }
            }

            if (loggerData != null)
            {
                loggerData.setLoggers(loggers);
            }
        }
        else
        {
            loggers = loggerData.getLoggers();
        }
        return loggers;
    }

    protected boolean isParentLoggerExplicit(final Object scope, final ReferenceScript script)
    {
        // determine parent script from call chain
        final List<? extends ReferenceScript> scriptCallChain = this.scriptProcessor.getScriptCallChain();
        final int scriptIndex = scriptCallChain.indexOf(script);
        final ReferenceScript parentScript = scriptIndex > 0 ? scriptCallChain.get(scriptIndex - 1) : null;

        final boolean result;
        if (parentScript != null)
        {
            // determine parent scope from explicit registration
            final Pair<WeakReference<Object>, ReferenceScript> scopeParentPair;

            this.scopeParentLock.readLock().lock();
            try
            {
                scopeParentPair = this.scopeParents.get(scope);
            }
            finally
            {
                this.scopeParentLock.readLock().unlock();
            }

            // use parent scope only if one has been registered and the script it was registered for is the identical script retrieved from
            // the call chain
            final Object parentScope = scopeParentPair == null || (scopeParentPair.getSecond() != parentScript) ? scope : scopeParentPair
                    .getFirst().get();

            final LoggerData parentLoggerData = parentScope != null ? this.getLoggerData(parentScope, parentScript, false) : null;

            // check immediate parent
            final boolean nextParentLoggerIsExplicit = parentLoggerData != null && parentLoggerData.getExplicitLogger() != null
                    && parentLoggerData.isInheritLoggerContext();
            // recursive check unless inheritance is off
            final boolean ancestorLoggerIsExplicit = (parentLoggerData == null || parentLoggerData.isInheritLoggerContext())
                    && this.isParentLoggerExplicit(parentScope, parentScript);
            result = nextParentLoggerIsExplicit || ancestorLoggerIsExplicit;
        }
        else
        {
            result = false;
        }
        return result;
    }

    protected LoggerData getLoggerData(final Object scope, final ReferenceScript script, final boolean createIfNull)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatory("script", script);

        LoggerData loggerData = null;

        final Map<ReferenceScript, LoggerData> loggerDataByScript = this.getScriptLoggerDataForContext(scope, createIfNull);
        loggerData = loggerDataByScript != null ? loggerDataByScript.get(script) : null;
        if (loggerDataByScript != null && loggerData == null && createIfNull)
        {
            loggerData = new LoggerData();
            loggerDataByScript.put(script, loggerData);
        }

        if (loggerData == null)
        {
            // determine parent scope from explicit registration
            final Pair<WeakReference<Object>, ReferenceScript> scopeParentPair;

            this.scopeParentLock.readLock().lock();
            try
            {
                scopeParentPair = this.scopeParents.get(scope);
            }
            finally
            {
                this.scopeParentLock.readLock().unlock();
            }
            if (scopeParentPair != null)
            {
                final Object parentScope = scopeParentPair.getFirst().get();
                if (parentScope != null)
                {
                    loggerData = this.getLoggerData(parentScope, script, createIfNull);
                }
            }
        }
        return loggerData;
    }

    protected Map<ReferenceScript, LoggerData> getScriptLoggerDataForContext(final Object scope, final boolean createIfNull)
    {
        Map<ReferenceScript, LoggerData> dataByScript = null;
        this.scopeLoggerDataLock.readLock().lock();
        try
        {
            dataByScript = this.scopeLoggerData.get(scope);
        }
        finally
        {
            this.scopeLoggerDataLock.readLock().unlock();
        }

        if (dataByScript == null && createIfNull)
        {
            dataByScript = new IdentityHashMap<ReferenceScript, RhinoLogFunction.LoggerData>();
            this.scopeLoggerDataLock.writeLock().lock();
            try
            {
                this.scopeLoggerData.put(scope, dataByScript);
            }
            finally
            {
                this.scopeLoggerDataLock.writeLock().unlock();
            }
        }

        return dataByScript;
    }
}
