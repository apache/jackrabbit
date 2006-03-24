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
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * The <code>XMLNodeConfiguration</code> extends the Apache Commons
 * <code>XMLConfiguration</code> by support for loading the XML configuratîon
 * from a repository property in addition to the standard loading source such as
 * file, URL, and streams.
 *
 * @author Felix Meschberger
 */
public class XMLNodeConfiguration extends XMLConfiguration implements
        RepositoryConfiguration {

    /**
     * The delegate object which takes care for actually loading and saving
     * configuration to and from the repository.
     */
    private final ConfigurationIODelegate delegate =
        new ConfigurationIODelegate(this);

    {
        // set the default encoding to UTF-8
        setEncoding(ConfigurationIODelegate.ENCODING);
    }

    /**
     * Creates an empty <code>XMLNodeConfiguration</code> object which can be
     * used to synthesize a new XML file by adding values and then saving().
     */
    public XMLNodeConfiguration() {
        super();
    }

    /**
     * Creates and loads the configuration from the specified file.
     *
     * @param fileName The name of the XML file to load.
     *
     * @throws ConfigurationException Error while loading the XML file
     */
    public XMLNodeConfiguration(String fileName) throws ConfigurationException {
        super(fileName);
    }

    /**
     * Creates and loads the configuration from the specified file.
     *
     * @param file The XML file to load.
     *
     * @throws ConfigurationException Error while loading the XML file
     */
    public XMLNodeConfiguration(File file) throws ConfigurationException {
        super(file);
    }

    /**
     * Creates and loads the configuration from the specified URL
     *
     * @param url The location of the XML file to load.
     *
     * @throws ConfigurationException Error while loading the XML file
     */
    public XMLNodeConfiguration(URL url) throws ConfigurationException {
        super(url);
    }

    /**
     * Creates and loads the configuration from the specified <code>node</code>.
     *
     * @param node The <code>Node</code> from which to load the configuration.
     * @throws ConfigurationException Error while loading the XML file
     */
    public XMLNodeConfiguration(javax.jcr.Node node)
            throws ConfigurationException {
        super();
        setNode(node);
        load();
    }

    /**
     * Returns the <code>Node</code> on which this configuration is based. If
     * this is not a repository-based configuration object or has not been
     * configured to load from the repository, this method returns
     * <code>null</code>.
     */
    public javax.jcr.Node getNode() {
        return delegate.getNode();
    }

    /**
     * Sets the <code>Node</code> on which this configuration is based.
     */
    public void setNode(javax.jcr.Node node) {
        delegate.setNode(node);
    }

    /**
     * Loads the configuration from the underlying location.
     *
     * @throws ConfigurationException if loading of the configuration fails
     */
    public void load() throws ConfigurationException {
        delegate.load();
    }

    /**
     * Loads the configuration from the <code>node</code>. The property to
     * use is found following the the node's primary item trail: While the
     * primary item is a node, the node's primary item is accessed. If it is a
     * property which is not a reference, the property is returned. If the
     * property is a reference, the reference is resolved and this step is
     * repeated.
     * <p>
     * If no property can be found using above mentioned algorithm, loading the
     * configuration fails.
     *
     * @param node The <code>Node</code> of the repository based configuration
     *            to load from.
     * @throws ConfigurationException if an error occurs during the load
     *             operation or if no property can be found containing the
     *             properties "file".
     */
    public void load(javax.jcr.Node node) throws ConfigurationException {
        delegate.load(node);
    }

    /**
     * Saves the configuration to the underlying location.
     *
     * @throws ConfigurationException if saving of the configuration fails
     */
    public void save() throws ConfigurationException {
        delegate.save();
    }

    /**
     * Saves the configuration in the <code>node</code>. The same algorithm
     * applies for finding the property to store the configuration in as is
     * applied by the {@link #load(javax.jcr.Node)} method. If no property can
     * be found saving the configuration fails.
     *
     * @param node The <code>Node</code> of the repository based configuration
     *      to save the configuration in.
     *
     * @throws ConfigurationException if an error occurs during the save
     *      operation.
     */
    public void save(javax.jcr.Node node) throws ConfigurationException {
        delegate.save(node);
    }
}
