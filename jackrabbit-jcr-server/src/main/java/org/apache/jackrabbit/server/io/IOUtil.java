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

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.util.HttpDateFormat;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * <code>IOUtil</code> provides utility methods used for import and export
 * operations.
 */
public final class IOUtil {

    /**
     * Avoid instantiation
     */
    private IOUtil() {}

    /**
     * Constant for undefined modification/creation time
     */
    public static final long UNDEFINED_TIME = DavConstants.UNDEFINED_TIME;

    /**
     * Constant for undefined content length
     */
    public static final long UNDEFINED_LENGTH = -1;

    /**
     * Return the last modification time as formatted string.
     *
     * @return last modification time as string.
     * @see org.apache.jackrabbit.webdav.util.HttpDateFormat#modificationDateFormat() 
     */
    public static String getLastModified(long modificationTime) {
        if (modificationTime <= IOUtil.UNDEFINED_TIME) {
            modificationTime = new Date().getTime();
        }
        return HttpDateFormat.modificationDateFormat().format(new Date(modificationTime));
    }

    /**
     * Return the creation time as formatted string.
     *
     * @return creation time as string.
     * @see org.apache.jackrabbit.webdav.util.HttpDateFormat#creationDateFormat()
     */
    public static String getCreated(long createdTime) {
        if (createdTime <= IOUtil.UNDEFINED_TIME) {
            createdTime = 0;
        }
        return HttpDateFormat.creationDateFormat().format(new Date(createdTime));
    }

    /**
     */
    public static void spool(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Build a valid content type string from the given mimeType and encoding:
     * <pre>
     * &lt;mimeType&gt;; charset="&lt;encoding&gt;"
     * </pre>
     * If the specified mimeType is <code>null</code>, <code>null</code> is returned.
     *
     * @param mimeType
     * @param encoding
     * @return contentType or <code>null</code> if the specified mimeType is
     * <code>null</code>
     */
    public static String buildContentType(String mimeType, String encoding) {
        String contentType = mimeType;
        if (contentType != null && encoding != null) {
            contentType += "; charset=" + encoding;
        }
        return contentType;
    }

    /**
     * Retrieve the mimeType from the specified contentType.
     *
     * @param contentType
     * @return mimeType or <code>null</code>
     */
    public static String getMimeType(String contentType) {
        String mimeType = contentType;
        if (mimeType == null) {
            // property will be removed.
            // Note however, that jcr:mimetype is a mandatory property with the
            // built-in nt:file nodetype.
            return mimeType;
        }
        // strip any parameters
        int semi = mimeType.indexOf(';');
        return (semi > 0) ? mimeType.substring(0, semi) : mimeType;
    }

    /**
     * Retrieve the encoding from the specified contentType.
     *
     * @param contentType
     * @return encoding or <code>null</code> if the specified contentType is
     * <code>null</code> or does not define a charset.
     */
    public static String getEncoding(String contentType) {
        // find the charset parameter
        int equal;
        if (contentType == null || (equal = contentType.indexOf("charset=")) == -1) {
            // jcr:encoding property will be removed
            return null;
        }
        String encoding = contentType.substring(equal + 8);
        // get rid of any other parameters that might be specified after the charset
        int semi = encoding.indexOf(';');
        if (semi != -1) {
            encoding = encoding.substring(0, semi);
        }
        return encoding;
    }

    /**
     * Builds a new temp. file from the given input stream.
     * <p>
     * It is left to the user to remove the file as soon as it is not used
     * any more.
     *
     * @param inputStream the input stream
     * @return temp. file or <code>null</code> if the specified input is
     * <code>null</code>.
     */
    public static File getTempFile(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        // we need a tmp file, since the import could fail
        File tmpFile = File.createTempFile("__importcontext", ".tmp");
        FileOutputStream out = new FileOutputStream(tmpFile);
        byte[] buffer = new byte[8192];
        int read;
        while ((read=inputStream.read(buffer))>0) {
            out.write(buffer, 0, read);
        }
        out.close();
        inputStream.close();
        return tmpFile;
    }

    /**
     * Recursively creates nodes below the specified root node.
     *
     * @param root
     * @param relPath
     * @return the node corresponding to the last segment of the specified
     * relative path.
     * @throws RepositoryException
     */
    public static Node mkDirs(Node root, String relPath, String dirNodeType) throws RepositoryException {
        for (String seg : Text.explode(relPath, '/')) {
            if (!root.hasNode(seg)) {
                root.addNode(seg, dirNodeType);
            }
            root = root.getNode(seg);
        }
        return root;
    }
}
