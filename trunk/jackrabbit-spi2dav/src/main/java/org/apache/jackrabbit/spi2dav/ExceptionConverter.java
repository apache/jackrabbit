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
package org.apache.jackrabbit.spi2dav;

import java.lang.reflect.Constructor;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;

/**
 * <code>ExceptionConverter</code>...
 */
public class ExceptionConverter {

    // avoid instantiation
    private ExceptionConverter() {}

    public static RepositoryException generate(DavException davExc) {
        return generate(davExc, null);
    }

    public static RepositoryException generate(DavException davExc, HttpRequestBase request) {
        String name = (request == null) ? "_undefined_" : request.getMethod();
        int code = DavMethods.getMethodCode(name);
        return generate(davExc, code, name);
    }

    public static RepositoryException generate(DavException davExc, int methodCode, String name) {
        String msg = davExc.getMessage();
        if (davExc.hasErrorCondition()) {
            try {
                Element error = davExc.toXml(DomUtil.createDocument());
                if (DomUtil.matches(error, DavException.XML_ERROR, DavConstants.NAMESPACE)) {
                    if (DomUtil.hasChildElement(error, "exception", null)) {
                        Element exc = DomUtil.getChildElement(error, "exception", null);
                        if (DomUtil.hasChildElement(exc, "message", null)) {
                            msg = DomUtil.getChildText(exc, "message", null);
                        }
                        if (DomUtil.hasChildElement(exc, "class", null)) {
                            Class<?> cl = Class.forName(DomUtil.getChildText(exc, "class", null));
                            Constructor<?> excConstr = cl.getConstructor(String.class);
                            if (excConstr != null) {
                                Object o = excConstr.newInstance(msg);
                                if (o instanceof PathNotFoundException && methodCode == DavMethods.DAV_POST) {
                                    // see JCR-2536
                                    return new InvalidItemStateException(msg);
                                } else if (o instanceof RepositoryException) {
                                    return (RepositoryException) o;
                                } else if (o instanceof Exception) {
                                    return new RepositoryException(msg, (Exception)o);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                return new RepositoryException(e);
            }
        }

        // make sure an exception is generated
        switch (davExc.getErrorCode()) {
            // TODO: mapping DAV_error to jcr-exception is ambiguous. to be improved
            case DavServletResponse.SC_NOT_FOUND :
                switch (methodCode) {
                    case DavMethods.DAV_DELETE:
                    case DavMethods.DAV_MKCOL:
                    case DavMethods.DAV_PUT:
                    case DavMethods.DAV_POST:
                        // target item has probably while transient changes have
                        // been made.
                        return new InvalidItemStateException(msg, davExc);
                    default:
                        return new ItemNotFoundException(msg, davExc);
                }
            case DavServletResponse.SC_LOCKED :
                return new LockException(msg, davExc);
            case DavServletResponse.SC_METHOD_NOT_ALLOWED :
                return new ConstraintViolationException(msg, davExc);
            case DavServletResponse.SC_CONFLICT :
                return new InvalidItemStateException(msg, davExc);
            case DavServletResponse.SC_PRECONDITION_FAILED :
                return new LockException(msg, davExc);
            case DavServletResponse.SC_NOT_IMPLEMENTED:
                if (methodCode > 0 && name != null) {
                    return new UnsupportedRepositoryOperationException(
                            "Missing implementation: Method "
                            + name + " could not be executed", davExc);
                } else {
                    return new UnsupportedRepositoryOperationException(
                            "Missing implementation", davExc);
                }
            default:
                return new RepositoryException(msg, davExc);
        }
    }
}
