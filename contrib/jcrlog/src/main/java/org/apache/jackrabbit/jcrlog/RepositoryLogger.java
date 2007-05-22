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
package org.apache.jackrabbit.jcrlog;

import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.TransientRepository;

/**
 * The public Repository object of the wrapper. Actually does not need to be
 * used directly by the application (if the RepositoryFactory is used).
 *
 * @author Thomas Mueller
 *
 */
public class RepositoryLogger extends LogObject implements Repository {

    private Repository repository;

    /**
     * Open a repository and wrap it using the RepositoryLogger wrapper.
     *
     * @param url the URL
     * @return the wrapped repository
     * @throws RepositoryException
     */
    public static Repository open(String url) throws RepositoryException {
        String wrappedURL;
        int idx = url.toLowerCase().indexOf("url=");
        if (idx < 0) {
            wrappedURL = "apache/jackrabbit/transient";
        } else {
            wrappedURL = url.substring(idx + "url=".length());
            url = url.substring(0, idx);
        }
        if (url == null || url.length() == 0) {
            url = "sysout=true";
        }
        Properties prop = parseSettings(url);
        Repository rep = RepositoryFactory.open(wrappedURL);
        return new RepositoryLogger(rep, prop, wrappedURL);
    }

    /**
     * Wrap a repository.
     *
     * @param repository
     * @param settings the settings to use
     * @return the wrapped repository
     * @throws RepositoryException
     */
    public static Repository wrap(Repository repository, String settings)
            throws RepositoryException {
        String url;
        if (repository instanceof TransientRepository) {
            url = "apache/jackrabbit/transient";
        } else {
            // TODO support other repositories
            url = "<unknown url, class: " + repository.getClass().getName()
                    + ">";
        }
        if (settings == null || settings.length() == 0) {
            settings = "sysout=true";
        }
        Properties prop = parseSettings(settings);
        return new RepositoryLogger(repository, prop, url);
    }

    private RepositoryLogger(Repository repository, Properties prop, String url)
            throws RepositoryException {
        this.repository = repository;
        String fileName = prop.getProperty("file");
        String sysOut = prop.getProperty("sysout");
        String logReturn = prop.getProperty("return");
        String logCaller = prop.getProperty("caller");
        String cast = prop.getProperty("cast");
        String stream = prop.getProperty("stream");
        Log log = new Log(fileName, parseBoolean(sysOut));
        log.setLogCaller(parseBoolean(logCaller));
        log.setLogReturn(parseBoolean(logReturn));
        log.setCastToRealApi(parseBoolean(cast));
        log.setLogStream(parseBoolean(stream));
        String call = RepositoryFactory.CLASSNAME + ".open("
                + StringUtils.quoteString(url) + ");";
        int nextId = getNextId(LogObject.INTERFACE_DEF_REPOSITORY.type);
        setLog(log, LogObject.INTERFACE_DEF_REPOSITORY.type, nextId);
        logStartCall(getAssign(LogObject.INTERFACE_DEF_REPOSITORY.type, nextId, call));
    }

    private boolean parseBoolean(String s) {
        return Boolean.valueOf(s).booleanValue();
    }

    private static Properties parseSettings(String settings)
            throws RepositoryException {
        Properties prop = new Properties();
        String[] list = StringUtils.arraySplit(settings, ';');
        for (int i = 0; list != null && i < list.length; i++) {
            String setting = list[i];
            if (setting.length() == 0) {
                continue;
            }
            int equal = setting.indexOf('=');
            if (equal < 0) {
                throw new RepositoryException("Invalid settings format: "
                        + settings);
            }
            String value = setting.substring(equal + 1).trim();
            String key = setting.substring(0, equal).trim();
            key = key.toLowerCase();
            String old = prop.getProperty(key);
            if (old != null && !old.equals(value)) {
                throw new RepositoryException("Duplicate settings: " + key);
            }
            prop.setProperty(key, value);
        }
        return prop;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescriptor(String key) {
        try {
            logStart(-1, false, "getDescriptor", StringUtils.quoteString(key));
            String result = repository.getDescriptor(key);
            logReturn(String.class, result);
            return result;
        } catch (Throwable t) {
            throw logRuntimeException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDescriptorKeys() {
        try {
            logStart(-1, false, "getDescriptorKeys", "");
            String[] result = repository.getDescriptorKeys();
            logReturn(String[].class, result);
            return result;
        } catch (Throwable t) {
            throw logRuntimeException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Session login() throws RepositoryException {
        try {
            int assignType = LogObject.INTERFACE_DEF_SESSION.type;
            int nextId = logStart(assignType, false, "login", "");
            Session result = repository.login();
            result = (Session) wrap(result, false, Session.class, assignType,
                    nextId);
            logReturn();
            return result;
        } catch (Throwable t) {
            throw logAndConvert(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Session login(Credentials credentials) throws RepositoryException {
        try {
            int assignType = LogObject.INTERFACE_DEF_SESSION.type;
            int nextId = logStart(assignType, false, "login", StringUtils
                    .quoteCredentials(credentials));
            Session result = repository.login(credentials);
            result = (Session) wrap(result, false, Session.class, assignType,
                    nextId);
            logReturn();
            return result;
        } catch (Throwable t) {
            throw logAndConvert(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Session login(String workspaceName) throws RepositoryException {
        try {
            int assignType = LogObject.INTERFACE_DEF_SESSION.type;
            int nextId = logStart(assignType, false, "login", StringUtils
                    .quoteString(workspaceName));
            Session result = repository.login(workspaceName);
            result = (Session) wrap(result, false, Session.class, assignType,
                    nextId);
            logReturn();
            return result;
        } catch (Throwable t) {
            throw logAndConvert(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Session login(Credentials credentials, String workspaceName)
            throws RepositoryException {
        try {
            int assignType = LogObject.INTERFACE_DEF_SESSION.type;
            int nextId = logStart(assignType, false, "login", StringUtils
                    .quoteCredentials(credentials)
                    + ", " + StringUtils.quoteString(workspaceName));
            Session result = repository.login(credentials, workspaceName);
            result = (Session) wrap(result, false, Session.class, assignType,
                    nextId);
            logReturn();
            return result;
        } catch (Throwable t) {
            throw logAndConvert(t);
        }
    }

}
