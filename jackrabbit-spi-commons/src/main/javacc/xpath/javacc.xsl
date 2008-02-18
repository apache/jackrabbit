<?xml version="1.0"?>

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

<!-- $Id: javacc.xsl,v 1.8 2004/06/02 13:46:25 sboag Exp $ -->
	
<!-- Use spec=xpath to generate an XQuery grammar rather
     than an XQuery grammar. -->
     
 <!-- ==============  CHANGE LOG: ==============
  $Log: javacc.xsl,v $
  Revision 1.8  2004/06/02 13:46:25  sboag
  Tweak to handle double quotes in strings.

  Revision 1.7  2004/05/31 18:29:58  sboag
  Latest grammar changes, part of last call comments response.  (Sorry for lack
  of fine-grained detail, but CVS has been down for a week.)

  Revision 1.6  2004/01/26 02:04:09  sboag
  Removed some dead code.

  Revision 1.5  2004/01/25 22:16:51  sboag
  Updata language proposal integrated.  Note that the language conditional mechanism has changed a bit.

  Revision 1.4  2003/09/10 17:38:45  sboag
  Items from http://lists.w3.org/Archives/Member/w3c-query-editors/2003Sep/0028.html all addressed.  Some fixes for construction of AST.  Build target for test with formal XQuery test suite.

  Revision 1.4  2003/09/02 03:55:21  sboag
  Make isStep into a global, in order to make it possible for "dot" to be treated as a path expression (though it is now a primary expression).  Basic smoke test passes now, which is not to say I expect deeper testing to pass.

  Revision 1.3  2003/09/01 19:55:55  sboag
  Incremental checkin: basic PathExpressions seem to work (based only on test of foo/baz and foo()).

  Revision 1.2  2003/08/26 15:12:26  sboag
  On Lionel's advice, am removing the src directory and associated bat files.

  Revision 1.3  2003/07/08 16:03:49  sboag
  Add handling of exposition-only attribute.

  Revision 1.2  2003/04/18 19:27:46  sboag
  Fix problem with curly brace state transition for computed element and attribute constructors.

  Revision 1.1  2003/04/07 22:18:01  sboag
  Initial checkin of language build and parser build.

  Revision 1.19  2003/03/20 12:48:04  sboag
  no message

  Revision 1.18  2003/03/05 15:33:00  sboag
  Major changes for new SequenceType, etc.

  Revision 1.17  2003/02/10 23:01:33  sboag
  Implemented new alias-for attribute, which makes a token act as a sort of a
  proxy for another definition.  In the JavaCC transformation, this token acts as
  a true token for the sake of state transitioning.  In the BNF, the token is converted
  to the token it is acting as an alias for.

  Revision 1.16  2003/01/15 04:41:01  sboag
  Major update to redo the structure of the lexical specification in xpath-grammar.xml
  to make maintenence easier.  Also, fixed up the DTD so xpath-grammar.xml now
  validates.

  Revision 1.15  2002/11/29 17:20:57  sboag
  Minor changes for pathx1 parser.

  Revision 1.14  2002/11/06 07:42:25  sboag
  1) I did some work on BNF production numbering.  At least it is consecutive
  now in regards to the defined tokens.

  2) (XQuery only) I added URL Literal to the main list of literals, and added a
  short note that it is defined equivalently to string literal.  URL Literal has to
  exist right now for relatively esoteric purposes for transitioning the lexical
  state (to DEFAULT rather than OPERATOR as StringLiteral does).  It is
  used in DefaultCollationDecl, NamespaceDecl, SubNamespaceDecl, and
  DefaultNamespaceDecl.  To be clear, URL Literal was already in the August
  draft, I just added it to the list of literals in the main doc.

  Revision 1.13  2002/10/22 16:51:08  sboag
  New Grammar Issues List.  New productions:
  OrderBy, ComputedTextConstructor, PositionVar, Castable, As (TypeDecl).
  Removed:
  unordered, SortExpr
  Fixed reserved word bugs with:
  empty, stable
  Other minor "fixes":
  Change precedence of UnaryExpr to be looser binding than UnionExpr
  Change RangeExpr to only allow one "to".

  Revision 1.12  2002/07/28 19:54:13  sboag
  Fixed problems with import, '*', '?', and ',', reported by Jonathan and Dana.

  Revision 1.11  2002/07/18 01:17:39  sboag
  Fixed some bugs.

  Revision 1.10  2002/07/15 07:25:47  sboag
  Bug fixes, added match patterns, and responses to
  Don's email http://lists.w3.org/Archives/Member/w3c-xml-query-wg/2002Jul/0156.html.

  Revision 1.9  2002/06/28 09:02:07  sboag
  Merged Don's latest work with new grammar proposal.  Changes too numerous
  to detail.

  Revision 1.8  2002/03/17 21:31:08  sboag
  Made new grammar element, <next/> for use in primary or prefix expressions,
  to control when the next element is going to be called.  Somewhat experemental.

  Changed javacc stylesheet and bnf stylesheets to handle g:next.

  Fixed bugs with child::text(), by adding text, comment, etc., tokens to after forward
  or reverse axis.  (note: have to do the same to names after @).  This is yet
  another bad hack.

  Fixed bug with @type, by adding At token to lexical stateswitch into QNAME state
  for XQuery.

  Revision 1.7  2002/03/13 15:45:05  sboag
  Don changes (XPathXQuery.xml, introduction.xml, fragment.xml):
  I have attempted to update these files with the latest terminology
   (mainly changing "simple value" to "atomic value" and related changes.)

  Grammar changes:
  Moral equal of Philip Wadler's structural changes of 02/05/2002.
    Make lookahead(2) so that ElementNameOrFunctionCall can be broken up
    without using long tokens.
  Integrated Robie's SequenceType productions.
  Added Add Validate Production.
  Reviewed and tweaked changes against Named Typing proposal.
  Fixed Dana's bug about ContentElementConstructor and
     ContentAttributeConstructor in ElementContent.
  Allow multiple variable binding for some/every.
  Lift restrictions of "." and "..".
  add  XmlComment and XmlProcessingInstruction and also CdataSection to the
  		  Constructor production.
  Remove The Ref and Colon tokens.
  Made multiply & star one token for XQuery, in spite of the fact that this causes
  ambiguity.
  Remove XQUERY_COMMENT state that is never entered.
  Add QNAME lexical state for qnames following explicit axes, i.e. child::div.
  BUG: child::text() will fail in XQuery.
  BUG: Validate does not work so well for XPath.

  Revision 1.6  2002/03/06 12:40:55  sboag
  Tweak to make it possible to have prefix productions with optional suffixes.

  Revision 1.5  2001/12/09 22:07:16  sboag
  Fixed problem with comments from previous checkin.

 	-sb 10/29/01  Make parser productions extensible by an importing stylesheet.
 ==============  END CHANGE LOG ============== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:g="http://www.w3.org/2001/03/XPath/grammar">
  
  <xsl:param name="spec" select="'xquery'"/>
  
  <xsl:strip-space elements="*"/>
  <!-- workaround for Xalan bug. -->
  <xsl:preserve-space elements="g:char"/>
  
  <xsl:output method="text" encoding="iso-8859-1"/>
  
  <xsl:key name="ref" match="g:token|g:production" 
    use="@name"/>

  <xsl:template match="g:*" 
    priority="-2000"/>

  <xsl:template match="g:*[@exposition-only='yes']" 
    priority="+2000"/>
  
  <xsl:template name="parser">
    PARSER_BEGIN(XPath)
    
    public class XPath {
    boolean isStep = false;
    public static void main(String args[]) throws ParseException {
    XPath parser = new XPath(System.in);
    parser.Input();
    }
    }
    
    PARSER_END(XPath)
  </xsl:template>

<!-- Action templates for overrides from derived stylesheets -->

<xsl:template name="action-production">
</xsl:template>

<xsl:template name="action-production-end">
</xsl:template>

<xsl:template name="action-exprProduction">
</xsl:template>

<xsl:template name="action-exprProduction-label">
</xsl:template>

<xsl:template name="action-exprProduction-end">
</xsl:template>

<xsl:template name="action-level">
</xsl:template>

<xsl:template name="action-level-jjtree-label"></xsl:template>
<xsl:template name="binary-action-level-jjtree-label"></xsl:template>

<xsl:template name="action-level-start">
</xsl:template>

<xsl:template name="action-level-end">
</xsl:template>

<xsl:template name="action-token-ref">
</xsl:template>

<!-- Begin LV -->
<xsl:template name="user-action-ref-start">
</xsl:template>

<xsl:template name="user-action-ref-end">
</xsl:template>
<!-- End LV -->

<xsl:template name="javacc-options">
  STATIC = false;
  LOOKAHEAD = 1;
</xsl:template>

<xsl:template name="input">
	void Input() :
	{}
	{
	  <xsl:value-of select="g:start/@name"/>()&lt;EOF&gt;
	}
</xsl:template>

<xsl:template match="g:grammar">options {
<xsl:call-template name="javacc-options"/>
}

<xsl:call-template name="parser"/>

TOKEN_MGR_DECLS : {
  private Stack stateStack = new Stack();
  // private Vector persistentLexStates  = new Vector();
  static final int PARENMARKER = 2000;
  
  /**
   * Push the current state onto the state stack.
   */
  private void pushState()
  {
    // System.err.println("pushing: "+curLexState); printLinePos();
    stateStack.addElement(new Integer(curLexState));
  }
  
  /**
   * Push the given state onto the state stack.
   * @param state Must be a valid state.
   */
  private void pushState(int state)
  {
    stateStack.push(new Integer(state));
  }
  
  /**
   * Pop the state on the state stack, and switch to that state.
   */
  private void popState()
  {
    if (stateStack.size() == 0)
    {
      printLinePos();
    }

    int nextState = ((Integer) stateStack.pop()).intValue();
    // System.err.println("pop "+nextState); printLinePos();
    if(nextState == PARENMARKER)
      printLinePos();
    SwitchTo(nextState);
  }
  
  /**
   * Push the given state onto the state stack.
   * @param state Must be a valid state.
   */
  private boolean isState(int state)
  {
	for (int i = 0; i &lt; stateStack.size(); i++) {
        if(((Integer) stateStack.elementAt(i)).intValue() == state)
        {
        	return true;
        }
	}
	return false;
  }

  /**
   * Push a parenthesis state.  This pushes, in addition to the 
   * lexical state value, a special marker that lets 
   * resetParenStateOrSwitch(int state)
   * know if it should pop and switch.  Used for the comma operator.
   */
  private void pushParenState(int commaState, int rparState)
  {
    stateStack.push(new Integer(rparState));
    stateStack.push(new Integer(commaState));
    stateStack.push(new Integer(PARENMARKER));
    SwitchTo(commaState);
  }


  /**
   * Print the current line position.
   */
  public void printLinePos()
  {
    System.err.println("Line: " + input_stream.getEndLine());
  }
}

<xsl:call-template name="input"/>

<xsl:apply-templates select="*"/>
</xsl:template>

<xsl:template match="g:state-list"/>

<!--  END SB CHANGE: Make parser productions extensible by an importing stylesheet -->


<xsl:template match="g:token|g:special">
    
  <xsl:variable name="tname" select="@name"/>
  <xsl:variable name="otoken" select="."/>
  <xsl:variable name="trefs"
    select="/g:grammar/g:state-list//g:tref[@name=$tname]"/>
  
  <!-- At this time, flag an error if the token is not defined in the
       transition tables! -->
  <xsl:if test="not($trefs)">
    <xsl:message terminate="yes">
      <xsl:text>Transition not defined: </xsl:text>
      <xsl:value-of select="@name"/>
    </xsl:message>
  </xsl:if>
  
  <xsl:variable name="token" select="key('ref', @alias-for)|self::*[not(@alias-for)]"/>
  
  <xsl:for-each select="$token">
    <xsl:for-each select="$trefs">
      <!-- Assert that this token occurs only once in this state! -->
      <xsl:variable name="state" select="ancestor::g:state"/>
      <xsl:if test="count($state//g:tref[@name=$tname]) > 1">
        <xsl:message terminate="yes">
          <xsl:text>Token multiply defined in same state: </xsl:text>
          <xsl:value-of select="$tname"/>
        </xsl:message>
      </xsl:if>
      <!-- JavaCC won't let us define a token per state, so test if this is 
           the first tref, then process the rest of the trefs to make a 
           comma delimited list per token. -->
      <xsl:variable name="allTRefsOfName"
        select="/g:grammar/g:state-list/g:state//g:tref[@name=$tname]"/>
      <xsl:variable name="isFirstOccurance"
        select="$allTRefsOfName[(position() = 1) 
                and (ancestor::g:state/@name = $state/@name)]"/>
      <xsl:if test="$isFirstOccurance">
        <xsl:text>&#10;</xsl:text>
        <xsl:if test="not($state/@name='ANY')">
          <xsl:text>&lt;</xsl:text>
          <xsl:for-each select="$allTRefsOfName">
            <xsl:value-of select="ancestor::g:state/@name"/>
            <xsl:if test="last()!=position()">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:for-each>
          
          <xsl:text>&gt;&#10;</xsl:text>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="($token/@more='yes') or $otoken/@more='yes'">
            <xsl:text>MORE </xsl:text>
          </xsl:when>
          <xsl:when test="($token/@special='yes') or $otoken/@special='yes'">
            <xsl:text>SPECIAL_TOKEN </xsl:text>
          </xsl:when>
          <xsl:when test="$token/@skip='yes' or $otoken/@skip='yes'">
            <xsl:text>SKIP </xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>TOKEN </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        
        <xsl:text>:&#10;{&#10; &lt; </xsl:text>
        <xsl:if test="$state/@name='ANY'">#</xsl:if>
        
        <xsl:value-of select="$tname"/>
        <xsl:text> : </xsl:text>
        <xsl:for-each select="$token">
          <xsl:call-template name="space"/>
        </xsl:for-each>
        <xsl:text> &gt;</xsl:text>
        <xsl:variable name="action" select="$trefs/ancestor::g:transition/@action"/>
        <!-- assert correctness -->
        <xsl:if test="$action and count($action) &gt; 1">
          <xsl:for-each select="$trefs[ancestor::g:transition/@action]">
            <xsl:if test="ancestor::g:transition/@action != $action[1]">
              <xsl:message terminate="yes">
                <xsl:text>Error! Actions of transitions are not consistent! </xsl:text>
                <xsl:value-of select="$tname"/>
              </xsl:message>
            </xsl:if>
          </xsl:for-each>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="contains($action, '(')">
            <xsl:text> { </xsl:text>
            <xsl:value-of select="$action"/>
            <xsl:text>; }</xsl:text>
          </xsl:when>  
          <xsl:when test="normalize-space($action) = 'input_stream.backup'">
            <xsl:message>$action(2): <xsl:value-of select="$action"/></xsl:message>
            <xsl:text> { </xsl:text>
            <!-- xsl:text>System.out.println("lengthOfMatch: "+lengthOfMatch);</xsl:text -->
            <xsl:value-of select="$action"/>
            <xsl:text>(</xsl:text>
            <xsl:text>lengthOfMatch</xsl:text>
            <xsl:text>)</xsl:text>
            <xsl:text>; }</xsl:text>
          </xsl:when>  
          <xsl:when test="$action">
            <xsl:text> { </xsl:text>
            <xsl:value-of select="$action"/>
            <xsl:text>(); }</xsl:text>
          </xsl:when>
        </xsl:choose>
        
        <xsl:variable name="nextLexState" 
          select="$trefs/ancestor::g:transition/@next-state"/>
        
        <!-- assert correctness -->
        <xsl:if test="$nextLexState and count($nextLexState) &gt; 1">
          <xsl:for-each select="$trefs[ancestor::g:transition/@next-state]">
            <xsl:if test="ancestor::g:transition/@next-state != $nextLexState[1]">
              <xsl:message terminate="yes">
                <xsl:text>Error! Next-state of transitions are not consistent! </xsl:text>
                <xsl:value-of select="$tname"/>
              </xsl:message>
            </xsl:if>
          </xsl:for-each>
        </xsl:if>
        
        <xsl:if test="$nextLexState">
          <xsl:text> : </xsl:text>
          <xsl:value-of select="$nextLexState"/>
        </xsl:if>
        <xsl:text>&#10;}&#10;</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:for-each>
</xsl:template>

<xsl:template match="g:skip">
    <xsl:if test="@recognize">
	    <xsl:text>&lt;</xsl:text>
	    <xsl:call-template name="replace-char">
	      <xsl:with-param name="string" select="@recognize"/>
	      <xsl:with-param name="from" select="' '"/>
	      <xsl:with-param name="to" select="', '"/>
	    </xsl:call-template>
	    <xsl:text>&gt;</xsl:text>
    </xsl:if>
SKIP:
{
  &lt; &lt;skip_&gt;&gt;
}

TOKEN :
{
  &lt; #skip_ : <xsl:call-template name="space"/> &gt;
}

</xsl:template>

<xsl:template match="g:char|g:string">
  <xsl:if test="@complement='yes'">
    <xsl:text>~</xsl:text>
  </xsl:if>
  <xsl:text>"</xsl:text>
  <xsl:call-template name="replace-char">
    <xsl:with-param name="string" select="."/>
    <xsl:with-param name="from" select="'&quot;'"/>
    <xsl:with-param name="to" select="'\&quot;'"/>
  </xsl:call-template>
  <xsl:text>"</xsl:text>
</xsl:template>

<!-- For some reason, the JavaCC generated lexer produces a lexical error for a "]" if
we use ]]> as a single token. -->
<xsl:template match="g:string[.=']]&gt;']">
 <xsl:text>("]" "]" ">")</xsl:text>
</xsl:template>

<xsl:template match="g:string[@ignoreCase]">
  <xsl:text>(</xsl:text>
  <xsl:call-template name="ignore-case">
    <xsl:with-param name="string" select="."/>
  </xsl:call-template>
  <xsl:text>)</xsl:text>
</xsl:template>

<xsl:variable name="upper" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
<xsl:variable name="lower" select="'abcdefghijklmnopqrstuvwxyz'"/>

<xsl:template name="ignore-case">
  <xsl:param name="string" select="''"/>
  <xsl:if test="$string">
    <xsl:variable name="c" select="substring($string,1,1)"/>
    <xsl:variable name="uc" select="translate($c,$lower,$upper)"/>
    <xsl:variable name="lc" select="translate($c,$upper,$lower)"/>
    <xsl:choose>
      <xsl:when test="$lc=$uc">
        <xsl:text>"</xsl:text>
        <xsl:value-of select="$c"/>
        <xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>
	<xsl:text>["</xsl:text>
	<xsl:value-of select="$uc"/>
	<xsl:text>", "</xsl:text>
	<xsl:value-of select="$lc"/>
	<xsl:text>"]</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="substring($string,2)">
      <xsl:text xml:space="preserve"> </xsl:text>
    </xsl:if>
    <xsl:call-template name="ignore-case">
      <xsl:with-param name="string" select="substring($string,2)"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template match="g:eof">&lt;EOF&gt;</xsl:template>

<xsl:template match="g:charCode">
  <xsl:text>"</xsl:text>
  <xsl:choose>
    <xsl:when test="@value='000A'">
      <xsl:text>\n</xsl:text>
    </xsl:when>
    <xsl:when test="@value='000D'">
      <xsl:text>\r</xsl:text>
    </xsl:when>
    <xsl:when test="@value='0009'">
      <xsl:text>\t</xsl:text>
    </xsl:when>
    <xsl:when test="@value='0020'">
      <xsl:text xml:space="preserve"> </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>\u</xsl:text>
      <xsl:value-of select="@value"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>"</xsl:text>
</xsl:template>

<xsl:template match="g:charClass">
 <xsl:text>[</xsl:text>
  <xsl:for-each select="*">
    <xsl:if test="position()!=1">
      <xsl:text>, </xsl:text>
    </xsl:if>
    <xsl:apply-templates select="."/>
  </xsl:for-each>
 <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="g:charRange">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@minChar"/>
  <xsl:text>" - "</xsl:text>
  <xsl:value-of select="@maxChar"/>
  <xsl:text>"</xsl:text>
</xsl:template>

<xsl:template match="g:charCodeRange">
  <xsl:text>"\u</xsl:text>
  <xsl:value-of select="@minValue"/>
  <xsl:text>" - "\u</xsl:text>
  <xsl:value-of select="@maxValue"/>
  <xsl:text>"</xsl:text>
</xsl:template>

<xsl:template match="g:complement">~<xsl:apply-templates/></xsl:template>

<xsl:template match="g:production">
  <xsl:text>void </xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>() </xsl:text>
  <xsl:choose>
    <xsl:when test="@is-binary='yes'">
      <xsl:call-template name="action-level-jjtree-label"/>
    </xsl:when>
    <xsl:when test="@node-type">
      <xsl:call-template name="action-level-jjtree-label">
        <xsl:with-param name="label" select="@node-type"/>
        <xsl:with-param name="condition" select="@condition"/>
      </xsl:call-template>
    </xsl:when>
    <!-- Begin LV -->
    <xsl:when test="@condition">    
      <xsl:call-template name="action-level-jjtree-label">
	    <xsl:with-param name="label" select="@name"/>
        <xsl:with-param name="condition" select="@condition"/>
      </xsl:call-template>
   	</xsl:when>
     <!-- End LV -->
    <xsl:otherwise>
    </xsl:otherwise>
  </xsl:choose>
<xsl:text> :</xsl:text>
{<xsl:call-template name="action-production"/>}
{
  <xsl:call-template name="space"/>
  <xsl:call-template name="action-production-end"/>
}

</xsl:template>

<xsl:template match="g:exprProduction">
<xsl:variable name="name"
              select="@name"/>void <xsl:value-of select="$name"/>() <xsl:call-template name="action-exprProduction-label"/> :
{<xsl:call-template name="action-exprProduction"/>}
{
	<xsl:variable name="levels" select="g:level[*]"/>
	<xsl:variable name="nextProd" select="concat($levels[1]/*/@name,'()')"/>
	<xsl:value-of select="$nextProd"/>
	<xsl:call-template name="action-exprProduction-end"/>
}

<xsl:for-each select="g:level[*]">
  <!-- xsl:variable name="thisProd" select="concat($name,'_',position(),'()')"/>
  <xsl:variable name="nextProd" select="concat($name,'_',position()+1,'()')"
/ -->
<xsl:variable name="thisProd" select="concat(*/@name,'()')"/>
<xsl:variable name="levels" select="../g:level[*]"/>
<xsl:variable name="position" select="position()"/>

<xsl:variable name="nextProd" select="concat($levels[$position+1]/*/@name,'()')"
/>void <xsl:value-of select="$thisProd"/>
       <xsl:call-template name="action-level-jjtree-label">
        <xsl:with-param name="label">
          <xsl:choose>
            <xsl:when test="@node-type">
              <xsl:value-of select="@node-type"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:choose>
                <!-- <xsl:when test="not(g:binary) and */g:sequence"> SMPG -->
                <xsl:when test="g:binary or */g:sequence">
                  <xsl:value-of select="*/@name"/>
                  <!-- xsl:text>void</xsl:text -->
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>void</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:with-param>

				<!-- Begin SMPG -->
        <xsl:with-param name="condition">
					<xsl:choose>
						<xsl:when test="g:binary or */g:sequence or */g:choice">
							<xsl:value-of select="*/@condition"/>
						</xsl:when>
						<xsl:otherwise/>
					</xsl:choose>
        </xsl:with-param>
				<!-- End SMPG -->

		   <xsl:with-param name="thisProd" select="$thisProd"/>
		   <xsl:with-param name="nextProd" select="$nextProd"/>
     </xsl:call-template> :
{<xsl:call-template name="action-level">
		   <xsl:with-param name="thisProd" select="$thisProd"/>
		   <xsl:with-param name="nextProd" select="$nextProd"/>
 </xsl:call-template>}
{
  <xsl:call-template name="action-level-start"/>
 <xsl:choose>
    <xsl:when test="g:binary and g:postfix">
      <xsl:value-of select="$nextProd"/>
      <xsl:text> ((</xsl:text>
      
      <xsl:call-template name="outputChoices">
        <xsl:with-param name="choices"
                        select="g:binary"/>
        <xsl:with-param name="lookahead" select="ancestor-or-self::*/@lookahead"/>
      </xsl:call-template>
      <xsl:text xml:space="preserve"> </xsl:text>
      <xsl:value-of select="$nextProd"/>
      <!-- xsl:value-of select="concat($name,'_1')"/><xsl:text>()</xsl:text -->
      
      <xsl:call-template name="binary-action-level-jjtree-label">
      		<xsl:with-param name="label" select="*/@name"/>
          <xsl:with-param name="which" select="1"/>
      </xsl:call-template>
      
      <xsl:text>) | </xsl:text>
      
      <xsl:call-template name="outputChoices">
        <xsl:with-param name="choices"
                        select="g:postfix"/>
        <xsl:with-param name="lookahead" select="ancestor-or-self::*/@lookahead"/>
      </xsl:call-template>
      <xsl:text>)*</xsl:text>
    </xsl:when>
    
    <xsl:when test="g:binary">
      <xsl:value-of select="$nextProd"/>
      <xsl:text> (</xsl:text>
      <xsl:call-template name="outputChoices">
         <xsl:with-param name="choices"
                        select="g:binary"/>
         <xsl:with-param name="lookahead" select="ancestor-or-self::*/@lookahead"/>
      </xsl:call-template>
            
      <xsl:text xml:space="preserve"> </xsl:text>
      
      <xsl:value-of select="$nextProd"/>
      <xsl:call-template name="binary-action-level-jjtree-label">
      		<xsl:with-param name="label" select="*/@name"/>
          <xsl:with-param name="which" select="2"/>
      </xsl:call-template>
      <!-- xsl:value-of select="concat($name,'_1')"/><xsl:text>()</xsl:text -->
      
      <!--
      <xsl:variable name="thisName" select="g:binary/@name"/>
      <xsl:text>(</xsl:text>
      <xsl:for-each select="../g:level[*]">
      		<xsl:variable name="theExprName" select="concat($name,'_',position(),'()')"/>
      		<xsl:if test="not(*/@name = $thisName)">
	      		<xsl:value-of select="$theExprName"/>
	      		<xsl:if test="not(last()=position())"> | </xsl:if>
      		</xsl:if>
      </xsl:for-each>
      <xsl:text>)</xsl:text>
      -->
      
      <xsl:text>)</xsl:text>
      <xsl:choose>
        <xsl:when test="g:binary/@prefix-seq-type">
          <xsl:value-of select="g:binary/@prefix-seq-type"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>*</xsl:text>
        </xsl:otherwise>
      </xsl:choose>

    </xsl:when>
    <xsl:when test="g:postfix">
      <xsl:if test="following-sibling::g:level">
      	<xsl:value-of select="$nextProd"/>
      </xsl:if> 
      <xsl:text xml:space="preserve"> </xsl:text>
      <xsl:call-template name="outputChoices">
        <xsl:with-param name="choices"
                        select="g:postfix"/>
      </xsl:call-template>
      <xsl:choose>
        <xsl:when test="g:postfix/@prefix-seq-type">
          <xsl:value-of select="g:postfix/@prefix-seq-type"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>*</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      
    </xsl:when>
    <xsl:when test="g:prefix">
      <xsl:choose>
        <xsl:when test="g:prefix/@suffix-optional='yes'">
          <xsl:text>(</xsl:text>
          <xsl:call-template name="outputChoices">
            <xsl:with-param name="choices"
              select="g:prefix"/>
          </xsl:call-template>
          <xsl:text>(</xsl:text>
          <xsl:value-of select="$nextProd"/>
          <xsl:text>)</xsl:text>
          <xsl:text>? </xsl:text>
          <xsl:value-of select="$nextProd"/>
          <xsl:text>)</xsl:text>
          
          <xsl:text> | </xsl:text>
          <xsl:if test="not(g:prefix//g:next)">
            <xsl:value-of select="$nextProd"/>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="outputChoices">
            <xsl:with-param name="choices"
              select="g:prefix"/>
          </xsl:call-template>
          <xsl:choose>
            <xsl:when test="g:prefix/@prefix-seq-type">
              <xsl:value-of select="g:prefix/@prefix-seq-type"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>*</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text> </xsl:text>
          <xsl:if test="not(g:prefix//g:next)">
            <xsl:value-of select="$nextProd"/>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="g:primary">
      <xsl:call-template name="outputChoices">
        <xsl:with-param name="choices"
                        select="g:primary"/>
      </xsl:call-template>
      <xsl:if test="g:primary/following-sibling::g:level | following-sibling::g:level">
        <xsl:text> | </xsl:text>
      	<xsl:value-of select="$nextProd"/>
      </xsl:if> 
    </xsl:when>
    <xsl:otherwise>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="action-level-end"/>
}

</xsl:for-each>
</xsl:template>

<xsl:template name="outputChoices">
  <xsl:param name="choices" select="/.."/>
  <xsl:param name="lookahead" select="ancestor-or-self::*/@lookahead"/>
  
  <xsl:if test="count($choices)>1">(</xsl:if>
  <xsl:for-each select="$choices">
    <xsl:if test="position()!=1"> | </xsl:if>
    <xsl:if test="count(*)>1">(</xsl:if>
    <xsl:for-each select="*">
      <xsl:if test="position()!=1" xml:space="preserve">  </xsl:if>
      <xsl:apply-templates select="."><xsl:with-param name="lookahead" select="$lookahead"/></xsl:apply-templates>
    </xsl:for-each>
    <xsl:if test="count(*)>1">)</xsl:if>
  </xsl:for-each>
  <xsl:if test="count($choices)>1">)</xsl:if>
</xsl:template>

<xsl:template match="g:optional">[<xsl:call-template name="lookahead"/>
<xsl:call-template name="space"/>]</xsl:template>

<xsl:template match="g:token//g:optional">(<xsl:call-template name="space"/>)?</xsl:template>
<xsl:template match="g:zeroOrMore">(<xsl:call-template name="lookahead"/>
<xsl:call-template name="space"/>)*</xsl:template>

<xsl:template match="g:oneOrMore">(<xsl:call-template name="lookahead"/>
<xsl:call-template name="space"/>)+ </xsl:template>

<xsl:template match="g:sequence">(<xsl:call-template name="lookahead"/>
<xsl:call-template name="space"/>)</xsl:template>

<xsl:template match="g:ref[@notational-only='yes']">
</xsl:template>

<xsl:template match="g:ref">
  <xsl:choose>
    <xsl:when test="key('ref',@name)/self::g:token">
      <xsl:text>&lt;</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>&gt;</xsl:text>
      <!-- show when a token match is a success.  -sb -->
	  <xsl:if test="ancestor::g:level | ancestor::g:production">
	 	  	<xsl:call-template name="action-token-ref"/>
	 	  	<xsl:if test="ancestor::g:binary">
	 	  	      <!-- xsl:value-of select="$nextProd"/ -->
	 	  	      
	 	  	      <!-- Awww... it's not so bad... -sb -->
	 	  	      <xsl:if test="false()">
				<xsl:variable name="levels" select="ancestor::g:exprProduction/g:level[*]"/>
				<xsl:variable name="position" select="count(ancestor::g:level/preceding-sibling::g:level[*])+1"/>
				<xsl:variable name="nextProd" select="concat($levels[$position+1]/*/@name,'()')"/>
	 	  	      
	 	  	      <!-- xsl:variable name="nextProd" select="concat(ancestor::g:exprProduction/@name, 
	 	  	                                                  '_',  count(ancestor::g:level/preceding-sibling::g:level[*])+2,'()')"/ -->

			      <!-- xsl:value-of select="concat(ancestor::g:exprProduction/@name,'_1')"/ -->
			      <!-- xsl:text>()</xsl:text -->
			      <xsl:value-of select="$nextProd"/>
			      
			      <xsl:call-template name="binary-action-level-jjtree-label">
              <xsl:with-param name="which" select="3"/>
            </xsl:call-template>
			      <xsl:text>
</xsl:text>
				</xsl:if>
	 	  	</xsl:if>
	  </xsl:if>
    </xsl:when>
    <xsl:otherwise>
    	<!-- Begin LV -->
      <xsl:call-template name="user-action-ref-start"/>
      <!-- End LV -->
    
      <xsl:value-of select="@name"/>
      <xsl:text>()</xsl:text>
      
      <!-- Begin LV -->
      <xsl:call-template name="user-action-ref-end"/>
      <!-- End LV -->
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="g:next">
  <!-- The assumption is this we're in a exprProduction, 
       in a prefix, primary, etc., and want to call the next level. -->
  <xsl:variable name="levels" select="ancestor::g:exprProduction[1]/g:level[*]"/>
  <xsl:variable name="position">
    <xsl:variable name="uniqueIdOfThisLevel" select="generate-id(ancestor::g:level[1])"/>
    <xsl:for-each select="$levels">
      <xsl:if test="generate-id(.) = $uniqueIdOfThisLevel">
        <xsl:value-of select="position()"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="nextProd" select="concat($levels[$position+1]/*/@name,'()')"/>
  <xsl:value-of select="$nextProd"/>
  <!-- xsl:text>()</xsl:text -->
</xsl:template>

<xsl:template match="g:choice">
  <xsl:variable name="lookahead" select="ancestor-or-self::*/@lookahead"/>
  <xsl:if test="$lookahead > 0">LOOKAHEAD(<xsl:value-of select="$lookahead"/>) </xsl:if>
  <xsl:text>(</xsl:text>
  <xsl:for-each select="*[not(@notational-only='yes')]">
    <xsl:if test="position()!=1">
      <xsl:text> | </xsl:text>
    </xsl:if>
    <xsl:apply-templates select=".">
    	<xsl:with-param name="lookahead" select="$lookahead"/>
    </xsl:apply-templates>
    
  </xsl:for-each>
  <xsl:text>)</xsl:text>
</xsl:template>

<!-- xsl:template match="g:choice" mode="binary">
  <xsl:param name="name"/>
  <xsl:text>(</xsl:text>
  <xsl:for-each select="*">
    <xsl:if test="position()!=1">
      <xsl:text> | </xsl:text>
    </xsl:if>
    <xsl:apply-templates select="."/>
    <xsl:value-of select="$name"/><xsl:text>()</xsl:text>
  </xsl:for-each>
  <xsl:text>)</xsl:text>
</xsl:template -->

<xsl:template match="g:requiredSkip">
  <xsl:text>(&lt;skip_&gt;)+</xsl:text>
</xsl:template>

<xsl:template match="g:optionalSkip">
  <xsl:text>(&lt;skip_&gt;)*</xsl:text>
</xsl:template>

<xsl:template name="lookahead" match="lookahead">
  <xsl:if test="@lookahead">
    <xsl:text>LOOKAHEAD(</xsl:text>
    <xsl:value-of select="@lookahead"/>
    <xsl:text>) </xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template name="space">
  <xsl:for-each select="*">
    <xsl:if test="position()!=1">
      <xsl:text> </xsl:text>
    </xsl:if>
    <xsl:apply-templates select="."/>
  </xsl:for-each>
</xsl:template>

<xsl:template name="replace-char">
  <xsl:param name="from" select="''"/>
  <xsl:param name="to" select="''"/>
  <xsl:param name="string" select="''"/>
  <xsl:if test="$string">
    <xsl:choose>
        <xsl:when test="substring($string,1,1)=$from">
          <!-- Added this to avoid empty commas in sequences of
               spaces. -sb -->
          <xsl:if test="not(substring($string,2,1)=$from and $from=' ')">
            <xsl:value-of select="$to"/>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="substring($string,1,1)"/>
        </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="replace-char">
      <xsl:with-param name="string" select="substring($string, 2)"/>
      <xsl:with-param name="to" select="$to"/>
      <xsl:with-param name="from" select="$from"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>