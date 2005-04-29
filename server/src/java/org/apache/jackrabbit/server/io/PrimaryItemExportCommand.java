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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;

/**
 * This Class implements an abstract export command for a nc-resource. It acts
 * as generic base class that retrieves the resource properties from the
 * exporting node.
 * </p>
 */
public class PrimaryItemExportCommand implements Command, JcrConstants {

    /**
     * Executes this command by calling {@link #execute(ExportContext)} if
     * the given context is of the correct class.
     *
     * @param context the (export) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(Context context) throws Exception {
        if (context instanceof ExportContext) {
            return execute((ExportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executs this command using the given export context.
     *
     * @param context the export context
     * @return
     * @throws Exception if an error occurrs
     */
    public boolean execute(ExportContext context) throws Exception {
        Item item = context.getNode();
        long creationTime=0;

        // find primary item
        while (item.isNode()) {
            Node node = (Node)item;
            // creation time not set with the files/folders
            if (node.hasProperty(JCR_CREATED)) {
                creationTime = node.getProperty(JCR_CREATED).getValue().getLong();
            }
            // avoid 'ItemNotFound' if no primary item is present
            try {
                item = ((Node) item).getPrimaryItem();
            } catch (ItemNotFoundException e) {
                // no primary item existing, stop searching
                break;
            }
        }

        // access property values
        if (!item.isNode()) {
            Property prop = (Property) item;
            Node parent = prop.getParent();

            context.setCreationTime(creationTime);
            // check for last modified sibling
            try {
                if (parent.hasProperty(JCR_LASTMODIFIED)) {
                    context.setModificationTime(parent.getProperty(JCR_LASTMODIFIED).getLong());
                }
            } catch (RepositoryException e) {
                // ignore
            }

            // check for contenttype and encoding sibling of the primary item.
            String contentType="application/octet-stream";
            if (!context.getNode().isSame(item)) {
                try {
                    if (parent.hasProperty(JCR_MIMETYPE)) {
                        contentType = parent.getProperty(JCR_MIMETYPE).getString();

                        if (parent.hasProperty(JCR_ENCODING)) {
                            String encoding = parent.getProperty(JCR_ENCODING).getString();
                            if (!encoding.equals("")) {
                                contentType+="; charset=\"" + encoding + "\"";
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    // ignore
                }
            } else {
                // property was requested > set content type according to type
                contentType = (prop.getType() == PropertyType.BINARY) ? "application/octet-stream" : "text/plain";
            }
            context.setContentType(contentType);

            // get content length
            if (prop.getDefinition().isMultiple()) {
                context.setInputStream(prop.getValues()[0].getStream());
                context.setContentLength(prop.getLengths()[0]);
            } else {
                context.setInputStream(prop.getValue().getStream());
                context.setContentLength(prop.getLength());
            }
            return true;
        } else {
            /* no primaryItem property could be retrieved, abort command */
            return false;

        }
    }
}
