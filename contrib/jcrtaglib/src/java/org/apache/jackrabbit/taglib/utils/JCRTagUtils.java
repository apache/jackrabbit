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
package org.apache.jackrabbit.taglib.utils;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.apache.jackrabbit.taglib.bean.BeanFactory;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * JCR taglib utils
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class JCRTagUtils
{
	private static Logger log = Logger.getLogger(JCRTagUtils.class);
	
	private static BeanFactory beanFactory ;

    /**
     * Get an object from jndi
     * 
     * @param name
     * @return @throws
     *         JspException
     */
    public static Object lookup(String name)
    {
        Object o = null;
        try
        {
            InitialContext ctx = new InitialContext();
            Context env = (Context) ctx.lookup("java:comp/env");
            o = env.lookup(name);
        } catch (NamingException e)
        {
            String msg = "Unable to get object from jndi: " + name + ". "
                    + e.getMessage();
            log.error(msg, e);
        }
        return o;
    }

    /**
     * Get a session for the given key
     * 
     * @param pc
     * @param key
     * @throws JspException
     */
    public static Session getSession(String tagName, String expression,
            Tag tag, PageContext pageCtx) throws JspException
    {
        Session session = null;
        try
        {
            session = (Session) ExpressionUtil.evalNotNull(tagName, "session",
                    expression, Object.class, tag, pageCtx);
            if (log.isDebugEnabled())
            {
                log.debug("Session found. User=" + session.getUserID());
            }

        } catch (ClassCastException e)
        {
            String msg = "Unable to get session for expression= " + expression
                    + ". " + e.getMessage();
            log.error(msg, e);
            throw new IllegalArgumentException(msg);
        }
        return session;
    }

    /**
     * <p>
     * Get a node.
     * </p>
     * <p>
     * The value can be a String or a EL expression referencing a Node instance.
     * </p>
     * 
     * @param tagName
     * @param attribute
     * @param expression
     * @param tag
     * @param pageCtx
     * @param session
     * @return a node
     * @throws JspException
     * @throws RepositoryException
     * @throws PathNotFoundException
     */
    public static Item getItem(String tagName, String expression, Tag tag,
            PageContext pageCtx, Session session) throws JspException,
            PathNotFoundException, RepositoryException
    {
        Item item = null;
        Object o = (Object) ExpressionUtil.evalNotNull(tagName, "node",
                expression, Object.class, tag, pageCtx);
        // Path to the node
        if (o instanceof String)
        {
            String path = (String) o;
            if (path.startsWith("/"))
            { // Absolute path
                item = (Item) session.getItem(path);
            } else
            { // Relative path
                item = getCD(tagName, tag, pageCtx, session).getNode(path);
            }
        } else if (o instanceof Item)
        {
            item = (Item) o;
        } else
        {
            String msg = "The node attribute evaluation "
                    + "returned an unexpected type. " + o.getClass().getName();
            log.warn(msg);
            throw new JspException(msg);
        }
        return item;
    }

    /**
     * Get the current working directory
     * 
     * @param session
     * @param pc
     * @return a node 
     * @throws RepositoryException
     * @throws JspException
     */
    private static Node getCD(String tagName, Tag tag, PageContext pageCtx,
            Session session) throws RepositoryException, JspException
    {
        Node item = null ;
        try {
            item = (Node) ExpressionUtil.evalNotNull(tagName, "node", "${"
                    + JCRTagConstants.KEY_CD + "}", Object.class, tag, pageCtx);
        } catch (NullAttributeException e) {
            item = session.getRootNode();
        }
        return item;
    }

    /**
     * Create a bean for the class specified in the given jndi entry
     * 
     * @param jndi
     * @return a bean
     */
    public static Object getBean(String id)
    {
        Object bean = getBeanFactory().getBean(id);
        if (bean == null)
        {
            log.warn("No bean for id = " + id);
        }
        return bean;
    }

    /**
     * Get the message from the Exception
     * @param e
     * @return
     */
    public static String getMessage(Exception e) {
        return e.getClass().getName() + ". " + e.getMessage() ;
    }
    
    private static BeanFactory getBeanFactory() {
    	if (beanFactory==null) {
    		try {
        		String impl = (String) lookup(JCRTagConstants.JNDI_BEAN_FACTORY);
        		beanFactory = (BeanFactory) Class.forName(impl).newInstance();
    		} catch (Exception e) {
				log.error("unable to create bean factory", e) ;
			}
    	}
    	return beanFactory ;
    }

}