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

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ExportContextImpl</code> implements an <code>ExportContext</code> that
 * wraps around the specified OutputContext as it was passed to
 * {@link DavResource#spool(OutputContext)}. If a stream is provided a temporary
 * file is created, which is deleted as soon as {@link #informCompleted(boolean)}
 * is called on this context. Note however, that the properties and the stream
 * are written to the  <code>OutputContext</code> but upon successful completion.
 *
 * @see #informCompleted(boolean)
 */
public class ExportContextImpl extends AbstractExportContext {

    private static Logger log = LoggerFactory.getLogger(ExportContextImpl.class);

    private final Map<String, String> properties = new HashMap<String, String>();
    private final OutputContext outputCtx;

    private File outFile;
    private OutputStream outStream;

    public ExportContextImpl(Item exportRoot, OutputContext outputCtx)
            throws IOException {
        super(exportRoot, outputCtx != null && outputCtx.hasStream(), null);
        this.outputCtx = outputCtx;
        if (hasStream()) {
            // we need a tmp file, since the export could fail
            outFile = File.createTempFile("__exportcontext", "tmp");
        }
    }

    /**
     * Returns a new <code>OutputStream</code> to the temporary file or
     * <code>null</code> if this context provides no stream.
     *
     * @see ExportContext#getOutputStream()
     * @see #informCompleted(boolean)
     */
    public OutputStream getOutputStream() {
        checkCompleted();
        if (hasStream()) {
            try {
                // clean up the stream retrieved by the preceding handler, that
                // did not behave properly and failed to export although initially
                // willing to handle the export.
                if (outStream != null) {
                    outStream.close();
                }
                outStream = new FileOutputStream(outFile);
                return outStream;
            } catch (IOException e) {
                // unexpected error... ignore and return null
            }
        }
        return null;
    }

    /**
     * @see ExportContext#setContentLanguage(String)
     */
    public void setContentLanguage(String contentLanguage) {
        properties.put(DavConstants.HEADER_CONTENT_LANGUAGE, contentLanguage);
    }

    /**
     * @see ExportContext#setContentLength(long)
     */
    public void setContentLength(long contentLength) {
        properties.put(DavConstants.HEADER_CONTENT_LENGTH, contentLength + "");
    }

    /**
     * @see ExportContext#setContentType(String,String)
     */
    public void setContentType(String mimeType, String encoding) {
        properties.put(DavConstants.HEADER_CONTENT_TYPE, IOUtil.buildContentType(mimeType, encoding));
    }

    /**
     * Does nothing since the wrapped output context does not understand
     * creation time
     *
     * @see ExportContext#setCreationTime(long)
     */
    public void setCreationTime(long creationTime) {
        // ignore since output-ctx does not understand creation time
    }

    /**
     * @see ExportContext#setModificationTime(long)
     */
    public void setModificationTime(long modificationTime) {
        if (modificationTime <= IOUtil.UNDEFINED_TIME) {
            modificationTime = new Date().getTime();
        }
        String lastMod = IOUtil.getLastModified(modificationTime);
        properties.put(DavConstants.HEADER_LAST_MODIFIED, lastMod);
    }

    /**
     * @see ExportContext#setETag(String)
     */
    public void setETag(String etag) {
        properties.put(DavConstants.HEADER_ETAG, etag);
    }

    /**
     * @see ExportContext#setProperty(Object, Object)
     */
    public void setProperty(Object propertyName, Object propertyValue) {
        if (propertyName != null && propertyValue != null) {
            properties.put(propertyName.toString(), propertyValue.toString());
        }
    }

    /**
     * If success is true, the properties set before an the output stream are
     * written to the wrapped <code>OutputContext</code>.
     *
     * @see ExportContext#informCompleted(boolean)
     */
    @Override
    public void informCompleted(boolean success) {
        checkCompleted();
        completed = true;
        // make sure the outputStream gets closed (and don't assume the handlers
        // took care of this.
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (success) {
            // write properties and data to the output-context
            if (outputCtx != null) {
                boolean seenContentLength = false;
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (name != null && value != null) {
                        outputCtx.setProperty(name, value);
                        seenContentLength |= DavConstants.HEADER_CONTENT_LENGTH.equals(name);
                    }
                }

                if (outputCtx.hasStream() && outFile != null) {
                    OutputStream out = outputCtx.getOutputStream();
                    try {
                        // make sure the content-length is set
                        if (!seenContentLength) {
                            outputCtx.setContentLength(outFile.length());
                        }
                        FileInputStream in = new FileInputStream(outFile);
                        IOUtil.spool(in, out);
                    } catch (IOException e) {
                        log.error(e.toString());
                    }
                }
            }
        }
        if (outFile != null) {
            outFile.delete();
        }
    }
}
