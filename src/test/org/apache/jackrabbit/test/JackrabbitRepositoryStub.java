/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.test;

import org.apache.jackrabbit.core.RepositoryFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.util.Properties;

/**
 * Implements the <code>RepositoryStub</code> for the JCR Reference Implementation.
 */
public class JackrabbitRepositoryStub extends RepositoryStub {

    /**
     * Property for the repositry name
     */
    public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.repository.name";

    public static final String PROP_REPOSITRY_HOME = "org.apache.jackrabbit.repository.home";

    /**
     * The repository instance
     */
    private Repository repository;

    private RepositoryFactory factory;

    /**
     * Constructor as required by the JCR TCK.
     *
     * @param env environment properties.
     */
    public JackrabbitRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * Returns the configured <code>Repository</code> instance.
     * <br>
     * The default repository name is 'localfs'.
     *
     * @return the configured <code>Repository</code> instance.
     * @throws RepositoryStubException if an error occurs while
     *                                 obtaining the Repository instance.
     */
    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String repName = environment.getProperty(PROP_REPOSITORY_NAME, "localfs");
                String repHome = environment.getProperty(PROP_REPOSITRY_HOME);
                factory = RepositoryFactory.create(repHome + "/config.xml", repHome);
                repository = factory.getRepository(repName);
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        factory.shutdown();
                    }
                });
            } catch (RepositoryException e) {
                throw new RepositoryStubException(e.toString());
            }
        }
        return repository;
    }
}
