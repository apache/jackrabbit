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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/**
 * This Class implements an export command that generates a docview or
 * sysview of the node to be exported.
 */
public class XMLExportCommand extends AbstractExportCommand {

    /**
     * the xml content type
     */
    public static final String XML_CONTENT_TYPE = "text/xml";

    /**
     * the 'sysview' mode constant.
     */
    public static final String MODE_SYSVIEW = "sysview";

    /**
     * the 'docview' mode contant
     */
    public static final String MODE_DOCVIEW = "docview";

    /**
     * the export mode. either 'sysview' or 'docview'
     */
    private String mode = MODE_DOCVIEW;

    /**
     * Creats a XMLExportCommand
     */
    public XMLExportCommand() {
    }

    /**
     * Creates a XMLExportCommand with the given mode.
     * @param mode
     */
    public XMLExportCommand(String mode) {
        setMode(mode);
    }

    /**
     * Returns the export mode.
     * @return the export mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the export mode. This mus be either {@link #MODE_DOCVIEW} or
     * {@link #MODE_SYSVIEW}, otherwise a IllegalArgumentException is thrown.
     * @param mode the export mode
     * @throws IllegalArgumentException if the mode is not correct.
     */
    public void setMode(String mode) {
        if (MODE_DOCVIEW.equals(mode) || MODE_SYSVIEW.equals(mode)) {
            this.mode = mode;
        } else {
            throw new IllegalArgumentException("mode must be either " + MODE_DOCVIEW + " or " + MODE_SYSVIEW);
        }
    }

    /**
     * Creates a docview response for the given node.
     * @param context the export context
     * @param content the node to be exported
     * @return <code>true</code>
     * @throws Exception if an error occurrs.
     */
    public boolean exportNode(ExportContext context, Node content) throws Exception {
        // first child of content is document root
        if (content.getNodes().hasNext()) {
            content = content.getNodes().nextNode();
        }
        File tmpfile = File.createTempFile("__webdav", ".xml");
        FileOutputStream out = new FileOutputStream(tmpfile);
        if (mode.equals(MODE_DOCVIEW)) {
            content.getSession().exportDocumentView(content.getPath(), out, true, false);
        } else {
            content.getSession().exportSystemView(content.getPath(), out, true, false);
        }
        out.close();
        context.setInputStream(new FileInputStream(tmpfile));
        context.setContentLength(tmpfile.length());
        tmpfile.deleteOnExit();
        return true;
    }

    /**
     * Returns {@link #XML_CONTENT_TYPE}.
     * @return {@link #XML_CONTENT_TYPE}.
     */
    public String getDefaultContentType() {
        return XML_CONTENT_TYPE;
    }

    /**
     * Returns <code>true</code>
     * @param node
     * @return <code>true</code>
     */
    public boolean canHandle(Node node) {
        return true;
    }
}
