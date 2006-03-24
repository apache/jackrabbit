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

import java.security.Principal;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;

/**
 * <p>
 * Creates a session stores it in a page scope variable.
 * </p>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class SessionTag extends TagSupport
{
    /** logger */
    private static Log log = LogFactory.getLog(SessionTag.class);

    /**
     * Session instance
     */
    private Session session;

    /**
     * Name of the jndi address of a repository other than the default.
     */
    private String repositoryJNDI = JCRTagConstants.JNDI_DEFAULT_REPOSITORY;;

    /**
     * Workspace name.
     */
    private String workspace;

    /**
     * Name of the target page context variable where the session will be
     * stored.
     */
    private String var;

    /**
     * Name of the user for loggin in. <br>
     * If no user is set then the request.getUserPrincipal is used. <br>
     * If no user is logged in the default user is used (See config options).
     */
    private String user;

    /**
     * User's password. <br>
     * Password used for creating a new JCR Session.
     */
    private String password;

    /**
     * previous session. (nested sessions)
     */
    private Object previousSession;

    /**
     * Constructor and initialization
     */
    public SessionTag()
    {
        super();
        this.init();
    }

    /**
     * Init
     */
    private void init()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Cleaning state");
        }
        this.password = null;
        this.repositoryJNDI = JCRTagConstants.JNDI_DEFAULT_REPOSITORY;
        this.var = JCRTagConstants.KEY_SESSION;
        this.user = null;
        this.workspace = null;
        this.session = null;
    }

    /**
     * Sets the password
     * 
     * @param pwd
     */
    public void setPassword(String pwd)
    {
        this.password = pwd;
    }

    /**
     * Sets the repository JNDI address
     * 
     * @param repository
     */
    public void setRepositoryJNDI(String repository)
    {
        this.repositoryJNDI = repository;
    }

    /**
     * Sets the variable where the Session will be stored
     * 
     * @param target
     */
    public void setVar(String target)
    {
        this.var = target;
    }

    /**
     * Sets the user
     * 
     * @param user
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * Sets the workspace
     * 
     * @param workspace
     */
    public void setWorkspace(String workspace)
    {
        this.workspace = workspace;
    }

    /**
     * @inheritDoc
     */
    public int doEndTag() throws JspException
    {
        // Restore the previous session (if any)
        if (this.previousSession != null)
        {
            pageContext.setAttribute(this.var, this.previousSession);
            this.previousSession=null ;
        } else {
            this.pageContext.removeAttribute(this.var, PageContext.PAGE_SCOPE);
        }

        // Logout
        session.logout();

        if (log.isDebugEnabled())
        {
            log.debug("logged out");
        }

        // Return
        return EVAL_PAGE;
    }

    /**
     * @inheritDoc
     */
    public int doStartTag() throws JspException
    {
        this.previousSession = pageContext.getAttribute(this.var);

        // Get the repository
        Repository repo = (Repository) JCRTagUtils.lookup(this.repositoryJNDI);

        // Get the session
        try
        {
            if (this.workspace == null)
            {
                session = repo.login(this.getCredentials());
            } else
            {
                session = repo.login(this.getCredentials(), this.workspace);
            }
            if (log.isDebugEnabled())
            {
                log.debug("logged in");
            }
        } catch (RepositoryException e)
        {
            String msg = JCRTagUtils.getMessage(e);
            log.error(msg, e);
            throw new JspException(msg, e);
        }

        // Store the session in the page scope
        pageContext.setAttribute(this.var, session);

        // Return
        return EVAL_BODY_INCLUDE;
    }

    /**
     * <p>
     * Create credentials
     * </p>
     * <ol>
     * <li>Try to create credentials for the given user (user and pwd)</li>
     * <li>Try to create credentials for the logged user (logged user and no
     * pwd)</li>
     * <li>Try to create credentials for anonymous user (JNDI config)</li>
     * </ol>
     * 
     * @return @throws
     *         JspException
     */
    private SimpleCredentials getCredentials() throws JspException
    {
        // Session credentials
        SimpleCredentials cred = null;

        // Try to create credentials for the given user
        if (this.user != null)
        {
            if (this.password == null)
            {
                cred = new SimpleCredentials(this.user, "".toCharArray());
            } else
            {
                cred = new SimpleCredentials(this.user, this.password
                        .toCharArray());
            }
        }

        // Try to create credentials for the logged user
        if (cred == null)
        {
            Principal p = ((HttpServletRequest) this.pageContext.getRequest())
                    .getUserPrincipal();
            if (p != null)
            {
                cred = new SimpleCredentials(p.getName(), "".toCharArray());
            }
        }

        // Try to create credentials for anonymous user
        if (cred == null)
        {
            String usr = (String) JCRTagUtils
                    .lookup(JCRTagConstants.JNDI_ANON_USER);

            String pwd = (String) JCRTagUtils
                    .lookup(JCRTagConstants.JNDI_ANON_PWD);

            if (usr == null || pwd == null)
            {
                log.error("Configure user name and password "
                        + "for anonymous user properly");
            }
            cred = new SimpleCredentials(usr, pwd.toCharArray());
        }

        return cred;
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