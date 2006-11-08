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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;

/**
 * Conditional tag that evaluates the existence of the given node.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class IfPresentTag extends ConditionalTagSupport
{
    /** Logger */
	private static Logger log = Logger.getLogger(IfPresentTag.class);

    /** Tag Name */
    public static String TAG_NAME = "set";

    /**
     * Session
     */
    private String session;

    /**
     * expression or full path.
     */
    private String item;

    /**
     * Property.
     */
    private String property;

    /**
     * Expected evaluation result (true | false)
     */
    private boolean value = true;

    /**
     * Constructor
     */
    public IfPresentTag()
    {
        super();
        this.init();
    }

    /**
     * @inheritDoc
     */
    protected boolean condition() throws JspTagException
    {
        boolean present = false;

        try
        {
            Session jcrSession = JCRTagUtils.getSession(TAG_NAME, this.session,
                    this, this.pageContext);
            try
            {
                Item i = JCRTagUtils.getItem(TAG_NAME, this.item, this,
                        this.pageContext, jcrSession);

                // If the property is set
                if (this.property != null)
                {
                    Node n = (Node) i;
                    i = n.getProperty(this.property);
                }

                present = true;
            } catch (PathNotFoundException e)
            {
                // Do nothing
            }
        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        } catch (JspException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        }

        return (present == this.value);
    }

    /**
     * @inheritDoc
     */
    protected void init()
    {
        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";
        this.item = null;
        this.value = true;
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
     * Sets the value
     * 
     * @param value
     */
    public void setValue(boolean value)
    {
        this.value = value;
    }

    /**
     * Sets the property. This value can be set only when the item is a node.
     * 
     * @param property
     */
    public void setProperty(String property)
    {
        this.property = property;
    }

}