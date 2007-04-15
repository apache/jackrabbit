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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ArchiveClassPathEntry</code> implements the {@link ClassPathEntry}
 * abstract class with support for archives containing classes and other
 * resources. The path used to construct the instance is the path of an item
 * resolving to a property containing the jar archive to access.
 *
 * @author Felix Meschberger
 */
class ArchiveClassPathEntry extends ClassPathEntry {

    /** Default logger */
    private static final Logger log =
        LoggerFactory.getLogger(ArchiveClassPathEntry.class);

    /** The property containing the archive */
    private final Property prop;

    /**
     * Cache all entries in the archive for faster decision on whether such
     * an entry is contained.
     */
    private Map entryMap;

    /**
     * The JAR file manifest. Set on demand by the {@link #getManifest()}
     * method.
     */
    private Manifest jarManifest;

    /**
     * Flag to indicate, whether the {@link #jarManifest} has already been read
     * from the archive. This field is used and set by the
     * {@link #getManifest()} to decide, whether to try to read the manifest.
     */
    private boolean jarManifestRead;

    /**
     * Creates an instance of the <code>ArchiveClassPathEntry</code> class.
     *
     * @param prop The <code>Property</code> containing the archive and
     *      the session used to access the repository.
     * @param path The original class path entry leading to the creation of
     *      this instance. This is not necessairily the same path as the
     *      properties path if the property was found through the primary
     *      item chain.
     *
     * @throws RepositoryException If an error occurrs retrieving the session
     *      from the property.
     */
    ArchiveClassPathEntry(Property prop, String path) throws RepositoryException {
        super(prop.getSession(), path);
        this.prop = prop;
    }

    /**
     * Clones the indicated <code>ArchiveClassPathEntry</code> object by
     * taking over its path, session and property.
     *
     * @param base The base <code>ArchiveClassPath</code> entry to clone.
     *
     * @see ClassPathEntry#ClassPathEntry(ClassPathEntry)
     */
    protected ArchiveClassPathEntry(ArchiveClassPathEntry base) {
        super(base);
        this.prop = base.prop;
    }

    /**
     * Returns the <code>Property</code> containing the JAR file of this
     * archive class path entry.
     */
    protected Property getProperty() {
        return prop;
    }

    /**
     * Returns a {@link ClassLoaderResource} for the named resource if it
     * can be found in the archive identified by the path given at
     * construction time. Note that if the archive property would exist but is
     * not readable by the current session, no resource is returned.
     * <p>
     * This method accesses the archive through an <code>InputStream</code>
     * retrievedfrom the property. This <code>InputStream</code> is closed before
     * returning to the caller to release the resources behind the stream
     * such that it might be updated, etc. For this reason the resource
     * instance returned will again open an <code>InputStream</code> on the
     * archive property to access the resource. Users of the resource
     * <code>InputStream</code> are encouraged to close the stream when no
     * longer used to prevent lockups in the Repository.
     *
     * @param name The name of the resource to return. If the resource would
     *      be a class the name must already be modified to denote a valid
     *      path, that is dots replaced by slashes and the <code>.class</code>
     *      extension attached.
     *
     * @return The {@link ClassLoaderResource} identified by the name or
     *      <code>null</code> if no resource is found for that name.
     */
    public ClassLoaderResource getResource(final String name) {

        JarInputStream zins = null;
        try {
            // get the archive and try to find the entry
            zins = getJarInputStream(prop);
            JarEntry entry = findEntry(zins, name);

            // if found create the resource to return
            if (entry != null) {
                return new ArchiveClassPathResource(this, entry);
            }

            log.debug("getResource: resource {} not found in archive {}", name,
                path);

        } catch (IOException ioe) {

            log.warn("getResource: problem accessing the archive {} for {}",
                new Object[]{ path, name}, ioe);

        } catch (RepositoryException re) {

            log.warn("getResource: problem accessing the archive {} for {}",
                new Object[]{ path, name}, re);

        } finally {

            // make sure streams are closed at the end
            if (zins != null) {
                try {
                    zins.close();
                } catch (IOException ignore) {
                }
            }

        }
        // invariant : not found or problem accessing the archive

        return null;
    }

    /**
     * Returns a <code>ClassPathEntry</code> with the same configuration as
     * this <code>ClassPathEntry</code>.
     * <p>
     * The <code>ArchiveClassPathEntry</code> class has internal state.
     * Therefore a new instance is created from the unmodifiable configuration
     * of this instance.
     */
    ClassPathEntry copy() {
        return new ArchiveClassPathEntry(this);
    }

    /**
     * Returns a JAR URL with no entry as the base URL of this class path entry.
     */
    public URL toURL() {
        if (baseURL == null) {
            try {
                baseURL = URLFactory.createJarURL(session, path, null);
            } catch (MalformedURLException mue) {
                log.warn("Problem creating baseURI for " + path, mue);
            }
        }

        return baseURL;
    }

    //----------- internal helper to find the entry ------------------------

    /**
     * Returns a JAR URL to access the named resource within the archive
     * underlying this class path entry. This is a helper method for the
     * {@link ClassLoaderResource} instance returned by
     * {@link #getResource(String)} method.
     * <p>
     * This method does not check, whether the named entry actually exists in
     * the underlying archive.
     *
     * @param name The name of the resource for which to create the JAR URL.
     */
    protected URL getURL(String name) {
        try {
            return URLFactory.createJarURL(session, path, name);
        } catch (MalformedURLException mue) {
            log.error("getURL: Cannot create URL for " + name, mue);
        }
        return null;
    }

    /**
     * Returns an URL to access the underlying archive itself of this class
     * path entry. The URL returned may be used as the code source for Java
     * securtiy protection domains. This is a helper method for the
     * {@link ClassLoaderResource} instance returned by
     * {@link #getResource(String)} method.
     *
     * @return The URL to access the underlying archive.
     */
    protected URL getCodeSourceURL() {
        try {
            return URLFactory.createURL(session, path);
        } catch (MalformedURLException mue) {
            log.warn("getCodeSourceURL: Cannot getURL for " + path, mue);
        }
        return null;
    }

    /**
     * Returns a <code>JarInputStream</code> from the property.
     *
     * @param property The <code>Property</code> containing the archive to
     *      access.
     *
     * @return A valid <code>JarInputStream</code>.
     *
     * @throws RepositoryException If an <code>InputStream</code> cannot be
     *      retrieved from the property.
     * @throws IOException If the <code>JarInputStream</code> cannot be
     *      created.
     */
    static JarInputStream getJarInputStream(Property property)
            throws RepositoryException, IOException {
        return new JarInputStream(property.getStream());
    }

    /**
     * Returns the <code>Manifest</code> object of the JAR archive file
     * underlying this archive class path entry. If no manifest exists in the
     * JAR file or if the archive is not a JAR file at - e.g. a plain ZIP
     * file - this method returns <code>null</code>. If an error occurrs
     * trying to access the manifest, <code>null</code> is also returned. Later
     * calls to this method, will not try again to read the manifest file,
     * though.
     * <p>
     * This method is synchronized to prevent two threads from trying to access
     * the manifest at the same time, which might result in false negative
     * returned.
     *
     * @return The manifest contained in the underlying JAR file or
     *      <code>null</code> if none exists or an error occurrs trying to
     *      load the manifest.
     */
    protected synchronized Manifest getManifest() {
        if (jarManifest == null && !jarManifestRead) {

            // immediately mark the manifest read, to prevent repeated read
            // in the case of missing manifest
            jarManifestRead = true;

            JarInputStream zipIns = null;
            try {
                zipIns = new JarInputStream(prop.getStream());
                jarManifest = zipIns.getManifest();
            } catch (RepositoryException re) {
                log.warn("Cannot access JAR file " + getPath(), re);
            } catch (IOException ioe) {
                log.warn("Cannot access manifest of JAR file " + getPath(), ioe);
            } finally {
                if (zipIns != null) {
                    try {
                        zipIns.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        return jarManifest;
    }

    /**
     * Returns the <code>JarEntry</code> for the path from the
     * <code>JarInputStream</code> or <code>null</code> if the path cannot
     * be found in the archive.
     *
     * @param zins The <code>JarInputStream</code> to search in.
     * @param path The path of the <code>JarEntry</code> to return.
     *
     * @return The <code>JarEntry</code> for the path or <code>null</code>
     *      if no such entry can be found.
     *
     * @throws IOException if a problem occurrs reading from the stream.
     */
    JarEntry findEntry(JarInputStream zins, String path)
        throws IOException {

        if (entryMap == null) {

            // make sure to not build the list twice
            synchronized (this) {

                /**
                 * make sure, we still need to build the list. this
                 * implementation surely does not cure all problems of the
                 * double-checked-locking problem, but it surely remmedies
                 * the main problem where the reference is already written
                 * to the field before the constructor has finished. Also
                 * this only assigns the field when the contents has been
                 * filled.
                 */
                if (entryMap == null) {

                    // prepare an empty entry map to be filled
                    Map tmpEntryMap = new HashMap();

                    try {
                        // build the name-to-index map
                        log.debug("findEntry: Building map while searching");

                        JarEntry result = null;
                        JarEntry entry = zins.getNextJarEntry();
                        for (int i=0; entry != null; i++) {

                            // add the entry to the map
                            String name = entry.getName();
                            Integer entryNumO = new Integer(i);
                            tmpEntryMap.put(name, entryNumO);
                            log.debug("findEntry: Entry {} ==> {}", name,
                                entryNumO);

                            // if we found our entry, keep it to be returned later
                            if (result == null && path.equals(name)) {
                                log.debug("findEntry: Found the entry, " +
                                        "continue indexing");
                                result = entry;
                            }

                            // on to the next entry
                            entry = zins.getNextJarEntry();
                        }
                        // invariant: path has the entry found or null

                        // return what we found
                        log.debug("findEntry: Indexing complete, returning {}",
                            result);
                        return result;

                    } finally {

                        /**
                         * assign the finished tmp entryMap to the field now.
                         * theoretically, this may still be null, which
                         * is no issue because it will be tried to be
                         * rebuilt - over and over again, though - by the
                         * next call to findEntry.
                         * in the case of build problems, the map be empty
                         * in which case it will not be rebuilt, which is
                         * ok, too, given that reading will still yield
                         * problems.
                         */

                        entryMap = tmpEntryMap;
                    }

                }
            }
        }
        // invariant: entryMap is not null, but might be empty
        // ( in case of problems creating the tmpEntryMap above, e.g.
        //   OutOfMemoryError, the entryMap might be null, but then we
        //   are thrown out of the method any way ... this is no issue
        //   here )

        // map exists, lets try to get via number
        Number entryNumO = (Number) entryMap.get(path);
        if (entryNumO == null) {
            log.debug("findEntry: This archive does not contain {}", path);
            return null;
        }

        // find the indexed entry
        log.debug("findEntry: {} is entry #{}", path, entryNumO);
        int entryNum = entryNumO.intValue();
        JarEntry entry = zins.getNextJarEntry();
        while (entryNum > 0 && entry != null) {
            entry = zins.getNextJarEntry();
            entryNum--;
        }
        return entry;
    }

    /**
     * The <code>ArchiveClassPathResource</code> extends the
     * {@link ClassLoaderResource} with support to extract resources from a
     * JAR or ZIP archive.
     *
     * @author Felix Meschberger
     */
    private static class ArchiveClassPathResource extends ClassLoaderResource {

        /**
         * The JAR/ZIP file entry providing the name, size and modification
         * time information.
         */
        private final JarEntry jarEntry;

        /**
         * Creates an instance of this resource for the given
         * {@link ArchiveClassPathEntry} and JAR/ZIP file entry.
         *
         * @param pathEntry The {@link ArchiveClassPathEntry} from which this
         *      resource has been loaded.
         * @param jarEntry The JAR/ZIP file entry describing this resource.
         */
        private ArchiveClassPathResource(ArchiveClassPathEntry pathEntry,
                JarEntry jarEntry) {
            super(pathEntry, jarEntry.getName(), pathEntry.getProperty());
            this.jarEntry = jarEntry;
        }

        /**
         * Returns an URL to access this resource.
         *
         * @see ArchiveClassPathEntry#getURL(String)
         */
        public URL getURL() {
            return getArchiveClassPathEntry().getURL(getName());
        }

        /**
         * Returns an URL identifying the archive from which this resource is
         * loaded.
         *
         * @see ArchiveClassPathEntry#getCodeSourceURL()
         */
        public URL getCodeSourceURL() {
            return getArchiveClassPathEntry().getCodeSourceURL();
        }

        /**
         * Returns an <code>InputStream</code> to read the contents of the
         * resource. Calling this method actually accesses the JAR/ZIP file
         * and seeks through the file until the entry is found.
         * <p>
         * Clients of this method must make sure to close the stream returned
         * if not used anymore to prevent resource drain.
         *
         * @throws RepositoryException If an error occurrs accessing or reading
         *      the archive.
         *
         * @see ArchiveClassPathEntry#findEntry(JarInputStream, String)
         */
        public InputStream getInputStream() throws RepositoryException {
            /**
             * Cannot reuse the ClassPathEntry instances entry and
             * JarInputStream, because this is shared and has to be
             * closed to release the property resource.
             */
            JarInputStream zipIns = null;
            JarEntry entry = null;

            try {

                zipIns = getJarInputStream(getProperty());
                entry = getArchiveClassPathEntry().findEntry(zipIns, getName());
                if (entry != null) {
                    return zipIns;
                }

                // otherwise
                log.warn("Cannot find entry {} in the archive {} anymore!",
                    getName(), getClassPathEntry().getPath());
                return null;

            } catch (IOException ioe) {

                // log
                throw new RepositoryException(ioe);

            } finally {

                // if thrown during findEntry(), entry is null but
                // the stream is open. As we exit by an exception,
                // the InputStream is not returned and must be
                // closed to release it.

                if (entry == null && zipIns != null) {
                    try {
                        zipIns.close();
                    } catch (IOException ignored) {
                    }
                }

            }
        }

        /**
         * Returns the value of the <code>size</code> field of the JAR/ZIP
         * file entry of this resource. If the size is not known to the entry,
         * <code>-1</code> may be returned.
         */
        public int getContentLength() {
            return (int) jarEntry.getSize();
        }

        /**
         * Returns the path to the property containing the archive or the
         * path with which the {@link ArchiveClassPathEntry} was created if the
         * former cannot be retrieved.
         */
        public String getPath() {
            try {
                return getProperty().getPath();
            } catch (RepositoryException re) {
                String archivePath = getClassPathEntry().getPath();
                log.warn("Cannot access the path of the archive " +
                        "property below " + archivePath, re);
                return archivePath;
            }
        }

        /**
         * Returns the value of the <code>time</code> field of the JAR/ZIP
         * file entry of this resource. If the time is not known to the entry,
         * <code>-1</code> may be returned.
         */
        public long getLastModificationTime() {
            return jarEntry.getTime();
        }

        /**
         * Returns the manifest of the archive from which this resource was
         * loaded or <code>null</code> if no such manifest exists or an error
         * occurrs reading the manifest.
         *
         * @see ArchiveClassPathEntry#getManifest()
         */
        public Manifest getManifest() {
            return getArchiveClassPathEntry().getManifest();
        }

        /**
         * Returns the {@link ArchiveClassPathEntry} from which this resource
         * was loaded.
         */
        protected ArchiveClassPathEntry getArchiveClassPathEntry() {
            return (ArchiveClassPathEntry) getClassPathEntry();
        }
    }
}
