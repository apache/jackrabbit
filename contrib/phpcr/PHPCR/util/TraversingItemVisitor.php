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


require_once 'PHPCR/ItemVisitor.php';
require_once 'PHPCR/Property.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/Node.php';


/**
 * An implementaion of <code>ItemVisitor</code>.
 * <p/>
 * <b>Level 1 and 2</b>
 * <p/>
 * <code>TraversingItemVisitor</code> is an abstract utility class
 * which allows to easily traverse an <code>Item</code> hierarchy.
 * <p/>
 * <p><code>TraversingItemVisitor</code> makes use of the Visitor Pattern
 * as described in the book 'Design Patterns' by the Gang Of Four
 * (Gamma et al.).
 * <p/>
 * <p>Tree traversal is done observing the left-to-right order of
 * child <code>Item</code>s if such an order is supported and exists.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage util
 */
abstract class TraversingItemVisitor implements ItemVisitor
{
    /**$currentQueue
     * indicates if traversal should be done in a breadth-first
     * manner rather than depth-first (which is the default)
     *
     * @var bool
     */
    protected $breadthFirst = false;

    /**
     * the 0-based level up to which the hierarchy should be traversed
     * (if it's -1, the hierarchy will be traversed until there are no
     * more children of the current item)
     *
     * @var int
     */
    protected $maxLevel = -1;

    /**
     * queues used to implement breadth-first traversal
     *
     * @var array
     */
    private $currentQueue;
    private $nextQueue;

    /**
     * used to track hierarchy level of item currently being processed
     *
     * @var int
     */
    private $currentLevel;


    /**
     * Constructs a new instance of this class.
     *
     * @param breadthFirst if <code>breadthFirst</code> is true then traversal
     *                     is done in a breadth-first manner; otherwise it is done in a
     *                     depth-first manner (which is the default behaviour).
     */
    public function __construct( $breadthFirst = false, $maxLevel = -1 ) {
        $this->breadthFirst = $breadthFirst;

        if ( $this->breadthFirst === true ) {
            $this->currentQueue = array();
            $this->nextQueue    = array();
        }

        $this->currentLevel = 0;
        $this->maxLevel = $maxLevel;
    }


    /**
     * Implement this method to add behaviour performed before a
     * <code>Property</code> or <code>Node</code> is visited.
     *
     * @param  entry that is accepting this visitor.
     * @param  level    hierarchy level of this property (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract function entering( $entry, $level );

    /**
     * Implement this method to add behaviour performed after a
     * <code>Property</code> is visited.
     *
     * @param  entry the <code>Property</code> that is accepting this visitor.
     * @param  level    hierarchy level of this property (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract function leaving( $entry, $level );

    /**
     * Called when the Visitor is passed to a <code>Node</code>.
     * <p/>
     * It calls <code>TraversingItemVisitor.entering(Node, int)</code> followed by
     * <code>TraversingItemVisitor.leaving(Node, int)</code>. Implement these abstract methods to
     * specify behaviour on 'arrival at' and 'after leaving' the <code>Node</code>.
     * <p/>
     * If this method throws, the visiting process is aborted.
     *
     * @param Either Node or Property that is accepting this visitor.
     * @throws RepositoryException if an error occurrs
     */
    public function visit( $entry ) {
        if ( $entry instanceof Property ) {
            $this->entering( $entry, $this->currentLevel );
            $this->leaving( $entry, $this->currentLevel );
        } else {
            try {
                if ( !$this->breadthFirst ) {
                    // depth-first traversal
                    $this->entering( $entry, $this->currentLevel );

                    if ( $this->maxLevel == -1 || $this->currentLevel < $this->maxLevel ) {
                        $this->currentLevel++;

                        $nodeIter = $entry->getNodes();

                        while ( $nodeIter->hasNext() ) {
                            $nodeIter->nextNode()->accept( $this );
                        }

                        $propIter = $entry->getProperties();

                        while ( $propIter->hasNext()) {
                            $propIter->nextProperty()->accept( $this );
                        }

                        $currentLevel--;
                    }

                    $this->leaving( $entry, $this->currentLevel );
                } else {
                    // breadth-first traversal
                    $this->entering( $entry, $this->currentLevel );
                    $this->leaving( $entry, $this->currentLevel );

                    if ( $this->maxLevel == -1 || $this->currentLevel < $this->maxLevel ) {
                        $nodeIter = $entry->getNodes();

                        while ( $nodeIter->hasNext() ) {
                            $this->nextQueue[] = $nodeIter->nextNode();
                        }

                        $propIter = $entry->getProperties();

                        while ( $propIter->hasNext() ) {
                            $this->nextQueue[] = $propIter->nextProperty();
                        }
                    }

                    while ( !empty( $this->currentQueue ) || !empty( $this->nextQueue ) ) {
                        if ( empty( $this->currentQueue ) ) {
                            $this->currentLevel++;
                            $this->currentQueue = $this->nextQueue;
                            $this->nextQueue = array();
                        }

                        $this->currentQueue = array_shift( $this->currentQueue );
                    }

                    $this->currentLevel = 0;
               }
            } catch ( RepositoryException $re ) {
                $this->currentLevel = 0;
                throw $re;
            }
        }
    }
}

?>