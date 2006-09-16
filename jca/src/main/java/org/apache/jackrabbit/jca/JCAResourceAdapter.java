package org.apache.jackrabbit.jca;

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
public class JCAResourceAdapter implements ResourceAdapter {

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
