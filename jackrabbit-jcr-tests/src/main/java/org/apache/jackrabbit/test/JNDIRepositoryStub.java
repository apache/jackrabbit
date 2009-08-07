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
package org.apache.jackrabbit.test;

import javax.jcr.Repository;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.util.Properties;

/**
 * Implements the abstract class <code>RepositoryStub</code> and uses JNDI
 * to obtain a <code>javax.jcr.Repository</code> instance.
 */
public class JNDIRepositoryStub extends RepositoryStub {

    public static final String REPOSITORY_LOOKUP_PROP = "javax.jcr.tck.jndi.repository_lookup_name";

    private Repository repository = null;

    public JNDIRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * Returns a reference to the <code>Repository</code> provided by this
     * <code>RepositoryStub</code>.
     * @return
     */
    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String lookupName = environment.getProperty(REPOSITORY_LOOKUP_PROP);
                if (lookupName == null) {
                    throw new RepositoryStubException("Property " + REPOSITORY_LOOKUP_PROP + " not defined!");
                }
                InitialContext initial = new InitialContext(environment);
                Object obj = initial.lookup(lookupName);

                repository = (Repository)PortableRemoteObject.narrow(obj, Repository.class);

            } catch (ClassCastException e) {
                // ClassCastException may be thrown by ProtableRemoteObject.narrow()
                throw new RepositoryStubException("Object cannot be narrowed to javax.jcr.Repository: " + e);
            } catch (NamingException e) {
                throw new RepositoryStubException(e.getMessage());
            }
        }
        return repository;
    }

}
