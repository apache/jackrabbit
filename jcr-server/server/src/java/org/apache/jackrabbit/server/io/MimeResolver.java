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
package org.apache.jackrabbit.server.io;

import java.util.Properties;
import java.io.IOException;

import org.apache.jackrabbit.util.Text;

/**
 * This Class implements a very simple mime type resolver.
 */
public class MimeResolver {

    /**
     * the loaded mime types
     */
    private Properties mimeTypes = new Properties();

    /**
     * the default mime type
     */
    private String defaultMimeType = "application/octet-stream";

    /**
     * Creates a new mime resolver.
     */
    public MimeResolver() {
        try {
            // init the mime types
            mimeTypes.load(getClass().getResourceAsStream("mimetypes.properties"));
        } catch (IOException e) {
            throw new InternalError("Unable to load mimetypes: " + e.toString());
        }
    }

    /**
     * Returns the default mime type
     * @return
     */
    public String getDefaultMimeType() {
	return defaultMimeType;
    }

    /**
     * Sets the default mime type
     * @param defaultMimeType
     */
    public void setDefaultMimeType(String defaultMimeType) {
	this.defaultMimeType = defaultMimeType;
    }

    /**
     * Retrusn the mime type for the given name.
     * @param filename
     * @return
     */
    public String getMimeType(String filename) {
	String ext = Text.getName(filename, '.');
        if (ext.equals("")) {
            ext = filename;
        }
	return mimeTypes.getProperty(ext.toLowerCase(), defaultMimeType);
    }

}
