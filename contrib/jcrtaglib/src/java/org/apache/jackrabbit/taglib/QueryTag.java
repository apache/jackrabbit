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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * <p>
 * Iterates over the nodes returned by the given query.
 * </p>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class QueryTag extends LoopTagSupport
{
    /** logger */
	private static Logger log = Logger.getLogger(QueryTag.class);

    /** tag name */
    public static String TAG_NAME = "query";

    /**
     * Name of the scoped variable where the jcr session is stored. If not set
     * then JCRTagConstants.KEY_SESSION is used.
     */
    private String session;

    /**
     * Query
     */
    private String stmt;

    /**
     * Query type ( SQL | XPATH )
     */
    private String lang;

    /**
     * Children Nodes
     */
    private Iterator nodes;

    /**
     * Constructor
     */
    public QueryTag()
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
            // get a session
            Session s = JCRTagUtils.getSession(TAG_NAME, this.session, this,
                    this.pageContext);

            Query q = s.getWorkspace().getQueryManager().createQuery(
                    this.getStmt(), this.getLang());

            QueryResult qr = q.execute();

            this.nodes = qr.getNodes();

        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspTagException(msg);
        } catch (JspException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.warn(msg);
            throw new JspTagException(msg);
        }
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
        this.nodes = null;

        this.stmt = null;

        this.lang = "xpath";

        this.session = "${" + JCRTagConstants.KEY_SESSION + "}";

    }

    /**
     * @return the query
     * @throws JspException
     */
    private String getStmt() throws JspException
    {
        return (String) ExpressionUtil.evalNotNull(TAG_NAME, "query",
                this.stmt, String.class, this, this.pageContext);
    }

    /**
     * Sets the query
     * 
     * @param query
     */
    public void setStmt(String query)
    {
        this.stmt = query;
    }

    /**
     * Gets the query type
     * 
     * @return @throws
     *         JspTagException
     */
    private String getLang() throws JspTagException
    {
        if (this.lang.equalsIgnoreCase("xpath"))
        {
            return Query.XPATH;
        } else if (this.lang.equalsIgnoreCase("sql"))
        {
            return Query.SQL;
        } else
        {
            throw new JspTagException("No such Query type. " + this.lang);
        }
    }

    /**
     * Sets the query type
     * 
     * @param type
     */
    public void setLang(String language)
    {
        this.lang = language;
    }
}