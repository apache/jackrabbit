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
package org.apache.jackrabbit.jca;

import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;


/**
 * JCR ResourceAdapter.
 */
public class JCAResourceAdapter implements ResourceAdapter, Serializable {

    private static final long serialVersionUID = 7335723888000232035L;
    
    private final XAResource[] xaResources = new XAResource[0];

    /**
     * Notify the RepositoryManager that the lifecycle is managed by
     * the container
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        JCARepositoryManager.getInstance().setAutoShutdown(false);
    }

    /**
     * Shutdown jackrabbit repositories
     */
    public void stop() {
        JCARepositoryManager.getInstance().shutdown();
    }

    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as) throws ResourceException {
    }

    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
    }

    public XAResource[] getXAResources(ActivationSpec[] as) throws ResourceException {
        return xaResources;
    }

}
