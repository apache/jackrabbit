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
package org.apache.jackrabbit.command;

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
import org.apache.commons.collections.IteratorUtils;

/**
 * Helper class for getting and setting context attributes.
 */
public class CommandHelper
{
	/** bundle */
	private static ResourceBundle bundle = ResourceBundle
			.getBundle(CommandHelper.class.getPackage().getName()
					+ ".resources");

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
	 * @throws CommandException
	 */
	public static void setCurrentNode(Context ctx, Node node)
			throws CommandException
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
	 * @throws CommandException
	 */
	public static Node getCurrentNode(Context ctx) throws CommandException
	{
		Node n = (Node) ctx.get(CURRENT_NODE_KEY);
		if (n == null)
		{
			throw new CommandException("exception.no.current.node");
		}
		return n;
	}

	/**
	 * Gets the current working Repository
	 * 
	 * @param ctx
	 * @return
	 * @throws CommandException
	 */
	public static Repository getRepository(Context ctx) throws CommandException
	{
		return (Repository) ctx.get(REPOSITORY_KEY);
	}

	/**
	 * Gets the current working Session
	 * 
	 * @param ctx
	 * @return
	 * @throws CommandException
	 */
	public static Session getSession(Context ctx) throws CommandException
	{
		return (Session) ctx.get(SESSION_KEY);
	}

	/**
	 * Gets node at the given path.
	 * 
	 * @param ctx
	 * @param path
	 * @return
	 * @throws CommandException
	 * @throws PathNotFoundException
	 * @throws RepositoryException
	 */
	public static Node getNode(Context ctx, String path)
			throws CommandException, PathNotFoundException, RepositoryException
	{
		Item i = getItem(ctx, path);
		if (!i.isNode())
		{
			throw new PathNotFoundException("the path " + i.getPath()
					+ "refers to a Property");
		}
		return (Node) i;
	}

	/**
	 * Gets the Item at the given path. <br>
	 * If the path is null it returns the current working node.
	 * 
	 * @param ctx
	 * @param path
	 * @return the Item for the given path
	 * @throws CommandException
	 * @throws PathNotFoundException
	 * @throws RepositoryException
	 */
	public static Item getItem(Context ctx, String path)
			throws CommandException, PathNotFoundException, RepositoryException
	{
		Node current = (Node) ctx.get(CURRENT_NODE_KEY);
		Item i = null;

		if (path == null)
		{
			i = current;
		} else if (path.equals("/"))
		{
			i = current.getSession().getRootNode();
		} else if (path.startsWith("/"))
		{
			i = current.getSession().getItem(path);
		} else
		{
			String newPath = current.getPath();
			// handle the root node
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
	 * @throws CommandException
	 * @throws RepositoryException
	 */
	public static boolean hasNode(Context ctx, String path)
			throws CommandException, RepositoryException
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
	 * Get the nodes under the given node that match the given pattern.
	 * 
	 * @param ctx
	 *            Command Context
	 * @param node
	 *            Parent node
	 * @param pattern
	 * @return an Iterator that contains the matching nodes
	 * @throws CommandException
	 * @throws RepositoryException
	 */
	public static NodeIterator getNodes(Context ctx, Node node, String pattern)
			throws CommandException, RepositoryException
	{
		if (pattern != null)
		{
			return node.getNodes(pattern);
		} else
		{
			return node.getNodes();
		}
	}

	/**
	 * Get the properties under the current working node for the given pattern
	 * 
	 * @param ctx
	 * @param node
	 *            Parent node
	 * @param pattern
	 * @return
	 * @throws CommandException
	 * @throws RepositoryException
	 */
	public static PropertyIterator getProperties(Context ctx, Node node,
			String pattern) throws CommandException, RepositoryException
	{
		if (pattern != null)
		{
			return node.getProperties(pattern);
		} else
		{
			return node.getProperties();
		}
	}

	/**
	 * @return default resource bundle
	 */
	public static ResourceBundle getBundle()
	{
		return bundle;
	}

	/**
	 * Get the items under the given node that match the pattern
	 * 
	 * @param ctx
	 * @return
	 * @throws CommandException
	 * @throws RepositoryException
	 */
	public static Iterator getItems(Context ctx, Node node, String pattern)
			throws CommandException, RepositoryException
	{
		return IteratorUtils.chainedIterator(getNodes(ctx, node, pattern),
				getProperties(ctx, node, pattern));
	}

}
