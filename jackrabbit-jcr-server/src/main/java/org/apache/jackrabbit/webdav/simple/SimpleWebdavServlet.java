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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.server.BasicCredentialsProvider;
import org.apache.jackrabbit.server.CredentialsProvider;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.SessionProviderImpl;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.tika.detect.Detector;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * WebdavServlet provides webdav support (level 1 and 2 complient) for
 * repository resources.
 * <p>
 * Implementations of this abstract class must implement the
 * {@link #getRepository()} method to access the repository.
 */
public abstract class SimpleWebdavServlet extends AbstractWebdavServlet {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(SimpleWebdavServlet.class);

    /**
     * init param name of the repository prefix
     */
    public static final String INIT_PARAM_RESOURCE_PATH_PREFIX = "resource-path-prefix";

    /**
     * Name of the optional init parameter that defines the value of the
     * 'WWW-Authenticate' header.<p/>
     * If the parameter is omitted the default value
     * {@link #DEFAULT_AUTHENTICATE_HEADER "Basic Realm=Jackrabbit Webdav Server"}
     * is used.
     *
     * @see #getAuthenticateHeaderValue()
     */
    public static final String INIT_PARAM_AUTHENTICATE_HEADER = "authenticate-header";

    /** the 'missing-auth-mapping' init parameter */
    public final static String INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping";

    /**
     * Name of the init parameter that specify a separate configuration used
     * for filtering the resources displayed.
     */
    public static final String INIT_PARAM_RESOURCE_CONFIG = "resource-config";

    /**
     * Name of the parameter that specifies the servlet resource path of
     * a custom &lt;mime-info/&gt; configuration file. The default setting
     * is to use the MIME media type database included in Apache Tika.
     */
    public static final String INIT_PARAM_MIME_INFO = "mime-info";

    /**
     * Servlet context attribute used to store the path prefix instead of
     * having a static field with this servlet. The latter causes problems
     * when running multiple
     */
    public static final String CTX_ATTR_RESOURCE_PATH_PREFIX = "jackrabbit.webdav.simple.resourcepath";

    /**
     * the resource path prefix
     */
    private String resourcePathPrefix;

    /**
     * Header value as specified in the {@link #INIT_PARAM_AUTHENTICATE_HEADER} parameter.
     */
    private String authenticate_header;

    /**
     * Map used to remember any webdav lock created without being reflected
     * in the underlying repository.
     * This is needed because some clients rely on a successful locking
     * mechanism in order to perform properly (e.g. mac OSX built-in dav client)
     */
    private LockManager lockManager;

    /**
     * the resource factory
     */
    private DavResourceFactory resourceFactory;

    /**
     * the locator factory
     */
    private DavLocatorFactory locatorFactory;

    /**
     * the webdav session provider
     */
    private DavSessionProvider davSessionProvider;

    /**
     * the repository session provider
     */
    private SessionProvider sessionProvider;

    /**
     * The config
     */
    private ResourceConfig config;

    /**
     * Init this servlet
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        resourcePathPrefix = getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
        if (resourcePathPrefix == null) {
            log.debug("Missing path prefix > setting to empty string.");
            resourcePathPrefix = "";
        } else if (resourcePathPrefix.endsWith("/")) {
            log.debug("Path prefix ends with '/' > removing trailing slash.");
            resourcePathPrefix = resourcePathPrefix.substring(0, resourcePathPrefix.length() - 1);
        }
        getServletContext().setAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX, resourcePathPrefix);
        log.info(INIT_PARAM_RESOURCE_PATH_PREFIX + " = '" + resourcePathPrefix + "'");

        authenticate_header = getInitParameter(INIT_PARAM_AUTHENTICATE_HEADER);
        if (authenticate_header == null) {
            authenticate_header = DEFAULT_AUTHENTICATE_HEADER;
        }
        log.info("WWW-Authenticate header = '" + authenticate_header + "'");

        config = new ResourceConfig(getDetector());
        String configParam = getInitParameter(INIT_PARAM_RESOURCE_CONFIG);
        if (configParam != null) {
            try {
                config.parse(getServletContext().getResource(configParam));
            } catch (MalformedURLException e) {
                log.debug("Unable to build resource filter provider.");
            }
        }
    }

    /**
     * Reads and returns the configured &lt;mime-info/&gt; database.
     *
     * @see #INIT_PARAM_MIME_INFO
     * @return MIME media type database
     * @throws ServletException if the database is invalid or can not be read
     */
    private Detector getDetector() throws ServletException {
        URL url;

        String mimeInfo = getInitParameter(INIT_PARAM_MIME_INFO);
        if (mimeInfo != null) {
            try {
                url = getServletContext().getResource(mimeInfo);
            } catch (MalformedURLException e) {
                throw new ServletException(
                        "Invalid " + INIT_PARAM_MIME_INFO
                        + " configuration setting: " + mimeInfo, e);
            }
        } else {
            url = MimeTypesFactory.class.getResource("tika-mimetypes.xml");
        }

        try {
            return MimeTypesFactory.create(url);
        } catch (MimeTypeException e) {
            throw new ServletException(
                    "Invalid MIME media type database: " + url, e);
        } catch (IOException e) {
            throw new ServletException(
                    "Unable to read MIME media type database: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isPreconditionValid(WebdavRequest request,
                                          DavResource resource) {
        return !resource.exists() || request.matchesIfHeader(resource);
    }

    /**
     * Returns the configured path prefix
     *
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public String getPathPrefix() {
        return resourcePathPrefix;
    }

    /**
     * Returns the configured path prefix
     *
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public static String getPathPrefix(ServletContext ctx) {
        return (String) ctx.getAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX);
    }

    /**
     * Returns the <code>DavLocatorFactory</code>. If no locator factory has
     * been set or created a new instance of {@link org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl} is
     * returned.
     *
     * @return the locator factory
     * @see AbstractWebdavServlet#getLocatorFactory()
     */
    public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {
            locatorFactory = new LocatorFactoryImplEx(resourcePathPrefix);
        }
        return locatorFactory;
    }

    /**
     * Sets the <code>DavLocatorFactory</code>.
     *
     * @param locatorFactory
     * @see AbstractWebdavServlet#setLocatorFactory(DavLocatorFactory)
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    /**
     * Returns the <code>LockManager</code>. If no lock manager has
     * been set or created a new instance of {@link SimpleLockManager} is
     * returned.
     *
     * @return the lock manager
     */
    public LockManager getLockManager() {
        if (lockManager == null) {
            lockManager = new SimpleLockManager();
        }
        return lockManager;
    }

    /**
     * Sets the <code>LockManager</code>.
     *
     * @param lockManager
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * Returns the <code>DavResourceFactory</code>. If no request factory has
     * been set or created a new instance of {@link ResourceFactoryImpl} is
     * returned.
     *
     * @return the resource factory
     * @see AbstractWebdavServlet#getResourceFactory()
     */
    public DavResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new ResourceFactoryImpl(getLockManager(), getResourceConfig());
        }
        return resourceFactory;
    }

    /**
     * Sets the <code>DavResourceFactory</code>.
     *
     * @param resourceFactory
     * @see AbstractWebdavServlet#setResourceFactory(org.apache.jackrabbit.webdav.DavResourceFactory)
     */
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    /**
     * Returns the <code>SessionProvider</code>. If no session provider has been
     * set or created a new instance of {@link SessionProviderImpl} that extracts
     * credentials from the request's <code>Authorization</code> header is
     * returned.
     *
     * @return the session provider
     */
    public synchronized SessionProvider getSessionProvider() {
        if (sessionProvider == null) {
            sessionProvider = new SessionProviderImpl(getCredentialsProvider());
        }
        return sessionProvider;
    }

    /**
     * Factory method for creating the credentials provider to be used for
     * accessing the credentials associated with a request. The default
     * implementation returns a {@link BasicCredentialsProvider} instance,
     * but subclasses can override this method to add support for other
     * types of credentials.
     *
     * @return the credentilas provider
     * @since 1.3
     */
    protected CredentialsProvider getCredentialsProvider() {
    	return new BasicCredentialsProvider(getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING));
    }

    /**
     * Sets the <code>SessionProvider</code>.
     *
     * @param sessionProvider
     */
    public synchronized void setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * Returns the <code>DavSessionProvider</code>. If no session provider has
     * been set or created a new instance of {@link DavSessionProviderImpl}
     * is returned.
     *
     * @return the session provider
     * @see AbstractWebdavServlet#getDavSessionProvider()
     */
    public synchronized DavSessionProvider getDavSessionProvider() {
        if (davSessionProvider == null) {
            davSessionProvider =
                new DavSessionProviderImpl(getRepository(), getSessionProvider());
        }
        return davSessionProvider;
    }

    /**
     * Sets the <code>DavSessionProvider</code>.
     *
     * @param sessionProvider
     * @see AbstractWebdavServlet#setDavSessionProvider(org.apache.jackrabbit.webdav.DavSessionProvider)
     */
    public synchronized void setDavSessionProvider(DavSessionProvider sessionProvider) {
        this.davSessionProvider = sessionProvider;
    }

    /**
     * Returns the header value retrieved from the {@link #INIT_PARAM_AUTHENTICATE_HEADER}
     * init parameter. If the parameter is missing, the value defaults to
     * {@link #DEFAULT_AUTHENTICATE_HEADER}.
     *
     * @return the header value retrieved from the corresponding init parameter
     * or {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * @see AbstractWebdavServlet#getAuthenticateHeaderValue()
     */
    public String getAuthenticateHeaderValue() {
        return authenticate_header;
    }

    /**
     * Returns the resource configuration to be applied
     *
     * @return the resource configuration.
     */
    public ResourceConfig getResourceConfig() {
        return config;
    }

    /**
     * Set the resource configuration
     *
     * @param config
     */
    public void setResourceConfig(ResourceConfig config) {
        this.config = config;
    }

    /**
     * Returns the <code>Repository</code>. If no repository has been set or
     * created the repository initialized by <code>RepositoryAccessServlet</code>
     * is returned.
     *
     * @return repository
     */
    public abstract Repository getRepository();

}
