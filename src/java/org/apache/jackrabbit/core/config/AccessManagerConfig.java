/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.config;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jdom.Element;

/**
 * A <code>AccessManagerConfig</code> represents the configuration of an
 * <code>AccessManager</code>.
 *
 * @see RepositoryConfig#getAccessManagerConfig()
 */
public class AccessManagerConfig extends BeanConfig {

    private static final String CLASS_ATTRIB = "class";

    /**
     * Creates a new <code>PersistenceManagerConfig</code>.
     *
     * @param config the config root element for this <code>PersistenceManagerConfig</code>.
     * @param vars   map of variable values.
     */
    static AccessManagerConfig parse(Element config, Map vars) {
        // FQN of persistence manager class
        String className = config.getAttributeValue(CLASS_ATTRIB);

        // read the PersistenceManager properties from the
        // <param/> elements in the config
        Properties params = new Properties();
        List paramList = config.getChildren(AbstractConfig.PARAM_ELEMENT);
        for (Iterator i = paramList.iterator(); i.hasNext();) {
            Element param = (Element) i.next();
            String paramName = param.getAttributeValue(AbstractConfig.NAME_ATTRIB);
            String paramValue = param.getAttributeValue(AbstractConfig.VALUE_ATTRIB);
            // replace variables in param value
            params.put(paramName, AbstractConfig.replaceVars(paramValue, vars));
        }
        
        return new AccessManagerConfig(className, params);
    }
    
    public AccessManagerConfig(String className, Properties properties) {
        super(className, properties);
    }

}
