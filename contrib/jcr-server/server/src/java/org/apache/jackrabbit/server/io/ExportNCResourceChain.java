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
package org.apache.jackrabbit.server.io;

import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Catalog;

/**
 * This Class implements a default chain for exporting non collection resources.
 * It adds the following commands:
 * <ul>
 * <li>{@link FileExportCommand}().
 * <li>{@link XMLExportCommand}({@link XMLExportCommand#MODE_DOCVIEW).
 * </ul>
 */
public class ExportNCResourceChain extends ChainBase {

    /**
     * default name of this chain
     */
    public static final String NAME = "export-nc-resource";

    /**
     * Creats a new command chain for exporting non collection resources.
     */
    public ExportNCResourceChain() {
        super();
        addCommand(new FileExportCommand());
        addCommand(new XMLExportCommand(XMLExportCommand.MODE_DOCVIEW));
    }

    /**
     * Returns an export chain. It first tries to lookup the command
     * in the default catalog. If this failes, a new instance of this class
     * is returned.
     * @return an export chain.
     */
    public static Command getChain() {
        Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();
        Command exportChain = catalog.getCommand(NAME);
        if (exportChain == null) {
            // generate default import chain
            exportChain = new ExportNCResourceChain();
        }
        return exportChain;
    }


}
