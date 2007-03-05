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
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.jar.JarException;
import java.util.jar.JarInputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ClassPathEntry</code> class encapsulates entries in the class path
 * of the {@link DynamicRepositoryClassLoader}. The main task is to retrieve
 * {@link ClassLoaderResource} instances for classes or resources to load from it.
 * <p>
 * This implementation is not currently integrated with Java security. That is
 * protection domains and security managers are not supported yet.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @author Felix Meschberger
 * @version $Rev$, $Date$
 */
abstract class ClassPathEntry {

    /** default logging */
    private static final Logger log =
        LoggerFactory.getLogger(ClassPathEntry.class);

    /** The session assigned to this class path entry */
    protected final Session session;

    /** The path to the item of this class path entry */
    protected final String path;

    /** The base URL for the class path entry to later construct resource URLs */
    protected URL baseURL;

    //---------- construction --------------------------------------------------

    /**
     * Creates an instance of the <code>ClassPathEntry</code> assigning the
     * session and path.
     *
     * @param session The <code>Session</code> to access the Repository.
     * @param path The path of the class path entry, this is either the
     *      path of a node containing a jar archive or is the path
     *      of the root of a hierarchy to look up resources in.
     */
    protected ClassPathEntry(Session session, String path) {
        this.path = path;
        this.session = session;
    }

    /**
     * Clones this instance of the <code>ClassPathEntry</code> setting the
     * path and session to the same value as the base instance.
     * <p>
     * Note that this constructor does not duplicate the session from the base
     * instance.
     *
     * @param base The <code>ClassPathEntry</code> from which to copy the path
     *      and the session.
     */
    protected ClassPathEntry(ClassPathEntry base) {
        this.path = base.path;
        this.session = base.session;
        this.baseURL = base.baseURL;
    }

    /**
     * Returns an instance of the <code>ClassPathEntry</code> class. This
     * instance will be a subclass correctly handling the type (directory or
     * jar archive) of class path entry is to be created.
     * <p>
     * If the path given has a trailing slash, it is taken as a directory root
     * else the path is first tested, whether it contains an archive. If not
     * the path is treated as a directory.
     *
     * @param session The <code>Session</code> to access the Repository.
     * @param path The path of the class path entry, this is either the
     *      path of a node containing a jar archive or is the path
     *      of the root of a hierharchy to look up resources in.
     *
     * @return An initialized <code>ClassPathEntry</code> instance for the
     *      path or <code>null</code> if an error occurred creating the
     *      instance.
     */
    static ClassPathEntry getInstance(Session session, String path) {

        // check we can access the path, don't care about content now
        try {
            session.checkPermission(path, "read");
        } catch (AccessControlException ace) {
            log.warn(
                "getInstance: Access denied reading from {}, ignoring entry",
                path);
            return null;
        } catch (RepositoryException re) {
            log.error("getInstance: Cannot check permission to " + path, re);
        }

        // only check for archive if no trailing slash in path
        if (!path.endsWith("/")) {
            InputStream is = null;
            JarInputStream zip = null;
            try {

                Property prop = Util.getProperty(session.getItem(path));
                if (prop != null) {

                    is = prop.getStream();
                    zip = new JarInputStream(is);
                    if (zip.getNextJarEntry() != null /* && zip.read() != -1 */ ) {
                        // use the expanding jar support if can expand
                        if (ExpandingArchiveClassPathEntry.canExpandArchives(session)) {
                            return new ExpandingArchiveClassPathEntry(prop, path);
                        }

                        // otherwise use the non-expanding
                        return new ArchiveClassPathEntry(prop, path);
                    }

                    log.debug(
                        "getInstance: {} might not be a jar archive, using as directory",
                        path);
                } else {
                    log.debug(
                        "getInstance: {} does not resolve to a property, using as directory",
                        path);
                }

            } catch (ItemNotFoundException infe) {

                // how to path ?
                // thrown from
                //   - Node.getPrimaryItem
                //   -

            } catch (PathNotFoundException pnfe) {

                // how to path ?
                // thrown from
                //   - session.getItem
                //   -

            } catch (RepositoryException re) {

                log.debug(
                    "getInstance: {} cannot be read from, using as directory",
                    path);

            } catch (JarException ze) {

                log.debug(
                    "getInstance: {} does not contain an archive, using as directory",
                    path);

            } catch (IOException ioe) {

                log.debug(
                    "getInstance: {} problem reading from the archive, using as directory",
                    path);

            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (IOException ignored) {}
                } else if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {}
                }
            }
            // assume the path designates a directory

            // append trailing slash now
            path += "/";
        }

        // we assume a directory class path entry, but we might have to check
        // whether the path refers to a node or not. On the other hande, this
        // class path entry will not be usable anyway if not, user beware :-)

        return new DirectoryClassPathEntry(session, path);
    }

    /**
     * Returns the path on which this <code>ClassPathEntry</code> is based.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns this <code>ClassPathEntry</code> represented as an URL to be
     * used in a list of URLs to further work on. If there is a problem creating
     * the URL for this instance, <code>null</code> is returned instead.
     */
    public URL toURL() {
        if (baseURL == null) {
            try {
                baseURL = URLFactory.createURL(session, path);
            } catch (MalformedURLException mue) {
                log.warn("DirectoryClassPathEntry: Creating baseURl for " +
                    path, mue);
            }
        }

        return baseURL;
    }

    /**
     * Returns a <code>ClassPathEntry</code> with the same configuration as
     * this <code>ClassPathEntry</code>.
     * <p>
     * The returned object may be but need not be a new instance. If the original
     * implementation is an immutable class, the instance returned may well
     * be the same as this.
     */
    abstract ClassPathEntry copy();

    /**
     * Searches for the named resource. The name is looked up as is, it is not
     * further modified such as appended with ".class" or made relative. That
     * is callers must make sure, that (1) this name is the full name of the
     * resource to find and that (2) it is a relative name, that is it should
     * not have a leading slash.
     * <p>
     * An example of a class to find would be :
     * <code>org/apache/jackrabbit/test/Tester.class</code>
     * which is converted from the generally used value
     * <code>org.apache.jackrabbit.test.Tester</code>
     * by the caller.
     *
     * @param name The name of the resource to find.
     */
    public abstract ClassLoaderResource getResource(String name);

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append(": path: ");
        buf.append(path);
        buf.append(", user: ");
        buf.append(session.getUserID());
        return buf.toString();
    }

    //----------- internal helper ----------------------------------------------

}
