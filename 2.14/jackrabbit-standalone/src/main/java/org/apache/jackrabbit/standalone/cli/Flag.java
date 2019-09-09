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
 * A command line flag <br>
 * A flag is a parameter that has no other value that the option name. e.g.
 * -[flag name].
 */
public class Flag extends AbstractParameter {
    /** true if flag is present in the user's input */
    private boolean present = false;

    /**
     * @return true if the flag exists in the user's input
     */
    public boolean isPresent() {
        return present;
    }

    /**
     * @param present
     *        the present to set
     */
    public void setPresent(boolean present) {
        this.present = present;
    }

    /**
     * {@inheritDoc}
     */
    public String getValue() {
        return Boolean.toString(present);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String value) {
        present = Boolean.getBoolean(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object clone() {
        Flag f = new Flag();
        f.present = this.present;
        this.clone(f);
        return f;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRequired() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalizedArgName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalizedDescription() {
        if (this.getDescription() == null) {
            return bundle.getString("param.flag." + this.getName() + ".desc");
        } else {
            return bundle.getString(this.getDescription());
        }
    }
}
