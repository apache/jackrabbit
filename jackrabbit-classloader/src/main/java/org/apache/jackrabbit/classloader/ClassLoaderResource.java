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
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.jar.Manifest;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ClassLoaderResource</code> class represents a resource looked up
 * by the {@link ClassPathEntry}s of the {@link RepositoryClassLoader}. The
 * class provides transparent access to the resource irrespective of the fact
 * on whether the resource is contained in a repository property or in an
 * JAR or ZIP archive.
 * <p>
 * This class is extended to implement depending features such as storing
 * resources in repository properties or JAR or ZIP archives.
 *
 * @author Felix Meschberger
 */
class ClassLoaderResource {

    /** default log category */
    private static final Logger log =
        LoggerFactory.getLogger(ClassLoaderResource.class);

    /**
     * The class path entry which loaded this class loader resource
     */
    private final ClassPathEntry pathEntry;

    /**
     * The name of this resource.
     */
    private final String name;

    /**
     * The repository property providing the resource's contents. This may be
     * <code>null</code> if the resource was loaded from a JAR/ZIP archive.
     */
    private final Property resProperty;

    /**
     * The class optionally loaded/defined through this resource.
     *
     * @see #getLoadedClass()
     * @see #setLoadedClass(Class)
     */
    private Class loadedClass;

    /**
     * The time in milliseconds at which this resource has been loaded from
     * the repository.
     */
    private final long loadTime;

    /**
     * Flag indicating that this resource has already been checked for expiry
     * and whether it is actually expired.
     *
     * @see #isExpired()
     */
    private boolean expired;

    /**
     * Creates an instance of this class for the class path entry.
     *
     * @param pathEntry The {@link ClassPathEntry} of the code source of this
     *      class loader resource.
     * @param name The path name of this resource.
     * @param resProperty The <code>Property</code>providing the content's of
     *      this resource. This may be <code>null</code> if the resource
     *      was loaded from an JAR or ZIP archive.
     */
    /* package */ ClassLoaderResource(ClassPathEntry pathEntry, String name,
            Property resProperty) {
        this.pathEntry = pathEntry;
        this.name = name;
        this.resProperty = resProperty;
        this.loadTime = System.currentTimeMillis();
    }

    /**
     * Returns the {@link ClassPathEntry} which loaded this resource.
     */
    protected ClassPathEntry getClassPathEntry() {
        return pathEntry;
    }

    /**
     * Returns the name of this resource. This is the name used to find the
     * resource, for example the class name or the properties file path.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the <code>Property</code> with which this resource is created.
     */
    protected Property getProperty() {
        return resProperty;
    }

    /**
     * Returns the time in milliseconds at which this resource has been loaded
     */
    protected long getLoadTime() {
        return loadTime;
    }

    /**
     * Returns the URL to access this resource, for example a JCR or a JAR URL.
     * If the URL cannot be created from the resource data, <code>null</code> is
     * returned.
     */
    public URL getURL() {
        try {
            return URLFactory.createURL(getClassPathEntry().session, getPath());
        } catch (Exception e) {
            log.warn("getURL: Cannot getURL for " + getPath(), e);
        }
        return null;
    }

    /**
     * Returns the URL to the code source of this entry. If there is no code
     * source available, <code>null</code> is returned.
     * <p>
     * This base class implementation returns the result of calling
     * {@link ClassPathEntry#toURL()} on the class path entry from which this
     * resource was loaded.
     */
    public URL getCodeSourceURL() {
        return getClassPathEntry().toURL();
    }

    /**
     * Returns an <code>InputStream</code> to read from the resource.
     * <p>
     * This base class implementation returns the result of calling the
     * <code>getStream()</code> method on the resource's property or
     * <code>null</code> if the property is not set.
     */
    public InputStream getInputStream() throws RepositoryException {
        return (getProperty() != null) ? getProperty().getStream() : null;
    }

    /**
     * Returns the size of the resource or -1 if the size cannot be found out.
     * <p>
     * This base class implementation returns the result of calling the
     * <code>getLength()</code> method on the resource's property or -1 if
     * the property is not set.
     *
     * @throws RepositoryException If an error occurrs trying to find the length
     *      of the property.
     */
    public int getContentLength() throws RepositoryException {
        return (getProperty() != null) ? (int) getProperty().getLength() : -1;
    }

    /**
     * Returns the path of the property containing the resource.
     * <p>
     * This base class implementation returns the absolute path of the
     * resource's property. If the property is not set or if an error occurrs
     * accesing the property's path, the concatentation of the class path
     * entry's path and the resource's name is returned.
     */
    public String getPath() {
        if (getProperty() != null) {
            try {
                return getProperty().getPath();
            } catch (RepositoryException re) {
                // fallback
                log.warn("getPath: Cannot retrieve path of entry " + getName(),
                    re);
            }
        }

        // fallback if no resource property or an error accessing the path of
        // the property
        return getSafePath();
    }

    /**
     * Returns the path of the property containing the resource by appending
     * the {@link #getName() name} to the path of the class path entry to which
     * this resource belongs. This path need not necessairily be the same as
     * the {@link #getProperty() path of the property} but will always succeed
     * as there is no repository access involved.
     */
    protected String getSafePath() {
        return getClassPathEntry().getPath() + getName();
    }

    /**
     * Returns the time of the the last modification of the resource or -1 if
     * the last modification time cannot be evaluated.
     * <p>
     * This base class implementation returns the result of calling the
     * {@link Util#getLastModificationTime(Property)} method on the resource's
     * property if not <code>null</code>. In case of an error or if the
     * property is <code>null</code>, -1 is returned.
     */
    public long getLastModificationTime() {
        if (getProperty() != null) {
            try {
                return Util.getLastModificationTime(getProperty());
            } catch (RepositoryException re) {
                log.info("getLastModificationTime of resource property", re);
            }
        }

        // cannot find the resource modification time, use epoch
        return -1;
    }

    /**
     * Returns the resource as an array of bytes
     */
    public byte[] getBytes() throws IOException, RepositoryException {
        InputStream in = null;
        byte[] buf = null;

        log.debug("getBytes");

        try {
            in = getInputStream();
            log.debug("getInputStream() returned {}", in);

            int length = getContentLength();
            log.debug("getContentLength() returned {}", new Integer(length));

            if (length >= 0) {

                buf = new byte[length];
                for (int read; length > 0; length -= read) {
                    read = in.read(buf, buf.length - length, length);
                    if (read == -1) {
                        throw new IOException("unexpected EOF");
                    }
                }

            } else {

                buf = new byte[1024];
                int count = 0;
                int read;

                // read enlarging buffer
                while ((read = in.read(buf, count, buf.length - count)) != -1) {
                    count += read;
                    if (count >= buf.length) {
                        byte buf1[] = new byte[count * 2];
                        System.arraycopy(buf, 0, buf1, 0, count);
                        buf = buf1;
                    }
                }

                // resize buffer if too big
                if (count != buf.length) {
                    byte buf1[] = new byte[count];
                    System.arraycopy(buf, 0, buf1, 0, count);
                    buf = buf1;
                }

            }

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }

        }

        return buf;
    }

    /**
     * Returns the manifest from the jar file for this class resource. If this
     * resource is not from a jar file, the method returns <code>null</code>,
     * which is what the default implementation does.
     */
    public Manifest getManifest() {
        return null;
    }

    /**
     * Returns the certificates from the jar file for this class resource. If
     * this resource is not from a jar file, the method returns
     * <code>null</code>, which is what the default implementation does.
     */
    public Certificate[] getCertificates() {
        return null;
    }

    /**
     * Returns the <code>Property</code> which is used to check whether this
     * resource is expired or not.
     * <p>
     * This base class method returns the same property as returned by the
     * {@link #getProperty()} method. This method may be overwritten by
     * implementations as appropriate.
     *
     * @see #isExpired()
     */
    protected Property getExpiryProperty() {
        return getProperty();
    }

    /**
     * Returns <code>true</code> if the last modification date of the expiry
     * property of this resource is loaded is later than the time at which this
     * resource has been loaded. If the last modification time of the expiry
     * property cannot be calculated or if an error occurrs checking the expiry
     * propertiy's last modification time, <code>true</code> is returned.
     */
    public boolean isExpired() {
        if (!expired) {
            // creation time of version if loaded now
            long currentPropTime = 0;
            Property prop = getExpiryProperty();
            if (prop != null) {
                try {
                    currentPropTime = Util.getLastModificationTime(prop);
                } catch (RepositoryException re) {
                    // cannot get last modif time from property, use current time
                    log.debug("expireResource: Cannot get current version for "
                        + toString() + ", will expire", re);
                    currentPropTime = System.currentTimeMillis();
                }
            }

            // creation time of version currently loaded
            long loadTime = getLoadTime();

            // expire if a new version would be loaded
            expired = currentPropTime > loadTime;
            if (expired && log.isDebugEnabled()) {
                log.debug(
                    "expireResource: Resource created {} superceded by version created {}",
                    new Date(loadTime), new Date(currentPropTime));
            }
        }

        return expired;
    }

    /**
     * Returns the class which was loaded through this resource. It is expected
     * that the class loader sets the class which was loaded through this
     * resource by calling the {@link #setLoadedClass(Class)} method. If this
     * class was not used to load a class or if the class loader failed to
     * set the class loaded through this resoource, this method will return
     * <code>null</code>.
     *
     * @return The class loaded through this resource, which may be
     *      <code>null</code> if this resource was never used to load a class
     *      or if the loader failed to set class through the
     *      {@link #setLoadedClass(Class)} method.
     *
     * @see #setLoadedClass(Class)
     */
    public Class getLoadedClass() {
        return loadedClass;
    }

    /**
     * Sets the class which was loaded through this resource. This method does
     * not check, whether it is plausible that this class was actually loaded
     * from this resource, nor does this method check whether the class object
     * is <code>null</code> or not.
     *
     * @param loadedClass The class to be loaded.
     */
    public void setLoadedClass(Class loadedClass) {
        this.loadedClass = loadedClass;
    }

    /**
     * Returns the <code>String</code> representation of this resource.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName());
        buf.append(": path=");
        buf.append(getSafePath());
        buf.append(", name=");
        buf.append(getName());
        return buf.toString();
    }
}
