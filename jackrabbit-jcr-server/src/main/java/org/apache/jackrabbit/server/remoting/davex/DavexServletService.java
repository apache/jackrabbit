/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.server.remoting.davex;

import java.util.Map;

import javax.jcr.Repository;
import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.util.CSRFUtil;

@Component(metatype = true, label = "%dav.name", description = "%dav.description")
@Service(Servlet.class)
@Properties({
    @Property(name = "service.description", value = "Apache Jackrabbit JcrRemoting Servlet"),
    @Property(name = JcrRemotingServlet.INIT_PARAM_AUTHENTICATE_HEADER, value = AbstractWebdavServlet.DEFAULT_AUTHENTICATE_HEADER),
    @Property(name = JcrRemotingServlet.INIT_PARAM_CSRF_PROTECTION, value = CSRFUtil.DISABLED),
    @Property(name = JcrRemotingServlet.INIT_PARAM_MISSING_AUTH_MAPPING, value = "") })
public class DavexServletService extends JcrRemotingServlet {

    /** Serial version UID */
    private static final long serialVersionUID = -901601294536148635L;

    private static final String DEFAULT_ALIAS = "/server";

    @Property(value = DEFAULT_ALIAS)
    private static final String PARAM_ALIAS = "alias";

    @Reference
    private Repository repository;

    private String alias;

    @Override
    protected Repository getRepository() {
        return repository;
    }

    @Override
    protected String getResourcePathPrefix() {
        return alias;
    }

    @Activate
    public void activate(Map<String, ?> config) {
        Object object = config.get(PARAM_ALIAS);
        String string = "";
        if (object != null) {
            string = object.toString();
        }
        if (string.length() > 0) {
            this.alias = string;
        } else {
            this.alias = DEFAULT_ALIAS;
        }
    }

}
