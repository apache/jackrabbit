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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import org.apache.jackrabbit.taglib.comparator.ItemComparator;
import org.apache.jackrabbit.taglib.filter.ItemFilter;
import org.apache.jackrabbit.taglib.traverser.Traverser;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Iterates through the traversed nodes from the given node
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class NodesTag extends LoopTagSupport
{
	private static Logger log = Logger.getLogger(NodesTag.class);

    public static String TAG_NAME = "nodes";

    /**
     * Name of the scoped variable where the jcr session is stored.
     */
    private String session;

    /**
     * JSTL expression referencing a node or or full path. <br>
     * e.g. /mynode <br>
     * or ${mynode}
     */
    private String node;

    /**
     * NodeComparator ID
     */
    private String sortID;

    /**
     * Expression used by the NodeComparator to evaluate nodes. <br>
     * The evaluation must return a Comparable instance. <br>
     */
    private String sortExp;

    /**
     * Sort order <br>
     * true / false
     */
    private boolean ascending = true;

    /**
     * Traverser ID.
     */
    private String traverserID;

    /**
     * Parameter that affects Traverser behaviour
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
     * Children Nodes
     */
    protected Iterator nodes;

    /**
     *  
     */
    public NodesTag()
    {
        super();
        this.init();
    }

    /**
     * @inheritDoc
     */
    protected boolean hasNext() throws JspTagException
    {
        return nodes.hasNext();
    }

    /**
     * @inheritDoc
     */
    protected Object next() throws JspTagException
    {
        return nodes.next();
    }

    /**
     * @inheritDoc
     */
    protected void prepare() throws JspTagException
    {
        try
        {
            // get the node
            Node jcrNode = this.getNode();

            // Configure traverse strategy
            Traverser traverser = (Traverser) JCRTagUtils
                    .getBean(this.traverserID);
            traverser.setDepth(this.traverserDepth);
            traverser.setNode(jcrNode);
            traverser.setParameter(this.getTraverserParam());

            // Sort
            if (this.sortExp != null)
            {
                ItemComparator order = (ItemComparator) JCRTagUtils
                        .getBean(this.sortID);
                order.setExpression(this.sortExp);
                order.setAscending(this.ascending);
                traverser.setOrder(order);
            }

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
            this.nodes = traverser.getNodes().iterator();

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
        } catch (JspException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        }
    }

    /**
     * Sets the order ( ascending | descending )
     * 
     * @param ascending
     */
    public void setAscending(boolean ascending)
    {
        this.ascending = ascending;
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
     * Sets the sort expression
     * 
     * @param sortExp
     */
    public void setSortExp(String sortExp)
    {
        this.sortExp = sortExp;
    }

    /**
     * Sets the sort ID
     * 
     * @param sortJNDI
     */
    public void setSortID(String sortJNDI)
    {
        this.sortID = sortJNDI;
    }

    /**
     * Sets the Traverser depth
     * 
     * @param traverseDepth
     */
    public void setTraverserDepth(int traverseDepth)
    {
        this.traverserDepth = traverseDepth;
    }

    /**
     * Sets the Traverser ID
     * 
     * @param traverseID
     */
    public void setTraverserID(String traverseID)
    {
        this.traverserID = traverseID;
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
     * init
     *  
     */
    private void init()
    {
        this.ascending = true;

        this.filterExp = null;
        this.filterID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_ITEM_FILTER);

        this.node = "/";
        this.nodes = null;

        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

        this.sortExp = null;
        this.sortID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_ITEM_COMPARATOR);

        this.traverserDepth = 1;
        this.traverserID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_TRAVERSER);

    }

    /**
     * Sets the Traverer parameter
     * 
     * @param traverserExp
     */
    public void setTraverserParam(String traverserExp)
    {
        this.traverserParam = traverserExp;
    }

    /**
     * Gets the traverser parameter evaluation
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

    /**
     * Retrieves the node
     * 
     * @return @throws
     *         JspException
     * @throws RepositoryException
     * @throws PathNotFoundException
     */
    protected Node getNode() throws PathNotFoundException, JspException,
            RepositoryException
    {
        // Get the session
        Session jcrSession = JCRTagUtils.getSession(TAG_NAME, this.session,
                this, this.pageContext);

        return (Node) JCRTagUtils.getItem(TAG_NAME, this.node, this,
                this.pageContext, jcrSession);
    }

}