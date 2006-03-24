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
 * The <code>ISO8601</code> utility class provides helper methods
 * to deal with date/time formatting using a specific ISO8601-compliant
 * format (see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601</a>).
 * <p/>
 * Currently we only support the format <code>yyyy-mm-ddThh:mm:ss</code>,
 * which includes the complete date plus hours, minutes, seconds and a decimal
 * fraction of a second. Currently there is no timezone support
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @todo Timezone support
 * @package phpcr
 * @subpackage util
 */
final class ISO8601
{
    /**
     * Parses a ISO8601-compliant date/time string.
     *
     * @param  text the date/time string to be parsed
     * @return <code>date</code>, or <code>null</code> if the input could
     *         not be parsed
     * @return mixed date or false if an parsing error occured
     * @static
     */
    public static function parse( $text ) {
        $year   = substr( $iso,  0, 4 );
        $month  = substr( $iso,  4, 2 );
        $day    = substr( $iso,  6, 2 );
        $hour   = substr( $iso,  9, 2 );
        $minute = substr( $iso, 12, 2 );
        $second = substr( $iso, 15, 2 );

        // correctly parsed? if not, return false
        return $year . $month . $day . 'T' . $hour . ':' . $minute . ':' . $second;
    }

    /**
     * Returns ISO formatted date
     *
     * @access public
     */
    public function format( $val ) {
        // TODO
    }
}

?>