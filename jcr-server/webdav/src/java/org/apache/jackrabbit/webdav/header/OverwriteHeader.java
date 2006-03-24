/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.header;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavConstants;

import javax.servlet.http.HttpServletRequest;

/**
 * <code>OverwriteHeader</code>...
 */
public class OverwriteHeader implements Header {

    private static Logger log = Logger.getLogger(OverwriteHeader.class);

    public static final String OVERWRITE_TRUE = "T";
    public static final String OVERWRITE_FALSE = "F";

    private boolean doOverwrite;

    public OverwriteHeader(boolean doOverwrite) {
        this.doOverwrite = doOverwrite;
    }

    public OverwriteHeader(HttpServletRequest request) {
        String overwriteHeader = request.getHeader(DavConstants.HEADER_OVERWRITE);
        if (overwriteHeader != null) {
            doOverwrite = overwriteHeader.equalsIgnoreCase(OVERWRITE_TRUE);
        }
    }

    public String getHeaderName() {
        return DavConstants.HEADER_OVERWRITE;
    }

    public String getHeaderValue() {
        return (doOverwrite) ? OVERWRITE_TRUE : OVERWRITE_FALSE;
    }

    public boolean isOverwrite() {
        return doOverwrite;
    }
}