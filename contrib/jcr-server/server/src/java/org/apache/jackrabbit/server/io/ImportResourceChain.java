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
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.jackrabbit.JcrConstants;

/**
 * This Class implements a default chain for importing non-collection resources.
 * It adds the following commands:
 * <ul>
 * <li>{@link SetContentTypeCommand}().
 * <li>{@link AddNodeCommand}("nt:file").
 * <li>{@link AddMixinCommand}("mix:versionable").
 * <li>{@link FileImportCommand}()
 * </ul>
 */
public class ImportResourceChain extends ChainBase implements JcrConstants {

    /**
     * the name of this chain
     */
    public static final String NAME = "import-resource";

    /**
     * Creates a new default import chain for importing non collection resource.
     */
    public ImportResourceChain() {
        super();
        addCommand(new SetContentTypeCommand());
        addCommand(new AddNodeCommand(NT_FILE));
        addCommand(new AddMixinCommand(MIX_VERSIONABLE));
        addCommand(new FileImportCommand());
    }

    /**
     * Returns an import chain. It first tries to lookup the command
     * in the default catalog. If this failes, a new instance of this class
     * is returned.
     *
     * @return an import chain.
     */
    public static Command getChain() {
        Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();
        Command importChain = catalog.getCommand(NAME);
        if (importChain == null) {
            // generate default import chain
            importChain = new ImportResourceChain();
        }
        return importChain;
    }
}
