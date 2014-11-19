package org.apache.jackrabbit.jcr2spi.security.authorization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.RepositoryService;

/**
 * Stub class that provide clients with access to a concrete AccessControlProvider implementation.
 * TODO: Explain the way the concrete provider is located, loaded and instantiated.
 * 
 */
public class AccessControlProviderStub {

    /**
     * This system parameter determines the file system path of the {@link AccessControlProvider} to
     * load and instantiate. This parameter is first check when locating a provider.
     */
    private static final String SYS_AC_PROVIDER_PROPS_FILE = "org.apache.jackrabbit.jcr2spi.accessControlProvider.properties";
    /**
     * The class property parameter determines the {@link AccessControlProvider}
     * to load and instantiate. This is a fall-back parameter if the SYS_PROP_AC_PROVIDER_IMPL is not set.
     */
    private static final String CLASS_AC_PROVIDER_PROPS_FILE = "accessControlProvider.properties";

    /**
     * Key look-up.
     */
    private static final String AC_PROVIDER_IMPL_PROP = "org.apache.jackrabbit.jcr2spi.AccessControlProvider_Impl";

    /**
     * Cache of instantiated providers.
     */
    private static Map<String, AccessControlProvider> providers = new HashMap<String, AccessControlProvider>();
    
    /**
     * Avoid instantiation.
     */
    private AccessControlProviderStub() {}

    /**
     * Instantiates and returns a concrete AccessControlProvider implementation.
     * @param service     The repository service.
     * @return
     * @throws RepositoryException
     */
    public static AccessControlProvider newInstance(RepositoryService service) throws RepositoryException {                     

        try {
            String className = getProviderClass();
            AccessControlProvider provider = providers.get(className);
            if (provider == null) {
                Class<?> implKlass = Class.forName(className);                
                Constructor<?> constr = implKlass.getConstructor(new Class[]{RepositoryService.class});
                provider = (AccessControlProvider) constr.newInstance(new Object[]{service});
                providers.put(className, provider);
            }
            return provider;
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage());
        }
    }
    
    private static String getProviderClass() throws RepositoryException {
        Properties prop = null;
        String providerImplProp = System.getProperty(SYS_AC_PROVIDER_PROPS_FILE);

        if (providerImplProp == null) {
            InputStream is = AccessControlProviderStub.class.getResourceAsStream(CLASS_AC_PROVIDER_PROPS_FILE);
            if (is != null) {
                prop = new Properties();
                try {
                    prop.load(is);
                } catch (IOException ioe) {
                    throw new RepositoryException(
                            "Unable to load properties file: "
                                    + providerImplProp);
                }
            } else {
                throw new RepositoryException(
                        "Fail to locate the access control provider properties file.");
            }
        } else {
            File file = new File(providerImplProp);
            if (file.exists()) { // check that path actually exist.
                prop = new Properties();
                try {
                    prop.load(new FileInputStream(file));
                } catch (IOException e) {
                    throw new RepositoryException(
                            "Unable to load properties file: "
                                    + providerImplProp);
                }
            } else {
                throw new RepositoryException(
                        "Fail to locate the access control provider properties file.");
            }
        }

        // loads the concrete class to instantiate.
         String  className = prop.getProperty(AC_PROVIDER_IMPL_PROP);
         if (className == null || className.length() == 0) {
             throw new RepositoryException("Fail to locate an AccessControlProvider");
         }
         return className;
    }
}