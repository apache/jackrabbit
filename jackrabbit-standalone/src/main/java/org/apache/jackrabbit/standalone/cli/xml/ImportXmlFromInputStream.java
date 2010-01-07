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
package org.apache.jackrabbit.standalone.cli.xml;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Import the xml view to the target <code>Node</code>
 */
public class ImportXmlFromInputStream implements Command {

    // ---------------------------- < keys >

    /** doc view file key */
    private String inputStreamKey = "inputStream";

    /** flag that indicates whether to use the transient space or not */
    private String persistentKey = "persistent";

    /** target node */
    private String destJcrPathKey = "destJcrPath";

    /** uuid behaviour key */
    private String uuidBehaviourKey = "uuidBehaviour";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        InputStream is = (InputStream) ctx.get(this.inputStreamKey);
        String dest = (String) ctx.get(this.destJcrPathKey);
        String persistent = (String) ctx.get(this.persistentKey);

        int uuidBehaviour = Integer.valueOf(
                (String) ctx.get(this.uuidBehaviourKey)).intValue();

        BufferedInputStream bis = new BufferedInputStream(is);
        Session s = CommandHelper.getSession(ctx);
        Node n = CommandHelper.getNode(ctx, dest);

        if (persistent != null
                && Boolean.valueOf(persistent).equals(Boolean.TRUE)) {
            s.getWorkspace().importXML(n.getPath(), bis, uuidBehaviour);
        } else {
            s.importXML(n.getPath(), bis, uuidBehaviour);
        }

        return false;
    }

    /**
     * @return the uuidBehaviourKey
     */
    public String getUuidBehaviourKey() {
        return uuidBehaviourKey;
    }

    /**
     * @param uuidBehaviourKey
     *            the uuidBehaviourKey to set
     */
    public void setUuidBehaviourKey(String uuidBehaviourKey) {
        this.uuidBehaviourKey = uuidBehaviourKey;
    }

    /**
     * @return the destination jcr path key
     */
    public String getDestJcrPathKey() {
        return destJcrPathKey;
    }

    /**
     * @param destJcrPathKey
     *            the destination jcr path key to set
     */
    public void setDestJcrPathKey(String destJcrPathKey) {
        this.destJcrPathKey = destJcrPathKey;
    }

    /**
     * @return the inputStreamKey
     */
    public String getInputStreamKey() {
        return inputStreamKey;
    }

    /**
     * @param inputStreamKey
     *            the inputStreamKey to set
     */
    public void setInputStreamKey(String inputStreamKey) {
        this.inputStreamKey = inputStreamKey;
    }

    /**
     * @return the persistentKey
     */
    public String getPersistentKey() {
        return persistentKey;
    }

    /**
     * @param persistentKey
     *            the persistentKey to set
     */
    public void setPersistentKey(String persistentKey) {
        this.persistentKey = persistentKey;
    }
}
