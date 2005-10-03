/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.jdom.Element;

import javax.jcr.*;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.*;
import java.util.HashMap;

/**
 * <code>JcrDavException</code> extends the {@link DavException} in order to
 * wrap various repository exceptions.
 */
public class JcrDavException extends DavException {

    private static Logger log = Logger.getLogger(JcrDavException.class);

    // mapping of Jcr exceptions to error codes.
    private static HashMap codeMap = new HashMap();
    static {
        codeMap.put(AccessDeniedException.class, new Integer(DavServletResponse.SC_FORBIDDEN));
        codeMap.put(ConstraintViolationException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(InvalidItemStateException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(InvalidSerializedDataException.class, new Integer(DavServletResponse.SC_BAD_REQUEST));
        codeMap.put(InvalidQueryException.class, new Integer(DavServletResponse.SC_BAD_REQUEST));
        codeMap.put(ItemExistsException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(ItemNotFoundException.class, new Integer(DavServletResponse.SC_FORBIDDEN));
        codeMap.put(LockException.class, new Integer(DavServletResponse.SC_LOCKED));
        codeMap.put(MergeException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(NamespaceException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(NoSuchNodeTypeException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(NoSuchWorkspaceException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(PathNotFoundException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(ReferentialIntegrityException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(RepositoryException.class, new Integer(DavServletResponse.SC_FORBIDDEN));
        codeMap.put(LoginException.class, new Integer(DavServletResponse.SC_UNAUTHORIZED));
        codeMap.put(UnsupportedRepositoryOperationException.class, new Integer(DavServletResponse.SC_NOT_IMPLEMENTED));
        codeMap.put(ValueFormatException.class, new Integer(DavServletResponse.SC_CONFLICT));
        codeMap.put(VersionException.class, new Integer(DavServletResponse.SC_CONFLICT));
    }

    private Class exceptionClass;

    public JcrDavException(Exception e, int errorCode) {
        super(errorCode, e.getMessage());
        exceptionClass = e.getClass();
    }

    public JcrDavException(RepositoryException e) {
        this(e, ((Integer)codeMap.get(e.getClass())).intValue());
    }

    /**
     * Returns a DAV:error Xml element containing the exceptions class and the
     * message as child elements.
     *
     * @return Xml representation of this exception.
     */
    public Element getError() {
        Element error = super.getError();
        Element excep = new Element("exception", ItemResourceConstants.NAMESPACE);
        excep.addContent(new Element("class", ItemResourceConstants.NAMESPACE).setText(exceptionClass.getName()));
        excep.addContent(new Element("message", ItemResourceConstants.NAMESPACE).setText(getMessage()));
        error.addContent(excep);
        return error;
    }
}