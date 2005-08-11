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
package org.apache.jackrabbit.chain.command.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Imports the xml view from the given file to the current working node. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class ImportXmlFromFile implements Command
{

    // ---------------------------- < literals >

    /** doc view file */
    private String from;

    /** uuid behaviour */
    private String uuidBehaviour;

    // ---------------------------- < keys >

    /** doc view file key */
    private String fromKey;

    /** uuid behaviour key */
    private String uuidBehaviourKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String file = CtxHelper.getAttr(this.from, this.fromKey, ctx);

        int uuidBehaviour = CtxHelper.getIntAttr(this.uuidBehaviour,
            this.uuidBehaviourKey, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
            ctx);

        File f = new File(file);

        if (!f.exists())
        {
            throw new JcrCommandException("file.not.found", new String[]
            {
                file
            });
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

        Session s = CtxHelper.getSession(ctx);
        Node n = CtxHelper.getCurrentNode(ctx);
        s.importXML(n.getPath(), in, uuidBehaviour);
        return false;
    }

    /**
     * @return Returns the from.
     */
    public String getFrom()
    {
        return from;
    }

    /**
     * @param from
     *            The from to set.
     */
    public void setFrom(String from)
    {
        this.from = from;
    }

    /**
     * @return Returns the fromKey.
     */
    public String getFromKey()
    {
        return fromKey;
    }

    /**
     * @param fromKey
     *            Set the context attribute key for the from attribute.
     */
    public void setFromKey(String fromKey)
    {
        this.fromKey = fromKey;
    }

    /**
     * @return Returns the uuidBehaviour.
     */
    public String getUuidBehaviour()
    {
        return uuidBehaviour;
    }

    /**
     * @param uuidBehaviour
     *            The uuidBehaviour to set.
     */
    public void setUuidBehaviour(String uuidBehaviour)
    {
        this.uuidBehaviour = uuidBehaviour;
    }

    /**
     * @return Returns the uuidBehaviourKey.
     */
    public String getUuidBehaviourKey()
    {
        return uuidBehaviourKey;
    }

    /**
     * @param uuidBehaviourKey
     *            Set the context attribute key for the uuidBehaviour attribute.
     */
    public void setUuidBehaviourKey(String uuidBehaviourKey)
    {
        this.uuidBehaviourKey = uuidBehaviourKey;
    }
}
