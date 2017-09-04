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
package org.apache.jackrabbit.server.util;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;

/**
 * <code>RequestData</code>...
 */
public class RequestData {

    private final HttpServletRequest request;
    private final HttpMultipartPost mpReq;

    public RequestData(HttpServletRequest request, File tmpDir) throws IOException {
        this.request = request;
        this.mpReq = new HttpMultipartPost(request, tmpDir);
    }

    /**
     * Dispose resources used.
     */
    public void dispose() {
        mpReq.dispose();
    }

    /**
     * Returns an iterator over all parameter names.
     * 
     * @return an iterator over strings.
     */
    public Iterator<String> getParameterNames() {
        @SuppressWarnings("unchecked")
        HashSet<String> names = new HashSet<String>(request.getParameterMap().keySet());
        names.addAll(mpReq.getParameterNames());

        return names.iterator();
    }

    /**
     * Returns the first value of the parameter with the given <code>name</code>.
     * The byte to string conversion is done using either the content type of
     * the parameter or the <code>formEncoding</code>.
     * <p>
     * Please note that if the addressed parameter is a file parameter, the
     * name of the original file is returned, and not its content.
     *
     * @param name the name of the parameter
     * @return the string of the first value or <code>null</code> if the
     *         parameter does not exist
     */
    public String getParameter(String name) {
        String ret = mpReq.getParameter(name);
        return (ret == null) ? request.getParameter(name) : ret;
    }

    /**
     * Returns the content types retrieved for parameters with the specified
     * name from the multipart or <code>null</code> if the multipart does not
     * contain parameter(s) with the given name.
     *
     * @param name parameter name
     * @return the parameter types retrieved for the specified parameter
     * name from the multipart or <code>null</code>.
     */
    public String[] getParameterTypes(String name) {
        String[] types = mpReq.getParameterTypes(name);
        return types == null ? null : types;
    }

    /**
     * Returns an array of Strings with all values of the parameter addressed
     * by <code>name</code>. the byte to string conversion is done using either
     * the content type of the multipart body or the <code>formEncoding</code>.
     * <p>
     * Please note that if the addressed parameter is a file parameter, the
     * name of the original file is returned, and not its content.
     *
     * @param name the name of the parameter
     * @return a string array of values or <code>null</code> if the parameter
     *         does not exist.
     */
    public String[] getParameterValues(String name) {
        String[] ret = mpReq.getParameterValues(name);
        return ret == null ? request.getParameterValues(name) : ret;
    }

    /**
     * Returns an array of input streams for uploaded file parameters.
     *
     * @param name the name of the file parameter(s)
     * @return an array of input streams or an empty array if no file params
     * with the given name exist.
     * @throws IOException if an I/O error occurs
     */
    public InputStream[] getFileParameters(String name) throws IOException {
        return mpReq.getFileParameterValues(name);
    }
}
