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

import static java.net.URLDecoder.decode;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
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
     * The repository URI parameter name used by the
     * {@link #getRepository(String)} method. All {@link RepositoryFactory}
     * implementations that want to support this repository access convention
     * should implement processing of this parameter.
     * <p>
     * Client applications are recommended to use the
     * {@link #getRepository(String)} method instead of directly referencing
     * this constant unless they explicitly want to pass also other
     * {@link RepositoryFactory} parameters through the
     * {@link #getRepository(Map)} method.
     */
    public static final String REPOSITORY_URI =
        "org.apache.jackrabbit.repository.uri";

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private JcrUtils() {
    }

    /**
     * Returns the default repository of the current environment.
     * Implemented by calling {@link #getRepository(Map)} with a
     * <code>null</code> parameter map.
     *
     * @see RepositoryFactory#getRepository(Map)
     * @return default repository
     * @throws RepositoryException if a default repository is not available
     *                             or can not be accessed
     */
    public static Repository getRepository() throws RepositoryException {
        return getRepository((Map<String, String>) null);
    }

    /**
     * Looks up the available {@link RepositoryFactory repository factories}
     * and returns the {@link Repository repository} that one of the factories
     * returns for the given settings.
     * <p>
     * Note that unlike {@link RepositoryFactory#getRepository(Map)} this
     * method will throw an exception instead of returning <code>null</code>
     * if the given parameters can not be interpreted.
     *
     * @param parameters repository settings
     * @return repository reference
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if an appropriate repository factory
     *                             is not available
     */
    public static Repository getRepository(Map<String, String> parameters)
            throws RepositoryException {
        // Use the query part of a repository URI as additional parameters
        if (parameters != null
                && parameters.containsKey(JcrUtils.REPOSITORY_URI)) {
            String uri = parameters.get(JcrUtils.REPOSITORY_URI);
            Map<String, String> copy = new HashMap<String, String>(parameters);
            try {
                URI u = new URI(uri);
                String query = u.getRawQuery();
                if (query != null) {
                    for (String entry : query.split("&")) {
                        int i = entry.indexOf('=');
                        if (i != -1) {
                            copy.put(
                                    decode(entry.substring(0, i), "UTF-8"),
                                    decode(entry.substring(i + 1), "UTF-8"));
                        } else {
                            copy.put(
                                    decode(entry, "UTF-8"),
                                    Boolean.TRUE.toString());
                        }
                    }
                    copy.put(
                            JcrUtils.REPOSITORY_URI,
                            new URI(u.getScheme(), u.getRawAuthority(),
                                    u.getRawPath(), null, u.getRawFragment()
                                    ).toASCIIString());
                    parameters = copy;
                }
            } catch (URISyntaxException e) {
                // Ignore invalid URIs
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException("UTF-8 is not supported!", e);
            }
        }

        String newline = System.getProperty("line.separator");

        // Prepare the potential error message (JCR-2459)
        StringBuilder log = new StringBuilder("Unable to access a repository");
        if (parameters != null) {
            log.append(" with the following settings:");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                log.append(newline);
                log.append("    ");
                log.append(entry.getKey());
                log.append(": ");
                log.append(entry.getValue());
            }
        } else {
            log.append(" with the default settings.");
        }

        // Iterate through the available RepositoryFactories, with logging
        log.append(newline);
        log.append("The following RepositoryFactory classes were consulted:");
        Iterator<RepositoryFactory> iterator =
            ServiceRegistry.lookupProviders(RepositoryFactory.class);
        while (iterator.hasNext()) {
            RepositoryFactory factory = iterator.next();
            log.append(newline);
            log.append("    ");
            log.append(factory.getClass().getName());
            try {
                Repository repository = factory.getRepository(parameters);
                if (repository != null) {
                    // We found the requested repository! Return it
                    // and just ignore the error message being built.
                    return repository;
                } else {
                    log.append(": declined");
                }
            } catch (Exception e) {
                log.append(": failed");
                for (Throwable c = e; c != null; c = c.getCause()) {
                    log.append(newline);
                    log.append("        because of ");
                    log.append(c.getClass().getSimpleName());
                    log.append(": ");
                    log.append(c.getMessage());
                }
            }
        }
        log.append(newline);
        log.append(
                "Perhaps the repository you are trying"
                + " to access is not available at the moment.");

        // No matching repository found. Throw an exception with the
        // detailed information we gathered during the above process.
        throw new RepositoryException(log.toString());
    }

    /**
     * Returns the repository identified by the given URI. This feature
     * is implemented by calling the {@link #getRepository(Map)} method
     * with the {@link #REPOSITORY_URI} parameter set to the given URI.
     * Any query parameters are moved from the URI to the parameter map.
     * <p>
     * See the documentation of the repository implementation you want
     * to use for whether it supports this repository URI convention and
     * for what the repository URI should look like. For example,
     * Jackrabbit 2.0 supports the following types of repository URIs:
     * <dl>
     *   <dt>http(s)://...</dt>
     *   <dd>
     *     A remote repository connection using SPI2DAVex with the given URL.
     *     See the jackrabbit-jcr2dav component for more details.
     *   </dd>
     *   <dt>file://...</dt>
     *   <dd>
     *     An embedded Jackrabbit repository located in the given directory.
     *     See the jackrabbit-core component for more details.
     *   </dd>
     *   <dt>jndi:...</dt>
     *   <dd>
     *     JNDI lookup for the named repository. See the
     *     {@link JndiRepositoryFactory} class for more details.
     *  </dd>
     * </dl>
     *
     * @param uri repository URI
     * @return repository instance
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if the given URI is unknown or invalid
     */
    public static Repository getRepository(String uri)
            throws RepositoryException {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(JcrUtils.REPOSITORY_URI, uri);
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

    /**
     * Returns the named child of the given node, creating the child if
     * it does not already exist. If the child node gets added, then its
     * type will be determined by the child node definitions associated
     * with the parent node. The caller is expected to take care of saving
     * or discarding any transient changes.
     *
     * @see Node#getNode(String)
     * @see Node#addNode(String)
     * @param parent parent node
     * @param name name of the child node
     * @return the child node
     * @throws RepositoryException if the child node can not be
     *                             accessed or created
     */
    public static Node getOrAddNode(Node parent, String name)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        } else {
            return parent.addNode(name);
        }
    }

    /**
     * Returns the named child of the given node, creating the child if
     * it does not already exist. If the child node gets added, then it
     * is created with the given node type. The caller is expected to take
     * care of saving or discarding any transient changes.
     *
     * @see Node#getNode(String)
     * @see Node#addNode(String, String)
     * @see Node#isNodeType(String)
     * @param parent parent node
     * @param name name of the child node
     * @param type type of the child node, ignored if the child already exists
     * @return the child node
     * @throws RepositoryException if the child node can not be accessed
     *                             or created
     */
    public static Node getOrAddNode(Node parent, String name, String type)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        } else {
            return parent.addNode(name, type);
        }
    }

    /**
     * Returns the named child of the given node, creating it as an
     * nt:folder node if it does not already exist. The caller is expected
     * to take care of saving or discarding any transient changes.
     * <p>
     * Note that the type of the returned node is <em>not</em> guaranteed
     * to match nt:folder in case the node already existed. The caller can
     * use an explicit {@link Node#isNodeType(String)} check if needed, or
     * simply use a data-first approach and not worry about the node type
     * until a constraint violation is encountered.
     *
     * @param parent parent node
     * @param name name of the child node
     * @return the child node
     * @throws RepositoryException if the child node can not be accessed
     *                             or created
     */
    public static Node getOrAddFolder(Node parent, String name)
            throws RepositoryException {
        return getOrAddNode(parent, name, NodeType.NT_FOLDER);
    }

    /**
     * Creates or updates the named child of the given node. If the child
     * does not already exist, then it is created using the nt:file node type.
     * This file child node is returned from this method.
     * <p>
     * If the file node does not already contain a jcr:content child, then
     * one is created using the nt:resource node type. The following
     * properties are set on the jcr:content node:
     * <dl>
     *   <dt>jcr:mimeType</dt>
     *   <dd>media type</dd>
     *   <dt>jcr:encoding (optional)</dt>
     *   <dd>charset parameter of the media type, if any</dd>
     *   <dt>jcr:lastModified</dt>
     *   <dd>current time</dd>
     *   <dt>jcr:data</dt>
     *   <dd>binary content</dd>
     * </dl>
     * <p>
     * Note that the types of the returned node or the jcr:content child are
     * <em>not</em> guaranteed to match nt:file and nt:resource in case the
     * nodes already existed. The caller can use an explicit
     * {@link Node#isNodeType(String)} check if needed, or simply use a
     * data-first approach and not worry about the node type until a constraint
     * violation is encountered.
     * <p>
     * The given binary content stream is closed by this method.
     *
     * @param parent parent node
     * @param name name of the file
     * @param mime media type of the file
     * @param data binary content of the file
     * @return the child node
     * @throws RepositoryException if the child node can not be created
     *                             or updated
     */
    public static Node putFile(
            Node parent, String name, String mime, InputStream data)
            throws RepositoryException {
        return putFile(parent, name, mime, data, Calendar.getInstance());
    }

    /**
     * Creates or updates the named child of the given node. If the child
     * does not already exist, then it is created using the nt:file node type.
     * This file child node is returned from this method.
     * <p>
     * If the file node does not already contain a jcr:content child, then
     * one is created using the nt:resource node type. The following
     * properties are set on the jcr:content node:
     * <dl>
     *   <dt>jcr:mimeType</dt>
     *   <dd>media type</dd>
     *   <dt>jcr:encoding (optional)</dt>
     *   <dd>charset parameter of the media type, if any</dd>
     *   <dt>jcr:lastModified</dt>
     *   <dd>date of last modification</dd>
     *   <dt>jcr:data</dt>
     *   <dd>binary content</dd>
     * </dl>
     * <p>
     * Note that the types of the returned node or the jcr:content child are
     * <em>not</em> guaranteed to match nt:file and nt:resource in case the
     * nodes already existed. The caller can use an explicit
     * {@link Node#isNodeType(String)} check if needed, or simply use a
     * data-first approach and not worry about the node type until a constraint
     * violation is encountered.
     * <p>
     * The given binary content stream is closed by this method.
     *
     * @param parent parent node
     * @param name name of the file
     * @param mime media type of the file
     * @param data binary content of the file
     * @param date date of last modification
     * @return the child node
     * @throws RepositoryException if the child node can not be created
     *                             or updated
     */
    public static Node putFile(
            Node parent, String name, String mime,
            InputStream data, Calendar date) throws RepositoryException {
        Binary binary =
            parent.getSession().getValueFactory().createBinary(data);
        try {
            Node file = getOrAddNode(parent, name, NodeType.NT_FILE);
            Node content =
                getOrAddNode(file, Node.JCR_CONTENT, NodeType.NT_RESOURCE);

            content.setProperty(Property.JCR_MIMETYPE, mime);
            String[] parameters = mime.split(";");
            for (int i = 1; i < parameters.length; i++) {
                int equals = parameters[i].indexOf('=');
                if (equals != -1) {
                    String parameter = parameters[i].substring(0, equals);
                    if ("charset".equalsIgnoreCase(parameter.trim())) {
                        content.setProperty(
                                Property.JCR_ENCODING,
                                parameters[i].substring(equals + 1).trim());
                    }
                }
            }

            content.setProperty(Property.JCR_LAST_MODIFIED, date);
            content.setProperty(Property.JCR_DATA, binary);
            return file;
        } finally {
            binary.dispose();
        }
    }

    /**
     * Returns a string representation of the given item. The returned string
     * is designed to be easily readable while providing maximum amount of
     * information for logging and debugging purposes.
     * <p>
     * The returned string is not meant to be parsed and the exact contents
     * can change in future releases. The current string representation of
     * a node is "/path/to/node [type]" and the representation of a property is
     * "@name = value(s)". Binary values are expressed like "&lt;123 bytes&gt;"
     * and other values as their standard binary representation. Multi-valued
     * properties have their values listed in like "[ v1, v2, v3, ... ]". No
     * more than the three first values are included. Long string values are
     * truncated.
     *
     * @param item given node or property
     * @return string representation of the given item
     */
    public static String toString(Item item) {
        StringBuilder builder = new StringBuilder();
        try {
            if (item.isNode()) {
                builder.append(item.getPath());
                builder.append(" [");
                builder.append(((Node) item).getPrimaryNodeType().getName());
                builder.append("]");
            } else {
                builder.append("@");
                builder.append(item.getName());
                builder.append(" = ");
                Property property = (Property) item;
                if (property.isMultiple()) {
                    builder.append("[ ");
                    Value[] values = property.getValues();
                    for (int i = 0; i < values.length && i < 3; i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        append(builder, values[i]);
                    }
                    if (values.length >= 3) {
                        builder.append(", ...");
                    }
                    builder.append(" ]");
                } else {
                    append(builder, property.getValue());
                }
            }
        } catch (RepositoryException e) {
            builder.append("!!! ");
            builder.append(e.getMessage());
            builder.append(" !!!");
        }
        return builder.toString();
    }

    /**
     * Private helper method that adds the string representation of the given
     * value to the given {@link StringBuilder}. Used by the
     * {{@link #toString(Item)} method.
     */
    private static void append(StringBuilder builder, Value value)
            throws RepositoryException {
        if (value.getType() == PropertyType.BINARY) {
            Binary binary = value.getBinary();
            try {
                builder.append("<");
                builder.append(binary.getSize());
                builder.append(" bytes>");
            } finally {
                binary.dispose();
            }
        } else {
            String string = value.getString();
            if (string.length() > 40) {
                builder.append(string.substring(0, 37));
                builder.append("...");
            } else {
                builder.append(string);
            }
        }
    }

}
