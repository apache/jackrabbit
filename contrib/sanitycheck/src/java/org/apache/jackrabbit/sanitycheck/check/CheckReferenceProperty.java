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
package org.apache.jackrabbit.sanitycheck.check;

import javax.jcr.PropertyType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.NoSuchReferencedNodeInconsistency;

/**
 * This class is responsible of
 * <ul>
 * <li>checking the existence of all the referenced nodes in the workspace PM
 * or the versioning PM</li>
 * <li>TODO: checking that the referenced node is referenceable.
 * </li>
 * </ul>
 */
public class CheckReferenceProperty extends AbstractPropertyCheck
{
    private static Log log = LogFactory.getLog(CheckReferenceProperty.class);

    /**
     * @inheritDoc
     */
    protected void internalExecute(
        NodeState node,
        PropertyState property,
        SanityCheckContext ctx) throws SanityCheckException
    {
        PersistenceManager pm = ctx.getPersistenceManager();
        PersistenceManager vPm = ctx.getVersioningPersistenceManager();

        if (property.getType() == PropertyType.REFERENCE)
        {
            InternalValue[] values = property.getValues();
            for (int i = 0; i < values.length; i++)
            {
                InternalValue value = values[i];
                UUID uuid = (UUID) value.internalValue();
                NodeId id = new NodeId(uuid.toString());
                try
                {
                    // Load referenced node
                    NodeState referencedNode = pm.load(id);

                    // FIXME: It's not possible to check id the state is
                    // referenceable without a NodeTypeRegistry instance.
                    // 
                    // if (!referencedNode.getMixinTypeNames().contains(
                    // Constants.MIX_REFERENCEABLE))
                    // {
                    // // Add inconsistency
                    // NotReferenceableInconsistency inc = new
                    // NotReferenceableInconsistency();
                    // inc.setPersistenceManager(ctx.getPersistenceManager());
                    // inc.setPersistenceManagerName(ctx.getPersistenceManagerName());
                    // inc.setNode(node);
                    // inc.setProperty(property);
                    // inc.setIndex(i);
                    // inc.setReferencedNode(referencedNode);
                    // ctx.addInconsistency(inc);
                    // }
                    //
                } catch (NoSuchItemStateException e)
                {
                    // Try to load it from the versioning PM
                    try
                    {
                        vPm.load(id);
                        log.info("Reference " + node.getUUID() + "/"
                                + property.getName()
                                + " points to a versioning node ("
                                + id.getUUID() + ")");
                    } catch (NoSuchItemStateException ve)
                    {
                        NoSuchReferencedNodeInconsistency inc = new NoSuchReferencedNodeInconsistency();
                        inc.setIndex(i);
                        inc.setPersistenceManager(pm);
                        inc.setPersistenceManagerName(ctx.getPersistenceManagerName());
                        inc.setNode(node);
                        inc.setProperty(property);
                        ctx.addInconsistency(inc);
                    } catch (ItemStateException ise)
                    {
                        throw new SanityCheckException(
                            "An error while running the check.",
                            ise);
                    }
                } catch (ItemStateException e)
                {
                    throw new SanityCheckException(
                        "An error while running the check.",
                        e);
                }
            }
        }
    }
}
