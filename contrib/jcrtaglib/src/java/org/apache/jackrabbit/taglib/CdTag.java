/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.taglib;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;

import org.apache.jackrabbit.taglib.utils.JCRTagConstants;

/**
 * Changes the current directory. Nested tags must read the paths relative to
 * this node.<br>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class CdTag extends SetTag
{

    /**
     * Constructor
     */
    public CdTag()
    {
        super();
        this.init();
    }

    /**
     * Init
     */
    private void init()
    {
        this.var = JCRTagConstants.KEY_CD;
    }

    /**
     * @inheritDoc
     */
    public int doEndTag() throws JspException
    {
        // Restore the previous attribute
        pageContext.removeAttribute(this.var, this.scope);
        return super.doEndTag();
    }

    /**
     * @inheritDoc
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();
        return EVAL_BODY_INCLUDE;
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
     * Sets the node
     * 
     * @param node
     */
    public void setNode(String node)
    {
        this.item = node;
    }

    /**
     * Retrieve and validate the Item. It must be a Node instance.
     * 
     * @return curren working node
     * @throws JspException
     * @throws RepositoryException
     */
    protected Item getItem() throws JspException, RepositoryException
    {
        Item item = super.getItem();

        // Validate
        if (!(item instanceof Node))
        {
            throw new JspException("The referenced item is not a Node instance");
        }

        return item;
    }

}