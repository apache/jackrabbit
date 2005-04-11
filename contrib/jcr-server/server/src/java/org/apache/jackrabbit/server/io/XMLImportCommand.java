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

import javax.jcr.Node;
import java.io.InputStream;
import java.util.Calendar;

/**
 * This Class implements an import command that deserializes the xml contained
 * in the import stream, using {@link javax.jcr.Session#importXML(String, java.io.InputStream)}.
 * It further sets the following properties:
 * <ul>
 * <li>jcr:mimeType (from {@link ImportContext#getContentType()})
 * <li>jcr:lastModified (from current time)
 * </ul>
 */
public class XMLImportCommand extends AbstractImportCommand {

    /**
     * the xml content type
     */
    public static final String XML_CONTENT_TYPE = "text/xml";

    /**
     * Imports the resource by deseriaizing the xml.
     * @param ctx
     * @param parentNode
     * @param in
     * @return
     * @throws Exception
     */
    public boolean importResource(ImportContext ctx, Node parentNode,
                                  InputStream in)
            throws Exception {
        Node content = parentNode.hasNode(JCR_CONTENT)
                ? parentNode.getNode(JCR_CONTENT)
                : parentNode.addNode(JCR_CONTENT, NT_UNSTRUCTURED);
        content.setProperty(JCR_MIMETYPE, ctx.getContentType());
        Calendar lastMod = Calendar.getInstance();
        if (ctx.getModificationTime() != 0) {
            lastMod.setTimeInMillis(ctx.getModificationTime());
        }
        content.setProperty(JCR_LASTMODIFIED, lastMod);
        parentNode.getSession().importXML(content.getPath(), in);
        return true;
    }

    /**
     * Returns <code>true</code> if the given content type is equal to
     * {@link #XML_CONTENT_TYPE}.
     * @param contentType the content type to check.
     * @return <code>true</code> if equal to {@link #XML_CONTENT_TYPE}.
     */
    public boolean canHandle(String contentType) {
        return XML_CONTENT_TYPE.equals(contentType);
    }
}
