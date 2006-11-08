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
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.jackrabbit.taglib.template.TemplateEngine;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;

/**
 * Displays Node and property values with the given template engine.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce</a>
 */
public class OutTag extends TagSupport
{
	private static Logger log = Logger.getLogger(OutTag.class);

    /** Tag Name */
    public static String TAG_NAME = "out";

    /**
     * Name of the scoped variable where the jcr session is stored. If not set
     * then JCRTagConstants.KEY_SESSION is used.
     */
    private String session;

    /**
     * expression or full path.
     */
    private String item;

    /**
     * Property of the given node
     */
    private String property;

    /**
     * Template engine id
     */
    private String templateEngineID;

    /**
     * Template name
     */
    private String template;

    /**
     * Constructor
     */
    public OutTag()
    {
        super();
        this.init();
    }

    /**
     * @inheritDoc
     */
    public int doEndTag() throws JspException
    {
        try
        {
            // Get a session
            Session s = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                    this.pageContext);

            // Get the node
            Item item = JCRTagUtils.getItem(TAG_NAME, this.item, this,
                    this.pageContext, s);

            // If the property is set
            if (this.property != null)
            {
                Node n = (Node) item;
                item = n.getProperty(this.property);
            }

            // Get the template Engine
            TemplateEngine engine = (TemplateEngine) JCRTagUtils
                    .getBean(this.templateEngineID);
            engine.setTemplate(this.template);
            engine.write(this.pageContext, item);

        } catch (PathNotFoundException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspException(msg, e);
        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspException(msg, e);
        }
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
     * Init
     *  
     */
    private void init()
    {
        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

        this.item = null;

        this.property = null;

        this.templateEngineID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_TEMPLATE_ENGINE);
        this.template = null;
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
     * Sets the template
     * 
     * @param template
     */
    public void setTemplate(String template)
    {
        this.template = template;
    }

    /**
     * Sets the template engine ID
     * 
     * @param templateEngine
     */
    public void setTemplateEngineID(String templateEngine)
    {
        this.templateEngineID = templateEngine;
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