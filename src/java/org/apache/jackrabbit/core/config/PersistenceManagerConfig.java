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

import org.jdom.Element;

import java.util.*;

/**
 * A <code>PersistenceManagerConfig</code> represents the configuration of a
 * <code>PersistenceManager</code>.
 *
 * @see WorkspaceConfig#getPersistenceManagerConfig()
 */
public class PersistenceManagerConfig {

    private static final String CLASS_ATTRIB = "class";

    /**
     * FQN of class implementing the <code>PersistenceManager</code> interface
     */
    private final String className;

    /**
     * Parameters for configuring the persistence manager.
     */
    private final Map params;

    /**
     * Creates a new <code>PersistenceManagerConfig</code>.
     *
     * @param config the config root element for this <code>PersistenceManagerConfig</code>.
     * @param vars   map of variable values.
     */
    PersistenceManagerConfig(Element config, Map vars) {
        // FQN of persistence manager class
        className = config.getAttributeValue(CLASS_ATTRIB);

        // read the PersistenceManager properties from the
        // <param/> elements in the config
        Map params = new HashMap();
        List paramList = config.getChildren(AbstractConfig.PARAM_ELEMENT);
        for (Iterator i = paramList.iterator(); i.hasNext();) {
            Element param = (Element) i.next();
            String paramName = param.getAttributeValue(AbstractConfig.NAME_ATTRIB);
            String paramValue = param.getAttributeValue(AbstractConfig.VALUE_ATTRIB);
            // replace variables in param value
            params.put(paramName, AbstractConfig.replaceVars(paramValue, vars));
        }
        this.params = Collections.unmodifiableMap(params);
    }

    /**
     * Returns configuration parameters. Each entry in the map represents
     * a name/value pair where both name and value are <code>String</code>s.
     *
     * @return Map of configuration parameters.
     */
    public Map getParameters() {
        return params;
    }

    /**
     * Returns the FQN of a class implementing the <code>PersistenceManager</code> interface
     *
     * @return FQN of persistence manager class
     */
    public String getClassName() {
        return className;
    }

}
