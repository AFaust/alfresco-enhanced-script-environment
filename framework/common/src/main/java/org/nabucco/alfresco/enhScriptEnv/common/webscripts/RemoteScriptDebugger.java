/*
 * Copyright 2014 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.common.webscripts;

import java.text.MessageFormat;
import java.util.List;

import org.alfresco.util.PropertyCheck;
import org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger;
import org.mozilla.javascript.ContextFactory;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.WebScriptException;

public class RemoteScriptDebugger implements InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteScriptDebugger.class);

    private static final int DEFAULT_PORT = 9000;
    private static final String DEFAULT_TRANSPORT = "socket";

    protected boolean startOnStartup = false;
    protected boolean suspend = false;
    protected boolean trace = false;
    protected int port = DEFAULT_PORT;
    protected String transport = DEFAULT_TRANSPORT;

    protected List<EnhancedScriptProcessor<?>> scriptProcessors;
    protected RhinoDebugger remoteDebugger;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptProcessors", this.scriptProcessors);
    }

    public boolean isActive()
    {
        final boolean result = this.remoteDebugger != null;

        return result;
    }

    public synchronized void activate()
    {
        if (!this.isActive())
        {
            // setup debugger based on configuration
            final String configString = MessageFormat.format("transport={0},suspend={1},address={2},trace={3}", new Object[] {
                    this.transport, this.suspend ? "y" : "n", String.valueOf(this.port), this.trace ? "y" : "n" });
            this.remoteDebugger = new RhinoDebugger(configString);
            ContextFactory.getGlobal().addListener(this.remoteDebugger);

            for (final EnhancedScriptProcessor<?> scriptProcessor : this.scriptProcessors)
            {
                scriptProcessor.debuggerAttached();
            }

            try
            {
                this.remoteDebugger.start();
            }
            catch (final Exception ex)
            {
                LOGGER.error("Error starting remote debugger agent", ex);
                this.shutdown();
                throw new WebScriptException("Error starting remote debugger agent", ex);
            }
        }
    }

    public synchronized void shutdown()
    {
        if (this.isActive())
        {
            try
            {
                this.remoteDebugger.stop();
            }
            catch (final Exception ex)
            {
                LOGGER.error("Error stopping remote debugger agent", ex);
                throw new WebScriptException("Error stopping remote debugger agent", ex);
            }

            ContextFactory.getGlobal().removeListener(this.remoteDebugger);
            this.remoteDebugger = null;

            for (final EnhancedScriptProcessor<?> scriptProcessor : this.scriptProcessors)
            {
                scriptProcessor.debuggerDetached();
            }
        }
    }

    /**
     * @param startOnStartup
     *            the startOnStartup to set
     */
    public final void setStartOnStartup(final boolean startOnStartup)
    {
        this.startOnStartup = startOnStartup;
    }

    /**
     * @param suspend
     *            the suspend to set
     */
    public final void setSuspend(final boolean suspend)
    {
        this.suspend = suspend;
    }

    /**
     * @param trace
     *            the trace to set
     */
    public final void setTrace(final boolean trace)
    {
        this.trace = trace;
    }

    /**
     * @param port
     *            the port to set
     */
    public final void setPort(final int port)
    {
        this.port = port;
    }

    /**
     * @param transport
     *            the transport to set
     */
    public final void setTransport(final String transport)
    {
        this.transport = transport;
    }

    /**
     * @param scriptProcessors
     *            the scriptProcessors to set
     */
    public final void setScriptProcessors(final List<EnhancedScriptProcessor<?>> scriptProcessors)
    {
        this.scriptProcessors = scriptProcessors;
    }

}
