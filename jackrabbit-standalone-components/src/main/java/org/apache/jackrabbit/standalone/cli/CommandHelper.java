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
package org.apache.jackrabbit.standalone.cli;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.chain.Context;
import org.apache.commons.collections4.IteratorUtils;

/**
 * Utility class for getting and setting context attributes.
 */
public final class CommandHelper {
    /** bundle */
    private static ResourceBundle bundle = ResourceBundle
        .getBundle(CommandHelper.class.getPackage().getName() + ".resources");

    /** Current node key */
    public static final String CURRENT_NODE_KEY = "jcr.current";

    /** repository key */
    public static final String REPOSITORY_KEY = "jcr.repository";

    /** session key */
    public static final String SESSION_KEY = "jcr.session";

    /** output key */
    public static final String OUTPUT_KEY = "jcr.output";

    /** address key */
    public static final String REPO_ADDRESS_KEY = "jcr.repo.address";
    
    /**
     * should never get called
     */
    private CommandHelper() {
        super();
    }

    /**
     * Sets the current <code>PrintWriter</code>.
     * @param ctx
     *        the <code>Context</code>
     * @param out
     *        the <code>PrintWriter</code>
     */
    public static void setOutput(Context ctx, PrintWriter out) {
        if (out == null) {
            ctx.remove(OUTPUT_KEY);
        } else {
            ctx.put(OUTPUT_KEY, out);
        }
    }

    /**
     * Sets the current working <code>Node</code>.
     * @param ctx
     *        the <code>Context</code>
     * @param node
     *        the current working <code>Node</code>.
     */
    public static void setCurrentNode(Context ctx, Node node) {
        if (node == null) {
            ctx.remove(CURRENT_NODE_KEY);
        } else {
            ctx.put(CURRENT_NODE_KEY, node);
        }
    }

    /**
     * Sets the current working <code>Repository</code>
     * @param ctx
     *        the <code>Context</code>
     * @param repository
     *        the current working <code>Repository</code>
     */
    public static void setRepository(Context ctx, Repository repository, String address) {
        if (repository == null) {
            ctx.remove(REPOSITORY_KEY);
            ctx.remove(REPO_ADDRESS_KEY);
        } else {
            ctx.put(REPOSITORY_KEY, repository);
            ctx.put(REPO_ADDRESS_KEY, address);
        }
    }

    /**
     * Sets the current working <code>Session</code>
     * @param ctx
     *        the <code>Context</code>
     * @param session
     *        the current working <code>Session</code>
     * @throws CommandException if there's an open working <code>Session</code>
     */
    public static void setSession(Context ctx, Session session) throws CommandException {
        if (session == null) {
            ctx.remove(SESSION_KEY);
        } else {
            if (ctx.get(SESSION_KEY) != null) {
                throw new CommandException("exception.already.logged.in");
            }
            ctx.put(SESSION_KEY, session);
        }
    }

    /**
     * Gets the current <code>PrintWriter</code>
     * @param ctx
     *        the <code>Context</code>
     * @return the current <code>PrintWriter</code>
     */
    public static PrintWriter getOutput(Context ctx) {
        PrintWriter out = (PrintWriter) ctx.get(OUTPUT_KEY);
        if (out == null) {
            out = new PrintWriter(System.out, true);
        }
        return out;
    }

    /**
     * Gets the current working <code>Node</code>
     * @param ctx
     *        the <code>Context</code>
     * @return the current working <code>Node</code>
     * @throws CommandException
     *         if the current working <code>Node</code> can't be found.
     */
    public static Node getCurrentNode(Context ctx) throws CommandException {
        Node n = (Node) ctx.get(CURRENT_NODE_KEY);
        if (n == null) {
            throw new CommandException("exception.no.current.node");
        }
        return n;
    }

    /**
     * Gets the current working <code>Repository</code>
     * @param ctx
     *        the <code>Context</code>
     * @return the current working <code>Repository</code>
     * @throws CommandException
     *         if the current working <code>Repository</code> is unset.
     */
    public static Repository getRepository(Context ctx) throws CommandException {
        Repository r = (Repository) ctx.get(REPOSITORY_KEY);
        if (r == null) {
            throw new CommandException("exception.no.current.repository");
        }
        return r;
    }
    
    public static String getRepositoryAddress(Context ctx) throws CommandException {
        String a = (String) ctx.get(REPO_ADDRESS_KEY);
        if (a == null) {
            throw new CommandException("exception.no.current.repository");
        }
        return a;        
    }

    /**
     * Gets the current working <code>Session</code>
     * @param ctx
     *        the <code>Context</code>
     * @return the current working <code>Session</code>
     * @throws CommandException
     *         if the current working <code>Session</code> is unset.
     */
    public static Session getSession(Context ctx) throws CommandException {
        Session s = (Session) ctx.get(SESSION_KEY);
        if (s == null) {
            throw new CommandException("exception.no.current.session");
        }
        return s;
    }

    /**
     * Gets the <code>Node</code> at the given path.
     * @param ctx
     *        the <code>Context</code>
     * @param path
     *        the path to the <code>Node</code>
     * @return the <code>Node</code> at the given path
     * @throws CommandException
     *         if the <code>Node</code> isn't found.
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    public static Node getNode(Context ctx, String path)
            throws CommandException, RepositoryException {
        try {
            Item i = getItem(ctx, path);
            if (!i.isNode()) {
                throw new CommandException("exception.no.node.at",
                    new String[] {
                        path
                    });
            }
            return (Node) i;
        } catch (PathNotFoundException e) {
            throw new CommandException("exception.no.node.at", new String[] {
                path
            });
        }
    }

    /**
     * Gets the <code>Item</code> at the given path. <br>
     * If the path is null it returns the current working <code>Node</code>.
     * @param ctx
     *        the <code>Context</code>
     * @param path
     *        the path to the <code>Item</code>
     * @return the <code>Item</code> at the given path
     * @throws CommandException
     *         if a <code>Command</code> internal error occurs.
     * @throws PathNotFoundException
     *         if there's no <code>Node</code> at the given path.
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    public static Item getItem(Context ctx, String path)
            throws CommandException, PathNotFoundException, RepositoryException {
        Node current = getCurrentNode(ctx);
        Item i = null;

        if (path == null) {
            i = current;
        } else if (path.equals("/")) {
            i = current.getSession().getRootNode();
        } else if (path.startsWith("/")) {
            i = current.getSession().getItem(path);
        } else {
            String newPath = current.getPath();
            // handle the root node
            if (!newPath.endsWith("/")) {
                newPath += "/";
            }
            newPath += path;
            i = current.getSession().getItem(newPath);
        }

        return i;
    }

    /**
     * Checks <code>Node</code> existence.
     * @param ctx
     *        the <code>Context</code>
     * @param path
     *        the path to the <code>Node</code>
     * @return true if the <code>Node</code> exists at the given path
     * @throws CommandException
     *         if the current working <code>Session</code> is unset.
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    public static boolean hasNode(Context ctx, String path)
            throws CommandException, RepositoryException {
        Session s = getSession(ctx);
        if (path.equals("/")) {
            return true;
        } else if (path.startsWith("/")) {
            return s.getRootNode().hasNode(path.substring(1));
        } else {
            Node current = (Node) ctx.get(CURRENT_NODE_KEY);
            return current.hasNode(path);
        }
    }

    /**
     * Gets the <code>Node</code> s under the given <code>Node</code> that
     * match the given pattern.
     * @param ctx
     *        the <code>Context</code>
     * @param node
     *        the parent <code>Node</code>
     * @param pattern
     *        the pattern
     * @return an <code>Iterator</code> that contains the matching nodes
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    public static NodeIterator getNodes(Context ctx, Node node, String pattern)
            throws RepositoryException {
        if (pattern != null) {
            return node.getNodes(pattern);
        } else {
            return node.getNodes();
        }
    }

    /**
     * Gets the <code>Property</code> s under the current working node for the
     * given pattern
     * @param ctx
     *        the <code>Context</code>
     * @param node
     *        the parent <code>Node</code>
     * @param pattern
     *        the pattern
     * @return a <code>PropertyIterator</code>
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    public static PropertyIterator getProperties(
        Context ctx,
        Node node,
        String pattern) throws RepositoryException {
        if (pattern != null) {
            return node.getProperties(pattern);
        } else {
            return node.getProperties();
        }
    }

    /**
     * @return the default <code>ResourceBundle</code>
     */
    public static ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Gets the <code>Item</code>s under the given <code>Node</code> that
     * match the pattern
     * @param ctx
     *        the <code>Context</code>
     * @param node
     *        the parent <code>Node</code>
     * @param pattern
     *        the pattern
     * @return an <code>Iterator</code> with the <code>Item</code> s that
     *         match the given pattern.
     * @throws RepositoryException
     *         if the underlying repository throws a
     *         <code>RepositoryException</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Item> getItems(Context ctx, Node node, String pattern)
            throws RepositoryException {
        return IteratorUtils.chainedIterator(getNodes(ctx, node, pattern),
            getProperties(ctx, node, pattern));
    }
}
