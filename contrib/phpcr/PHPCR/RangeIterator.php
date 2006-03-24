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
 * Extends <code>Iterator</code> with the <code>skip</code>, <code>getSize</code>
 * and <code>getPos</code> methods.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface RangeIterator extends Iterator
{
    /**
     * Skip a number of elements in the iterator.
     *
     * @param skipNum the non-negative number of elements to skip
     */
    public function skip( $skipNum );

    /**
     * Returns the number of elements in the iterator.
     * If this information is unavailable, returns -1.
     *
     * @return a long
     */
    public function getSize();

    /**
     * Returns the current position within the iterator. The number
     * returned is the 0-based index of the next element in the iterator,
     * i.e. the one that will be returned on the subsequent <code>next</code> call.
     * <p/>
     * Note that this method does not check if there is a next element,
     * i.e. an empty iterator will always return 0.
     *
     * @return a long
     */
    public function getPos();
}

?>