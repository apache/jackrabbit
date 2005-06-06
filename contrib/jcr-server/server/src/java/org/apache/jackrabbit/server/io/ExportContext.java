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
import java.util.Properties;

/**
 * This Class implements a export context which is passed to the respective
 * export commands.
 * </p>
 * Please note, that this export context lacks an explicit
 * {@link org.apache.jackrabbit.webdav.DavResource} member. Currently, this
 * information is not needed in any of the known export commands but leaves this
 * I/O framework more generic.
 */
public class ExportContext extends AbstractContext {

    /**
     * the node to be exported
     */
    private Node node;

    /**
     * the input stream that needs to be set by the export commands
     */
    private InputStream inputStream;

    /**
     * the content length of the data in the input stream
     */
    private long contentLength;

    /**
     * the last modifiaction time of the resource
     */
    private long modificationTime;

    /**
     * the creation time of the resource
     */
    private long creationTime;

    /**
     * the content type of the resource
     */
    private String contentType;

    /**
     * Creates a new ExportContext for the given node
     *
     * @param exportRoot
     */
    public ExportContext(Node exportRoot) {
        this(null, exportRoot);
    }

    /**
     * Creats a new import context with the given root node and property defaults.
     * @param props
     * @param exportRoot
     */
    public ExportContext(Properties props, Node exportRoot) {
        super(props);
        if (exportRoot == null) {
            throw new IllegalArgumentException("exportRoot can not be null.");
        }
        this.node = exportRoot;
    }

    /**
     * Creates a new sub context which bases on this contexts properties
     * @param node
     * @return
     */
    public ExportContext createSubContext(Node node) {
        return new ExportContext(this, node);
    }


    /**
     * Returns the input stream
     *
     * @return the input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets a the inpurt stream to the data to be exported. A successfull
     * export command must set this memeber.
     *
     * @param inputStream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Returns the node to be exported
     *
     * @return
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns the length of the data to be exported
     *
     * @return the content length
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Sets the length of the data to be exported. A successfull export command
     * must set this memeber.
     *
     * @param contentLength the content length
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * Returns the last modification time.
     *
     * @return the last modification time.
     */
    public long getModificationTime() {
        return modificationTime;
    }

    /**
     * Sets the last modification time. A successfull export command may set
     * this member.
     *
     * @param modificationTime the last modification time
     */
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

    /**
     * Returns the creation time of the resource.
     *
     * @return the creation time
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the creation time of the resource. A successfull export command may
     * set this member.
     *
     * @param creationTime the creation time
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Returns the content type of the resource.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of the resource. A successfull export command
     * may set this member.
     * 
     * @param contentType the content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
