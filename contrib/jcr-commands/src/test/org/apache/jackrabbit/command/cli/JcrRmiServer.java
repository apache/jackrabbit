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
package org.apache.jackrabbit.command.cli;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

/**
 * 16/08/2005
 */
public class JcrRmiServer
{
    /** config */
    private static String CONFIG = "applications/test/repository.xml";

    /** home */
    private static String HOME = "applications/test/repository";

    /** rmi url */
    private static String URL = "jcr/repository";

    public static void main(String[] args) throws RepositoryException, MalformedURLException, RemoteException, AlreadyBoundException
    {
        RepositoryConfig conf = RepositoryConfig.create(CONFIG, HOME);
        Repository repo = RepositoryImpl.create(conf);
       
        RemoteAdapterFactory factory = new ServerAdapterFactory();
        RemoteRepository remote = factory.getRemoteRepository(repo);
        Naming.bind(URL, remote);  
        
    }
}
