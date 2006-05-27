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
package org.apache.jackrabbit.net;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.jcr.Session;

/**
 * The <code>FileParts</code> class provides composing and parsing functionality
 * to create and analize JCR Repository URL file components.
 * <p>
 * The file component of a JCR Repository URL has the format
 * <pre>
 *      file = [ "jcr:" [ "//" authority ] ] "/" repository "/" workspace jarpath .
 *      authority = // URL authority specification
 *      repository = // URL encoded repository name
 *      workspace = // URL encoded workspace name
 *      jarpath = path [ "!/" [ entry ] ] .
 *      path = // The absolute item path (with a leading slash)
 *      entry = // The (relative) path to the entry in an archive
 * </pre>
 * <p>
 * To facitility use of this class with JCRJar URLs, the
 * {@link #FileParts(String)} supports file specifications which contains
 * the JCR Repository URL scheme name and an optional URL authority
 * specification. This prefix in front of the real file specification is
 * silently discarded. It is not included in the string representation returned
 * by the {@link #toString()} method.
 * <p>
 * To make sure parsing is not complicated by implementation and use case
 * specific repository and workspace names, those names are URL encoded using
 * the <code>URLEncoder</code> class and <i>UTF-8</i> character encoding.
 *
 * @author Felix Meschberger
 */
class FileParts {

    /** The decoded name of the repository */
    private final String repository;

    /** The decoded name of the workspace */
    private final String workspace;

    /** The repository item path part of the URL path */
    private final String path;

    /**
     * The path to the entry in the archive, if the file spec contains the
     * jar entry separator <i>!/</i>. If no entry path is specified, this is
     * <code>null</code>. If no path is specified after the <i>!/</i> this
     * is an empty string.
     */
    private final String entryPath;

    /**
     * Creates a new instance for the root node of the given session. The
     * repository name is currently set to the fixed string "_" as there has not
     * been established a repository naming convention yet. The name of the
     * workspace is set to the name of the workspace to which the session is
     * attached. The path is set to <code>"/"</code> to indicate the root node
     * if the <code>path</code> argument is <code>null</code>.
     *
     * @param session The session for which to create this instance.
     * @param path The absolute item path to initialize this instance with. If
     *      <code>null</code> the item path is set to the <code>/</code>.
     * @param entryPath The path to the archive entry to set on this instance.
     *      This is expected to be a relative path without a leading slash and
     *      may be <code>null</code>.
     *
     * @throws NullPointerException if <code>session</code> is
     *      <code>null</code>.
     */
    FileParts(Session session, String path, String entryPath) {
        this.repository = "_";
        this.workspace = session.getWorkspace().getName();
        this.path = (path == null) ? "/" : path;
        this.entryPath = entryPath;
    }

    /**
     * Creates an instance of this class setting the repository, workspace and
     * path fields from the given <code>file</code> specification.
     *
     * @param file The specification providing the repository, workspace and
     *      path values.
     *
     * @throws NullPointerException if <code>file</code> is
     *      <code>null</code>.
     * @throws IllegalArgumentException if <code>file</code> is not the
     *      correct format.
     */
    FileParts(String file) {
        if (!file.startsWith("/")) {
            if (file.startsWith(URLFactory.REPOSITORY_SCHEME+":")) {
                file = strip(file);
            } else {
                throw failure("Not an absolute file", file);
            }
        }

        // find the repository name
        int slash0 = 1;
        int slash1 = file.indexOf('/', slash0);
        if (slash1 < 0 || slash1-slash0 == 0) {
            throw failure("Missing repository name", file);
        }
        this.repository = decode(file.substring(slash0, slash1));

        // find the workspace name
        slash0 = slash1 + 1;
        slash1 = file.indexOf('/', slash0);
        if (slash1 < 0 || slash1-slash0 == 0) {
            throw failure("Missing workspace name", file);
        }
        this.workspace = decode(file.substring(slash0, slash1));

        String fullPath = file.substring(slash1);
        int bangSlash = JCRJarURLHandler.indexOfBangSlash(fullPath);
        if (bangSlash < 0) {
            this.path = fullPath;
            this.entryPath = null;
        } else {
            this.path = fullPath.substring(0, bangSlash-1);
            this.entryPath = fullPath.substring(bangSlash+1);
        }
    }

    /**
     * Returns the plain name of the repository.
     */
    String getRepository() {
        return repository;
    }

    /**
     * Returns the plain name of the workspace.
     */
    String getWorkspace() {
        return workspace;
    }

    /**
     * Returns the absolute repository path of the item.
     */
    String getPath() {
        return path;
    }

    /**
     * Returns the entry path of <code>null</code> if no entry exists.
     */
    String getEntryPath() {
        return entryPath;
    }

    //---------- Object overwrites --------------------------------------------

    /**
     * Returns a hash code for this instance composed of the hash codes of the
     * repository, workspace and path names.
     */
    public int hashCode() {
        return getRepository().hashCode() +
            17 * getWorkspace().hashCode() +
            33 * getPath().hashCode();
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is the same as this or
     * if other is a <code>FileParts</code> with the same path, workspace and
     * repository. Otherwise <code>false</code> is returned.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof FileParts) {
            FileParts other = (FileParts) obj;

            // catch null entry path, fail if other has a defined entry path
            if (getEntryPath() == null) {
                if (other.getEntryPath() != null) {
                    return false;
                }
            }

            return getPath().equals(other.getPath()) &&
                getWorkspace().equals(other.getWorkspace()) &&
                getRepository().equals(other.getRepository()) &&
                getEntryPath().equals(other.getEntryPath());
        }

        // fall back on null or other class
        return false;
    }

    /**
     * Returns the encoded string representation of this instance, which may
     * later be fed to the {@link #FileParts(String)} constructor to recreate
     * an equivalent instance.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('/').append(encode(getRepository()));
        buf.append('/').append(encode(getWorkspace()));
        buf.append(getPath());

        if (getEntryPath() != null) {
            buf.append("!/").append(getEntryPath());
        }

        return buf.toString();
    }

    //---------- internal -----------------------------------------------------

    /**
     * @throws IllegalArgumentException If there is no path element after the
     *      authority.
     */
    private String strip(String file) {
        // cut off jcr: prefix - any other prefix, incl. double slash
        // would cause an exception to be thrown in the constructor
        int start = 4;

        // check whether the remainder contains an authority specification
        if (file.length() >= start+2 && file.charAt(start) == '/' &&
                file.charAt(start+1) == '/') {

            // find the slash after the authority, fail if missing
            start = file.indexOf('/', start + 2);
            if (start < 0) {
                throw failure("Missing path after authority", file);
            }
        }

        // return the file now
        return file.substring(start);
    }

    /**
     * Encodes the given string value using the <code>URLEncoder</code> and
     * <i>UTF-8</i> character encoding.
     *
     * @param value The string value to encode.
     *
     * @return The encoded string value.
     *
     * @throws InternalError If <code>UTF-8</code> character set encoding is
     *      not supported. As <code>UTF-8</code> is required to be implemented
     *      on any Java platform, this error is not expected.
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // not expected, throw an InternalError
            throw new InternalError("UTF-8 not supported");
        }
    }

    /**
     * Decodes the given string value using the <code>URLDecoder</code> and
     * <i>UTF-8</i> character encoding.
     *
     * @param value The string value to decode.
     *
     * @return The decoded string value.
     *
     * @throws InternalError If <code>UTF-8</code> character set encoding is
     *      not supported. As <code>UTF-8</code> is required to be implemented
     *      on any Java platform, this error is not expected.
     */
    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // not expected, throw an InternalError
            throw new InternalError("UTF-8 not supported");
        }
    }

    /**
     * Returns a <code>IllegalArgumentException</code> formatted with the
     * given reason and causing file specification.
     *
     * @param reason The failure reason.
     * @param file The original file specification leading to failure.
     *
     * @return A <code>IllegalArgumentException</code> with the given
     *      reason and causing file specification.
     */
    private IllegalArgumentException failure(String reason, String file) {
        return new IllegalArgumentException(reason + ": '" + file + "'");
    }
}
