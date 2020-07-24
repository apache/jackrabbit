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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

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
    public static final String REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";

    /**
     * A pre-allocated empty array of values.
     *
     * @since Apache Jackrabbit 2.3
     */
    public static final Value[] NO_VALUES = new Value[0];

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

        // Use the query part of a repository URI as additional parameters
        if (parameters != null
                && parameters.containsKey(JcrUtils.REPOSITORY_URI)) {
            String uri = parameters.get(JcrUtils.REPOSITORY_URI);
            try {
                URI u = new URI(uri);
                String query = u.getRawQuery();
                if (query != null) {
                   Map<String, String> copy = new HashMap<String, String>(parameters);
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
                log.append(newline);
                log.append("Note that the given repository URI was invalid:");
                log.append(newline);
                log.append("        ").append(uri);
                log.append(newline);
                log.append("        ").append(e.getMessage());
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException("UTF-8 is not supported!", e);
            }
        }

        // Iterate through the available RepositoryFactories, with logging
        log.append(newline);
        log.append("The following RepositoryFactory classes were consulted:");
        Iterator<RepositoryFactory> iterator =
                ServiceLoader.load(RepositoryFactory.class).iterator();
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
                try (StringWriter writer = new StringWriter(); PrintWriter printWriter = new PrintWriter(writer)) {
                    e.printStackTrace(printWriter);
                    log.append(newline).append(writer.getBuffer());
                } catch (IOException e1) {
                    log.append("Could not determine root cause due to ").append(e.getMessage());
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
     * Returns an {@link Iterable} over the shared set of the given node.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getSharedSet()
     * @param node shared node
     * @return nodes in the shared set
     * @throws RepositoryException if the {@link Node#getSharedSet()} call fails
     */
    public static Iterable<Node> getSharedSet(final Node node)
            throws RepositoryException {
        final NodeIterator iterator = node.getSharedSet();
        return new Iterable<Node>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Node> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getSharedSet();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over the children of the given node.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getNodes()
     * @param node parent node
     * @return child nodes
     * @throws RepositoryException if the {@link Node#getNodes()} call fails
     */
    public static Iterable<Node> getChildNodes(final Node node)
            throws RepositoryException {
        final NodeIterator iterator = node.getNodes();
        return new Iterable<Node>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Node> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getNodes();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those children of the given node
     * that match the given name pattern.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getNodes(String)
     * @param node parent node
     * @param pattern node name pattern
     * @return matching child nodes
     * @throws RepositoryException
     *         if the {@link Node#getNodes(String)} call fails
     */
    public static Iterable<Node> getChildNodes(
            final Node node, final String pattern) throws RepositoryException {
        final NodeIterator iterator = node.getNodes(pattern);
        return new Iterable<Node>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Node> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getNodes(pattern);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those children of the given node
     * that match the given name patterns.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getNodes(String[])
     * @param node parent node
     * @param globs node name pattern
     * @return matching child nodes
     * @throws RepositoryException
     *         if the {@link Node#getNodes(String[])} call fails
     */
    public static Iterable<Node> getChildNodes(
            final Node node, final String[] globs) throws RepositoryException {
        final NodeIterator iterator = node.getNodes(globs);
        return new Iterable<Node>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Node> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getNodes(globs);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over the properties of the given node.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getProperties()
     * @param node node
     * @return properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties()} call fails
     */
    public static Iterable<Property> getProperties(final Node node)
            throws RepositoryException {
        final PropertyIterator iterator = node.getProperties();
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getProperties();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those properties of the
     * given node that match the given name pattern.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getProperties(String)
     * @param node node
     * @param pattern property name pattern
     * @return matching properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties(String)} call fails
     */
    public static Iterable<Property> getProperties(
            final Node node, final String pattern) throws RepositoryException {
        final PropertyIterator iterator = node.getProperties(pattern);
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getProperties(pattern);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those properties of the
     * given node that match the given name patterns.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getProperties(String[])
     * @param node node
     * @param globs property name globs
     * @return matching properties of the node
     * @throws RepositoryException
     *         if the {@link Node#getProperties(String[])} call fails
     */
    public static Iterable<Property> getProperties(
            final Node node, final String[] globs) throws RepositoryException {
        final PropertyIterator iterator = node.getProperties(globs);
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getProperties(globs);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over references to the given node.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getReferences()
     * @param node reference target
     * @return references that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getReferences()} call fails
     */
    public static Iterable<Property> getReferences(final Node node)
            throws RepositoryException {
        final PropertyIterator iterator = node.getReferences();
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getReferences();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those references to the
     * given node that have the given name.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getReferences(String)
     * @param node reference target
     * @param name reference property name
     * @return references with the given name that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getReferences(String)} call fails
     */
    public static Iterable<Property> getReferences(
            final Node node, final String name) throws RepositoryException {
        final PropertyIterator iterator = node.getReferences(name);
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getReferences(name);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over weak references to the given node.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getWeakReferences()
     * @param node reference target
     * @return weak references that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getWeakReferences()} call fails
     */
    public static Iterable<Property> getWeakReferences(final Node node)
            throws RepositoryException {
        final PropertyIterator iterator = node.getWeakReferences();
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getWeakReferences();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over those weak references to the
     * given node that have the given name.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see Node#getWeakReferences(String)
     * @param node reference target
     * @param name reference property name
     * @return weak references with the given name that point to the given node
     * @throws RepositoryException
     *         if the {@link Node#getWeakReferences(String)} call fails
     */
    public static Iterable<Property> getWeakReferences(
            final Node node, final String name) throws RepositoryException {
        final PropertyIterator iterator = node.getWeakReferences(name);
        return new Iterable<Property>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Property> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return node.getWeakReferences(name);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over nodes in the given query result.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see QueryResult#getNodes()
     * @param result query result
     * @return nodes in the query result
     * @throws RepositoryException
     *         if the {@link QueryResult#getNodes()} call fails
     */
    public static Iterable<Node> getNodes(final QueryResult result)
            throws RepositoryException {
        final NodeIterator iterator = result.getNodes();
        return new Iterable<Node>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Node> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return result.getNodes();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Returns an {@link Iterable} over nodes in the given query result.
     * <p>
     * The first iterator is acquired directly during this method call to
     * allow a possible {@link RepositoryException} to be thrown as-is.
     * Further iterators are acquired lazily when needed, with possible
     * {@link RepositoryException}s wrapped into {@link RuntimeException}s.
     *
     * @see QueryResult#getRows()
     * @param result query result
     * @return rows in the query result
     * @throws RepositoryException
     *         if the {@link QueryResult#getRows()} call fails
     */
    public static Iterable<Row> getRows(final QueryResult result)
            throws RepositoryException {
        final RowIterator iterator = result.getRows();
        return new Iterable<Row>() {
            private boolean first = true;
            @Override @SuppressWarnings("unchecked")
            public synchronized Iterator<Row> iterator() {
                if (first) {
                    first = false;
                    return iterator;
                } else {
                    try {
                        return result.getRows();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * Transform any type of {@link Iterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param <I> type
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    public static <I> Iterable<I> in(final Iterator<I> iterator) {
        return new Iterable<I>() {
            private boolean stale = false;

            @Override
            public synchronized Iterator<I> iterator() {
                if (stale) {
                    throw new IllegalStateException("Cannot reuse Iterable intended for single use");
                }

                stale = true;
                return iterator;
            }
        };
    }

    /**
     * Transform an {@link AccessControlPolicyIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<AccessControlPolicyIterator> in(AccessControlPolicyIterator iterator) {
        return in((Iterator<AccessControlPolicyIterator>) iterator);
    }

    /**
     * Transform an {@link EventIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Event> in(EventIterator iterator) {
        return in((Iterator<Event>) iterator);
    }

    /**
     * Transform an {@link EventListenerIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<EventListener> in(EventListenerIterator iterator) {
        return in((Iterator<EventListener>) iterator);
    }

    /**
     * Transform an {@link NodeIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Node> in(NodeIterator iterator) {
        return in((Iterator<Node>) iterator);
    }

    /**
     * Transform an {@link NodeTypeIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<NodeType> in(NodeTypeIterator iterator) {
        return in((Iterator<NodeType>) iterator);
    }

    /**
     * Transform an {@link PropertyIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Property> in(PropertyIterator iterator) {
        return in((Iterator<Property>) iterator);
    }

    /**
     * Transform an {@link RowIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Row> in(RowIterator iterator) {
        return in((Iterator<Row>) iterator);
    }

    /**
     * Transform an {@link VersionIterator} into an {@link Iterable}
     * <strong>for single use</strong> in a Java 5 for-each loop.
     * <p>
     * <strong>While general purpose <code>Iterables</code> tend to be reusable,
     * this wrapper <code>Iterable</code> consumes the argument
     * <code>Iterator</code>, leaving it in a non-reusable state. The returned
     * <code>Iterable</code> will throw an <code>IllegalStateException</code> if
     * its <code>iterator()</code> method is invoked a second time.</strong>
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Version> in(VersionIterator iterator) {
        return in((Iterator<Version>) iterator);
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
        return getOrAddNode(parent, name, null);
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
     * @param type type of the child node or {@code null},
     *             ignored if the child already exists
     * @return the child node
     * @throws RepositoryException if the child node can not be accessed
     *                             or created
     */
    public static Node getOrAddNode(Node parent, String name, String type)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        } else if (type != null) {
            return parent.addNode(name, type);
        } else {
            return parent.addNode(name);
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
        Binary binary = parent.getSession().getValueFactory().createBinary(data);
        try {
            Node file = getOrAddNode(parent, name, NodeType.NT_FILE);
            Node content = getOrAddNode(file, Node.JCR_CONTENT, NodeType.NT_RESOURCE);

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
     * Returns a stream for reading the contents of the file stored at the
     * given node. This method works with both on nt:file and nt:resource and
     * on any other similar node types, as it only looks for the jcr:data
     * property or a jcr:content child node.
     * <p>
     * The returned stream contains a reference to the underlying
     * {@link Binary} value instance that will be disposed when the stream
     * is closed. It is the responsibility of the caller to close the stream
     * once it is no longer needed.
     *
     * @since Apache Jackrabbit 2.3
     * @param node node to be read
     * @return stream for reading the file contents
     * @throws RepositoryException if the file can not be accessed
     */
    public static InputStream readFile(Node node) throws RepositoryException {
        if (node.hasProperty(Property.JCR_DATA)) {
            Property data = node.getProperty(Property.JCR_DATA);
            final Binary binary = data.getBinary();
            return new FilterInputStream(binary.getStream()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    binary.dispose();
                }
            };
        } else if (node.hasNode(Node.JCR_CONTENT)) {
            return readFile(node.getNode(Node.JCR_CONTENT));
        } else {
            throw new RepositoryException(
                    "Unable to read file node: " + node.getPath());
        }
    }

    /**
     * Writes the contents of file stored at the given node to the given
     * stream. Similar file handling logic is used as in the
     * {@link #readFile(Node)} method.
     *
     * @since Apache Jackrabbit 2.3
     * @param node node to be read
     * @param output to which the file contents are written
     * @throws RepositoryException if the file can not be accessed
     * @throws IOException if the file can not be read or written
     */
    public static void readFile(Node node, OutputStream output)
            throws RepositoryException, IOException {
        InputStream input = readFile(node);
        try {
            byte[] buffer = new byte[16 * 1024];
            int n = input.read(buffer);
            while (n != -1) {
                output.write(buffer, 0, n);
                n = input.read(buffer);
            }
        } finally {
            input.close();
        }
    }

    /**
     * Returns the last modified date of the given file node. The value is
     * read from the jcr:lastModified property of this node or alternatively
     * from a jcr:content child node.
     *
     * @since Apache Jackrabbit 2.3
     * @param node file node
     * @return last modified date, or <code>null</code> if not available
     * @throws RepositoryException if the last modified date can not be accessed
     */
    public static Calendar getLastModified(Node node) throws RepositoryException {
        if (node.hasProperty(Property.JCR_LAST_MODIFIED)) {
            return node.getProperty(Property.JCR_LAST_MODIFIED).getDate();
        } else if (node.hasNode(Node.JCR_CONTENT)) {
            return getLastModified(node.getNode(Node.JCR_CONTENT));
        } else {
            return null;
        }
    }

    /**
     * Sets the last modified date of the given file node. The value is
     * written to the jcr:lastModified property of a jcr:content child node
     * or this node if such a child does not exist.
     *
     * @since Apache Jackrabbit 2.3
     * @param node file node
     * @param date modified date
     * @throws RepositoryException if the last modified date can not be set
     */
    public static void setLastModified(Node node, Calendar date) throws RepositoryException {
        if (node.hasNode(Node.JCR_CONTENT)) {
            setLastModified(node.getNode(Node.JCR_CONTENT), date);
        } else {
            node.setProperty(Property.JCR_LAST_MODIFIED, date);
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

    private static final List<String> PROPERTY_TYPES_NAMES = new ArrayList<String>();
    private static final Map<String, Integer> PROPERTY_TYPES = new HashMap<String, Integer>();
    static {
        for (int i = PropertyType.UNDEFINED; i <= PropertyType.DECIMAL; i++) {
            String typeName = PropertyType.nameFromValue(i);
            PROPERTY_TYPES_NAMES.add(typeName);
            PROPERTY_TYPES.put(typeName.toLowerCase(), i);
        }
    }

    /**
     * Returns the numeric constant value of the property type with
     * the specified name. This method is like
     * {@link PropertyType#valueFromName(String)}, but the name lookup
     * is case insensitive.
     *
     * @since Apache Jackrabbit 2.3
     * @param name name of the property type (case insensitive)
     * @return property type constant
     * @throws IllegalArgumentException if the given name is not a valid
     *                                  property type name
     */
    public static int getPropertyType(String name)
            throws IllegalArgumentException {
        Integer type = PROPERTY_TYPES.get(name.toLowerCase());
        if (type != null) {
            return type;
        } else {
            throw new IllegalArgumentException("Unknown property type: " + name);
        }
    }

    /**
     * Return the property type names including or excluding 'undefined' depending
     * on the specified flag.
     *
     * @param includeUndefined If true the returned array will contain the name
     * of the 'undefined' property type.
     * @return array of property type names.
     */
    public static String[] getPropertyTypeNames(boolean includeUndefined) {
        if (includeUndefined) {
            return PROPERTY_TYPES_NAMES.toArray(new String[PROPERTY_TYPES_NAMES.size()]);
        } else {
            String[] typeNames = new String[PROPERTY_TYPES_NAMES.size()-1];
            int i = 0;
            for (String name : PROPERTY_TYPES_NAMES) {
                if (!PropertyType.TYPENAME_UNDEFINED.equals(name)) {
                    typeNames[i++] = name;
                }
            }
            return typeNames;
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     * In case it has to create the Node all non-existent intermediate path-elements
     * will be created with the given NodeType.
     *
     * <p>
     * Changes made are not saved by this method, so <code>session.save()</code>
     * has to be called to persist them.
     *
     * @param absolutePath     absolute path to create
     * @param nodeType to use for creation of nodes If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param session  to use
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getOrCreateByPath(String absolutePath, String nodeType, Session session)
            throws RepositoryException {
        return getOrCreateByPath(absolutePath, false, nodeType, nodeType, session, false);
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     * In case it has to create the Node all non-existent intermediate path-elements
     * will be created with the given intermediate node type and the returned node
     * will be created with the given nodeType.
     *
     * @param absolutePath         absolute path to create
     * @param intermediateNodeType to use for creation of intermediate nodes. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param nodeType             to use for creation of the final node. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param session              to use
     * @param autoSave             Should save be called when a new node is created?
     * @return the Node at absolutePath
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getOrCreateByPath(String absolutePath,
                                         String intermediateNodeType,
                                         String nodeType, Session session,
                                         boolean autoSave)
            throws RepositoryException {
        return getOrCreateByPath(absolutePath, false, intermediateNodeType, nodeType, session, autoSave);
    }

    /**
     * Creates a {@link javax.jcr.Node Node} at the given Path. In case it has
     * to create the Node all non-existent intermediate path-elements will be
     * created with the given intermediate node type and the returned node will
     * be created with the given nodeType.
     *
     * <p>
     * If the path points to an existing node, the leaf node name will be
     * regarded as a name hint and a unique node name will be created by
     * appending a number to the given name (eg. <code>/some/path/foobar2</code>).
     * Please note that <b>the uniqueness check is not an atomic JCR operation</b>,
     * so it is possible that you get a {@link RepositoryException} (path
     * already exists) if another concurrent session created the same node in
     * the meantime.
     *
     * <p>
     * Changes made are not saved by this method, so <code>session.save()</code>
     * has to be called to persist them.
     *
     * @param pathHint
     *            path to create
     * @param nodeType
     *            to use for creation of nodes. . If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param session
     *            to use
     * @return the newly created Node
     * @throws RepositoryException
     *             in case of exception accessing the Repository
     */
    public static Node getOrCreateUniqueByPath(String pathHint, String nodeType, Session session)
            throws RepositoryException {
        return getOrCreateByPath(pathHint, true, nodeType, nodeType, session, false);
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path. In
     * case it has to create the Node all non-existent intermediate
     * path-elements will be created with the given intermediate node type and
     * the returned node will be created with the given nodeType.
     *
     * <p>
     * If the parameter <code>createUniqueLeaf</code> is set, it will not get
     * an existing node but rather try to create a unique node by appending a
     * number to the last path element (leaf node). Please note that <b>the
     * uniqueness check is not an atomic JCR operation</b>, so it is possible
     * that you get a {@link RepositoryException} (path already exists) if
     * another concurrent session created the same node in the meantime.
     *
     * @param absolutePath
     *            absolute path to create
     * @param createUniqueLeaf
     *            whether the leaf of the path should be regarded as a name hint
     *            and a unique node name should be created by appending a number
     *            to the given name (eg. <code>/some/path/foobar2</code>)
     * @param intermediateNodeType
     *            to use for creation of intermediate nodes. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param nodeType
     *            to use for creation of the final node. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param session
     *            to use
     * @param autoSave
     *            Should save be called when a new node is created?
     * @return the Node at absolutePath
     * @throws RepositoryException
     *             in case of exception accessing the Repository
     */
    public static Node getOrCreateByPath(String absolutePath,
                                         boolean createUniqueLeaf,
                                         String intermediateNodeType,
                                         String nodeType, Session session,
                                         boolean autoSave)
            throws RepositoryException {
        if (absolutePath == null || absolutePath.length() == 0 || "/".equals(absolutePath)) {
            // path denotes root node
            return session.getRootNode();
        } else if (!absolutePath.startsWith("/")) {
            throw new IllegalArgumentException("not an absolute path: " + absolutePath);
        } else if (session.nodeExists(absolutePath) && !createUniqueLeaf) {
            return session.getNode(absolutePath);
        } else {
            // find deepest existing parent node
            String path = absolutePath;
            int currentIndex = path.lastIndexOf('/');
            String existingPath = null;
            while (currentIndex > 0 && existingPath == null) {
                path = path.substring(0, currentIndex);
                if (session.nodeExists(path)) {
                    existingPath = path;
                } else {
                    currentIndex = path.lastIndexOf('/');
                }
            }
            // create path relative to the root node
            return getOrCreateByPath(existingPath == null ? session.getRootNode() : session.getNode(existingPath),
                    absolutePath.substring(currentIndex + 1), createUniqueLeaf, intermediateNodeType, nodeType, autoSave);
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node node} at the given path. In
     * case it has to create the node, nodes for all non-existent intermediate
     * path-elements will be created with the given intermediate node type and
     * the returned node will be created with the given nodeType.
     * <b>Note</b>: When the given path contains parent elements this method might
     * create multiple nodes at leaf position (e.g "a/../b" will create the
     * child nodes "a" and "b" on the current node).
     *
     * <p>
     * If the node name points to an existing node, the node name will be
     * regarded as a name hint and a unique node name will be created by
     * appending a number to the given name (eg. <code>/some/path/foobar2</code>).
     * Please note that <b>the uniqueness check is not an atomic JCR operation</b>,
     * so it is possible that you get a {@link RepositoryException} (path
     * already exists) if another concurrent session created the same node in
     * the meantime.
     *
     * <p>
     * Changes made are not saved by this method, so <code>session.save()</code>
     * has to be called to persist them.
     *
     * @param parent
     *            existing parent node for the new node
     * @param nodeNameHint
     *            name hint for the new node
     * @param nodeType
     *            to use for creation of the node. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @return the newly created Node
     * @throws RepositoryException
     *             in case of exception accessing the Repository
     */
    public static Node getOrCreateUniqueByPath(Node parent,
                                               String nodeNameHint,
                                               String nodeType)
            throws RepositoryException {
        return getOrCreateByPath(parent, nodeNameHint, true, nodeType, nodeType, false);
    }

    /**
     * Creates or gets the {@link javax.jcr.Node node} at the given path
     * relative to the baseNode. In case it has to create the node, nodes for
     * all non-existent intermediate path-elements will be created with the given
     * intermediate node type and the returned node will be created with the
     * given nodeType. <b>Note</b>: When the given path contains parent elements
     * this method might create multiple nodes at leaf position (e.g "a/../b"
     * will create the child nodes "a" and "b" on the current node).
     * <p>
     * If the parameter <code>createUniqueLeaf</code> is set, it will not get
     * an existing node but rather try to create a unique node by appending a
     * number to the last path element (leaf node). Please note that <b>the
     * uniqueness check is not an atomic JCR operation</b>, so it is possible
     * that you get a {@link RepositoryException} (path already exists) if
     * another concurrent session created the same node in the meantime.
     *
     * @param baseNode
     *            existing node that should be the base for the relative path
     * @param path
     *            relative path to create
     * @param createUniqueLeaf
     *            whether the leaf of the path should be regarded as a name hint
     *            and a unique node name should be created by appending a number
     *            to the given name (eg. <code>/some/path/foobar2</code>)
     * @param intermediateNodeType
     *            to use for creation of intermediate nodes. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param nodeType
     *            to use for creation of the final node. If <code>null</code> the node type
     *            is determined by the child node definitions of the parent node.
     * @param autoSave
     *            Should save be called when a new node is created?
     * @return the Node at path
     * @throws RepositoryException
     *             in case of exception accessing the Repository
     */
    public static Node getOrCreateByPath(Node baseNode,
                                         String path,
                                         boolean createUniqueLeaf,
                                         String intermediateNodeType,
                                         String nodeType,
                                         boolean autoSave)
            throws RepositoryException {

        if (!createUniqueLeaf && baseNode.hasNode(path)) {
            // node at path already exists, quicker way
            return baseNode.getNode(path);
        }

        // find the parent that exists
        // we can start from the deepest child in tree
        String fullPath = baseNode.getPath().equals("/") ? "/" + path : baseNode.getPath() + "/" + path;
        int currentIndex = fullPath.lastIndexOf('/');
        String temp = fullPath;
        String existingPath = null;
        while (currentIndex > 0) {
            temp = temp.substring(0, currentIndex);
            // break when first existing parent is found
            if (baseNode.getSession().itemExists(temp)) {
                existingPath = temp;
                break;
            }
            currentIndex = temp.lastIndexOf('/');
        }

        if (existingPath != null) {
            baseNode = baseNode.getSession().getNode(existingPath);
            path = fullPath.substring(existingPath.length() + 1);
        }

        Node node = baseNode;
        int pos = path.lastIndexOf('/');

        // intermediate path elements
        if (pos != -1) {
            final StringTokenizer st = new StringTokenizer(path.substring(0, pos), "/");
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                if (!node.hasNode(token)) {
                    try {
                        if (intermediateNodeType != null) {
                            node.addNode(token, intermediateNodeType);
                        } else {
                            node.addNode(token);
                        }
                        if (autoSave) {
                            node.getSession().save();
                        }
                    } catch (RepositoryException e) {
                        // we ignore this as this folder might be created from a different task
                        node.refresh(false);
                    }
                }
                node = node.getNode(token);
            }
            path = path.substring(pos + 1);
        }

        // last path element (path = leaf node name)
        if (!node.hasNode(path)) {
            if (nodeType != null) {
                node.addNode(path, nodeType);
            } else {
                node.addNode(path);
            }
            if (autoSave) {
                node.getSession().save();
            }
        } else if (createUniqueLeaf) {
            // leaf node already exists, create new unique name
            String leafNodeName;
            int i = 0;
            do {
                leafNodeName = path + String.valueOf(i);
                i++;
            } while (node.hasNode(leafNodeName));

            Node leaf;
            if (nodeType != null) {
                leaf = node.addNode(leafNodeName, nodeType);
            } else {
                leaf = node.addNode(leafNodeName);
            }
            if (autoSave) {
                node.getSession().save();
            }
            return leaf;
        }

        return node.getNode(path);
    }

    /**
     * Get the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath relative path to the node to get
     * @return  the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            return baseNode.getNode(relPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets the node at <code>absPath</code> or <code>null</code> if no such node exists.
     *
     * @param absPath  the absolute path to the node to return
     * @param session  to use
     * @return  the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(String absPath, Session session) throws RepositoryException {
        try {
            return session.getNode(absPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static String getStringProperty(Node baseNode, String relPath, String defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getString();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static long getLongProperty(Node baseNode, String relPath, long defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static double getDoubleProperty(Node baseNode, String relPath, double defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static boolean getBooleanProperty(Node baseNode, String relPath, boolean defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Node baseNode, String relPath, Calendar defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDate();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the decimal property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the decimal property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Node baseNode, String relPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDecimal();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Node baseNode, String relPath, Binary defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getBinary();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the string property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the string property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static String getStringProperty(Session session, String absPath, String defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getString();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session  to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static long getLongProperty(Session session, String absPath, long defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static double getDoubleProperty(Session session, String absPath, double defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static boolean getBooleanProperty(Session session, String absPath, boolean defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the date property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the date property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Session session, String absPath, Calendar defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDate();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the decimal property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the decimal property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Session session, String absPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDecimal();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the binary property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the binary property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Session session, String absPath, Binary defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getBinary();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }
}
