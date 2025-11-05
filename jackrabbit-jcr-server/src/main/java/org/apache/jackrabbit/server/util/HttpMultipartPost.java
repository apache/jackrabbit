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

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.RequestContext;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <code>HttpMultipartPost</code>...
 */
class HttpMultipartPost {

    private static final Logger log = LoggerFactory.getLogger(HttpMultipartPost.class);

    private final Map<String, List<FileItem>> nameToItems = new LinkedHashMap<String, List<FileItem>>();
    private final Set<String> fileParamNames = new HashSet<String>();

    private boolean initialized;

    HttpMultipartPost(HttpServletRequest request, File tmpDir) throws IOException {
        extractMultipart(request, tmpDir);
        initialized = true;
    }

    private static FileItemFactory getFileItemFactory(File tmpDir) {
        return DiskFileItemFactory.builder().setFile(tmpDir == null ? PathUtils.getTempDirectory().toFile() : tmpDir).get();
    }

    private void extractMultipart(HttpServletRequest request, File tmpDir)
            throws IOException {
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            log.debug("Request does not contain multipart content -> ignoring.");
            return;
        }

        JakartaServletFileUpload upload = new JakartaServletFileUpload(getFileItemFactory(tmpDir)) {

            @Override
            public List<FileItem> parseRequest(final RequestContext requestContext) throws FileUploadException {
                final List<FileItem> itemList = new ArrayList<>();
                var successful = false;
                try {
                    final var fileItemFactory = Objects.requireNonNull(getFileItemFactory(), "No FileItemFactory has been set.");
                    final var buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                    getItemIterator(requestContext).forEachRemaining(fileItemInput -> {
                        // Don't use getName() here to prevent an InvalidFileNameException.
                        final FileItem fileItem = (FileItem) fileItemFactory.fileItemBuilder()
                                .setFieldName(fileItemInput.getFieldName())
                                .setContentType(fileItemInput.getContentType())
                                .setFormField(fileItemInput.isFormField())
                                .setFileItemHeaders(fileItemInput.getHeaders())
                                .get();
                        itemList.add((FileItem) fileItem);
                        try (var inputStream = fileItemInput.getInputStream();
                             var outputStream = fileItem.getOutputStream()) {
                            IOUtils.copyLarge(inputStream, outputStream, buffer);
                        } catch (final FileUploadException e) {
                            throw e;
                        } catch (final IOException e) {
                            throw new FileUploadException(String.format("Processing of %s request failed. %s", MULTIPART_FORM_DATA, e.getMessage()), e);
                        }
                    });
                    successful = true;
                    return itemList;
                } catch (final FileUploadException e) {
                    throw e;
                } catch (final IOException e) {
                    throw new FileUploadException(e.getMessage(), e);
                } finally {
                    if (!successful) {
                        for (final FileItem fileItem : itemList) {
                            try {
                                fileItem.delete();
                            } catch (final Exception ignored) {}
                        }
                    }
                }
            }
        };
        // make sure the content disposition headers are read with the charset
        // specified in the request content type (or UTF-8 if no charset is specified).
        // see JCR
        if (request.getCharacterEncoding() == null) {
            upload.setHeaderCharset(StandardCharsets.UTF_8);
        }
        try {
            @SuppressWarnings("unchecked")
            List<FileItem> fileItems = upload.parseRequest(request);
            for (FileItem fileItem : fileItems) {
                addItem(fileItem);
            }
        } catch (FileUploadException e) {
            log.error("Error while processing multipart.", e);
            throw new IOException(e.toString());
        }
    }

    /**
     * Add the given file item to the list defined for its name and make the
     * list is present in the map. If the item does not represent a simple
     * form field its name is also added to the <code>fileParamNames</code> set.
     *
     * @param item The {@link FileItem} to add.
     */
    private void addItem(FileItem item) {
        String name = item.getFieldName();
        List<FileItem> l = nameToItems.get(item.getFieldName());
        if (l == null) {
            l = new ArrayList<FileItem>();
            nameToItems.put(name, l);
        }
        l.add(item);

        // if file parameter, add name to the set of file parameters in order to
        // be able to extract the file parameter values later on without iterating
        // over all keys.
        if (!item.isFormField()) {
            fileParamNames.add(name);
        }
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("HttpMultipartPost not initialized (or already disposed).");
        }
    }

    /**
     * Release all file items hold with the name-to-items map. Especially those
     * having a temporary file associated with.
     * 
     * @see FileItem#delete()
     */
    synchronized void dispose() throws IOException {
        checkInitialized();

        for (List<FileItem> fileItems : nameToItems.values()) {
            for (FileItem fileItem : fileItems) {
                fileItem.delete();
            }
        }

        nameToItems.clear();
        fileParamNames.clear();
        initialized = false;
    }

    /**
     * Returns an iterator over all file item names.
     *
     * @return a set of strings.
     */
    Set<String> getParameterNames() {
        checkInitialized();
        return nameToItems.keySet();
    }


    /**
     * Returns the content types of the parameters with the given name. If
     * the parameter does not exist <code>null</code> is returned. If the content
     * type of any of the parameter values is not known, the corresponding entry
     * in the array returned is <code>null</code>.
     * <p>
     * The content type of a parameter is only known here if the information
     * has been sent by the client browser. This is generally only the case
     * for file upload fields of HTML forms which have been posted using the
     * HTTP <em>POST</em> with <em>multipart/form-data</em> encoding.
     * <p>
     * Example : For the form
     * <pre>
         <form name="Upload" method="POST" ENCTYPE="multipart/form-data">
            <input type="file" name="Upload"><br>
            <input type="text" name="Upload"><br>
            <input type="submit">
         </form>
     * </pre>
     * this method will return an array of two entries when called for the
     * <em>Upload</em> parameter. The first entry will contain the content
     * type (if transmitted by the client) of the file uploaded. The second
     * entry will be <code>null</code> because the content type of the text
     * input field will generally not be sent by the client.
     *
     * @param name The name of the parameter whose content type is to be
     * returned.
     * @return The content types of the file items with the specified name.
     */
    String[] getParameterTypes(String name) {
        checkInitialized();
        String[] cts = null;
        List<FileItem> l = nameToItems.get(name);
        if (l != null && !l.isEmpty()) {
            cts = new String[l.size()];
            for (int i = 0; i < cts.length; i++) {
                cts[i] = l.get(i).getContentType();
            }
        }
        return cts;
    }
    
    /**
     * Returns the first value of the file items with the given <code>name</code>.
     * The byte to string conversion is done using either the content type of
     * the file items or the <code>formEncoding</code>.
     * <p>
     * Please note that if the addressed parameter is an uploaded file rather
     * than a simple form entry, the name of the original file is returned    
     * instead of the content.
     *
     * @param name the name of the parameter
     * @return the string of the first value or <code>null</code> if the
     *         parameter does not exist
     */
    String getParameter(String name) throws IOException {
        checkInitialized();
        List<FileItem> l = nameToItems.get(name);
        if (l == null || l.isEmpty()) {
            return null;
        } else {
            FileItem item = l.get(0);
            if (item.isFormField()) {
                return item.getString();
            } else {
                return item.getName();
            }
        }
    }

    /**
     * Returns an array of Strings with all values of the parameter addressed
     * by <code>name</code>. the byte to string conversion is done using either
     * the content type of the multipart body or the <code>formEncoding</code>.
     * <p>
     * Please note that if the addressed parameter is an uploaded file rather
     * than a simple form entry, the name of the original file is returned
     * instead of the content.
     *
     * @param name the name of the parameter
     * @return a string array of values or <code>null</code> if no entry with the
     * given name exists.
     */
    String[] getParameterValues(String name) throws IOException {
        checkInitialized();
        List<FileItem> l = nameToItems.get(name);
        if (l == null || l.isEmpty()) {
            return null;
        } else {
            String[] values = new String[l.size()];
            for (int i = 0; i < values.length; i++) {
                FileItem item = l.get(i);
                if (item.isFormField()) {
                    values[i] = item.getString();
                } else {
                    values[i] = item.getName();
                }
            }
            return values;
        }
    }

    /**
     * Returns a set of the file parameter names. An empty set if
     * no file parameters were present in the request.
     *
     * @return an set of file item names representing the file
     * parameters available with the request.
     */
    Set<String> getFileParameterNames() {
        checkInitialized();
        return fileParamNames;
    }

    /**
     * Returns an array of input streams for uploaded file parameters.
     *
     * @param name the name of the file parameter(s)
     * @return an array of input streams or <code>null</code> if no file params
     * with the given name exist.
     * @throws IOException if an I/O error occurs
     */
    InputStream[] getFileParameterValues(String name) throws IOException {
        checkInitialized();
        InputStream[] values = null;
        if (fileParamNames.contains(name)) {
            List<FileItem> l = nameToItems.get(name);
            if (l != null && !l.isEmpty()) {
                List<InputStream> ins = new ArrayList<InputStream>(l.size());
                for (FileItem item : l) {
                    if (!item.isFormField()) {
                        ins.add(item.getInputStream());
                    }
                }
                values = ins.toArray(new InputStream[ins.size()]);
            }
        }
        return values;
    }
}
