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
package org.apache.jackrabbit.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;

/**
 * RepositoryStub implementation for Apache Jackrabbit.
 *
 * @since Apache Jackrabbit 1.6
 */
public class JackrabbitRepositoryStub extends RepositoryStub {

    /**
     * Property for the repository configuration file. Defaults to
     * &lt;repository home&gt;/repository.xml if not specified.
     */
    public static final String PROP_REPOSITORY_CONFIG =
        "org.apache.jackrabbit.repository.config";

    /**
     * Property for the repository home directory. Defaults to
     * target/repository for convenience in Maven builds.
     */
    public static final String PROP_REPOSITORY_HOME =
        "org.apache.jackrabbit.repository.home";

    /**
     * Repository settings.
     */
    private final Properties settings;

    /**
     * Map of repository instances. Key = repository home, value = repository
     * instance.
     */
    private static final Map<String, Repository> REPOSITORY_INSTANCES = new HashMap<String, Repository>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                synchronized (REPOSITORY_INSTANCES) {
                    for (Repository repo : REPOSITORY_INSTANCES.values()) {
                        if (repo instanceof RepositoryImpl) {
                            ((RepositoryImpl) repo).shutdown();
                        }
                    }
                }
            }
        }));
    }

    public static RepositoryContext getRepositoryContext(
            Repository repository) {
        synchronized (REPOSITORY_INSTANCES) {
            for (Repository r : REPOSITORY_INSTANCES.values()) {
                if (r == repository) {
                    return ((RepositoryImpl) r).context;
                }
            }
        }
        throw new RuntimeException("Not a test repository: " + repository);
    }

    private static Properties getStaticProperties() {
        Properties properties = new Properties();
        try {
            InputStream stream =
                getResource("JackrabbitRepositoryStub.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            // TODO: Log warning
        }
        return properties;
    }

    private static InputStream getResource(String name) {
        return JackrabbitRepositoryStub.class.getResourceAsStream(name);
    }

    /**
     * Constructor as required by the JCR TCK.
     *
     * @param settings repository settings
     */
    public JackrabbitRepositoryStub(Properties settings) {
        super(getStaticProperties());
        // set some attributes on the sessions
        superuser.setAttribute("jackrabbit", "jackrabbit");
        readwrite.setAttribute("jackrabbit", "jackrabbit");
        readonly.setAttribute("jackrabbit", "jackrabbit");

        // Repository settings
        this.settings = settings;
    }

    /**
     * Returns the configured repository instance.
     *
     * @return the configured repository instance.
     * @throws RepositoryStubException if an error occurs while
     *                                 obtaining the repository instance.
     */
    public synchronized Repository getRepository()
            throws RepositoryStubException {
        try {
            String dir = settings.getProperty(PROP_REPOSITORY_HOME);
            if (dir == null) {
                dir = new File("target", "repository").getAbsolutePath();
            } else {
                dir = new File(dir).getAbsolutePath();
            }

            String xml = settings.getProperty(PROP_REPOSITORY_CONFIG);
            if (xml == null) {
                xml = new File(dir, "repository.xml").getPath();
            }

            return getOrCreateRepository(dir, xml);
        } catch (Exception e) {
            throw new RepositoryStubException("Failed to start repository", e);
        }
    }

    protected Repository createRepository(String dir, String xml)
            throws Exception {
        new File(dir).mkdirs();

        if (!new File(xml).exists()) {
            InputStream input = getResource("repository.xml");
            try {
                OutputStream output = new FileOutputStream(xml);
                try {
                    IOUtils.copy(input, output);
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
        }

        RepositoryConfig config = RepositoryConfig.create(xml, dir);
        return RepositoryImpl.create(config);
    }

    protected Repository getOrCreateRepository(String dir, String xml)
            throws Exception {
        synchronized (REPOSITORY_INSTANCES) {
            Repository repo = REPOSITORY_INSTANCES.get(dir);
            if (repo == null) {
                repo = createRepository(dir, xml);
                Session session = repo.login(superuser);
                try {
                    TestContentLoader loader = new TestContentLoader();
                    loader.loadTestContent(session);
                } finally {
                    session.logout();
                }

                REPOSITORY_INSTANCES.put(dir, repo);
            }
            return repo;
        }
    }

    @Override
    public Principal getKnownPrincipal(Session session) throws RepositoryException {

        Principal knownPrincipal = null;

        if (session instanceof SessionImpl) {
            for (Principal p : ((SessionImpl)session).getSubject().getPrincipals()) {
                if (!GroupPrincipals.isGroup(p)) {
                    knownPrincipal = p;
                }
            }
        }

        if (knownPrincipal != null) {
            return knownPrincipal;
        }
        else {
            throw new RepositoryException("no applicable principal found");
        }
    }

    private static Principal UNKNOWN_PRINCIPAL = new Principal() {
        public String getName() {
            return "an_unknown_user";
        }
    };

    @Override
    public Principal getUnknownPrincipal(Session session) throws RepositoryException, NotExecutableException {
        return UNKNOWN_PRINCIPAL;
    }

}
