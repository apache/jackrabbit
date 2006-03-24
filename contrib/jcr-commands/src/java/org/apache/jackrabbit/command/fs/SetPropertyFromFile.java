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
package org.apache.jackrabbit.command.fs;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandException;
import org.apache.jackrabbit.command.CommandHelper;
import org.apache.jackrabbit.command.core.AbstractSetProperty;

/**
 * Set a <code>Property</code> <code>Value</code> with the content of the
 * given file.
 */
public class SetPropertyFromFile extends AbstractSetProperty {
    /** logger */
    private static Log log = LogFactory.getLog(SetPropertyFromFile.class);

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String value = (String) ctx.get(this.valueKey);
        String name = (String) ctx.get(this.nameKey);
        String propertyType = (String) ctx.get(this.typeKey);
        String parent = (String) ctx.get(this.parentPathKey);

        Node node = CommandHelper.getNode(ctx, parent);

        if (log.isDebugEnabled()) {
            log.debug("setting property " + node.getPath() + "/" + name
                    + " with content of file " + value);
        }

        File f = new File(value);
        if (!f.exists()) {
            throw new CommandException("exception.file.not.found",
                new String[] {
                    value
                });
        }
        if (propertyType.equals(PropertyType.TYPENAME_BINARY)) {
            node.setProperty(name, new FileInputStream(f));
        } else {
            CharArrayWriter cw = new CharArrayWriter();
            PrintWriter out = new PrintWriter(cw);
            BufferedReader in = new BufferedReader(new FileReader(f));
            String str;
            while ((str = in.readLine()) != null) {
                out.println(str);
            }
            in.close();
            node.setProperty(name, cw.toString(), PropertyType
                .valueFromName(propertyType));
        }
        return false;
    }
}
