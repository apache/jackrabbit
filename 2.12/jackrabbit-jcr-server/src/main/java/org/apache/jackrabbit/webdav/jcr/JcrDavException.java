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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;
import java.util.Map;
import java.util.LinkedHashMap;


/**
 * <code>JcrDavException</code> extends the {@link DavException} in order to
 * wrap various repository exceptions.
 */
public class JcrDavException extends DavException {

    private static Logger log = LoggerFactory.getLogger(JcrDavException.class);

    // ordered mapping of Jcr exceptions to error codes.
    private static Map<Class<? extends Throwable>, Integer> codeMap = new LinkedHashMap<Class<? extends Throwable>, Integer>(20);
    static {
        codeMap.put(AccessDeniedException.class, DavServletResponse.SC_FORBIDDEN);
        codeMap.put(ConstraintViolationException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(InvalidItemStateException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(InvalidSerializedDataException.class, DavServletResponse.SC_BAD_REQUEST);
        codeMap.put(InvalidQueryException.class, DavServletResponse.SC_BAD_REQUEST);
        codeMap.put(ItemExistsException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(ItemNotFoundException.class, DavServletResponse.SC_FORBIDDEN);
        codeMap.put(LockException.class, DavServletResponse.SC_LOCKED);
        codeMap.put(MergeException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(NamespaceException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(NoSuchNodeTypeException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(NoSuchWorkspaceException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(PathNotFoundException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(ReferentialIntegrityException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(LoginException.class, DavServletResponse.SC_UNAUTHORIZED);
        codeMap.put(UnsupportedRepositoryOperationException.class, DavServletResponse.SC_NOT_IMPLEMENTED);
        codeMap.put(ValueFormatException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(VersionException.class, DavServletResponse.SC_CONFLICT);
        codeMap.put(RepositoryException.class, DavServletResponse.SC_FORBIDDEN);
    }

    private static int lookupErrorCode(Class<? extends Throwable> exceptionClass) {
        Integer code = codeMap.get(exceptionClass);
        if (code == null) {
            for (Class<? extends Throwable> jcrExceptionClass : codeMap.keySet()) {
                if (jcrExceptionClass.isAssignableFrom(exceptionClass)) {
                    code = codeMap.get(jcrExceptionClass);
                    break;
                }
            }
            if (code == null) {
                code = DavServletResponse.SC_FORBIDDEN; // fallback
            }
        }
        return code;
    }

    /**
     * The exception wrapped by this DavException instance.
     */
    private Class<? extends Throwable> exceptionClass;

    /**
     * Create a new <code>JcrDavException</code>.
     *
     * @param cause The original cause of this <code>DavException</code>. Note, that
     * in contrast to {@link Throwable#Throwable(Throwable)}, {@link Throwable#Throwable(String, Throwable)} and
     * {@link Throwable#initCause(Throwable)} the cause must not be <code>null</code>.
     * @param errorCode Status code for the response.
     * @throws NullPointerException if the given exception is <code>null</code>.
     * @see DavException#DavException(int, String)
     * @see DavException#DavException(int)
     */
    public JcrDavException(Throwable cause, int errorCode) {
        super(errorCode, cause.getMessage(), cause, null);
        exceptionClass = cause.getClass();
        if (log.isDebugEnabled()) {
            log.debug("Handling exception with error code " + errorCode, cause);
        }
    }

    /**
     * Same as {@link JcrDavException#JcrDavException(Throwable, int)} where the
     * error code is retrieved from an internal mapping.
     *
     * @param cause Cause of this DavException
     * @throws NullPointerException if the given exception is <code>null</code>.
     * @see JcrDavException#JcrDavException(Throwable, int)
     */
    public JcrDavException(RepositoryException cause) {
        this(cause, lookupErrorCode(cause.getClass()));
    }

    /**
     * Always returns true.
     *
     * @return true
     */
    @Override
    public boolean hasErrorCondition() {
        return true;
    }

    /**
     * Returns a DAV:error Xml element containing the exceptions class and the
     * message as child elements.
     *
     * @return Xml representation of this exception.
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    @Override
    public Element toXml(Document document) {
        Element error = DomUtil.createElement(document, XML_ERROR, DavConstants.NAMESPACE);
        Element excep = DomUtil.createElement(document, "exception", ItemResourceConstants.NAMESPACE);
        DomUtil.addChildElement(excep, "class", ItemResourceConstants.NAMESPACE, exceptionClass.getName());
        DomUtil.addChildElement(excep, "message", ItemResourceConstants.NAMESPACE, getMessage());
        error.appendChild(excep);
        return error;
    }
}
