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

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>DirectoryClassPathEntry</code> implements the
 * {@link ClassPathEntry} abstract class with support for directory like
 * class path access. The path used to construct the instance is the
 * root path of a page structure finally containing the classes and
 * resources.
 *
 * @author Felix Meschberger
 */
class DirectoryClassPathEntry extends ClassPathEntry {

    /** Default log */
    private static final Logger log =
        LoggerFactory.getLogger(DirectoryClassPathEntry.class);

    /**
     * Creates an instance of the <code>DirectoryClassPathEntry</code> class.
     * <p>
     * The path given is expected to have a trailing slash character else
     * results will not be as expected when getting resources.
     *
     * @param session The <code>Ticket</code> to access the ContentBus.
     * @param path The path of the class path entry, which is the path
     *      of the root of a hierarchy to look up resources in.
     */
    DirectoryClassPathEntry(Session ticket, String handle) {
        super(ticket, handle);
    }

    /**
     * Returns a {@link ClassLoaderResource} for the named resource if it
     * can befound below this directory root identified by the path given
     * at construction time. Note that if the page would exist but does
     * either not contain content or is not readable by the current session,
     * no resource is returned.
     *
     * @param name The name of the resource to return. If the resource would
     *      be a class the name must already be modified to denote a valid
     *      path, that is dots replaced by dashes and the <code>.class</code>
     *      extension attached.
     *
     * @return The {@link ClassLoaderResource} identified by the name or
     *      <code>null</code> if no resource is found for that name.
     */
    public ClassLoaderResource getResource(final String name) {

        try {
            final Property prop = Util.getProperty(session.getItem(path + name));
            if (prop != null) {
                return new ClassLoaderResource(this, name, prop);
            }

            log.debug("getResource: resource {} not found below {} ", name,
                path);

        } catch (PathNotFoundException pnfe) {

            log.debug("getResource: Classpath entry {} does not have resource {}",
                path, name);

        } catch (RepositoryException cbe) {

            log.warn("getResource: problem accessing the resource {} below {}",
                new Object[] { name, path }, cbe);

        }
        // invariant : no page or problem accessing the page

        return null;
    }

    /**
     * Returns a <code>ClassPathEntry</code> with the same configuration as
     * this <code>ClassPathEntry</code>.
     * <p>
     * Becase the <code>DirectoryClassPathEntry</code> class does not have
     * internal state, this method returns this instance to be used as
     * the "copy".
     */
    ClassPathEntry copy() {
        return this;
    }
}
