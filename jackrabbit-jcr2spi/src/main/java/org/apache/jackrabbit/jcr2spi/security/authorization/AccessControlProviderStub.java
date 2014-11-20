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
    private static final String AC_PROVIDER_IMPL_PROP = "implementation_class";

    /**
     * The default provider implementation to instantiate.
     */
    private static final String DEFAULT_PROVIDER_IMPL = "org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl.AccessControlProviderImpl";
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
     * Note: Since the implementation classes need not be public, for reducing the api, the method tries with
     * all constructors that matches the specified argument before to instantiate a given implementation.
     * @param service     The repository service.
     * @return
     * @throws RepositoryException
     */
    public static AccessControlProvider newInstance(RepositoryService service) throws RepositoryException {                     

        try {
            String className = getProviderClass();
            className = (className == null) ? DEFAULT_PROVIDER_IMPL : className;
            AccessControlProvider provider = getProvider(className);
            if (provider == null) {
                Class<?> implKlass = Class.forName(className);                
                Constructor<?> constr = implKlass.getDeclaredConstructor(new Class[]{RepositoryService.class});
                constr.setAccessible(true);
                provider = (AccessControlProvider) constr.newInstance(new Object[]{service});                
                registerProvider(className, provider);                
            }
            return provider;
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage());
        }
    }
    /**
     * Returns the fully qualified name of the class to instantiate.
     * @return the fully qualified class name.
     * @throws RepositoryException
     */
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
                    return null;
                }
            } else {
                return null;
            }
        } else {
            File file = new File(providerImplProp);
            if (file.exists()) { // check that path actually exist.
                prop = new Properties();
                try {
                    prop.load(new FileInputStream(file));
                } catch (IOException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
         return  prop.getProperty(AC_PROVIDER_IMPL_PROP);
    }
    
    /**
     * provider registeration.
     */
    private static void registerProvider(String className, AccessControlProvider provider) {
        providers.put(className, provider);
    }
    
    private static AccessControlProvider getProvider(String className) {
        return providers.get(className);
    }
}