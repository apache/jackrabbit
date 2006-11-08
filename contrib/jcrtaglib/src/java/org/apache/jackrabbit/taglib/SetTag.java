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
package org.apache.jackrabbit.taglib;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.Util;

/**
 * Stores the given node or property in a scoped variable.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class SetTag extends TagSupport
{
    /** logger */
	private static Logger log = Logger.getLogger(SetTag.class);

    /** Tag Name */
    public static String TAG_NAME = "set";

    /**
     * Name of the scoped variable where the jcr session will be stored. If not set
     * then JCRTagConstants.KEY_SESSION is used.
     */
    protected String session;

    /**
     * Item
     */
    protected String item;

    /**
     * Property
     */
    private String property;

    /**
     * Scoped variable where the jcr Item will be stored
     */
    protected String var;

    /**
     * Scope
     */
    protected int scope;

    /**
     * Constructor
     */
    public SetTag()
    {
        super();
        this.init();
    }

    /**
     * @inheritDoc
     */
    public int doEndTag() throws JspException
    {
        return EVAL_PAGE;
    }

    /**
     * Sets the item
     * 
     * @param item
     */
    public void setItem(String item)
    {
        this.item = item;
    }

    /**
     * Sets the session
     * 
     * @param session
     */
    public void setSession(String session)
    {
        this.session = session;
    }

    /**
     * init
     */
    private void init()
    {
        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";
        this.item = null;
        this.scope = PageContext.PAGE_SCOPE;
    }

    /**
     * @inheritDoc
     */
    public void release()
    {
        super.release();
        this.init();
    }

    /**
     * Sets the variable name where the Item will be stored.
     * 
     * @param var
     */
    public void setVar(String var)
    {
        this.var = var;
    }

    /**
     * @inheritDoc
     */
    public int doStartTag() throws JspException
    {
        try
        {
            Item i = this.getItem();
            // If the property is set
            if (this.property != null)
            {
                Node n = (Node) i;
                i = n.getProperty(this.property);
            }
            this.pageContext.setAttribute(this.var, i, this.scope);
        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspException(msg, e);
        }
        return SKIP_BODY;
    }

    /**
     * Gets the item
     * 
     * @return @throws
     *         JspException
     * @throws RepositoryException
     */
    protected Item getItem() throws JspException, RepositoryException
    {
        // Get a session
        Session session = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                this.pageContext);

        // Get the item
        Item item = JCRTagUtils.getItem(TAG_NAME, this.item, this,
                this.pageContext, session);

        return item;
    }

    /**
     * Sets the property. This can be set only when the item is a node.
     * 
     * @param property
     */
    public void setProperty(String property)
    {
        this.property = property;
    }

    /**
     * Sets the scope
     * 
     * @param scope
     */
    public void setScope(String scope)
    {
        this.scope = Util.getScope(scope);
    }
}