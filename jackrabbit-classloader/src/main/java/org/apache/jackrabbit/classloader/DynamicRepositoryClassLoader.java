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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.classloader.DynamicPatternPath.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>DynamicRepositoryClassLoader</code> class extends the
 * {@link org.apache.jackrabbit.classloader.RepositoryClassLoader} and provides the
 * functionality to load classes and resources from the JCR Repository.
 * Additionally, this class supports the notion of getting 'dirty', which means,
 * that if a resource loaded through this class loader has been modified in the
 * Repository, this class loader marks itself dirty, which flag can get
 * retrieved. This helps the user of this class loader to decide on whether to
 * {@link #reinstantiate(Session, ClassLoader) reinstantiate} it or continue
 * using this class loader.
 * <p>
 * When a user of the class loader recognizes an instance to be dirty, it can
 * easily be reinstantiated with the {@link #reinstantiate} method. This
 * reinstantiation will also rebuild the internal real class path from the same
 * list of path patterns as was used to create the internal class path for the
 * original class loader. The resulting internal class path need not be the
 * same, though.
 * <p>
 * As an additional feature the class loaders provides the functionality for
 * complete reconfiguration of the list of path patterns defined at class loader
 * construction time through the {@link #reconfigure(String[])} method. This
 * reconfiguration replaces the internal class path with a new one built from
 * the new path list and also replaces that path list. Reinstantiating a
 * reconfigured class loader gets a class loader containing the same path list
 * as the original class loader had after reconfiguration. That is the original
 * configuration is lost. While reconfiguration is not able to throw away
 * classes already loaded, it will nevertheless mark the class loader dirty, if
 * any classes have already been loaded through it.
 * <p>
 * This class is not intended to be extended by clients.
 *
 * @author Felix Meschberger
 */
public class DynamicRepositoryClassLoader extends RepositoryClassLoader
        implements EventListener, Listener {

    /** default log category */
    private static final Logger log =
        LoggerFactory.getLogger(DynamicRepositoryClassLoader.class);

    /**
     * Cache of resources used to check class loader expiry. The map is indexed
     * by the paths of the expiry properties of the cached resources. This map
     * is not complete in terms of resources which have been loaded through this
     * class loader. That is for resources loaded through an archive class path
     * entry, only one of those resources (the last one loaded) is kept in this
     * cache, while the others are ignored.
     *
     * @see #onEvent(EventIterator)
     * @see #findClassLoaderResource(String)
     */
    private Map modTimeCache;

    /**
     * Flag indicating whether there are loaded classes which have later been
     * expired (e.g. invalidated or modified)
     */
    private boolean dirty;

    /**
     * The list of repositories added through either the {@link #addURL} or the
     * {@link #addHandle} method.
     */
    private ClassPathEntry[] addedRepositories;

    /**
     * Creates a <code>DynamicRepositoryClassLoader</code> from a list of item
     * path strings containing globbing pattens for the paths defining the
     * class path.
     *
     * @param session The <code>Session</code> to use to access the class items.
     * @param classPath The list of path strings making up the (initial) class
     *      path of this class loader. The strings may contain globbing
     *      characters which will be resolved to build the actual class path.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *      <code>null</code>.
     *
     * @throws NullPointerException if either the session or the handles list
     *      is <code>null</code>.
     */
    public DynamicRepositoryClassLoader(Session session,
            String[] classPath, ClassLoader parent) {

        // initialize the super class with an empty class path
        super(session, new DynamicPatternPath(session, classPath), parent);

        // set fields
        dirty = false;
        modTimeCache = new HashMap();

        // register with observation service and path pattern list
        registerModificationListener();

        log.debug("DynamicRepositoryClassLoader: {} ready", this);
    }

    /**
     * Creates a <code>DynamicRepositoryClassLoader</code> with the same
     * configuration as the given <code>DynamicRepositoryClassLoader</code>.
     * This constructor is used by the {@link #reinstantiate} method.
     * <p>
     * Before returning from this constructor the <code>old</code> class loader
     * is destroyed and may not be used any more.
     *
     * @param session The session to associate with this class loader.
     * @param old The <code>DynamicRepositoryClassLoader</code> to copy the
     *            cofiguration from.
     * @param parent The parent <code>ClassLoader</code>, which may be
     *            <code>null</code>.
     */
    private DynamicRepositoryClassLoader(Session session,
            DynamicRepositoryClassLoader old, ClassLoader parent) {

        // initialize the super class with an empty class path
        super(session, old.getHandles(), parent);

        // set the configuration and fields
        dirty = false;
        modTimeCache = new HashMap();

        // create a repository from the handles - might get a different one
        setRepository(resetClassPathEntries(old.getRepository()));
        setAddedRepositories(resetClassPathEntries(old.getAddedRepositories()));
        buildRepository();

        // register with observation service and path pattern list
        registerModificationListener();

        // finally finalize the old class loader
        old.destroy();

        log.debug(
            "DynamicRepositoryClassLoader: Copied {}. Do not use that anymore",
            old);
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

        // remove ourselves as listeners from other places
        unregisterListener();

        addedRepositories = null;

        super.destroy();
    }

    //---------- reload support ------------------------------------------------

    /**
     * Checks whether this class loader already loaded the named resource and
     * would load another version if it were instructed to do so. As a side
     * effect the class loader sets itself dirty in this case.
     * <p>
     * Calling this method yields the same result as calling
     * {@link #shouldReload(String, boolean)} with the <code>force</code>
     * argument set to <code>false</code>.
     *
     * @param name The name of the resource to check.
     *
     * @return <code>true</code> if the resource is loaded and reloading would
     *      take another version than currently loaded.
     *
     * @see #isDirty
     */
    public synchronized boolean shouldReload(String name) {
        return shouldReload(name, false);
    }

    /**
     * Checks whether this class loader already loaded the named resource and
     * whether the class loader should be set dirty depending on the
     * <code>force</code> argument. If the argument is <code>true</code>, the
     * class loader is marked dirty and <code>true</code> is returned if the
     * resource has been loaded, else the loaded resource is checked for expiry
     * and the class loader is only set dirty if the loaded resource has
     * expired.
     *
     * @param name The name of the resource to check.
     * @param force <code>true</code> if the class loader should be marked dirty
     *      if the resource is loaded, else the class loader is only marked
     *      dirty if the resource is loaded and has expired.
     *
     * @return <code>true</code> if the resource is loaded and
     *      <code>force</code> is <code>true</code> or if the resource has
     *      expired. <code>true</code> is also returned if this class loader
     *      has already been destroyed.
     *
     * @see #isDirty
     */
    public synchronized boolean shouldReload(String name, boolean force) {
        if (isDestroyed()) {
            log.warn("Classloader already destroyed, reload required");
            return true;
        }

        ClassLoaderResource res = getCachedResource(name);
        if (res != null) {
            log.debug("shouldReload: Expiring cache entry {}", res);
            if (force) {
                log.debug("shouldReload: Forced dirty flag");
                dirty = true;
                return true;
            }

            return expireResource(res);
        }

        return false;
    }

    /**
     * Returns <code>true</code> if any of the loaded classes need reload. Also
     * sets this class loader dirty. If the class loader is already set dirty
     * or if this class loader has been destroyed before calling this method,
     * it returns immediately.
     *
     * @return <code>true</code> if any class loader needs to be reinstantiated.
     *
     * @see #isDirty
     */
    public synchronized boolean shouldReload() {

        // check whether we are already dirty
        if (isDirty()) {
            log.debug("shouldReload: Dirty, need reload");
            return true;
        }

        // Check whether any class has changed
        for (Iterator iter = getCachedResources(); iter.hasNext();) {
            if (expireResource((ClassLoaderResource) iter.next())) {
                log.debug("shouldReload: Found expired resource, need reload");
                return true;
            }
        }

        // No changes, no need to reload
        log.debug("shouldReload: No expired resource found, no need to reload");
        return false;
    }

    /**
     * Returns whether the class loader is dirty. This can be the case if any
     * of the {@link #shouldReload(String)} or {@link #shouldReload()}
     * methods returned <code>true</code> or if a loaded class has been expired
     * through the observation.
     * <p>
     * This method may also return <code>true</code> if the <code>Session</code>
     * associated with this class loader is not valid anymore.
     * <p>
     * Finally the method always returns <code>true</code> if the class loader
     * has already been destroyed. Note, however, that a destroyed class loader
     * cannot be reinstantiated. See {@link #reinstantiate(Session, ClassLoader)}.
     * <p>
     * If the class loader is dirty, it should be reinstantiated through the
     * {@link #reinstantiate} method.
     *
     * @return <code>true</code> if the class loader is dirty and needs
     *      reinstantiation.
     */
    public boolean isDirty() {
        return isDestroyed() || dirty || !getSession().isLive();
    }

    /**
     * Reinstantiates this class loader. That is, a new ClassLoader with no
     * loaded class is created with the same configuration as this class loader.
     * <p>
     * When the new class loader is returned, this class loader has been
     * destroyed and may not be used any more.
     *
     * @param parent The parent <code>ClassLoader</code> for the reinstantiated
     * 	    <code>DynamicRepositoryClassLoader</code>, which may be
     *      <code>null</code>.
     *
     * @return a new instance with the same configuration as this class loader.
     *
     * @throws IllegalStateException if <code>this</code>
     *      {@link DynamicRepositoryClassLoader} has already been destroyed
     *      through the {@link #destroy()} method.
     */
    public DynamicRepositoryClassLoader reinstantiate(Session session, ClassLoader parent) {
        log.debug("reinstantiate: Copying {} with parent {}", this, parent);

        if (isDestroyed()) {
            throw new IllegalStateException("Destroyed class loader cannot be recreated");
        }

        // create the new loader
        DynamicRepositoryClassLoader newLoader =
                new DynamicRepositoryClassLoader(session, this, parent);

        // return the new loader
        return newLoader;
    }

    //---------- URLClassLoader overwrites -------------------------------------

    /**
     * Reconfigures this class loader with the pattern list. That is the new
     * pattern list completely replaces the current pattern list. This new
     * pattern list will also be used later to configure the reinstantiated
     * class loader.
     * <p>
     * If this class loader already has loaded classes using the old, replaced
     * path list, it is set dirty.
     * <p>
     * If this class loader has already been destroyed, this method has no
     * effect.
     *
     * @param classPath The list of path strings making up the (initial) class
     *      path of this class loader. The strings may contain globbing
     *      characters which will be resolved to build the actual class path.
     */
    public void reconfigure(String[] classPath) {
        if (log.isDebugEnabled()) {
            log.debug("reconfigure: Reconfiguring the with {}",
                Arrays.asList(classPath));
        }

        // whether the loader is destroyed
        if (isDestroyed()) {
            log.warn("Cannot reconfigure this destroyed class loader");
            return;
        }

        // deregister to old handles
        ((DynamicPatternPath) getHandles()).removeListener(this);

        // assign new handles and register
        setHandles(new DynamicPatternPath(getSession(), classPath));
        buildRepository();
        ((DynamicPatternPath) getHandles()).addListener(this);

        dirty = !hasLoadedResources();
        log.debug("reconfigure: Class loader is dirty now: {}", (isDirty()
                ? "yes"
                : "no"));
    }

    //---------- RepositoryClassLoader overwrites -----------------------------

    /**
     * Calls the base class implementation to actually retrieve the resource.
     * If the resource could be found and provides a non-<code>null</code>
     * {@link ClassLoaderResource#getExpiryProperty() expiry property}, the
     * resource is registered with an internal cache to check with when
     * a repository modification is observed in {@link #onEvent(EventIterator)}.
     *
     * @param name The name of the resource to be found
     *
     * @return the {@link ClassLoaderResource} found for the name or
     *      <code>null</code> if no such resource is available in the class
     *      path.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    /* package */ ClassLoaderResource findClassLoaderResource(String name) {
        // call the base class implementation to actually search for it
        ClassLoaderResource res = super.findClassLoaderResource(name);

        // if it could be found, we register it with the caches
        if (res != null) {
            // register the resource in the expiry map, if an appropriate
            // property is available
            Property prop = res.getExpiryProperty();
            if (prop != null) {
                try {
                    modTimeCache.put(prop.getPath(), res);
                } catch (RepositoryException re) {
                    log.warn("Cannot register the resource " + res +
                        " for expiry", re);
                }
            }
        }

        // and finally return the resource
        return res;
    }

    /**
     * Builds the repository list from the list of path patterns and appends
     * the path entries from any added handles. This method may be used multiple
     * times, each time replacing the currently defined repository list.
     *
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    protected synchronized void buildRepository() {
        super.buildRepository();

        // add added repositories
        ClassPathEntry[] addedPath = getAddedRepositories();
        if (addedPath != null && addedPath.length > 0) {
            ClassPathEntry[] oldClassPath = getRepository();
            ClassPathEntry[] newClassPath =
                new ClassPathEntry[oldClassPath.length + addedPath.length];

            System.arraycopy(oldClassPath, 0, newClassPath, 0,
                oldClassPath.length);
            System.arraycopy(addedPath, 0, newClassPath, oldClassPath.length,
                addedPath.length);

            setRepository(newClassPath);
        }
    }

    //---------- ModificationListener interface -------------------------------

    /**
     * Handles a repository item modifcation events checking whether a class
     * needs to be expired. As a side effect, this method sets the class loader
     * dirty if a loaded class has been modified in the repository.
     *
     * @param events The iterator of repository events to be handled.
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            String path;
            try {
                path = event.getPath();
            } catch (RepositoryException re) {
                log.warn("onEvent: Cannot get path of event, ignoring", re);
                continue;
            }

            log.debug(
                "onEvent: Item {} has been modified, checking with cache", path);

            ClassLoaderResource resource = (ClassLoaderResource) modTimeCache.get(path);
            if (resource != null) {
                log.debug("pageModified: Expiring cache entry {}", resource);
                expireResource(resource);
            } else {
                // might be in not-found cache - remove from there
                if (event.getType() == Event.NODE_ADDED
                    || event.getType() == Event.PROPERTY_ADDED) {
                    log.debug("pageModified: Clearing not-found cache for possible new class");
                    cleanCache();
                }
            }

        }
    }

    // ----------- PatternPath.Listener interface -------------------------

    /**
     * Handles modified matched path set by setting the class loader dirty.
     * The internal class path is only rebuilt when the class loader is
     * reinstantiated.
     */
    public void pathChanged() {
        log.debug("handleListChanged: The path list has changed");
        buildRepository();
        dirty = true;
    }

    //----------- Object overwrite ---------------------------------------------

    /**
     * Returns a string representation of this class loader.
     */
    public String toString() {
        if (isDestroyed()) {
            return super.toString();
        }

        StringBuffer buf = new StringBuffer(super.toString());
        buf.append(", dirty: ");
        buf.append(isDirty());
        return buf.toString();
    }

    //---------- internal ------------------------------------------------------

    /**
     * Sets the list of class path entries to add to the class path after
     * reconfiguration or reinstantiation.
     *
     * @param addedRepositories The list of class path entries to keep for
     *      readdition.
     */
    protected void setAddedRepositories(ClassPathEntry[] addedRepositories) {
        this.addedRepositories = addedRepositories;
    }

    /**
     * Returns the list of added class path entries to readd them to the class
     * path after reconfiguring the class loader.
     */
    protected ClassPathEntry[] getAddedRepositories() {
        return addedRepositories;
    }

    /**
     * Adds the class path entry to the current class path list. If the class
     * loader has already been destroyed, this method creates a single entry
     * class path list with the new class path entry.
     * <p>
     * Besides adding the entry to the current class path, it is also added to
     * the list to be readded after reconfiguration and/or reinstantiation.
     *
     * @see #getAddedRepositories()
     * @see #setAddedRepositories(ClassPathEntry[])
     */
    protected void addClassPathEntry(ClassPathEntry cpe) {
        super.addClassPathEntry(cpe);

        // add the repsitory to the list of added repositories
        ClassPathEntry[] oldClassPath = getAddedRepositories();
        ClassPathEntry[] newClassPath = addClassPathEntry(oldClassPath, cpe);
        setAddedRepositories(newClassPath);
    }

    /**
     * Registers this class loader with the observation service to get
     * information on page updates in the class path and to the path
     * pattern list to get class path updates.
     *
     * @throws NullPointerException if this class loader has already been
     *      destroyed.
     */
    private final void registerModificationListener() {
        ((DynamicPatternPath) getHandles()).addListener(this);

        log.debug("registerModificationListener: Registering to the observation service");
        try {
            ObservationManager om = getSession().getWorkspace().getObservationManager();
            om.addEventListener(this, 255, "/", true, null, null, false);
        } catch (RepositoryException re) {
            log.error("registerModificationListener: Cannot register " +
                this + " with observation manager", re);
        }
    }

    /**
     * Removes this instances registrations from the observation service and
     * the path pattern list.
     *
     * @throws NullPointerException if this class loader has already been
     *      destroyed.
     */
    private final void unregisterListener() {
        ((DynamicPatternPath) getHandles()).removeListener(this);

        log.debug("registerModificationListener: Deregistering from the observation service");
        try {
            ObservationManager om = getSession().getWorkspace().getObservationManager();
            om.removeEventListener(this);
        } catch (RepositoryException re) {
            log.error("unregisterListener: Cannot unregister " +
                this + " from observation manager", re);
        }
    }

    /**
     * Checks whether the page backing the resource has been updated with a
     * version, such that this new version would be used to access the resource.
     * In this case the resource has expired and the class loader needs to be
     * set dirty.
     *
     * @param resource The <code>ClassLoaderResource</code> to check for
     *      expiry.
     */
    private boolean expireResource(ClassLoaderResource resource) {

        // check whether the resource is expired (only if a class has been loaded)
        boolean exp = resource.getLoadedClass() != null && resource.isExpired();

        // update dirty flag accordingly
        dirty |= exp;
        log.debug("expireResource: Loader dirty: {}", new Boolean(isDirty()));

        // return the expiry status
        return exp;
    }

    /**
     * Returns the list of classpath entries after resetting each of them.
     *
     * @param list The list of {@link ClassPathEntry}s to reset
     *
     * @return The list of reset {@link ClassPathEntry}s.
     */
    private static ClassPathEntry[] resetClassPathEntries(
            ClassPathEntry[] oldClassPath) {
        if (oldClassPath != null) {
            for (int i=0; i < oldClassPath.length; i++) {
                ClassPathEntry entry = oldClassPath[i];
                log.debug("resetClassPathEntries: Cloning {}", entry);
                oldClassPath[i] = entry.copy();
            }
        } else {
            log.debug("resetClassPathEntries: No list to reset");
        }
        return oldClassPath;
    }
}
