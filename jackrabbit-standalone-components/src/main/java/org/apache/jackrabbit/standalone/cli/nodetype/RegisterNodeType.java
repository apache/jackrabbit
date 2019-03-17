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
package org.apache.jackrabbit.standalone.cli.nodetype;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Register node types via CND file
 *
 */
public class RegisterNodeType implements Command {

    private static final Log log = LogFactory.getLog(RegisterNodeType.class);

    private final String srcFsPathKey = "srcFsPath";

    /*
     * (non-Javadoc)
     * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
     */
    public boolean execute(Context context) throws Exception {
        String path = (String) context.get(srcFsPathKey);

        // Register the custom node types defined in the CND file
        InputStream is = new FileInputStream(path);

        if (log.isDebugEnabled()) {
              log.debug("Import CND from path " + path);
        } 
        CndImporter.registerNodeTypes(new InputStreamReader(is), CommandHelper.getSession(context));
        return false;
    }

}
