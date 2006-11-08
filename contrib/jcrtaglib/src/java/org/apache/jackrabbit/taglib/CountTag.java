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

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.jackrabbit.taglib.filter.ItemFilter;
import org.apache.jackrabbit.taglib.traverser.Traverser;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * <p>
 * Counts the nodes returned by the given <code>Traverser</code> and writes
 * the value.
 * </p>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class CountTag extends TagSupport
{
	private static Logger log = Logger.getLogger(CountTag.class);

    public static String TAG_NAME = "count";

    /**
     * Name of the scoped variable where the jcr session is stored. If not set
     * then JCRTagConstants.KEY_SESSION is used.
     */
    private String session;

    /**
     * JSTL expression or full path. <br>
     * e.g. /mynode <br>
     * or ${mynode}
     */
    private String node;

    /**
     * Traverser ID.
     */
    private String traverserID;

    /**
     * Expression that affects Traverser behaviour
     */
    private String traverserParam;

    /**
     * Traverse depth
     */
    private int traverserDepth = 0;

    /**
     * NodePredicate ID.
     */
    private String filterID;

    /**
     * Expression used by the NodePredicate to evaluate nodes. The evaluation
     * must return a Boolean instance.
     */
    private String filterExp;

    /**
     * Constructor
     */
    public CountTag()
    {
        super();
        this.init();
    }

    /**
     * Sets the filter expression
     * 
     * @param filterExp
     */
    public void setFilterExp(String filterExp)
    {
        this.filterExp = filterExp;
    }

    /**
     * Sets the filter ID
     * 
     * @param filterID
     */
    public void setFilterID(String filterID)
    {
        this.filterID = filterID;
    }

    /**
     * Sets the node
     * 
     * @param node
     */
    public void setNode(String node)
    {
        this.node = node;
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
     * Set the traverser depth
     * 
     * @param traverseDepth
     */
    public void setTraverserDepth(int traverseDepth)
    {
        this.traverserDepth = traverseDepth;
    }

    /**
     * Sets the traverser ID
     * 
     * @param traverseID
     */
    public void setTraverserID(String traverseID)
    {
        this.traverserID = traverseID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release()
    {
        super.release();
        this.init();
    }

    /**
     * init
     */
    private void init()
    {
        this.filterExp = null;
        this.filterID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_ITEM_FILTER);

        this.node = "/";

        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

        this.traverserDepth = 1;
        this.traverserID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_TRAVERSER);

    }

    /**
     * Sets the traverser parameter. The paremeter is Traverser specific.
     * 
     * @param traverserParam
     */
    public void setTraverserParam(String traverserParam)
    {
        this.traverserParam = traverserParam;
    }

    /**
     * Gets the traverser instance
     * 
     * @return @throws
     *         JspException
     */
    private Object getTraverserParam() throws JspException
    {
        Object o = null;
        try
        {
            o = ExpressionUtil.evalNotNull(TAG_NAME, "traverserParam",
                    this.traverserParam, Object.class, this, this.pageContext);
        } catch (NullAttributeException e)
        {
        }
        return o;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     */
    public int doEndTag() throws JspException
    {
        try
        {
            // get a session
            Session s = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                    this.pageContext);

            // get the node
            Node jcrNode = (Node) JCRTagUtils.getItem(TAG_NAME, this.node, this,
                    this.pageContext, s);

            // Configure traverse strategy
            Traverser traverser = (Traverser) JCRTagUtils
                    .getBean(this.traverserID);
            traverser.setDepth(this.traverserDepth);
            traverser.setNode(jcrNode);
            traverser.setParameter(this.getTraverserParam());

            // Filter
            if (this.filterExp != null)
            {
                ItemFilter predicate = (ItemFilter) JCRTagUtils
                        .getBean(this.filterID);
                predicate.setExpression(this.filterExp);
                traverser.setFilter(predicate);
            }
            // Traverse nodes
            traverser.traverse();
            // Retrieve Nodes
            pageContext.getOut().write(
                    String.valueOf(traverser.getNodes().size()));

        } catch (PathNotFoundException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.warn(msg, e);
            throw new JspTagException(msg);
        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        } catch (IOException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        }
        return EVAL_PAGE;
    }

}