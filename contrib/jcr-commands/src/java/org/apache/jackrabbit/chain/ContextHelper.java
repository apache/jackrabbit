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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.chain.Context;

/**
 * Context helper to facilitate the Commands to access common context's
 * resources
 */
public class ContextHelper
{
    /** Current node key */
    private static String CURRENT_NODE_KEY = "currentNode";

    /** repository key */
    private static String REPOSITORY_KEY = "repository";

    /** session key */
    private static String SESSION_KEY = "session";

    /**
     * Sets the current working Node
     * @param ctx
     * @param node
     */
    public static void setCurrentNode(Context ctx, Node node)
    {
        ctx.put(CURRENT_NODE_KEY, node);
    }

    /**
     * Sets the current working Repository
     * @param ctx
     * @param repository
     */
    public static void setRepository(Context ctx, Repository repository)
    {
        ctx.put(REPOSITORY_KEY, repository);
    }

    /**
     * Sets the current working Session
     * @param ctx
     * @param session
     */
    public static void setSession(Context ctx, Session session)
    {
        ctx.put(SESSION_KEY, session);
    }

    /**
     * Gets the current working Node
     * @param ctx
     * @return
     */
    public static Node getCurrentNode(Context ctx)
    {
        return (Node) ctx.get(CURRENT_NODE_KEY);
    }

    /**
     * Gets the current working Repository
     * @param ctx
     * @return
     */
    public static Repository getRepository(Context ctx)
    {
        return (Repository) ctx.get(REPOSITORY_KEY);
    }

    /**
     * Gets the current working Session
     * @param ctx
     * @return
     */
    public static Session getSession(Context ctx)
    {
        return (Session) ctx.get(SESSION_KEY);
    }

}
