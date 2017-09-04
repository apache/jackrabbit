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
import java.io.InputStream;

/**
 * <code>ImportContext</code>...
 */
public interface ImportContext extends IOContext {

    /**
     * Returns the import root of the resource to import, i.e. the parent node
     * of the new content to be created.
     *
     * @return the import root of the resource to import.
     */
    public Item getImportRoot();

    /**
     * Returns the system id of the resource to be imported. This id depends on
     * the system the resource is coming from. it can be a filename, a
     * display name of a webdav resource, an URI, etc.
     *
     * @return the system id of the resource to import
     */
    public String getSystemId();

    /**
     * Returns the input stream of the data to import or <code>null</code> if
     * there are none.
     *
     * @return the input stream.
     * @see #hasStream()
     */
    public InputStream getInputStream();

    /**
     * Returns the modification time of the resource or the current time if
     * the modification time has not been set.
     *
     * @return the modification time.
     */
    public long getModificationTime();

    /**
     * Returns the content language or <code>null</code>
     *
     * @return contentLanguage
     */
    public String getContentLanguage();

    /**
     * Returns the length of the data or {@link IOUtil#UNDEFINED_LENGTH -1} if
     * the content length could not be determined.
     *
     * @return the content length
     */
    public long getContentLength();

    /**
     * Returns the main media type. It should be retrieved from a content type
     * (as present in a http request) or from the systemId. If either value
     * is undefined <code>null</code> should be returned.
     *
     * @return the mimetype of the resource to be imported
     */
    public String getMimeType();

    /**
     * Returns the encoding extracted from a content type as present in a
     * request header or <code>null</code>
     *
     * @return the encoding to be used for importing
     */
    public String getEncoding();

    /**
     */
    public Object getProperty(Object propertyName);
}
