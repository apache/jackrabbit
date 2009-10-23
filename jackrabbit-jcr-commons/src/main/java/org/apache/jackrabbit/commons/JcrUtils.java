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
package org.apache.jackrabbit.commons;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.apache.jackrabbit.commons.iterator.PropertyIterable;
import org.apache.jackrabbit.commons.iterator.RowIterable;

/**
 * Collection of static utility methods for use with the JCR API.
 *
 * @since Apache Jackrabbit 2.0
 */
public class JcrUtils {

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private JcrUtils() {
    }

    /**
     * Looks up the available {@link RepositoryFactory repository factories}
     * and returns the {@link Repository repository} that one of the factories
     * returns for the given settings.
     *
     * @see RepositoryFactory#getRepository(Map)
     * @param parameters repository settings
     * @return repository reference
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if an appropriate repository factory
     *                             is not available
     */
    public static Repository getRepository(Map<String, String> parameters)
            throws RepositoryException {
        Iterator<RepositoryFactory> iterator =
            ServiceRegistry.lookupProviders(RepositoryFactory.class);

        while (iterator.hasNext()) {
            RepositoryFactory factory = iterator.next();
            Repository repository = factory.getRepository(parameters);
            if (repository != null) {
                return repository;
            }
        }

        throw new RepositoryException(
                "No repository factory can handle the given configuration: "
                + parameters);
    }

    /**
     * Returns the repository identified by the given URI.
     * <p>
     * The currently supported URI types are:
     * <dl>
     *   <dt>http(s)://...</dt>
     *   <dd>
     *     A remote repository connection using SPI2DAVex with the given URL.
     *   </dd>
     *   <dt>file://...</dt>
     *   <dd>
     *     An embedded Jackrabbit repository located in the given directory.
     *   </dd>
     *   <dt>jndi:...</dt>
     *   <dd>
     *     JNDI lookup for a repository stored under the given name in the
     *     default JNDI context.
     *   </dd>
     * </dl>
     * <p>
     *
     * @param uri repository URI
     * @return repository instance
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if the given URI is unknown or invalid
     */
    public static Repository getRepository(String uri)
            throws RepositoryException {
        try {
            URI parsed = new URI(uri.trim());
            String scheme = parsed.getScheme();
            if ("jndi".equalsIgnoreCase(scheme)) {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put(
                        "org.apache.jackrabbit.repository.jndi.name",
                        parsed.getSchemeSpecificPart());
                return getRepository(parameters);
            } else if ("file".equalsIgnoreCase(scheme)) {
                return getRepository(new File(parsed));
            } else if ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme)){
                return getRepository(parsed);
            } else {
                throw new RepositoryException(
                        "Unsupported repository URI: " + uri);
            }
        } catch (URISyntaxException e) {
            throw new RepositoryException("Invalid repository URI: " + uri, e);
        }
    }

    /**
     * Returns an SPI2DAVex client that accesses a repository at the given URI.
     *
     * @param uri repository URI
     * @return SPI2DAVex repository client
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if the required SPI2DAVex libraries
     *                             are not available
     */
    private static Repository getRepository(URI uri)
            throws RepositoryException {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(
                "org.apache.jackrabbit.spi.RepositoryServiceFactory",
                "org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory");
        parameters.put(
                "org.apache.jackrabbit.spi2davex.uri", uri.toASCIIString());
        return getRepository(parameters);
    }

    /**
     * Returns an embedded Jackrabbit repository located at the given
     * file system path. The path can point either to the repository
     * directory (in which case a "repository.xml" configuration file
     * is expected to be found inside the directory) or to the repository
     * configuration file (in which case the repository directory is assumed
     * to be the directory that contains the configuration file).
     *
     * @param file repository path
     * @return Jackrabbit repository
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if the required Jackrabbit libraries
     *                             are not available
     */
    private static Repository getRepository(File file)
            throws RepositoryException {
        Map<String, String> parameters = new HashMap<String, String>();

        String home, conf;
        if (file.isFile()) {
            home = file.getParentFile().getPath();
            conf = file.getPath();
        } else {
            home = file.getPath();
            conf = new File(file, "repository.xml").getPath();
        }
        parameters.put("org.apache.jackrabbit.repository.home", home);
        parameters.put("org.apache.jackrabbit.repository.conf", conf);

        return getRepository(parameters);
    }

    /**
     * Calls {@link Node#getSharedSet()} on the given node and returns
     * the resulting {@link NodeIterator} as an {@link Iterable<Node>} instance
     * for use in a Java 5 for-each loop.
     *
     * @see NodeIterable
     * @param node shared node
     * @return nodes in the shared set
     * @throws RepositoryException if the {@link Node#getSharedSet()} call fails
     */
    public static Iterable<Node> getSharedSet(Node node)
            throws RepositoryException {
        return new NodeIterable(node.getSharedSet());
    }

    /**
     * Calls {@link Node#getNodes()} on the given node and returns the
     * resulting {@link NodeIterator} as an {@link Iterable<Node>} instance
     * for use in a Java 5 for-each loop.
     *
     * @see NodeIterable
     * @param node parent node
     * @return child nodes
     * @throws RepositoryException if the {@link Node#getNodes()} call fails
     */
    public static Iterable<Node> getChildNodes(Node node)
            throws RepositoryException {
        return new NodeIterable(node.getNodes());
    }

    /**
     * Calls {@link Node#getNodes(String)} on the given node with the given
     * name pattern and returns the resulting {@link NodeIterator} as an
     * {@link Iterable<Node>} instance for use in a Java 5 for-each loop.
     *
     * @see NodeIterable
     * @param node parent node
     * @param pattern node name pattern
     * @return matching child nodes
     * @throws RepositoryException
     *         if the {@link Node#getNodes(String)} call fails
     */
    public static Iterable<Node> getChildNodes(Node node, String pattern)
            throws RepositoryException {
        return new NodeIterable(node.getNodes(pattern));
    }

    /**
     * Calls {@link Node#getNodes(String[])} on the given node with the given
     * name globs and returns the resulting {@link NodeIterator} as an
     * {@link Iterable<Node>} instance for use in a Java 5 for-each loop.
     *
     * @see NodeIterable
     * @param node parent node
     * @param pattern node name pattern
     * @return matching child nodes
     * @throws RepositoryException
     *         if the {@link Node#getNodes(String[])} call fails
     */
    public static Iterable<Node> getChildNodes(Node node, String[] globs)
            throws RepositoryException {
        return new NodeIterable(node.getNodes(globs));
    }

    /**
     * Calls {@link Node#getProperties()} on the given node and returns the
     * resulting {@link NodeIterator} as an {@link Iterable<Node>} instance
     * for use in a Java 5 for-each loop.
     *
     * @see PropertyIterable
     * @param node node
     * @return properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties()} call fails
     */
    public static Iterable<Property> getProperties(Node node)
            throws RepositoryException {
        return new PropertyIterable(node.getProperties());
    }

    /**
     * Calls {@link Node#getProperties(String)} on the given node with the
     * given name pattern and returns the resulting {@link PropertyIterator}
     * as an {@link Iterable<Property>} instance for use in a Java 5
     * for-each loop.
     *
     * @see PropertyIterable
     * @param node node
     * @param pattern property name pattern
     * @return matching properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties(String)} call fails
     */
    public static Iterable<Property> getProperties(Node node, String pattern)
            throws RepositoryException {
        return new PropertyIterable(node.getProperties(pattern));
    }

    /**
     * Calls {@link Node#getProperty(String[])} on the given node with the
     * given name globs and returns the resulting {@link PropertyIterator}
     * as an {@link Iterable<Property>} instance for use in a Java 5
     * for-each loop.
     *
     * @see PropertyIterable
     * @param node node
     * @param pattern property name globs
     * @return matching properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties(String[])} call fails
     */
    public static Iterable<Property> getProperties(Node node, String[] globs)
            throws RepositoryException {
        return new PropertyIterable(node.getProperties(globs));
    }

    /**
     * Calls {@link Node#getReferences()} on the given node and returns the
     * resulting {@link PropertyIterator} as an {@link Iterable<Property>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see PropertyIterable
     * @param node reference target
     * @return references that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getReferences()} call fails
     */
    public static Iterable<Property> getReferences(Node node)
            throws RepositoryException {
        return new PropertyIterable(node.getReferences());
    }

    /**
     * Calls {@link Node#getReferences(String)} on the given node and returns
     * the resulting {@link PropertyIterator} as an {@link Iterable<Property>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see PropertyIterable
     * @param node reference target
     * @param name reference property name
     * @return references with the given name that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getReferences(String)} call fails
     */
    public static Iterable<Property> getReferences(Node node, String name)
            throws RepositoryException {
        return new PropertyIterable(node.getReferences(name));
    }

    /**
     * Calls {@link Node#getWeakReferences()} on the given node and returns the
     * resulting {@link PropertyIterator} as an {@link Iterable<Property>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see PropertyIterable
     * @param node reference target
     * @return weak references that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getWeakReferences()} call fails
     */
    public static Iterable<Property> getWeakReferences(Node node)
            throws RepositoryException {
        return new PropertyIterable(node.getWeakReferences());
    }

    /**
     * Calls {@link Node#getReferences(String)} on the given node and returns
     * the resulting {@link PropertyIterator} as an {@link Iterable<Property>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see PropertyIterable
     * @param node reference target
     * @param name reference property name
     * @return weak references with the given name that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getWeakReferences(String)} call fails
     */
    public static Iterable<Property> getWeakReferences(Node node, String name)
            throws RepositoryException {
        return new PropertyIterable(node.getWeakReferences(name));
    }

    /**
     * Calls {@link QueryResult#getNodes()} on the given query result and
     * returns the resulting {@link NodeIterator} as an {@link Iterable<Node>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see NodeIterable
     * @param result query result
     * @return nodes in the query result
     * @throws RepositoryException
     *         if the {@link QueryResult#getNodes()} call fails
     */
    public static Iterable<Node> getNodes(QueryResult result)
            throws RepositoryException {
        return new NodeIterable(result.getNodes());
    }

    /**
     * Calls {@link QueryResult#getRows()} on the given query result and
     * returns the resulting {@link RowIterator} as an {@link Iterable<Row>}
     * instance for use in a Java 5 for-each loop.
     *
     * @see RowIterable
     * @param result query result
     * @return rows in the query result
     * @throws RepositoryException
     *         if the {@link QueryResult#getRows()} call fails
     */
    public static Iterable<Row> getRows(QueryResult result)
            throws RepositoryException {
        return new RowIterable(result.getRows());
    }

}
