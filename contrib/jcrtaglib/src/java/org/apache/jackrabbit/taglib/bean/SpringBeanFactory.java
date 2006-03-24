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
 */
package org.apache.jackrabbit.taglib.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * BeanFactory backed by the Spring Framework
 */
public class SpringBeanFactory implements BeanFactory
{
    /** Logger */
    private static Log log = LogFactory.getLog(SpringBeanFactory.class);

    /** Bean registration */
    private String config = "jcrtaglib-beans.xml";

    /** Spring Factory instance */
    org.springframework.beans.factory.BeanFactory factory;

    /**
     * 
     */
    public SpringBeanFactory()
    {
        super();
    }

    /**
     * Init the factory
     */
    private void init()
    {
        ClassPathResource res = new ClassPathResource(this.config);
        if (!res.exists())
        {
            log.error("Unable to init Spring bean Factory. Config file not found at "
                    + res.getFilename() + ".");
            return;
        }
        factory = new XmlBeanFactory(res);
    }

    /**
     * @inheritDoc
     */
    public Object getBean(String id)
    {
        if (factory == null)
        {
            init();
        }
        return factory.getBean(id);
    }

    /**
     * Sets the spring bean config file 
     * @param config
     */
    public void setConfig(String config)
    {
        this.config = config;
    }
}
