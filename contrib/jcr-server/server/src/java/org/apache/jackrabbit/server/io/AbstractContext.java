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
package org.apache.jackrabbit.server.io;

import org.apache.commons.chain.Context;

import java.util.Properties;

/**
 */
public class AbstractContext extends Properties implements Context {

    /**
     * Creates a new AbstractContext that used the given properties as default
     * @param defaults
     */
    public AbstractContext(Properties defaults) {
        super(defaults);
    }

    /**
     * Returns the value of the property or <code>def</code> if the property
     * does not exist.
     * @param name the name of the property
     * @param def the default value to return if the property does not exist.
     * @return the value of the property or <code>def</code>
     */
    public String getProperty(String name, String def) {
        String val = getProperty(name);
        return val == null ? def : val;
    }

    /**
     * Returns the value of the property or <code>def</code> if the property
     * does not exist.
     * @param name the name of the property
     * @param def the default value to return if the property does not exist.
     * @return the value of the property or <code>def</code>
     */
    public boolean getProperty(String name, boolean def) {
        String val = getProperty(name);
        return val == null ? def : Boolean.valueOf(getProperty(name)).booleanValue();
    }

    /**
     * Enables or disables a command by setting the &lt;id>.enabled property
     * to <code>enabled</code>
     *
     * @param id
     * @param enable
     */
    public void enableCommand(String id, boolean enable) {
        setProperty(id + ".enabled", Boolean.toString(enable));
    }

    /**
     * Enables or disables a command by setting the &lt;id>.enabled property
     * to <code>enabled</code>
     *
     * @param id
     * @param enable
     */
    public void enableCommand(String id, String enable) {
        setProperty(id + ".enabled", enable);
    }

    /**
     * Checks if this command is enabled. if the respective property does not
     * exist, the value of <code>def</code> is returned.
     *
     * @param id
     * @param def
     * @return
     */
    public boolean isCommandEnabled(String id, boolean def) {
        return getProperty(id + ".enabled", def);
    }

}
