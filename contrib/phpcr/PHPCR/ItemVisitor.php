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


require_once 'PHPCR/Property.php';
require_once 'PHPCR/Node.php';
require_once 'PHPCR/RepositoryException.php';


/**
 * The <code>ItemVisitor</code> defines an interface for the
 * <i>Visitor</i> design pattern (see, for example, <i>Design Patterns</i>,
 * Gamma <i>et al.</i>, 1995).
 * This interface defines two signatures of the
 * <code>visit</code> method; one taking a <code>Node</code>, the other a
 * <code>Property</code>. When an object implementing this interface is passed
 * to <code>Item#accept(ItemVisitor visitor)</code> the appropriate
 * <code>visit</code> method is automatically called, depending on whether the
 * <code>Item</code> in question is a <code>Node</code> or a
 * <code>Property</code>. Different implementations of this interface can be
 * written for different purposes. It is, for example, possible for the
 * <code>visit(Node node)</code> method to call <code>accept</code> on the
 * children of the passed node and thus recurse through the tree performing some
 * operation on each <code>Item</code>.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface ItemVisitor
{
    /**
     * This method is called when the <code>ItemVisitor</code> is
     * passed to the <code>accept</code> method of a <code>Property</code>.
     * If this method throws an exception the visiting process is aborted.
     *
     * @param property The <code>Property</code> or <code>Node</code> that is accepting this visitor.
     *
     * @throws RepositoryException if an error occurrs
     */
    public function visit( $entry );
}

?>