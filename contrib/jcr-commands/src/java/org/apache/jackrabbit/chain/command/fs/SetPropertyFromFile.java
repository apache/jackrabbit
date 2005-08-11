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
package org.apache.jackrabbit.chain.command.fs;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;
import org.apache.jackrabbit.chain.command.AbstractSetProperty;

/**
 * Set a property value with the contents of the given files. The PropertyType
 * may be specified.
 */
public class SetPropertyFromFile extends AbstractSetProperty
{

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
     */
    public boolean execute(Context ctx) throws Exception
    {
        String value = CtxHelper.getAttr(this.value, this.valueKey, ctx);

        String name = CtxHelper.getAttr(this.propertyName,
            this.propertyNameKey, ctx);

        String propertyType = CtxHelper.getAttr(this.propertyType,
            this.propertyTypeKey, PropertyType.TYPENAME_STRING, ctx);

        File f = new File(value);
        if (!f.exists())
        {
            throw new JcrCommandException("file.not.found", new String[]
            {
                value
            });
        }
        Node node = CtxHelper.getCurrentNode(ctx);

        if (propertyType.equals(PropertyType.TYPENAME_BINARY))
        {
            node.setProperty(name, new FileInputStream(f));
        } else
        {
            CharArrayWriter cw = new CharArrayWriter();
            PrintWriter out = new PrintWriter(cw);
            BufferedReader in = new BufferedReader(new FileReader(f));
            String str;
            while ((str = in.readLine()) != null)
            {
                out.println(str);
            }
            in.close();
            node.setProperty(name, cw.toString(), PropertyType
                .valueFromName(propertyType));
        }
        return false;
    }

}
