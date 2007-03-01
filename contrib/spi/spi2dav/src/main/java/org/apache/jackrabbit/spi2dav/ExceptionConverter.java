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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;
import java.lang.reflect.Constructor;

/**
 * <code>ExceptionConverter</code>...
 */
public class ExceptionConverter {

    private static Logger log = LoggerFactory.getLogger(ExceptionConverter.class);

    // avoid instanciation
    private ExceptionConverter() {}

    static RepositoryException generate(DavException davExc) throws RepositoryException {
        String msg = davExc.getMessage();
        if (davExc.hasErrorCondition()) {
            try {
                Element error = davExc.toXml(DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument());
                if (DomUtil.matches(error, DavException.XML_ERROR, DavConstants.NAMESPACE)) {
                    if (DomUtil.hasChildElement(error, "exception", null)) {
                        Element exc = DomUtil.getChildElement(error, "exception", null);
                        if (DomUtil.hasChildElement(exc, "message", null)) {
                            msg = DomUtil.getChildText(exc, "message", null);
                        }
                        if (DomUtil.hasChildElement(exc, "class", null)) {
                            Class cl = Class.forName(DomUtil.getChildText(exc, "class", null));
                            Constructor excConstr = cl.getConstructor(new Class[]{String.class});
                            if (excConstr != null) {
                                Object o = excConstr.newInstance(new String[]{msg});
                                if (o instanceof RepositoryException) {
                                    return (RepositoryException) o;
                                } else if (o instanceof Exception) {
                                    return new RepositoryException(msg, (Exception)o);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }

        // make sure an exception is generated
        switch (davExc.getErrorCode()) {
            // TODO: mapping DAV_error to jcr-exception is ambiguous. to be improved
            case DavServletResponse.SC_NOT_FOUND : return new ItemNotFoundException(msg);
            case DavServletResponse.SC_LOCKED : return new LockException(msg);
            case DavServletResponse.SC_METHOD_NOT_ALLOWED : return new ConstraintViolationException(msg);
            case DavServletResponse.SC_CONFLICT : return new InvalidItemStateException(msg);
            case DavServletResponse.SC_PRECONDITION_FAILED : return new LockException(msg);
            default: return new RepositoryException(msg);
        }
    }
}