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


require_once 'PHPCR/IllegalArgumentException.php';


/**
 * The property types supported by the JCR standard.
 * <p/>
 * <p>This interface defines following property types:
 * <ul>
 * <li><code>STRING</code>
 * <li><code>BINARY</code>
 * <li><code>LONG</code>
 * <li><code>DOUBLE</code>
 * <li><code>DATE</code>
 * <li><code>BOOLEAN</code>
 * <li><code>NAME</code>
 * <li><code>PATH</code>
 * <li><code>REFERENCE</code>
 * </ul>
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
final class PropertyType
{
    /*
     * The supported property types.
     */
    const STRING    = 1;
    const BINARY    = 2;
    const LONG      = 3;
    const DOUBLE    = 4;
    const DATE      = 5;
    const BOOLEAN   = 6;
    const NAME      = 7;
    const PATH      = 8;
    const REFERENCE = 9;

    /*
     * Undefined type.
     */
    const UNDEFINED = 0;

    /*
     * The names of the supported property types,
     * as used in serialization.
     */
    const TYPENAME_STRING    = "String";
    const TYPENAME_BINARY    = "Binary";
    const TYPENAME_LONG      = "Long";
    const TYPENAME_DOUBLE    = "Double";
    const TYPENAME_DATE      = "Date";
    const TYPENAME_BOOLEAN   = "Boolean";
    const TYPENAME_NAME      = "Name";
    const TYPENAME_PATH      = "Path";
    const TYPENAME_REFERENCE = "Reference";

    const TYPENAME_UNDEFINED = "undefined";


    /**
     * Returns the name of the specified <code>type</code>,
     * as used in serialization.
     * @param  int    $type the property type
     * @return string the name of the specified <code>type</code>
     * @throws IllegalArgumentException if <code>type</code>
     * is not a valid property type.
     */
    public static function nameFromValue( $type ) {
        switch ( $type ) {
            case self::STRING:
                return self::TYPENAME_STRING;

            case self::BINARY:
                return self::TYPENAME_BINARY;

            case self::BOOLEAN:
                return self::TYPENAME_BOOLEAN;

            case self::LONG:
                return self::TYPENAME_LONG;

            case self::DOUBLE:
                return self::TYPENAME_DOUBLE;

            case self::DATE:
                return self::TYPENAME_DATE;

            case self::PATH:
                return self::TYPENAME_PATH;

            case self::NAME:
                return self::TYPENAME_NAME;

            case self::REFERENCE:
                return self::TYPENAME_REFERENCE;

            default:
                throw new IllegalArgumentException( "unknown type: " + $type );
        }
    }

    /**
     * Returns the numeric constant value of the type with the specified name.
     * @param  string $name the name of the property type
     * @return int    the numeric constant value
     * @throws IllegalArgumentException if <code>name</code>
     * is not a valid property type name.
     */
    public static function valueFromName( $name ) {
        if ( $name == self::TYPENAME_STRING ) {
            return self::STRING;
        } else if ( $name == self::TYPENAME_BINARY ) {
            return self::BINARY;
        } else if ( $name == self::TYPENAME_BOOLEAN ) {
            return self::BOOLEAN;
        } else if ( $name == self::TYPENAME_LONG ) {
            return self::LONG;
        } else if ( $name == self::TYPENAME_DOUBLE ) {
            return self::DOUBLE;
        } else if ( $name == self::TYPENAME_DATE ) {
            return self::DATE;
        } else if ( $name == self::TYPENAME_PATH ) {
            return self::PATH;
        } else if ( $name == self::TYPENAME_NAME ) {
            return self::NAME;
        }  else if ( $name == self::TYPENAME_REFERENCE ) {
            return self::REFERENCE;
        } else {
            throw new IllegalArgumentException( "unknown type: " + $name );
        }
    }
}

?>