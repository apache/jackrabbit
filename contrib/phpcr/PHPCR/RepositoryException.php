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
 * Main exception thrown by classes in this package. May either contain
 * an error message or another exception wrapped inside this exception.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
class RepositoryException extends Exception
{
    /**
     * Constructs a new instance of this class.
     */
    public function __construct() {
        $args = func_get_args();
        $msg  = null;
        $ex   = null;

        if ( count( $args ) ) {
            if ( is_string( $args[0] ) && isset( $args[1] ) ) {
                if ( $args[1] instanceof Exception ) {
                    $ex = $args[1];
                }
            } else if ( $args[0] instanceof Exception ) {
                $ex = $args[0];
            }

            if ( isset( $ex ) ) {
                $msg .= $ex->getMessage();
            }

            if ( is_string( $args[0] ) ) {
                $msg .= ' (' . $args[0] . ')';
            }

            parent::__construct( $msg );
        }
    }
}

?>