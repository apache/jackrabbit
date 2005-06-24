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

import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Calendar;

/**
 * This Class implements an import command that deserializes the xml contained
 * in the import stream, using {@link javax.jcr.Session#importXML(String, java.io.InputStream, int)}.
 * It further sets the following properties:
 * <ul>
 * <li>jcr:mimeType (from {@link ImportContext#getContentType()})
 * <li>jcr:lastModified (from current time)
 * </ul>
 */
public class XMLImportCommand extends AbstractCommand {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(XMLImportCommand.class);

    /**
     * the xml content type
     */
    public static final String XML_CONTENT_TYPE = "text/xml";

    /**
     * the node type for the jcr:content node
     */
    private String contentNodeType = NT_UNSTRUCTURED;

    /**
     * the nodetype for the node
     */
    private String nodeType = "nt:file";

    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It checks if this command can handle the content
     * type and delegates it to {@link #importResource}. If the import is
     * successfull, the input stream of the importcontext is cleared.
     *
     * @param context the import context
     * @return false
     * @throws Exception if an error occurrs
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        InputStream in = context.getInputStream();
        if (in == null) {
            // assume already consumed
            return false;
        }
        if (!canHandle(context.getContentType())) {
            // ignore imports
            return false;
        }

        // we need a tmp file, since the import could fail
        File tmpFile = File.createTempFile("__xmlimport", "xml");
        FileOutputStream out = new FileOutputStream(tmpFile);
        byte[] buffer = new byte[8192];
        boolean first = true;
        boolean isGenericXML = true;
        int read;
        while ((read=in.read(buffer))>0) {
            out.write(buffer, 0, read);
            if (first) {
                first = false;
                // could be too less information. is a bit a lazy test
                //isSysView = new String(buffer, 0, read).indexOf("<sv:node") >= 0;
                isGenericXML = new String(buffer, 0, read).indexOf("jcr:primaryType") < 0;
            }
        }
        out.close();
        in.close();
        context.setInputStream(new FileInputStream(tmpFile));

        if (!isGenericXML) {
            // just import sys/doc view
            in = context.getInputStream();
            try {
                parentNode.getSession().importXML(parentNode.getPath(), in,
                        ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
                context.setInputStream(null);
                // no further processing
                return true;
            } catch (RepositoryException e) {
                // if error occurrs, reset input stream
                context.setInputStream(new FileInputStream(tmpFile));
                log.error("Unable to import sys/doc view. will try default xml import: " + e.toString());
                parentNode.refresh(false);
            } finally {
                in.close();
            }
        }

        // check 'file' node
        in = context.getInputStream();
        Node fileNode = parentNode.hasNode(context.getSystemId())
                ? parentNode.getNode(context.getSystemId())
                : parentNode.addNode(context.getSystemId(), nodeType);
        if (importResource(context, fileNode, in)) {
            context.setInputStream(null);
            // set current node
            context.setNode(fileNode);
        } else {
            context.setInputStream(new FileInputStream(tmpFile));
        }
        return false;
    }

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
                : parentNode.addNode(JCR_CONTENT, contentNodeType);
        try {
            content.setProperty(JCR_MIMETYPE, ctx.getContentType());
        } catch (RepositoryException e) {
            // ignore, since given nodetype could not allow mimetype
        }
        Calendar lastMod = Calendar.getInstance();
        if (ctx.getModificationTime() != 0) {
            lastMod.setTimeInMillis(ctx.getModificationTime());
        }
        try {
            content.setProperty(JCR_LASTMODIFIED, lastMod);
        } catch (RepositoryException e) {
            // ignore
        }
        try {
            parentNode.getSession().importXML(content.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (RepositoryException e) {
            // if this fails, we ignore import and pass to next command
            log.error("Error while importing XML. Will pass to next command: " + e.toString());
            if (content.isNew()) {
                content.remove();
            }
            return false;
        } finally {
            in.close();
        }
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

    /**
     * Returns the nodetype for the jcr:content node
     * @return
     */
    public String getContentNodeType() {
        return contentNodeType;
    }

    /**
     * Sets the nodetype for the jcr:content node.
     *
     * @param contentNodeType
     */
    public void setContentNodeType(String contentNodeType) {
        this.contentNodeType = contentNodeType;
    }

    /**
     * Sets the node type
     * @param nodeType
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

}
