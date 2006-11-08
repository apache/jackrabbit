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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.taglib.comparator.ItemComparator;
import org.apache.jackrabbit.taglib.filter.ItemFilter;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;

/**
 * Iterates over the properties of the given node.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class PropertiesTag extends LoopTagSupport
{
    /** logger */
	private static Logger log = Logger.getLogger(PropertiesTag.class);

    /** tag name */
    public static String TAG_NAME = "properties";

    /**
     * Name of the scoped variable where the jcr session is stored. If not set
     * then JCRTagConstants.KEY_SESSION is used.
     */
    private String session;

    /**
     * JSTL expression or or full path. <br>
     * e.g. /mynode <br>
     * or ${mynode}
     */
    private String node;

    /**
     * JNDI address where the <code>ItemComparator</code> is bound. <br>
     * e.g. myproperty
     */
    private String sortID;

    /**
     * Expression used by the <code>ItemComparator</code><br>
     * The evaluation must return a Comparable instance. <br>
     */
    private String sortExp;

    /**
     * Sort order <br>
     * true / false
     */
    private boolean ascending = true;

    /**
     * JNDI address where the NodePredicate is bound.
     */
    private String filterID;

    /**
     * Expression used by the NodePredicate to evaluate nodes. The evaluation
     * must return a Boolean instance.
     */
    private String filterExp;

    /**
     * Properties
     */
    private Iterator properties;

    /**
     *  Constructor
     */
    public PropertiesTag()
    {
        super();
        this.init();
    }

    /**
     * @inheritDoc
     */
    protected boolean hasNext() throws JspTagException
    {
        return this.properties.hasNext();
    }

    /**
     * @inheritDoc
     */
    protected Object next() throws JspTagException
    {
        return this.properties.next();
    }

    /**
     * @inheritDoc
     */
    protected void prepare() throws JspTagException
    {
        try
        {
            // get a session
            Session jcrSession = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                    this.pageContext);

            // get the node
            Node node = (Node) JCRTagUtils.getItem(TAG_NAME, this.node, this,
                    this.pageContext, jcrSession);

            // Get the properties
            List props = IteratorUtils.toList(node.getProperties());

            // Sort
            if (this.sortExp != null)
            {
                ItemComparator order = (ItemComparator) JCRTagUtils
                        .getBean(this.sortID);
                order.setExpression(this.sortExp);
                order.setAscending(this.ascending);
                Collections.sort(props, order);
            }

            // Filter
            if (this.filterExp != null)
            {
                ItemFilter predicate = (ItemFilter) JCRTagUtils
                        .getBean(this.filterID);
                predicate.setExpression(this.filterExp);
                CollectionUtils.filter(props, predicate);
            }

            // get iterator
            this.properties = props.iterator();
        } catch (PathNotFoundException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
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
     * Set the order ( ascending | descending)
     * @param ascending
     */
    public void setAscending(boolean ascending)
    {
        this.ascending = ascending;
    }

    /**
     * Sets the filter expression
     * @param filterExp
     */
    public void setFilterExp(String filterExp)
    {
        this.filterExp = filterExp;
    }

    /**
     * Sets the filter ID
     * @param filterID
     */
    public void setFilterID(String filterID)
    {
        this.filterID = filterID;
    }

    /**
     * Sets the node
     * @param node
     */
    public void setNode(String node)
    {
        this.node = node;
    }

    /**
     * Sets the session
     * @param session
     */
    public void setSession(String session)
    {
        this.session = session;
    }

    /**
     * Sets the sort expression
     * @param sortExp
     */
    public void setSortExp(String sortExp)
    {
        this.sortExp = sortExp;
    }

    /**
     * Sets the sortID
     * @param sortID
     */
    public void setSortID(String sortID)
    {
        this.sortID = sortID;
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

        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

        this.sortExp = null;
        this.sortID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_ITEM_COMPARATOR);

    }

    /**
     * @inheritDoc
     */
    public void release()
    {
        super.release();
        this.init();
    }
}