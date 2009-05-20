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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

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
     * Returns the nodes in the shared set of the given node as an
     * {@link Iterable} for use in a Java 5 for-each loop. The return value
     * encapsulates the {@link Node#getSharedSet()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node shared node
     * @return nodes in the shared set
     */
    public static Iterable<Node> getSharedSet(final Node node) {
        return new Iterable<Node>() {
            @SuppressWarnings("unchecked")
            public Iterator<Node> iterator() {
                try {
                    return node.getSharedSet();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access child nodes of " + node, e);
                }
            }
        };
    }

    /**
     * Returns the child nodes of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getNodes()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node parent node
     * @return child nodes
     */
    public static Iterable<Node> getChildNodes(final Node node) {
        return new Iterable<Node>() {
            @SuppressWarnings("unchecked")
            public Iterator<Node> iterator() {
                try {
                    return node.getNodes();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access child nodes of " + node, e);
                }
            }
        };
    }

    /**
     * Returns matching child nodes of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getNodes(String)} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node parent node
     * @param pattern node name pattern
     * @return matching child nodes
     */
    public static Iterable<Node> getChildNodes(
            final Node node, final String pattern) {
        return new Iterable<Node>() {
            @SuppressWarnings("unchecked")
            public Iterator<Node> iterator() {
                try {
                    return node.getNodes(pattern);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access child nodes of " + node, e);
                }
            }
        };
    }

    /**
     * Returns matching child nodes of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getNodes(String[])} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node parent node
     * @param globs node name globs
     * @return matching child nodes
     */
    public static Iterable<Node> getChildNodes(
            final Node node, final String[] globs) {
        return new Iterable<Node>() {
            @SuppressWarnings("unchecked")
            public Iterator<Node> iterator() {
                try {
                    return node.getNodes(globs);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access child nodes of " + node, e);
                }
            }
        };
    }

    /**
     * Returns the properties of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getProperties()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node node
     * @return properties of the node
     */
    public static Iterable<Property> getProperties(final Node node) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getProperties();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access properties of " + node, e);
                }
            }
        };
    }

    /**
     * Returns matching properties of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getProperties(String)} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node node
     * @param pattern property name pattern
     * @return matching properties of the node
     */
    public static Iterable<Property> getProperties(
            final Node node, final String pattern) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getProperties(pattern);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access properties of " + node, e);
                }
            }
        };
    }

    /**
     * Returns matching properties of the given node as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link Node#getProperty(String[])} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node node
     * @param globs property name globs
     * @return matching properties of the node
     */
    public static Iterable<Property> getProperties(
            final Node node, final String[] globs) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    // TODO: method name will be changed in JCR 2.0
                    return node.getProperty(globs);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access properties of " + node, e);
                }
            }
        };
    }

    /**
     * Returns the references that point to the given node as an
     * {@link Iterable} for use in a Java 5 for-each loop. The return value
     * encapsulates the {@link Node#getReferences()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param node reference target
     * @return references that point to the given node
     */
    public static Iterable<Property> getReferences(final Node node) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getReferences();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access references of " + node, e);
                }
            }
        };
    }

    /**
     * Returns specifically named references that point to the given node as
     * an {@link Iterable} for use in a Java 5 for-each loop. The return value
     * encapsulates the {@link Node#getReferences(String)} method call.
     * Potential {@link RepositoryException}s are converted to
     * {@link RuntimeException}s.
     *
     * @param node reference target
     * @param name reference property name
     * @return references with the given name that point to the given node
     */
    public static Iterable<Property> getReferences(
            final Node node, final String name) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getReferences(name);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access references of " + node, e);
                }
            }
        };
    }

    /**
     * Returns the weak references that point to the given node as an
     * {@link Iterable} for use in a Java 5 for-each loop. The return value
     * encapsulates the {@link Node#getWeakReferences()} method call.
     * Potential {@link RepositoryException}s are converted to
     * {@link RuntimeException}s.
     *
     * @param node reference target
     * @return weak references that point to the given node
     */
    public static Iterable<Property> getWeakReferences(final Node node) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getWeakReferences();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access references of " + node, e);
                }
            }
        };
    }

    /**
     * Returns specifically named weak references that point to the given node
     * as an {@link Iterable} for use in a Java 5 for-each loop. The return
     * value encapsulates the {@link Node#getWeakReferences(String)} method
     * call. Potential {@link RepositoryException}s are converted to
     * {@link RuntimeException}s.
     *
     * @param node reference target
     * @param name reference property name
     * @return weak references with the given name that point to the given node
     */
    public static Iterable<Property> getWeakReferences(
            final Node node, final String name) {
        return new Iterable<Property>() {
            @SuppressWarnings("unchecked")
            public Iterator<Property> iterator() {
                try {
                    return node.getWeakReferences(name);
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access references of " + node, e);
                }
            }
        };
    }

    /**
     * Returns the nodes in the given query result as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link QueryResult#getNodes()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param result query result
     * @return nodes in the query result
     */
    public static Iterable<Node> getNodes(final QueryResult result) {
        return new Iterable<Node>() {
            @SuppressWarnings("unchecked")
            public Iterator<Node> iterator() {
                try {
                    return result.getNodes();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access nodes in " + result, e);
                }
            }
        };
    }

    /**
     * Returns the rows in the given query result as an {@link Iterable}
     * for use in a Java 5 for-each loop. The return value encapsulates
     * the {@link QueryResult#getRows()} method call. Potential
     * {@link RepositoryException}s are converted to {@link RuntimeException}s.
     *
     * @param result query result
     * @return rows in the query result
     */
    public static Iterable<Row> getRows(final QueryResult result) {
        return new Iterable<Row>() {
            @SuppressWarnings("unchecked")
            public Iterator<Row> iterator() {
                try {
                    return result.getRows();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Unable to access rows in " + result, e);
                }
            }
        };
    }

}
