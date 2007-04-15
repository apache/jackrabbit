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

import java.beans.Introspector;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.net.JCRURLConnection;
import org.apache.jackrabbit.net.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>RepositoryClassLoader</code> class extends the
 * <code>URLClassLoader</code> and provides the functionality to load classes
 * and resources from JCR Repository.
 * <p>
 * This class loader supports loading classes from the Repository hierarchy,
 * such as a <em>classes</em> 'folder', but also from Jar and Zip files stored
 * in the Repository.
 * <p>
 * For enhanced performance, this class loader keeps a list of resources and
 * classes which have already been loaded through this class loader. If later
 * requests ask for already cached resources, these are returned without
 * checking whether the underlying repository actually still exists.
 * <p>
 * This class is not intended to be extended by clients.
 *
 * @author Felix Meschberger
 */
public class RepositoryClassLoader extends URLClassLoader {

    /** default log category */
    private static final Logger log =
        LoggerFactory.getLogger(RepositoryClassLoader.class);

    /** An empty list of url paths to call superclass constructor */
    private static final URL[] NULL_PATH = {};

    /**
     * The special resource representing a resource which could not be
     * found in the class path.
     *
     * @see #cache
     * @see #findClassLoaderResource(String)
     */
    /* package */ static final ClassLoaderResource NOT_FOUND_RESOURCE =
        new ClassLoaderResource(null, "[sentinel]", null) {
            public boolean isExpired() {
                return false;
            }
        };

    /**
     * The classpath which this classloader searches for class definitions.
     * Each element of the vector should be either a directory, a .zip
     * file, or a .jar file.
     * <p>
     * It may be empty when only system classes are controlled.
     */
    private ClassPathEntry[] repository;

    /**
     * The list of handles to use as a classpath. These is the unprocessed
     * list of handles given to the constructor.
     */
    private PatternPath handles;

    /**
     * The <code>Session</code> grants access to the Repository to access the
     * resources.
     * <p>
     * This field is not final such that it may be cleared when the class loader
     * is destroyed.
     */
    private Session session;

    /**
     * Cache of resources found or not found in the class path. The map is
     * indexed by resource name and contains mappings to instances of the
     * {@link ClassLoaderResource} class. If a resource has been tried to be
     * loaded, which could not be found, the resource is cached with the
     * special mapping to {@link #NOT_FOUND_RESOURCE}.
     *
     * @see #NOT_FOUND_RESOURCE
     * @see #findClassLoaderResource(String)
     */
    private Map cache;

    /**
     * Flag indicating whether the {@link #destroy()} method has already been
     * called (<code>true</code>) or not (<code>false</code>)
     */
    private boolean destroyed;

    /**
     * Creates a <code>RepositoryClassLoader</code> from a list of item path
     * strings containing globbing pattens for the paths defining the class
     * path.
     *
     * @param session The <code>Session</code> to use to access the class items.
     * @param classPath The list of path strings making up the (initial) class
     *      path of this class loader. The strings may contain globbing
     *      characters which will be resolved to build the actual class path.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *            <code>null</code>.
     *
     * @throws NullPointerException if either the session or the handles list is
     *             <code>null</code>.
     */
    public RepositoryClassLoader(Session session, String[] classPath,
        ClassLoader parent) {
        this(session, new DynamicPatternPath(session, classPath), parent);
    }

    /**
     * Creates a <code>RepositoryClassLoader</code> from a
     * {@link PatternPath} containing globbing pattens for the handles
     * defining the class path.
     *
     * @param session The <code>Session</code> to use to access the class items.
     * @param handles The {@link PatternPath} of handles.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *            <code>null</code>.
     *
     * @throws NullPointerException if either the session or the handles list is
     *             <code>null</code>.
     */
    /* package */ RepositoryClassLoader(Session session, PatternPath handles,
            ClassLoader parent) {

        // initialize the super class with an empty class path
        super(NULL_PATH, parent);

        // check session and handles
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (handles == null) {
            throw new NullPointerException("handles");
        }

        // set fields
        this.session = session;
        this.setHandles(handles);
        this.cache = new HashMap();
        this.destroyed = false;

        // build the class repositories list
        buildRepository();

        log.debug("RepositoryClassLoader: {} ready", this);
    }

    /**
     * Returns <code>true</code> if this class loader has already been destroyed
     * by calling {@link #destroy()}.
     */
    protected boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Destroys this class loader. This process encompasses all steps needed
     * to remove as much references to this class loader as possible.
     * <p>
     * <em>NOTE</em>: This method just clears all internal fields and especially
     * the class path to render this class loader unusable.
     * <p>
     * This implementation does not throw any exceptions.
     */
    public void destroy() {
        // we expect to be called only once, so we stop destroyal here
        if (isDestroyed()) {
            log.debug("Instance is already destroyed");
            return;
        }

        // set destroyal guard
        destroyed = true;

        // clear caches and references
        setRepository(null);
        setHandles(null);
        session = null;

        // clear the cache of loaded resources and flush cached class
        // introspections of the JavaBean framework
        if (cache != null) {
            for (Iterator ci=cache.values().iterator(); ci.hasNext(); ) {
                ClassLoaderResource res = (ClassLoaderResource) ci.next();
                if (res.getLoadedClass() != null) {
                    Introspector.flushFromCaches(res.getLoadedClass());
                    res.setLoadedClass(null);
                }
                ci.remove();
            }
        }
    }

    //---------- URLClassLoader overwrites -------------------------------------

    /**
     * Finds and loads the class with the specified name from the class path.
     *
     * @param name the name of the class
     * @return the resulting class
     *
     * @throws ClassNotFoundException If the named class could not be found or
     *      if this class loader has already been destroyed.
     */
    protected Class findClass(final String name) throws ClassNotFoundException {

        if (isDestroyed()) {
            throw new ClassNotFoundException(name + " (Classloader destroyed)");
        }

        log.debug("findClass: Try to find class {}", name);

        try {
            return (Class) AccessController
                .doPrivileged(new PrivilegedExceptionAction() {

                    public Object run() throws ClassNotFoundException {
                        return findClassPrivileged(name);
                    }
                });
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    /**
     * Finds the resource with the specified name on the search path.
     *
     * @param name the name of the resource
     *
     * @return a <code>URL</code> for the resource, or <code>null</code>
     *      if the resource could not be found or if the class loader has
     *      already been destroyed.
     */
    public URL findResource(String name) {

        if (isDestroyed()) {
            log.warn("Destroyed class loader cannot find a resource");
            return null;
        }

        log.debug("findResource: Try to find resource {}", name);

        ClassLoaderResource res = findClassLoaderResource(name);
        if (res != null) {
            log.debug("findResource: Getting resource from {}, created {}",
                res, new Date(res.getLastModificationTime()));
            return res.getURL();
        }

        return null;
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * on the search path having the specified name.
     *
     * @param name the resource name
     *
     * @return an <code>Enumeration</code> of <code>URL</code>s. This is an
     *      empty enumeration if no resources are found by this class loader
     *      or if this class loader has already been destroyed.
     */
    public Enumeration findResources(String name) {

        if (isDestroyed()) {
            log.warn("Destroyed class loader cannot find resources");
            return new Enumeration() {
                public boolean hasMoreElements() {
                    return false;
                }
                public Object nextElement() {
                    throw new NoSuchElementException("No Entries");
                }
            };
        }

        log.debug("findResources: Try to find resources for {}", name);

        List list = new LinkedList();
        for (int i=0; i < repository.length; i++) {
            final ClassPathEntry cp = repository[i];
            log.debug("findResources: Trying {}", cp);

            ClassLoaderResource res = cp.getResource(name);
            if (res != null) {
                log.debug("findResources: Adding resource from {}, created {}",
                    res, new Date(res.getLastModificationTime()));
                URL url = res.getURL();
                if (url != null) {
                    list.add(url);
                }
            }

        }

        // return the enumeration on the list
        return Collections.enumeration(list);
    }

    /**
     * Returns the search path of URLs for loading classes and resources.
     * This includes the original list of URLs specified to the constructor,
     * along with any URLs subsequently appended by the {@link #addURL(URL)}
     * and {@link #addHandle(String)} methods.
     *
     * @return the search path of URLs for loading classes and resources. The
     *      list is empty, if this class loader has already been destroyed.
     */
    public URL[] getURLs() {
        if (isDestroyed()) {
            log.warn("Destroyed class loader has no URLs any more");
            return new URL[0];
        }

        List urls = new ArrayList();
        for (int i=0; i < repository.length; i++) {
            URL url = repository[i].toURL();
            if (url != null) {
                urls.add(url);
            }
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources. Only Repository URLs with the protocol set to
     * <code>JCR</code> are considered for addition. The system will find out
     * whether the URL points to a directory or a jar archive.
     * <p>
     * URLs added using this method will be preserved through reconfiguration
     * and reinstantiation.
     * <p>
     * If this class loader has already been destroyed this method has no
     * effect.
     *
     * @param url the <code>JCR</code> URL to be added to the search path of
     *      URLs.
     */
    protected void addURL(URL url) {
        if (isDestroyed()) {
            log.warn("Cannot add URL to destroyed class loader");

        } else if (checkURL(url)) {
            // Repository URL
            log.debug("addURL: Adding URL {}", url);
            try {
                JCRURLConnection conn = (JCRURLConnection) url.openConnection();
                ClassPathEntry cp = ClassPathEntry.getInstance(
                    conn.getSession(), conn.getPath());
                addClassPathEntry(cp);
            } catch (IOException ioe) {
                log.warn("addURL: Cannot add URL " + url, ioe);
            }

        } else {
            log.warn("addURL: {} is not a Repository URL, ignored", url);
        }
    }

    /**
     * Appends the specified path to the list of handles to search for classes
     * and resources. The system will find out whether the path points to a
     * directory or a JAR or ZIP archive. The path is added as is, provided it
     * is valid to be used in the class path and therefore must not contain any
     * globbing characters.
     * <p>
     * If this class loader has already been destroyed, this method has no
     * effect.
     *
     * @param path The path to be added to the search path.
     */
    public void addHandle(String path) {
        if (isDestroyed()) {
            log.warn("Cannot add handle to destroyed class loader");
            return;
        }

        log.debug("addURL: Adding Handle {}", path);
        ClassPathEntry cp = ClassPathEntry.getInstance(session, path);
        if (cp != null) {
            addClassPathEntry(cp);
        } else {
            log.debug("addHandle: Cannot get a ClassPathEntry for {}", path);
        }
    }

    //---------- Property access ----------------------------------------------

    /**
     * Sets the {@link PatternPath} list to be used as the initial search
     * path of this class loader. This new list replaces the path pattern list
     * set in the constructor or in a previous call to this method.
     * <p>
     * After setting the list, this class loader's class path has to be rebuilt
     * by calling the {@link #buildRepository()} method.
     *
     * @param handles The {@link PatternPath} to set on this class loader.
     */
    /* package */ void setHandles(PatternPath handles) {
        this.handles = handles;
    }

    /**
     * Returns the current {@link PatternPath} from which the search path
     * of this class loader is configured.
     */
    /* package */ PatternPath getHandles() {
        return handles;
    }

    /**
     * Returns the named {@link ClassLoaderResource} if it is contained in the
     * cache. If the resource does not exist in the cache or has not been found
     * in the class path at an earlier point in time, <code>null</code> is
     * returned.
     *
     * @param name The name of the resource to retrieve from the cache.
     *
     * @return The named <code>ClassLoaderResource</code> or <code>null</code>
     *      if not loaded.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    /* package */ ClassLoaderResource getCachedResource(String name) {
        Object res = cache.get(name);
        if (res == null || res == NOT_FOUND_RESOURCE) {
            log.debug("Resource {} not cached", name);
            return null;
        }

        return (ClassLoaderResource) res;
    }

    /**
     * Returns an <code>Iterator</code> on all resources in the cache. This
     * iterator may also contain {@link #NOT_FOUND_RESOURCE sentinel} entries
     * for resources, which failed to load. Callers of this method should take
     * care to filter out such resources before acting on it.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    /* package */ Iterator getCachedResources() {
        return cache.values().iterator();
    }

    /**
     * Removes all entries from the cache of loaded resources, which mark
     * resources, which have not been found as of yet.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    protected void cleanCache() {
        for (Iterator ci=cache.values().iterator(); ci.hasNext(); ) {
            if (ci.next() == NOT_FOUND_RESOURCE) {
                ci.remove();
            }
        }
    }

    /**
     * Returns <code>true</code>, if the cache is not empty. If the
     * {@link #cleanCache()} method is not called before calling this method, a
     * false positive result may be returned.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    protected boolean hasLoadedResources() {
        return cache.isEmpty();
    }

    /**
     * Returns the session used by this class loader to access the repository.
     * If this class loader has already been destroyed, this <code>null</code>
     * is returned.
     */
    protected Session getSession() {
        return session;
    }

    /**
     * Sets the current active class path to the list of class path entries.
     */
    protected void setRepository(ClassPathEntry[] classPath) {
        this.repository = classPath;
    }

    /**
     * Returns the current active class path entries list or <code>null</code>
     * if this class loader has already been destroyed.
     */
    protected ClassPathEntry[] getRepository() {
        return repository;
    }

    /**
     * Adds the class path entry to the current class path list. If the class
     * loader has already been destroyed, this method creates a single entry
     * class path list with the new class path entry.
     */
    protected void addClassPathEntry(ClassPathEntry cpe) {
        log.debug("addHandle: Adding path {}", cpe.getPath());

        // append the entry to the current class path
        ClassPathEntry[] oldClassPath = getRepository();
        ClassPathEntry[] newClassPath = addClassPathEntry(oldClassPath, cpe);
        setRepository(newClassPath);
    }

    /**
     * Helper method for class path handling to a new entry to an existing
     * list and return the new list.
     * <p>
     * If <code>list</code> is <code>null</code> a new array is returned with
     * a single element <code>newEntry</code>. Otherwise the array returned
     * contains all elements of <code>list</code> and <code>newEntry</code>
     * at the last position.
     *
     * @param list The array of class path entries, to which a new entry is
     *      to be appended. This may be <code>null</code>.
     * @param newEntry The new entry to append to the class path list.
     *
     * @return The extended class path list.
     */
    protected ClassPathEntry[] addClassPathEntry(ClassPathEntry[] list,
            ClassPathEntry newEntry) {

        // quickly define single entry array for the first entry
        if (list == null) {
            return new ClassPathEntry[]{ newEntry };
        }

        // create new array and copy old and new contents
        ClassPathEntry[] newList = new ClassPathEntry[list.length+1];
        System.arraycopy(list, 0, newList, 0, list.length);
        newList[list.length] = newEntry;
        return newList;
    }

    //---------- Object overwrite ---------------------------------------------

    /**
     * Returns a string representation of this instance.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName());

        if (isDestroyed()) {
            buf.append(" - destroyed");
        } else {
            buf.append(": parent: { ");
            buf.append(getParent());
            buf.append(" }, user: ");
            buf.append(session.getUserID());
        }

        return buf.toString();
    }

    //---------- internal ------------------------------------------------------

    /**
     * Builds the repository list from the list of path patterns and appends
     * the path entries from any added handles. This method may be used multiple
     * times, each time replacing the currently defined repository list.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    protected synchronized void buildRepository() {

        // build new repository
        List handles;
        try {
            handles = getHandles().getExpandedPaths();
        } catch (RepositoryException re) {
            log.error("Cannot expand handle list", re);
            return;
        }

        List newRepository = new ArrayList(handles.size());

        // build repository from path patterns
        for (int i=0; i < handles.size(); i++) {
            String entry = (String) handles.get(i);
            ClassPathEntry cp = null;

            // try to find repository based on this path
            if (getRepository() != null) {
                for (int j=0; j < repository.length; j++) {
                    ClassPathEntry tmp = repository[i];
                    if (tmp.getPath().equals(entry)) {
                        cp = tmp;
                        break;
                    }
                }
            }

            // not found, creating new one
            if (cp == null) {
                cp = ClassPathEntry.getInstance(session, entry);
            }

            if (cp != null) {
                log.debug("Adding path {}", entry);
                newRepository.add(cp);
            } else {
                log.debug("Cannot get a ClassPathEntry for {}", entry);
            }
        }

        // replace old repository with new one
        ClassPathEntry[] newClassPath = new ClassPathEntry[newRepository.size()];
        newRepository.toArray(newClassPath);
        setRepository(newClassPath);

        // clear un-found resource cache
        cleanCache();
    }

    /**
     * Tries to find the class in the class path from within a
     * <code>PrivilegedAction</code>. Throws <code>ClassNotFoundException</code>
     * if no class can be found for the name.
     *
     * @param name the name of the class
     *
     * @return the resulting class
     *
     * @throws ClassNotFoundException if the class could not be found
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private Class findClassPrivileged(String name) throws ClassNotFoundException {

        // prepare the name of the class
        final String path = name.replace('.', '/').concat(".class");
        log.debug("findClassPrivileged: Try to find path {} for class {}",
            path, name);

        ClassLoaderResource res = findClassLoaderResource(path);
        if (res != null) {

             // try defining the class, error aborts
             try {
                 log.debug(
                    "findClassPrivileged: Loading class from {}, created {}",
                    res, new Date(res.getLastModificationTime()));

                 Class c = defineClass(name, res);
                 if (c == null) {
                     log.warn("defineClass returned null for class {}", name);
                     throw new ClassNotFoundException(name);
                 }
                 return c;

             } catch (IOException ioe) {
                 log.debug("defineClass failed", ioe);
                 throw new ClassNotFoundException(name, ioe);
             } catch (Throwable t) {
                 log.debug("defineClass failed", t);
                 throw new ClassNotFoundException(name, t);
             }
         }

        throw new ClassNotFoundException(name);
     }

    /**
     * Returns a {@link ClassLoaderResource} for the given <code>name</code> or
     * <code>null</code> if not existing. If the resource has already been
     * loaded earlier, the cached instance is returned. If the resource has
     * not been found in an earlier call to this method, <code>null</code> is
     * returned. Otherwise the resource is looked up in the class path. If
     * found, the resource is cached and returned. If not found, the
     * {@link #NOT_FOUND_RESOURCE} is cached for the name and <code>null</code>
     * is returned.
     *
     * @param name The name of the resource to return.
     *
     * @return The named <code>ClassLoaderResource</code> if found or
     *      <code>null</code> if not found.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    /* package */ ClassLoaderResource findClassLoaderResource(String name) {

        // check for cached resources first
        ClassLoaderResource res = (ClassLoaderResource) cache.get(name);
        if (res == NOT_FOUND_RESOURCE) {
            log.debug("Resource '{}' known to not exist in class path", name);
            return null;
        } else if (res != null) {
            return res;
        }

        // walk the repository list and try to find the resource
        for (int i = 0; i < repository.length; i++) {
            final ClassPathEntry cp = repository[i];
            log.debug("Checking {}", cp);

            res = cp.getResource(name);
            if (res != null) {
                log.debug("Found resource in {}, created ", res, new Date(
                    res.getLastModificationTime()));
                cache.put(name, res);
                return res;
            }

        }

        log.debug("No classpath entry contains {}", name);
        cache.put(name, NOT_FOUND_RESOURCE);
        return null;
    }

    /**
     * Defines a class getting the bytes for the class from the resource
     *
     * @param name The fully qualified class name
     * @param res The resource to obtain the class bytes from
     *
     * @throws RepositoryException If a problem occurrs getting at the data.
     * @throws IOException If a problem occurrs reading the class bytes from
     *      the resource.
     * @throws ClassFormatError If the class bytes read from the resource are
     *      not a valid class.
     */
    private Class defineClass(String name, ClassLoaderResource res)
            throws IOException, RepositoryException {

        log.debug("defineClass({}, {})", name, res);

        Class clazz = res.getLoadedClass();
        if (clazz == null) {

            /**
             * This following code for packages is duplicate from URLClassLoader
             * because it is private there. I would like to not be forced to
             * do this, but I still have to find a way ... -fmeschbe
             */

            // package support
            int i = name.lastIndexOf('.');
            if (i != -1) {
                String pkgname = name.substring(0, i);
                // Check if package already loaded.
                Package pkg = getPackage(pkgname);
                URL url = res.getCodeSourceURL();
                Manifest man = res.getManifest();
                if (pkg != null) {
                    // Package found, so check package sealing.
                    boolean ok;
                    if (pkg.isSealed()) {
                        // Verify that code source URL is the same.
                        ok = pkg.isSealed(url);
                    } else {
                        // Make sure we are not attempting to seal the package
                        // at this code source URL.
                        ok = (man == null) || !isSealed(pkgname, man);
                    }
                    if (!ok) {
                        throw new SecurityException("sealing violation");
                    }
                } else {
                    if (man != null) {
                        definePackage(pkgname, man, url);
                    } else {
                        definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                }
            }

            byte[] data = res.getBytes();
            clazz = defineClass(name, data, 0, data.length);
            res.setLoadedClass(clazz);
        }

        return clazz;
    }

    /**
     * Returns true if the specified package name is sealed according to the
     * given manifest
     * <p>
     * This code is duplicate from <code>URLClassLoader.isSealed</code> because
     * the latter has private access and we need the method here.
     */
    private boolean isSealed(String name, Manifest man) {
         String path = name.replace('.', '/').concat("/");
         Attributes attr = man.getAttributes(path);
         String sealed = null;
         if (attr != null) {
             sealed = attr.getValue(Attributes.Name.SEALED);
         }
         if (sealed == null) {
             if ((attr = man.getMainAttributes()) != null) {
                 sealed = attr.getValue(Attributes.Name.SEALED);
             }
         }
         return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Returns <code>true</code> if the <code>url</code> is a <code>JCR</code>
     * URL.
     *
     * @param url The URL to check whether it is a valid <code>JCR</code> URL.
     *
     * @return <code>true</code> if <code>url</code> is a valid <code>JCR</code>
     *      URL.
     *
     * @throws NullPointerException if <code>url</code> is <code>null</code>.
     */
    private boolean checkURL(URL url) {
        return URLFactory.REPOSITORY_SCHEME.equalsIgnoreCase(url.getProtocol());
    }
}
