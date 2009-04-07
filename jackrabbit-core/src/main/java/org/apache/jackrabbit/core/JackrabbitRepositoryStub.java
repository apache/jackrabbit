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
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
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
     * The repository instance.
     */
    private Repository repository;

    private static Properties getStaticProperties() {
        Properties properties = new Properties();
        try {
            InputStream stream =
                JackrabbitRepositoryStub.class.getResourceAsStream(
                        "JackrabbitRepositoryStub.properties");
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
        if (repository == null) {
            try {
                String dir = settings.getProperty(PROP_REPOSITORY_HOME);
                if (dir == null) {
                    dir = new File("target", "repository").getPath();
                }

                new File(dir).mkdirs();

                String xml = settings.getProperty(PROP_REPOSITORY_CONFIG);
                if (xml == null) {
                    xml = new File(dir, "repository.xml").getPath();
                }

                if (!new File(xml).exists()) {
                    InputStream input =
                        RepositoryImpl.class.getResourceAsStream("repository.xml");
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
                repository = RepositoryImpl.create(config);
            } catch (Exception e) {
                RepositoryStubException exception =
                    new RepositoryStubException("Failed to start repository");
                exception.initCause(e);
                throw exception;
            }
        }
        return repository;
    }
}
