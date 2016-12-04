<?xml version="1.0" encoding="utf-8"?>

<!--
 * Copyright (c) 2002 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de
 * Recherche en Informatique et en Automatique, Keio University). All
 * Rights Reserved. This program is distributed under the W3C's Software
 * Intellectual Property License. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 * See W3C License http://www.w3.org/Consortium/Legal/ for more details.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0" xmlns:g="http://www.w3.org/2001/03/XPath/grammar">
  <xsl:param name="spec1" select="'xquery'"/>
  <xsl:param name="spec2" select="'dummy'"/>
  <xsl:param name="spec3" select="'dummy'"/>

  <xsl:template match="@*|node()[@exposition-only='yes']">
      <!-- empty -->
  </xsl:template>
  
  <xsl:template match="@*|node()[not(@exposition-only='yes')]">
      
        <xsl:if test="self::node()[not(@if) 
                      or contains(@if, $spec1) 
                      or contains(@if, $spec2) 
                      or contains(@if, $spec3)]">
          <!--
              Additional check for StringLiteral token. XQuery 1.0 uses
              a more restricted version than XPath 2.0. JCR XPath must
              use the XPath 2.0 variant.
              See: http://issues.apache.org/jira/browse/JCR-739
          -->
          <xsl:if test="not(ancestor::g:token[@name = 'StringLiteral']) or not(@if)">
            <xsl:copy>
              <xsl:apply-templates select="@*|node()"/>
            </xsl:copy>        
          </xsl:if>
        </xsl:if>
    
  </xsl:template>

</xsl:stylesheet>
