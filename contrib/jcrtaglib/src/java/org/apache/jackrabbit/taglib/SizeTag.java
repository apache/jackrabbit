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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.jackrabbit.taglib.filter.ItemFilter;
import org.apache.jackrabbit.taglib.size.SizeCalculator;
import org.apache.jackrabbit.taglib.traverser.Traverser;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Estimates the cumulative size of the nodes returned by the given
 * <code>Traverser</code> and displays the value.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class SizeTag extends TagSupport
{
    /** logger */
	private static Logger log = Logger.getLogger(SizeTag.class);

    /** tag name */
    public static String TAG_NAME = "size";

    /** number format */
    private static NumberFormat NF;

    // Default Format
    static
    {
        NF = new DecimalFormat("###,##0.0");
        NF.setMaximumFractionDigits(2);
    }

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
     * Storage calculator ID.
     */
    private String calculatorID;

    /**
     * Unit.
     */
    private int unit;

    /**
     * Decimal format pattern
     */
    private String format;

    /**
     * Constructor
     */
    public SizeTag()
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
     * Sets the traverser depth
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
        this.filterExp = null;
        this.filterID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_ITEM_FILTER);

        this.node = "/";

        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

        this.traverserDepth = 1;
        this.traverserID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_TRAVERSER);

        this.calculatorID = (String) JCRTagUtils
                .lookup(JCRTagConstants.JNDI_DEFAULT_SIZE_CALCULATOR);

        this.unit = SizeCalculator.BYTES;

        this.format = null;

    }

    /**
     * Sets the traverser parameter
     * 
     * @param traverserExp
     */
    public void setTraverserParam(String traverserExp)
    {
        this.traverserParam = traverserExp;
    }

    /**
     * gets the traverser parameter evaluation
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
     * @inheritDoc
     */
    public int doEndTag() throws JspException
    {
        try
        {
            // get a session
            Session s = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                    this.pageContext);

            // get the node
            Node jcrNode = (Node) JCRTagUtils.getItem(TAG_NAME, this.node,
                    this, this.pageContext, s);

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

            // Estimate size
            SizeCalculator calculator = (SizeCalculator) JCRTagUtils
                    .getBean(this.calculatorID);
            calculator.setUnit(this.unit);

            double size = 0;
            Iterator iter = traverser.getNodes().iterator();
            while (iter.hasNext())
            {
                Node n = (Node) iter.next();
                size = size + calculator.getSize(n);
            }

            // Write the size
            pageContext.getOut().write(this.getNumberFormat().format(size));

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

    /**
     * Sets the unit ( bytes | kb | mb | gb )
     * 
     * @param unit
     * @throws JspTagException
     */
    public void setUnit(String unit) throws JspTagException
    {
        if (unit.equalsIgnoreCase("bytes"))
        {
            this.unit = SizeCalculator.BYTES;
        } else if (unit.equalsIgnoreCase("kb"))
        {
            this.unit = SizeCalculator.KILOBYTES;
        } else if (unit.equalsIgnoreCase("mb"))
        {
            this.unit = SizeCalculator.MEGABYTES;
        } else if (unit.equalsIgnoreCase("gb"))
        {
            this.unit = SizeCalculator.GIGABYTES;
        } else
        {
            throw new JspTagException("No such unit. " + unit);
        }
    }

    /**
     * Sets the storage calculator ID
     * 
     * @param calculatorID
     */
    public void setCalculatorID(String calculatorID)
    {
        this.calculatorID = calculatorID;
    }

    /**
     * Sets the format pattern
     * 
     * @param format
     */
    public void setFormat(String format)
    {
        this.format = format;
    }

    /**
     * @return the number format
     */
    private NumberFormat getNumberFormat()
    {
        if (this.format == null)
        {
            return NF;
        } else
        {
            return new DecimalFormat(this.format);
        }
    }
}