package org.apache.jackrabbit.client.spilogger;

import org.apache.jackrabbit.client.AbstractRepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.logging.SpiLoggerFactory;
import org.apache.jackrabbit.spi.commons.logging.LogWriterProvider;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>RepositoryConfigImpl</code>...
 */
public class RepositoryConfigImpl extends AbstractRepositoryConfig {

    /**
     * LogWriterProvider configuration parameter: If the parameter is present the
     * <code>RepositoryService</code> defined by the specified
     * <code>RepositoryConfig</code> will be wrapped by calling
     * {@link SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService, org.apache.jackrabbit.spi.commons.logging.LogWriterProvider) }
     * if the param value is an instance of <code>LogWriterProvider</code> or
     * {@link SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService)}
     * otherwise.
     *
     * @see SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService)
     * @see SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService, org.apache.jackrabbit.spi.commons.logging.LogWriterProvider)
     */
    public static final String PARAM_LOG_WRITER_PROVIDER = "org.apache.jackrabbit.repository.spi.logging.logwriterprovider";

    private final RepositoryService service;

    public static RepositoryConfig create(RepositoryConfig config, Map params) throws RepositoryException {
        if (config == null || params == null || !params.containsKey(PARAM_LOG_WRITER_PROVIDER)) {
            return config;
        } else {
            return new RepositoryConfigImpl(config, params);
        }
    }

    private RepositoryConfigImpl(RepositoryConfig config, Map params) throws RepositoryException {
        super(config.getCacheBehaviour(), config.getItemCacheSize(), config.getPollTimeout());
        Object lwProvider = params.get(PARAM_LOG_WRITER_PROVIDER);
        if (lwProvider == null || !(lwProvider instanceof LogWriterProvider)) {
            service = SpiLoggerFactory.create(config.getRepositoryService());
        } else {
            service = SpiLoggerFactory.create(config.getRepositoryService(), (LogWriterProvider) lwProvider);
        }
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }
}
