/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jmeter.protocol.java.sampler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestListener;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * A sampler for executing custom Commons Chain Commands
 * 
 * @author Edgar Poce
 */
public class ChainSampler extends AbstractSampler implements TestListener
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3618986663356151348L;

    /**
     * chainCtx key
     */
    public static final String CHAINS_CONTEXT = "chainCtx";

    /**
     * The Command instance used by this sampler to actually perform the sample.
     */
    private transient Command command = null;

    /**
     * Logging
     */
    private static transient Logger log = LoggingManager.getLoggerForClass();

    /**
     * Set used to register all active JavaSamplers. This is used so that the
     * samplers can be notified when the test ends.
     */
    private static Set allSamplers = new HashSet();

    /**
     * Create a JavaSampler.
     */
    public ChainSampler()
    {
        setArguments(new Arguments());
        synchronized (allSamplers)
        {
            allSamplers.add(this);
        }
    }

    /**
     * Set the arguments (parameters) for the JavaSamplerClient to be executed
     * with.
     * 
     * @param args
     *            the new arguments. These replace any existing arguments.
     */
    public void setArguments(Arguments args)
    {
        setProperty(new TestElementProperty(JavaSampler.ARGUMENTS, args));
    }

    /**
     * Get the arguments (parameters) for the JavaSamplerClient to be executed
     * with.
     * 
     * @return the arguments
     */
    public Arguments getArguments()
    {
        return (Arguments) getProperty(JavaSampler.ARGUMENTS).getObjectValue();
    }

    /**
     * Releases Command Client.
     */
    private void releaseClient()
    {
        this.command = null;
    }

    /**
     * Sets the Classname attribute of the JavaConfig object
     * 
     * @param classname
     *            the new Classname value
     */
    public void setClassname(String classname)
    {
        setProperty(JavaSampler.CLASSNAME, classname);
    }

    /**
     * Gets the Classname attribute of the JavaConfig object
     * 
     * @return the Classname value
     */
    public String getClassname()
    {
        return getPropertyAsString(JavaSampler.CLASSNAME);
    }

    /**
     * Performs a test sample.
     * 
     * The <code>sample()</code> method retrieves the reference to the command
     * and calls its <code>execute()</code> method.
     * 
     * @param entry
     *            the Entry for this sample
     * @return test SampleResult
     */
    public SampleResult sample(Entry entry)
    {
        SampleResult results = new SampleResult();
        try
        {
            if (command == null)
            {
                log.debug(whoAmI() + "Creating Command");
                createCommand();
            }

            updateCommand();
            results.setSampleLabel(this.getName());
            Context ctx = new JMeterContextAdapter(getThreadContext());
            results.sampleStart();
            command.execute(ctx);
            results.sampleEnd();
            results.setSuccessful(true);
        } catch (Exception e)
        {
            results.setSuccessful(false);
            log.error("Unable to run test", e);
        }
        return results;
    }

    /**
     * Returns reference to <code>Command</code>.
     * 
     * @return Command reference.
     */
    private Command createCommand() throws Exception
    {
        Class javaClass = Class.forName(getClassname().trim(), false, Thread
            .currentThread().getContextClassLoader());

        command = (Command) javaClass.newInstance();

        if (log.isDebugEnabled())
        {
            log.debug(whoAmI() + "\tCreated:\t" + getClassname() + "@"
                    + Integer.toHexString(command.hashCode()));
        }

        return command;
    }

    /**
     * Updates the command attributes
     * 
     * @throws Exception
     */
    private void updateCommand() throws Exception
    {
        Map descrip = BeanUtils.describe(command);
        Iterator iter = descrip.keySet().iterator();
        while (iter.hasNext())
        {
            String key = (String) iter.next();
            Object value = this.getArguments().getArgumentsAsMap().get(key);
            if (value != null && value.toString().length() > 0)
            {
                BeanUtils.setProperty(command, key, value);
            }
        }
    }

    /**
     * Retrieves reference to JavaSamplerClient.
     * 
     * Convience method used to check for null reference without actually
     * creating a JavaSamplerClient
     * 
     * @return reference to JavaSamplerClient NOTUSED private JavaSamplerClient
     *         retrieveJavaClient() { return javaClient; }
     */

    /**
     * Generate a String identifier of this instance for debugging purposes.
     * 
     * @return a String identifier for this sampler instance
     */
    private String whoAmI()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(Thread.currentThread().getName());
        sb.append("@");
        sb.append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    // TestListener implementation
    /* Implements TestListener.testStarted() */
    public void testStarted()
    {
        log.debug(whoAmI() + "\ttestStarted");
    }

    /* Implements TestListener.testStarted(String) */
    public void testStarted(String host)
    {
        log.debug(whoAmI() + "\ttestStarted(" + host + ")");
    }

    /**
     * Method called at the end of the test. This is called only on one instance
     * of ChainSampler. This method will loop through all of the other samplers
     * which have been registered (automatically in the constructor) and notify
     * them that the test has ended, allowing the ChainSamplerClients to
     * cleanup.
     */
    public void testEnded()
    {
        log.debug(whoAmI() + "\ttestEnded");
        synchronized (allSamplers)
        {
            Iterator i = allSamplers.iterator();
            while (i.hasNext())
            {
                ChainSampler sampler = (ChainSampler) i.next();
                sampler.releaseClient();
                i.remove();
            }
        }
    }

    /* Implements TestListener.testEnded(String) */
    public void testEnded(String host)
    {
        testEnded();
    }

    /* Implements TestListener.testIterationStart(LoopIterationEvent) */
    public void testIterationStart(LoopIterationEvent event)
    {
    }
}
