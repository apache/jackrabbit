/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.extension.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;

/**
 * The <code>ConfigurationIODelegate</code> class provides common IO
 * functionality for the
 * {@link org.apache.jackrabbit.extension.configuration.PropertiesNodeConfiguration} and
 * {@link org.apache.jackrabbit.extension.configuration.XMLNodeConfiguration} classes to
 * access configuration Repository Properties to load and save configuration
 * data. In fact, this class may be used to extend any
 * <code>FileConfiguration</code> implementation with support for loading and
 * saveing from/to a JCR repository, not just the above mentioned.
 *
 * @author Felix Meschberger
 */
public class ConfigurationIODelegate {

    /**
     * The <code>FileConfiguration</code> object used to write the
     * configuration.
     */
    private final FileConfiguration config;

    /**
     * The <code>Node</code> from which the configuration is loaded.
     */
    private Node jcrNode;

    /**
     * The default character encoding when serializing strings from/to files
     * (value is "UTF-8").
     */
    /* package */ static final String ENCODING = "UTF-8";

    /**
     * Creates a new instance delegating actual writing of the data to the
     * underlying repository to the given <code>FileConfiguration</code>.
     *
     * @param config The <code>FileConfiguration</code> used for
     *      (de-)serializing the configuration data.
     */
    /* package */ ConfigurationIODelegate(FileConfiguration config) {
        this.config = config;
    }

    /**
     * Returns the repository <code>Node</code> from which the configuration is
     * loaded resp. to which it is stored.
     */
    /* package */ Node getNode() {
        return jcrNode;
    }

    /**
     * Sets the repository <code>Node</code> from which the configuration is
     * loaded resp. to whch it is stored.
     */
    /* package */ void setNode(Node node) {
        this.jcrNode = node;
    }

    /**
     * Calls the {@link #load(Node)} method if a repository <code>Node</code>
     * has been set on this delegate. Otherwise calls the <code>load()</code>
     * method of the <code>FileConfiguration</code> object which has been
     * given to this instance at construction time.
     *
     * @throws ConfigurationException If an error occurrs loading the
     *      configuration.
     */
    public void load() throws ConfigurationException {
        if (jcrNode != null) {
            load(jcrNode);
        } else {
            config.load();
        }
    }

    /**
     * Accesses the configuration property of the given repository
     * <code>Node</code> to open an <code>InputStream</code> and calls the
     * <code>FileConfiguration</code>'s <code>load(InputStream)</code> method
     * to actually load the configuration.
     *
     * @param node The configuration <code>Node</code> from which the
     *      configuration is to be read.
     *
     * @throws ConfigurationException If an error occurrs accessing the
     *      repository or loading the configuration.
     */
    public void load(Node node) throws ConfigurationException {
        InputStream ins = null;
        try {
            Property configProp = getConfigurationProperty(node);
            ins = configProp.getStream();

            config.load(ins);

        } catch (RepositoryException re) {
            throw new ConfigurationException(re);
        } finally {
            tryClose(ins);
        }
    }

    /**
     * Calls the {@link #save(Node)} method if a repository <code>Node</code>
     * has been set on this delegate. Otherwise calls the <code>save()</code>
     * method of the <code>FileConfiguration</code> object which has been
     * given to this instance at construction time.
     *
     * @throws ConfigurationException If an error occurrs saving the
     *      configuration.
     */
    public void save() throws ConfigurationException {
        if (jcrNode != null) {
            save(jcrNode);
        } else {
            config.save();
        }
    }

    /**
     * Calls the <code>save(OutputStream)</code> method of the
     * <code>FileConfiguration</code> to store the configuration data into a
     * temporary file, which is then fed into the configuration property
     * retrieved from the given <code>Node</code>.
     *
     * @param node The configuration <code>Node</code> to which the
     *      configuration is to be saved.
     *
     * @throws ConfigurationException If an error occurrs accessing the
     *      repository or saving the configuration.
     */
    public void save(javax.jcr.Node node) throws ConfigurationException {
        // write the configuration to a temporary file
        OutputStream out = null;
        File tmp = null;
        boolean success = false;
        try {
            tmp = File.createTempFile("srvcfg", ".tmp");
            out = new FileOutputStream(tmp);
            config.save(out);
            success = true;
        } catch (IOException ioe) {
            throw new ConfigurationException(ioe);
        } finally {
            tryClose(out);

            // delete the temp file, if saving failed (--> success == false)
            if (!success && tmp != null) {
                tmp.delete();
            }
        }

        InputStream ins = null;
        try {
            ins = new FileInputStream(tmp);
            Property configProp = getConfigurationProperty(node);

            // create version before update ???
            boolean doCheckIn = false;
            if (configProp.getParent().isNodeType("mix:versionable") &&
                    !configProp.getParent().isCheckedOut()) {
                configProp.getParent().checkout();
                doCheckIn = true;
            }

            configProp.setValue(ins);
            configProp.save();

            if (doCheckIn) {
                configProp.getParent().checkin();
            }

        } catch (IOException ioe) {
            throw new ConfigurationException(ioe);
        } catch (RepositoryException re) {
            throw new ConfigurationException(re);
        } finally {
            tryClose(ins);
            tmp.delete();
        }
    }

    /**
     * Returns the property containing configuration data in the given
     * <code>configurationNode</code>. The property to use is found following
     * the the node's primary item trail: While the primary item is a node,
     * the node's primary item is accessed. If it is a property which is not a
     * reference, the property is returned. If the property is a reference,
     * the reference is resolved and this step is repeated.
     * <p>
     * If no configuration property can be found this method throws a
     * <code>RepositoryException</code>.
     *
     * @param configurationNode The <code>Node</code> containing property to
     *      access at the end of the primary item trail.
     *
     * @return The property containing the configuration.
     *
     * @throws RepositoryException If an error occurrs accessing the node or if
     *      no configuration property can be found.
     */
    /* package */ static Property getConfigurationProperty(
            Node configurationNode) throws RepositoryException {

        // find the primary item now
        for (;;) {
            Item item = configurationNode.getPrimaryItem();
            if (!item.isNode()) {
                Property prop = (Property) item;

                // if the property is not a reference return it
                if (prop.getType() != PropertyType.REFERENCE) {
                    return prop;
                }

                // otherwise get the referred to node and continue finding
                // the primary item
                item = prop.getNode();
            }

            configurationNode = (Node) item;
        }
    }

    /**
     * Closes the <code>InputStream</code> <code>in</code> if not
     * <code>null</code> and ignores a potential <code>IOException</code> thrown
     * from closing the stream.
     *
     * @param in The <code>InputStream</code> to close. This may be
     *          <code>null</code>.
     */
    public static void tryClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignored by intent
            }
        }
    }

    /**
     * Closes the <code>OutputStream</code> <code>out</code> if not
     * <code>null</code> and ignores a potential <code>IOException</code> thrown
     * from closing the stream.
     *
     * @param out The <code>OutputStream</code> to close. This may be
     *          <code>null</code>.
     */
    public static void tryClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                // ignored by intent
            }
        }
    }
}
