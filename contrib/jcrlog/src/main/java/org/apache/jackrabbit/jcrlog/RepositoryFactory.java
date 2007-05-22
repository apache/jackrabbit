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

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.TransientRepository;

/**
 * This is an idea how opening a repository could be made vendor-independent.
 * Only one conrecte class (this one) needs to be provided by the JCR API. The
 * mechanism is similar to java.sql.DriverManager
 *
 * @author Thomas Mueller
 *
 */
public class RepositoryFactory {
    static final String CLASSNAME = RepositoryFactory.class.getName();

    /**
     * Open a repository using a repository URL.
     *
     * @param url the URL
     * @return the repository
     * @throws RepositoryException
     */
    public static Repository open(String url) throws RepositoryException {
        if (url.startsWith("apache/jackrabbit/")) {
            url = url.substring("apache/jackrabbit/".length());
            if (url.equals("transient")) {
                try {
                    return new TransientRepository();
                } catch (IOException e) {
                    RepositoryException re = new RepositoryException(
                            "Error creating TransientRepository");
                    re.initCause(e);
                    throw re;
                }
            } else if (url.startsWith("logger/")) {
                url = url.substring("logger/".length());
                return RepositoryLogger.open(url);
            } else {
                throw new RepositoryException("unknown repository type: " + url);
            }
        } else {
            throw new RepositoryException("unknown provider: " + url);
        }
    }
}
