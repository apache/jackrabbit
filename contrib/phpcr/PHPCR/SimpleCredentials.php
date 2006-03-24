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


require_once 'PHPCR/Credentials.php';
require_once 'PHPCR/IllegalArgumentException.php';


/**
 * <code>SimpleCredentials</code> implements the <code>Credentials</code>
 * interface and represents simple user ID/password credentials.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
final class SimpleCredentials implements Credentials
{
    /**
     * @var String
     * @access private
     */
    private $userId;

    /**
     * @var string
     * @access private
     */
    private $password;

    /**
     * @var array
     * @access private
     */
    private $attributes;


    /**
     * Create a new <code>SimpleCredentials</code> object, given a user ID
     * and password.
     * <p/>
     * Note that the given user password is cloned before it is stored
     * in the new <code>SimpleCredentials</code> object. This should
     * avoid the risk of having unnecessary references to password data
     * lying around in memory.
     * <p/>
     *
     * @param userId   the user ID
     * @param password the user's password
     */
    public function __construct( $userId, $password ) {
        $this->userId = $userId;
        $this->password = $password;
    }


    /**
     * Returns the user password.
     * <p/>
     * Note that this method returns a reference to the password.
     * It is the caller's responsibility to zero out the password information
     * after it is no longer needed.
     *
     * @return the password
     */
    public function getPassword() {
        return $this->password;
    }

    /**
     * Returns the user ID.
     *
     * @return String the user ID.
     */
    public function getUserId() {
        return userId;
    }

    /**
     * Stores an attribute in this credentials instance.
     *
     * @param name  a <code>String</code> specifying the name of the attribute
     * @param value the <code>Object</code> to be stored
     */
    public function setAttribute( $name = null, $value = null ) {
        // name cannot be null
        if ( !isset( $name ) ) {
            throw new IllegalArgumentException("name cannot be null");
        }

        // null value is the same as removeAttribute()
        if ( !isset( $value ) ) {
            $this->removeAttribute( $name );
            return;
        }

        $this->attributes[$name] = $value;
    }

    /**
     * Returns the value of the named attribute as an <code>Object</code>,
     * or <code>null</code> if no attribute of the given name exists.
     *
     * @param name a <code>String</code> specifying the name of the attribute
     * @return  an <code>Object</code> containing the value of the attribute,
     * or <code>null</code> if the attribute does not exist
     */
    public function getAttribute( $name ) {
        return $this->attributes[$name];
    }

    /**
     * Removes an attribute from this credentials instance.
     *
     * @param name a <code>String</code> specifying the name of the attribute
     *             to remove
     */
    public function removeAttribute( $name ) {
        unset( $this->attributes[$name] );
    }

    /**
     * Returns the names of the attributes available to this
     * credentials instance. This method returns an empty array
     * if the credentials instance has no attributes available to it.
     * <p/>
     * <b>Level 1 and 2</b>
     * <p/>
     *
     * @return a string array containing the names of the stored attributes
     */
    public function getAttributeNames() {
        return array_keys( $this->attributes );
    }
}

?>