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
import java.util.HashSet;
import java.util.Set;

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
 * Converter utility for mapping WebDAV protocol errors ({@link DavException}) into
 * corresponding JCR API exceptions ({@link RepositoryException}).
 * <p>
 * This class translates WebDAV server responses back into native JCR exception types so that
 * remote repository client operations maintain standard JCR API error behaviors.
 * </p>
 */
public class ExceptionConverter {

    // these exception classes are allowed to be instantiated
    private static final Set<String> SAFE_EXCEPTION_NAMES = new HashSet<>();
    static {
        SAFE_EXCEPTION_NAMES.add(javax.jcr.AccessDeniedException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.InvalidItemStateException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.InvalidLifecycleTransitionException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.InvalidSerializedDataException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.ItemExistsException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.ItemNotFoundException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.LoginException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.MergeException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.NamespaceException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.NoSuchWorkspaceException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.PathNotFoundException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.ReferentialIntegrityException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.RepositoryException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.UnsupportedRepositoryOperationException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.ValueFormatException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.lock.LockException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.nodetype.ConstraintViolationException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.nodetype.InvalidNodeTypeDefinitionException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.nodetype.NoSuchNodeTypeException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.nodetype.NodeTypeExistsException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.query.InvalidQueryException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.security.AccessControlException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.version.ActivityViolationException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.version.LabelExistsVersionException.class.getName());
        SAFE_EXCEPTION_NAMES.add(javax.jcr.version.VersionException.class.getName());
    }

    // avoid instantiation
    private ExceptionConverter() {}

    /**
     * @param davExc the WebDAV exception to convert
     * @return the mapped JCR {@link RepositoryException}
     * @see #generate(DavException, HttpRequestBase)
     */
    public static RepositoryException generate(DavException davExc) {
        return generate(davExc, null);
    }

    /**
     * @param davExc  the WebDAV exception to convert
     * @param request the HTTP request that triggered the WebDAV exception, or {@code null} if unavailable
     * @return the mapped JCR {@link RepositoryException}
     * @see #generate(DavException, int, String)
     */
    public static RepositoryException generate(DavException davExc, HttpRequestBase request) {
        String name = (request == null) ? "_undefined_" : request.getMethod();
        int code = DavMethods.getMethodCode(name);
        return generate(davExc, code, name);
    }

    /**
     * Generates a JCR {@link RepositoryException} from a {@link DavException}, HTTP method code,
     * and method name string.
     * <p>
     * The method first attempts to extract explicit exception information (class name and message)
     * from the WebDAV XML error response. If the class name matches an entry in {@link #SAFE_EXCEPTION_NAMES},
     * it reflectively instantiates that specific JCR exception.
     * </p>
     * <p>
     * If no XML error details exist or the exception class is not in the allow-list, it converts
     * the HTTP status code (and method context where appropriate) into a standard JCR exception type
     * such as {@link ItemNotFoundException}, {@link LockException}, or {@link ConstraintViolationException}.
     * </p>
     *
     * @param davExc     the WebDAV exception to convert
     * @param methodCode the numeric code of the WebDAV HTTP method (from {@link DavMethods})
     * @param name       the HTTP method name string (e.g., "GET", "POST", "MKCOL")
     * @return the resulting JCR {@link RepositoryException}
     */
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
                            String className = DomUtil.getChildText(exc, "class", null);
                            // never Class.forName or construct on wire data:
                            // only the standard JCR exception types are
                            // reconstructed; any other class name falls
                            // through to the status-code based mapping below.
                            if (className != null && SAFE_EXCEPTION_NAMES.contains(className)) {
                                return getRepositoryException(methodCode, className, msg);
                            } else {
                                throw new InvalidItemStateException(
                                        String.format("Class '%s' not in allow list for exception conversion", className));
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

    /**
     * Reflectively instantiates a {@link RepositoryException} sub-class for the given class name and error message.
     * <p>
     * Performs a type-check to confirm that the requested class extends {@link RepositoryException}.
     * Special handling is applied for {@link PathNotFoundException} when encountered during a {@code POST} request,
     * translating it into an {@link InvalidItemStateException}.
     * </p>
     *
     * @param methodCode the numeric code of the WebDAV HTTP method
     * @param className  the fully qualified class name of the target JCR exception
     * @param msg        the error detail message
     * @return the reflectively created {@link RepositoryException}
     * @throws ReflectiveOperationException if class loading, constructor resolution, or instantiation fails
     * @throws InvalidItemStateException    if the target class is not a subclass of {@link RepositoryException}
     */
    private static RepositoryException getRepositoryException(int methodCode, String className, String msg) throws ReflectiveOperationException, InvalidItemStateException {
        Class<?> cl = Class.forName(className);
        // type check before construction
        if (RepositoryException.class.isAssignableFrom(cl)) {
            Constructor<?> excConstr = cl.getConstructor(String.class);
            Object o = excConstr.newInstance(msg);
            if (o instanceof PathNotFoundException && methodCode == DavMethods.DAV_POST) {
                // see JCR-2536
                return new InvalidItemStateException(msg);
            } else {
                return (RepositoryException) o;
            }
        } else {
            throw new InvalidItemStateException(
                    String.format("Class '%s' not assignable from '%s'", RepositoryException.class.getName(), className));
        }
    }
}
