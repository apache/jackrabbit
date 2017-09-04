/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import javax.jcr.Item;

import java.io.OutputStream;

/**
 * <code>ExportContext</code>...
 */
public interface ExportContext extends IOContext {

    /**
     * Returns the item to be exported
     */
    public Item getExportRoot();

    /**
     * Return the output stream to be used for the export or <code>null</code>
     *
     * @return output stream or <code>null</code>
     */
    public OutputStream getOutputStream();

    /**
     * Set the content type for the resource content
     */
    public void setContentType(String mimeType, String encoding);

    /**
     * Sets the content language.
     */
    public void setContentLanguage(String contentLanguage);

    /**
     * Sets the length of the data.
     *
     * @param contentLength the content length
     */
    public void setContentLength(long contentLength);

    /**
     * Sets the creation time of the resource. A successful properties export may
     * set this member.
     *
     * @param creationTime the creation time
     */
    public void setCreationTime(long creationTime);

    /**
     * Sets the modification time of the resource
     *
     * @param modificationTime the modification time
     */
    public void setModificationTime(long modificationTime);

    /**
     * Sets the ETag of the resource. A successful export command
     * may set this member.
     *
     * @param etag the ETag
     */
    public void setETag(String etag);

    /**
     * Sets an arbitrary property to this export context.
     */
    public void setProperty(Object propertyName, Object propertyValue);
}
