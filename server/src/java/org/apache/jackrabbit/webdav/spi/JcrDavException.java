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
package org.apache.jackrabbit.webdav.spi;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.jdom.Element;

import javax.jcr.*;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.*;

/**
 * <code>JcrDavException</code> extends the {@link DavException} in order to
 * wrap various repository exceptions.
 */
public class JcrDavException extends DavException {

    private static Logger log = Logger.getLogger(JcrDavException.class);

    private Class exceptionClass;

    public JcrDavException(Exception e, int errorCode) {
        super(errorCode, e.getMessage());
        exceptionClass = e.getClass();
    }

    public JcrDavException(AccessDeniedException e) {
        this(e, DavServletResponse.SC_FORBIDDEN);
    }

    public JcrDavException(ConstraintViolationException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(InvalidItemStateException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(InvalidSerializedDataException e) {
        this(e, DavServletResponse.SC_BAD_REQUEST);
    }

    public JcrDavException(InvalidQueryException e) {
        this(e, DavServletResponse.SC_BAD_REQUEST);
    }

    public JcrDavException(ItemExistsException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(ItemNotFoundException e) {
        this(e, DavServletResponse.SC_FORBIDDEN);
    }

    public JcrDavException(LockException e) {
        this(e, DavServletResponse.SC_LOCKED);
    }

    public JcrDavException(MergeException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(NamespaceException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(NoSuchNodeTypeException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(NoSuchWorkspaceException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(PathNotFoundException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(ReferentialIntegrityException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(RepositoryException e) {
        this(e, DavServletResponse.SC_FORBIDDEN);
    }

    public JcrDavException(UnsupportedRepositoryOperationException e) {
        this(e, DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    public JcrDavException(ValueFormatException e) {
        this(e, DavServletResponse.SC_CONFLICT);
    }

    public JcrDavException(VersionException e) {
        this(e, DavServletResponse.SC_CONFLICT);
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