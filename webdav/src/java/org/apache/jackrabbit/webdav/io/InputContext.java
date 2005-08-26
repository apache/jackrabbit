/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.io;

import java.io.InputStream;

/**
 * <code>InputContext</code> class encapsulates the <code>InputStream</code>
 * and some header values as present in the POST, PUT or MKCOL request.
 */
public class InputContext {

    private InputStream inputStream;
    private String contentType;
    private String contentLanguage;

    /**
     * Returns the input stream to be imported or <code>null</code> if no stream
     * has been set or if the stream has been consumed.
     *
     * @return the input stream.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets the input stream to be imported. The input stream should be set to
     * <code>null</code> if it has been consumed.
     *
     * @param inputStream the input stream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Returns the content type of the resource to be imported or null, if
     * no has previously been set.
     *
     * @return the content type of the resource
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of the resource.
     *
     * @param contentType the content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the content language or <code>null</code>.
     *
     * @return contentLanguage
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Sets the content language.
     *
     * @param contentLanguage
     */
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }
}
