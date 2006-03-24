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
package org.apache.jmeter.protocol.java.control.gui;

import java.awt.BorderLayout;

import org.apache.jmeter.protocol.java.config.JavaConfig;
import org.apache.jmeter.protocol.java.config.gui.ChainConfigGui;
import org.apache.jmeter.protocol.java.sampler.ChainSampler;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * Sample GUI for Commons Chain Commands
 * 
 * @author Edgar Poce
 */
public class ChainTestSamplerGui extends AbstractSamplerGui
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256721766930788918L;
    
    /** Panel containing the configuration options. */
    private ChainConfigGui javaPanel = null;

    /**
     * Constructor for JavaTestSamplerGui
     */
    public ChainTestSamplerGui()
    {
        super();
        init();
    }

    public String getLabelResource()
    {
        return "chain_request";
    }

    /**
     * Initialize the GUI components and layout.
     */
    private void init()
    {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        add(makeTitlePanel(), BorderLayout.NORTH);

        javaPanel = new ChainConfigGui(false);

        add(javaPanel, BorderLayout.CENTER);
    }

    /* Implements JMeterGuiComponent.createTestElement() */
    public TestElement createTestElement()
    {
        ChainSampler sampler = new ChainSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    /* Implements JMeterGuiComponent.modifyTestElement(TestElement) */
    public void modifyTestElement(TestElement sampler)
    {
        sampler.clear();
        JavaConfig config = (JavaConfig) javaPanel.createTestElement();
        configureTestElement(sampler);
        sampler.addTestElement(config);
    }

    /* Overrides AbstractJMeterGuiComponent.configure(TestElement) */
    public void configure(TestElement el)
    {
        super.configure(el);
        javaPanel.configure(el);
    }
}
