/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.rmi;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.easymock.MockControl;

/**
 * TODO
 */
public class RemoteTestHelper {

    private Map methods;
    
    public RemoteTestHelper(Class iface) {
        methods = new HashMap();
        Method[] m = iface.getDeclaredMethods();
        for (int i = 0; i < m.length; i++) {
            methods.put(m[i].getName(), m[i]);
        }
    }
    
    public void ignoreMethod(String name) {
        methods.remove(name);
    }
    
    private Object[] getParameters(Method method) {
        Class[] types = method.getParameterTypes();
        Object[] parameters = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (!types[i].isPrimitive()) {
                parameters[i] = null;
            } else if ("int".equals(types[i].getName())) {
                parameters[i] = new Integer(0);
            } else if ("boolean".equals(types[i].getName())) {
                parameters[i] = new Boolean(false);
            } else {
                System.out.println(types[i].getName());
                parameters[i] = null;
            }
        }
        return parameters;
    }

    private void setReturnValue(Method method, MockControl control) {
        Class type = method.getReturnType();
        if (!type.isPrimitive()) {
            control.setReturnValue(null);
        } else if ("void".equals(type.getName())) {
            control.setVoidCallable();
        } else if ("int".equals(type.getName())) {
            control.setReturnValue((int) 0);
        } else if ("long".equals(type.getName())) {
            control.setReturnValue((long) 0);
        } else if ("boolean".equals(type.getName())) {
            control.setReturnValue(false);
        } else {
            System.out.println(type.getName());
            control.setReturnValue(null);
        }
    }
    
    public void testRemoteMethods(Object frontend, Object backend,
            MockControl control) throws Exception {
        Iterator iterator = methods.values().iterator();
        while (iterator.hasNext()) {
            Method method = (Method) iterator.next();
            Object[] parameters = getParameters(method);
            
            method.invoke(backend, parameters);
            setReturnValue(method, control);
            control.replay();
            
            method.invoke(frontend, parameters);
            control.verify();
            
            control.reset();
        }
    }
    
}
