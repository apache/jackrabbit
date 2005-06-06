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

/**
 * This Class implements an import context which is passed to the respective
 * import commands. An import command can alter the current node for creating
 * a recursive structure, thus is can lead to errors, if the configuration
 * is not done properly. A import command should clear the input stream,
 * after having processed it.
 * </p>
 * Please note, that this import context lacks an explicit
 * {@link org.apache.jackrabbit.webdav.DavResource} member. Currently, this
 * information is not needed in any of the known import commands but leaves this
 * I/O framework more generic.
 */
public class ImportContext extends AbstractContext {

    /**
     * The import root node, i.e. the parent node of the new content.
     */
    private final Node importRoot;

    /**
     * The 'current' node.
     */
    private Node node = null;

    /**
     * The input stream of the resource data
     */
    private InputStream inputStream;

    /**
     * The systemid of the resource, eg. the display name of a dav resource,
     * the filename of a file, etc.
     */
    private String systemId;

    /**
     * The content type of the resource to be imported.
     */
    private String contentType;

    /**
     * The modification time of the resource, if known
     */
    private long modificationTime;

    /**
     * Creates a new import context with the given root node
     * @param importRoot the import root node
     */
    public ImportContext(Node importRoot) {
        this(null, importRoot);
    }

    /**
     * Creats a new import context with the given root node and property defaults.
     * @param base
     * @param importRoot
     */
    public ImportContext(ImportContext base, Node importRoot) {
        super(base);
        if (importRoot == null) {
            throw new IllegalArgumentException("importRoot can not be null.");
        }
        this.importRoot = importRoot;
    }

    /**
     * Creates a new sub context which bases on this contexts properties
     * @param importRoot
     * @return
     */
    public ImportContext createSubContext(Node importRoot) {
        return new ImportContext(this, importRoot);
    }

    /**
     * Retruns the input stream of the resource to import.
     * @return the input stream.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets the inpurt stream of the resource to import. A import command that
     * consumed the input stream should set this member to <code>null</code>.
     * @param inputStream the input stream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Returns the import root of the resource to import.
     * @return the import root of the resource to import.
     */
    public Node getImportRoot() {
        return importRoot;
    }

    /**
     * Returns the current parent node of the resource to import. If no current
     * parent node is defined, the import root is returned.
     * @return the parent node.
     */
    public Node getNode() {
        return node == null ? importRoot : node;
    }

    /**
     * Sets the current parent node of the resource to import. A command can
     * set this member in order to generate recursive structured.
     * @param node
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * Returns the system id of the resource to be imported. This id depends on
     * the system the resource is comming from. it can be a filename, a
     * display name of a webdav resource, an URI, etc.
     * @return the system id of the resource to import
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * sets the system id of this resource.
     * @param systemId
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Returns the content type of the resource to be imported or null, if
     * no contenttype was defined.
     * @return the content type of the resource
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of the resource.
     * @param contentType the content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the modification time of the resource
     * @return the modification time.
     */
    public long getModificationTime() {
        return modificationTime;
    }

    /**
     * Sets the modification time of the resource
     * @param modificationTime the modification time
     */
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

}