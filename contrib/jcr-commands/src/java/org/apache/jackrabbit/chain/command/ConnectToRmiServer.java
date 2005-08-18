/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.chain.command;

import javax.jcr.Repository;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;

/**
 * Connect to a JCR-RMI server<br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class ConnectToRmiServer implements Command
{
    // ---------------------------- < literals >
    /** url */
    private String url;

    // ---------------------------- < keys >
    /** url key */
    private String urlKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String url = CtxHelper.getAttr(this.url, this.urlKey, ctx);
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository = factory.getRepository(url);
        CtxHelper.setRepository(ctx, repository);
        return false;
    }

    /**
     * @return Returns the url.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url
     *            The url to set.
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return Returns the urlKey.
     */
    public String getUrlKey()
    {
        return urlKey;
    }

    /**
     * @param urlKey
     *            The urlKey to set.
     */
    public void setUrlKey(String urlKey)
    {
        this.urlKey = urlKey;
    }
}
