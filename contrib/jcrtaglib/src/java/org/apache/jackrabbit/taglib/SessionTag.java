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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.jackrabbit.taglib.utils.JCRTagConstants;
import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;

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
	private static Logger log = Logger.getLogger(SessionTag.class);
	
	/**
	 * jcr repository
	 */
	private static Map repositories = new HashMap() ;
	
	/**
	 * JNDI address where a Repository is registered
	 */
	private String jndiAddress = JCRTagConstants.REPOSITORY_JNDI_ADDRESS ;

	/**
	 * JNDI properties to create the initial context
	 */
	private String jndiProperties = JCRTagConstants.REPOSITORY_JNDI_PROPERTIES ;
	
    /**
     * Session instance
     */
    private Session session;

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
        Repository repo = (Repository) this.getRepository();

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
    
    /**
     * get a repository either from a cache or via JNDI
     * 
     * @return
     */
    private Repository getRepository() {
    	String address = (String) JCRTagUtils.lookup(this.jndiAddress) ;
    	String props = (String) JCRTagUtils.lookup(this.jndiProperties) ;
    	String key = props + address; 
    	
    	// lookup cached repository
    	if (repositories.get(key)== null) {
    		try {
            	InitialContext ctx = null ;
            	if (props!=null) {
            		Properties properties = new Properties() ;
                	InputStream is = new ByteArrayInputStream(props.getBytes("UTF-8")) ;
                	properties.load(is) ;
                	ctx = new InitialContext(properties) ; 
            	} else {
            		ctx = new InitialContext();
            	}
            	Repository repo = (Repository) ctx.lookup(address) ;
            	 if (repo!=null) {
            		 synchronized (this) {
            			 repositories.put(key, repo) ;	
        			}
            	 }
    		} catch (Exception e) {
				log.error("unable to get repository", e);
			}
    	}
 		return (Repository) repositories.get(key) ;
    }

	public void setJndiAddress(String jndiAddress) {
		this.jndiAddress = jndiAddress;
	}

	public void setJndiProperties(String jndiProperties) {
		this.jndiProperties = jndiProperties;
	}

}