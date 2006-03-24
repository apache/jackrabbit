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
package org.apache.jackrabbit.command.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.command.CommandException;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Import the xml view from the given file to the current working <code>Node</code>
 */
public class ImportXmlFromFile implements Command {

    // ---------------------------- < keys >

    /** doc view file key */
    private String srcFsPathKey = "srcFsPath";

    /** target node */
    private String destJcrPathKey = "destJcrPath";

    /** uuid behaviour key */
    private String uuidBehaviourKey = "uuidBehaviour";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String file = (String) ctx.get(this.srcFsPathKey);
        String dest = (String) ctx.get(this.destJcrPathKey);
        int uuidBehaviour = Integer.valueOf(
            (String) ctx.get(this.uuidBehaviourKey)).intValue();
        File f = new File(file);
        if (!f.exists()) {
            throw new CommandException("exception.file.not.found",
                new String[] {
                    file
                });
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        Session s = CommandHelper.getSession(ctx);
        Node n = CommandHelper.getNode(ctx, dest);
        s.importXML(n.getPath(), in, uuidBehaviour);
        return false;
    }

    /**
     * @return the from key
     */
    public String getSrcFsPathKey() {
        return srcFsPathKey;
    }

    /**
     * @param fromKey
     *        the from key to set
     */
    public void setSrcFsPathKey(String fromKey) {
        this.srcFsPathKey = fromKey;
    }

    /**
     * @return the uuidBehaviourKey
     */
    public String getUuidBehaviourKey() {
        return uuidBehaviourKey;
    }

    /**
     * @param uuidBehaviourKey
     *        the uuidBehaviourKey to set
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
     *        the destination jcr path key to set
     */
    public void setDestJcrPathKey(String destJcrPathKey) {
        this.destJcrPathKey = destJcrPathKey;
    }
}
