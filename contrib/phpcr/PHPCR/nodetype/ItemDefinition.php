<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

/**
 * An item definition.
 *
 * @author Markus Nix <mnix@mayflower.de>
 */
interface ItemDefinition
{
    /**
     * Gets the node type that contains the declaration of <i>this</i>
     * <code>ItemDefinition</code>.
     *
     * @return a <code>NodeType</code> object.
     */
    public function getDeclaringNodeType();

    /**
     * Gets the name of the child item. If <code>"*"</code>, this
     * <code>ItemDefinition</code> defines a residual set of child items. That is,
     * it defines the characteristics of all those child items with names apart
     * from the names explicitly used in other child item definitions.
     *
     * @return a <code>String</code> denoting the name or <code>"*"</code>.
     */
    public function getName();

    /**
     * Reports whether the item is to be automatically created when its parent node is created.
     * If <code>true</code>, then this <code>ItemDefinition</code> will necessarily not be a residual
     * set definition but will specify an actual item name (in other words getName() will not
     * return �*�).
     *
     * @return a <code>boolean</code>.
     */
    public function isAutoCreate();

    /**
     * Reports whether the item is mandatory. A mandatory child node is one that,
     * if its parent node exists, must also exist. A mandatory property is one that
     * must have a value. In the case of single-value properties this means that it
     * must exist (since there is no such thing a null value). In the case of
     * multi-value properties this means that the property must exist and must have
     * at least one value (it cannot hold an empty array).
     * <p/>
     * A mandatory item cannot be removed, short of removing its parent.
     * Nor can it be set to the empty array (if it is a multi-value property).
     * <p/>
     * An attempt to save a node that has a mandatory child item without first
     * creating that child item and, if it is a property, giving it a value,
     * will throw a <code>ConstraintViolationException</code> on <code>save</code>.
     *
     * @return a <code>boolean</code>
     */
    public function isMandatory();

    /**
     * Gets the on-parent-version status of the child item. This governs what to do if
     * the parent node of this child item is versioned.
     *
     * @return an <code>int</code>.
     */
    public function getOnParentVersion();

    /**
     * Reports whether the child item is protected. In level 2 implementations, a protected item is one that cannot be removed
     * (except by removing its parent) or modified through the the standard write methods of this API (that is, Item.remove,
     * Node.addNode, Node.setProperty and Property.setValue).
     * <p/>
     * A protected node may be removed or modified (in a level 2 implementation), however, through some
     * mechanism not defined by this specification or as a side-effect of operations other than
     * the standard write methods of the API. For example, in those repositories that support versioning, the
     * <code>Node.checkin</code> method has the side-effect of changing a node's <code>jcr:isCheckedOut</code>
     * property, even though that property is protected.
     * <p/>
     * Note that when a node is protected this means that all its
     * properties are also protected (regardless of their protected setting). The protected status of a property
     * only becomes relevant if its parent node is not protected.
     *
     * @return a <code>boolean</code>.
     */
    public function isProtected();
}

?>
