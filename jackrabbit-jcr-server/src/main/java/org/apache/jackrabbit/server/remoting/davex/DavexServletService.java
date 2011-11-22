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

import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.util.CSRFUtil;
import org.osgi.service.http.HttpService;
import org.slf4j.LoggerFactory;

@Component(metatype = true, label = "%dav.name", description = "%dav.description")
@Properties({
    @Property(name = "service.description", value = "Apache Jackrabbit JcrRemoting Servlet"),
    @Property(name = JcrRemotingServlet.INIT_PARAM_AUTHENTICATE_HEADER, value = AbstractWebdavServlet.DEFAULT_AUTHENTICATE_HEADER),
    @Property(name = JcrRemotingServlet.INIT_PARAM_CSRF_PROTECTION, value = CSRFUtil.DISABLED),
    @Property(name = JcrRemotingServlet.INIT_PARAM_MISSING_AUTH_MAPPING, value = "") })
public class DavexServletService extends JcrRemotingServlet {

    /** Serial version UID */
    private static final long serialVersionUID = -8588285209666835376L;

    private static final String DEFAULT_ALIAS = "/server";

    @Property(value = DEFAULT_ALIAS)
    private static final String PARAM_ALIAS = "alias";

    @Reference
    private Repository repository;

    @Reference
    private HttpService httpService;

    private String alias;

    @Override
    protected Repository getRepository() {
        return repository;
    }

    @SuppressWarnings("unused")
    @Activate
    private void activate(Map<String, ?> config) {
        String alias;
        Object aliasPar = config.get(PARAM_ALIAS);
        if (aliasPar == null) {
            alias = DEFAULT_ALIAS;
        } else {
            alias = aliasPar.toString();
            if (alias.length() == 0) {
                alias = DEFAULT_ALIAS;
            }
        }

        Hashtable<String, ?> initparams = new Hashtable<String, Object>(config);
        initparams.remove(PARAM_ALIAS);

        try {
            this.httpService.registerServlet(alias, this, initparams, null);
            this.alias = alias;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("activate: Failed registering DavEx Servlet at " + alias, e);
        }
    }

    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate() {
        if (this.alias != null) {
            this.httpService.unregister(alias);
            this.alias = null;
        }
    }

}
