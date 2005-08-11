/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.chain;

import java.io.PrintWriter;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.chain.Context;
import org.apache.commons.collections.IteratorUtils;

/**
 * Helper class for getting and setting context attributes.
 */
public class CtxHelper
{
    /** Current node key */
    private static String CURRENT_NODE_KEY = "jcr.current";

    /** repository key */
    private static String REPOSITORY_KEY = "jcr.repository";

    /** session key */
    private static String SESSION_KEY = "jcr.session";

    /** session key */
    private static String OUTPUT_KEY = "jcr.output";

    /**
     * Sets the current output
     * 
     * @param ctx
     * @param output
     */
    public static void setOutput(Context ctx, PrintWriter out)
    {
        if (out == null)
        {
            ctx.remove(OUTPUT_KEY);
        } else
        {
            ctx.put(OUTPUT_KEY, out);
        }
    }

    /**
     * Sets the current working Node
     * 
     * @param ctx
     * @param node
     * @throws JcrCommandException
     */
    public static void setCurrentNode(Context ctx, Node node)
            throws JcrCommandException
    {
        if (node == null)
        {
            ctx.remove(CURRENT_NODE_KEY);
        } else
        {
            ctx.put(CURRENT_NODE_KEY, node);
        }
    }

    /**
     * Sets the current working Repository
     * 
     * @param ctx
     * @param repository
     */
    public static void setRepository(Context ctx, Repository repository)
    {
        if (repository == null)
        {
            ctx.remove(REPOSITORY_KEY);
        } else
        {
            ctx.put(REPOSITORY_KEY, repository);
        }

    }

    /**
     * Sets the current working Session
     * 
     * @param ctx
     * @param session
     */
    public static void setSession(Context ctx, Session session)
    {
        if (session == null)
        {
            ctx.remove(SESSION_KEY);
        } else
        {
            ctx.put(SESSION_KEY, session);
        }
    }

    /**
     * Gets the current working Node
     * 
     * @param ctx
     * @return
     */
    public static PrintWriter getOutput(Context ctx)
    {
        PrintWriter out = (PrintWriter) ctx.get(OUTPUT_KEY);
        if (out == null)
        {
            out = new PrintWriter(System.out, true);
        }
        return out;
    }

    /**
     * Gets the current working Node
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     */
    public static Node getCurrentNode(Context ctx) throws JcrCommandException
    {
        Node n = (Node) ctx.get(CURRENT_NODE_KEY);
        if (n == null)
        {
            throw new JcrCommandException("no.current.node");
        }
        return n;
    }

    /**
     * Gets the current working Repository
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     */
    public static Repository getRepository(Context ctx)
            throws JcrCommandException
    {
        return (Repository) ctx.get(REPOSITORY_KEY);
    }

    /**
     * Gets the current working Session
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     */
    public static Session getSession(Context ctx) throws JcrCommandException
    {
        return (Session) ctx.get(SESSION_KEY);
    }

    /**
     * Gets node at the given path
     * 
     * @param ctx
     * @param path
     * @return
     * @throws JcrCommandException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static Node getNode(Context ctx, String path)
            throws JcrCommandException, PathNotFoundException,
            RepositoryException
    {
        Node current = (Node) ctx.get(CURRENT_NODE_KEY);
        Node node = null;
        if (path.equals("/"))
        {
            node = current.getSession().getRootNode();
        } else if (path.startsWith("/"))
        {
            node = current.getSession().getRootNode()
                .getNode(path.substring(1));
        } else
        {
            node = current.getNode(path);
        }

        return node;
    }

    /**
     * Gets the Item at the given path
     * 
     * @param ctx
     * @param path
     * @return
     * @throws JcrCommandException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static Item getItem(Context ctx, String path)
            throws JcrCommandException, PathNotFoundException,
            RepositoryException
    {
        Node current = (Node) ctx.get(CURRENT_NODE_KEY);
        Item i = null;
        if (path.equals("/"))
        {
            i = current.getSession().getRootNode();
        } else if (path.startsWith("/"))
        {
            i = current.getSession().getItem(path);
        } else
        {
            String newPath = current.getPath();
            if (!newPath.endsWith("/"))
            {
                newPath += "/";
            }
            newPath += path;
            i = current.getSession().getItem(newPath);
        }

        return i;
    }

    /**
     * Returns true if the node exists at the given path
     * 
     * @param ctx
     * @param path
     * @return
     * @throws JcrCommandException
     * @throws RepositoryException
     */
    public static boolean hasNode(Context ctx, String path)
            throws JcrCommandException, RepositoryException
    {
        if (path.equals("/"))
        {
            return true;
        } else if (path.startsWith("/"))
        {
            return getSession(ctx).getRootNode().hasNode(path.substring(1));
        } else
        {
            Node current = (Node) ctx.get(CURRENT_NODE_KEY);
            return current.hasNode(path);
        }
    }

    /**
     * Get the nodes under the current working node for the given pattern
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     * @throws RepositoryException
     */
    public static NodeIterator getNodes(Context ctx, String pattern)
            throws JcrCommandException, RepositoryException
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        if (pattern != null)
        {
            return n.getNodes(pattern);
        } else
        {
            return n.getNodes();
        }
    }

    /**
     * Get the properties under the current working node for the given pattern
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     * @throws RepositoryException
     */
    public static PropertyIterator getProperties(Context ctx, String pattern)
            throws JcrCommandException, RepositoryException
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        if (pattern != null)
        {
            return n.getProperties(pattern);
        } else
        {
            return n.getProperties();
        }
    }

    /**
     * Get the items under the current working node for the given pattern
     * 
     * @param ctx
     * @return
     * @throws JcrCommandException
     * @throws RepositoryException
     */
    public static Iterator getItems(Context ctx, String pattern)
            throws JcrCommandException, RepositoryException
    {
        return IteratorUtils.chainedIterator(getNodes(ctx, pattern),
            getProperties(ctx, pattern));
    }

    /**
     * @param literal
     * @param key
     * @param ctx
     * @return the literal value if it's not null. Otherwise it returns the
     *         context attribute under the given key.
     */
    public static String getAttr(String literal, String key, Context ctx)
    {
        if (literal != null)
        {
            return literal;
        }

        if (key == null)
        {
            return null;
        }

        return (String) ctx.get(key);
    }

    /**
     * 
     * @param literal
     * @param key
     * @param defa
     * @param ctx
     * @return the literal value if it's not null. Otherwise it returns the
     *         context attribute under the given key.
     */
    public static boolean getBooleanAttr(
        String literal,
        String key,
        boolean defa,
        Context ctx)
    {
        String retu = getAttr(literal, key, ctx);
        if (retu == null)
        {
            return defa;
        } else
        {
            return Boolean.valueOf(retu).booleanValue();
        }
    }

    /**
     * 
     * @param key
     * @param defa
     * @param ctx
     * @return the literal value if it's not null. Otherwise it returns the
     *         context attribute under the given key. If both are null it
     *         returns the given default value.
     */
    public static boolean getBooleanAttr(String key, boolean defa, Context ctx)
    {
        String retu = getAttr(null, key, ctx);
        if (retu == null)
        {
            return defa;
        } else
        {
            return Boolean.valueOf(retu).booleanValue();
        }
    }

    /**
     * 
     * @param literal
     * @param key
     * @param defa
     * @param ctx
     * @return the literal value if it's not null. Otherwise it returns the
     *         context attribute under the given key. If both are null it
     *         returns the given default value.
     */
    public static int getIntAttr(
        String literal,
        String key,
        int defa,
        Context ctx)
    {
        String retu = getAttr(literal, key, ctx);
        if (retu == null)
        {
            return defa;
        } else
        {
            return Integer.parseInt(retu);
        }
    }

    /**
     * 
     * @param literal
     * @param key
     * @param defa
     * @param ctx
     * @return the literal value if it's not null. Otherwise it returns the
     *         context attribute under the given key. If both are null it
     *         returns the given default value.
     * @throws JcrCommandException
     */
    public static String getAttr(
        String literal,
        String key,
        String defa,
        Context ctx) throws JcrCommandException
    {
        String retu = getAttr(literal, key, ctx);
        if (retu == null)
        {
            return defa;
        } else
        {
            return retu;
        }
    }

}
