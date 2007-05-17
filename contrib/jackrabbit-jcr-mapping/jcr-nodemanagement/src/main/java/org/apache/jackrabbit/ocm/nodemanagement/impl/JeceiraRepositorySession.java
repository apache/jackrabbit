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
package org.apache.jackrabbit.ocm.nodemanagement.impl;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.nodemanagement.RepositorySession;

/** 
 * This class is the Jeceira JCR Repository session implementation.
 * TODO : 
 * 		Uncomment the code in this class &  Add Jeceira dependencies in maven pom.xml
 *      I don't know which Jeceira version to use.
 *      If nobody is interested by a Jeceira support, I would like to suggest to drop this class 
 *       
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class JeceiraRepositorySession implements RepositorySession
{
    
    /** Creates a new instance of JeceiraRepositorySession. */
    public JeceiraRepositorySession()
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.RepositorySession#getSession
     */    
    public Session getSession(String username, String password,
            RepositoryConfiguration configuration)
    {
        Session session = null;

        
//        try {
//            RepositoryFactory repositoryFactory = RepositoryFactory.getInstance();
//            Repository repository = repositoryFactory.getRepository(configuration.getRepositoryName());
//            
//            session = repository.login();
//        }
//        catch (RepositoryException re)
//        {
//            re.printStackTrace();
//        }
        
        return session;
    } 
}
