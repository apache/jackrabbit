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

import javax.jcr.Node;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

/**
 * The <code>RepositoryConfiguration</code> interface extends the
 * <code>Configuration</code> with support for loading configuration from
 * a JCR repository and storing the configuration in a JCR repository.
 *
 * @author Felix Meschberger
 */
public interface RepositoryConfiguration extends Configuration {

    /**
     * Returns the repository <code>Node</code> to which this configuration
     * is attached.
     */
    Node getNode();

    /**
     * Attaches this configuration to the repository <code>Node</code>.
     *
     * @param node The <code>Node</code> to which the configuration object
     *      is attached or <code>null</code> to actually detach the
     *      configuration from the configuration node.
     */
    void setNode(Node node);

    /**
     * Loads the configuration from the <code>node</code> and child items.
     *
     * @throws ConfigurationException if an error occurs during the load
     *      operation
     */
    void load() throws ConfigurationException;

    /**
     * Loads the configuration from the <code>node</code> and child items.
     *
     * @param node The root <code>Node</code> of the repository based
     *      configuration to load from.
     *
     * @throws ConfigurationException if an error occurs during the load
     *      operation
     */
    void load(Node node) throws ConfigurationException;

    /**
     * Save the configuration to the specified node.
     * <p>
     * Before actually storing any configuration data all properties and child
     * nodes of the <code>node</code> are removed to be able to write clean
     * configuration.
     * <p>
     * If the node is versionable, the node is checked out (if required) before
     * saving the configuration and checked in again after saving the
     * configuration.
     * <p>
     * Invariants:
     * <ul>
     * <li>If an error occurrs, all modifications must be rolled back and the
     *      <code>node</code> and all properties and child nodes must remain
     *      in the former state.
     * <li>If all goes well, the <code>node</code>, properties and child nodes
     *      reflect the current state of the configuration and the
     *      <code>node</code> has been persisted in the repository.
     * <li>If all goes well and the <code>node</code> is versionable, the
     *      <code>node</code> is checked in. If an error occurrs it is not
     *      specified whether the versionable <code>node</code> is checked in
     *      or not.
     * </ul>
     *
     * @throws ConfigurationException if an error occurs during the save
     *      operation
     *
     * @see #setNode(Node)
     * @see #getNode()
     */
    void save() throws ConfigurationException;

    /**
     * Save the configuration to the specified node.
     * <p>
     * Before actually storing any configuration data all properties and child
     * nodes of the <code>node</code> are removed to be able to write clean
     * configuration.
     * <p>
     * If the node is versionable, the node is checked out (if required) before
     * saving the configuration and checked in again after saving the
     * configuration.
     * <p>
     * Invariants:
     * <ul>
     * <li>If an error occurrs, all modifications must be rolled back and the
     *      <code>node</code> and all properties and child nodes must remain
     *      in the former state.
     * <li>If all goes well, the <code>node</code>, properties and child nodes
     *      reflect the current state of the configuration and the
     *      <code>node</code> has been persisted in the repository.
     * <li>If all goes well and the <code>node</code> is versionable, the
     *      <code>node</code> is checked in. If an error occurrs it is not
     *      specified whether the versionable <code>node</code> is checked in
     *      or not.
     * </ul>
     *
     * @param node The root <code>Node</code> of the repository based
     *      configuration to save to.
     *
     * @throws ConfigurationException if an error occurs during the save
     *      operation
     */
    void save(Node node) throws ConfigurationException;
}
