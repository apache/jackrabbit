/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.bundle;

import java.io.File;
import java.util.Hashtable;

import javax.jcr.Repository;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private volatile RepositoryImpl repository;

    private volatile ServiceRegistration registration;

    public void start(BundleContext context) throws Exception {
        repository = RepositoryImpl.create(
                RepositoryConfig.install(new File("jackrabbit")));

        Hashtable<String, String> properties = new Hashtable<String, String>();
        for (String key : repository.getDescriptorKeys()) {
            String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                properties.put(key, descriptor);
            }
        }
        registration = context.registerService(
                Repository.class.getName(), repository, properties);
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();

        repository.shutdown();
    }

}
