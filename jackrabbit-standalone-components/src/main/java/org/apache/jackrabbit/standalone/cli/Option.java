/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.standalone.cli;

/**
 * Command Line option. <br>
 * An option is a pair with the following pattern -[option name] [option value]
 */
public class Option extends AbstractParameter {

    /** argument name */
    private String argName;

    /** required */
    private boolean required = false;

    /**
     * @return true if this <code>Option</code> is required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required
     *        set required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return the argument name
     */
    public String getArgName() {
        return argName;
    }

    /**
     * @return the localized argument name
     */
    public String getLocalizedArgName() {
        return bundle.getString(this.getArgName());
    }

    /**
     * @param argName
     *        the argument name to set
     */
    public void setArgName(String argName) {
        this.argName = argName;
    }

    /**
     * {@inheritDoc}
     */
    public Object clone() {
        Option o = new Option();
        this.clone(o);
        return o;
    }

    /**
     * {@inheritDoc}
     */
    protected void clone(Option opt) {
        super.clone(opt);
        opt.argName = this.argName;
        opt.required = this.required;

    }

    /**
     * {@inheritDoc}
     */
    public String getLocalizedDescription() {
        return bundle.getString(this.getDescription());
    }
}
