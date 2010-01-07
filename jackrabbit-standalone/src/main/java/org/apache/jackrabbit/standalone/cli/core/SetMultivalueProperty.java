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
package org.apache.jackrabbit.standalone.cli.core;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Set a multivalue <code>Property</code> to the current working
 * <code>Node</code>.<br>
 * The default regular expression is ",".
 */
public class SetMultivalueProperty extends AbstractSetProperty {
    /** logger */
    private static Log log = LogFactory.getLog(SetMultivalueProperty.class);

    // ---------------------------- < keys >

    /** regular expression key */
    private String regExpKey = "regExp";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String regExp = (String) ctx.get(this.regExpKey);
        String value = (String) ctx.get(this.valueKey);
        String name = (String) ctx.get(this.nameKey);
        String type = (String) ctx.get(this.typeKey);
        String parent = (String) ctx.get(this.parentPathKey);

        Node node = CommandHelper.getNode(ctx, parent);

        if (log.isDebugEnabled()) {
            log.debug("setting multivalue property from node at "
                    + node.getPath() + ". regexp=" + regExp + " value=" + value
                    + " property=" + name);
        }

        String[] values = value.split(regExp);

        if (type == null) {
            node.setProperty(name, values);
        } else {
            node.setProperty(name, values, PropertyType.valueFromName(type));
        }

        return false;
    }

    /**
     * @return the regular expression key
     */
    public String getRegExpKey() {
        return regExpKey;
    }

    /**
     * @param regExpKey
     *        the regular expression key to set
     */
    public void setRegExpKey(String regExpKey) {
        this.regExpKey = regExpKey;
    }
}
