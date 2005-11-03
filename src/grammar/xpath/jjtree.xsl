<?xml version="1.0" encoding="UTF-8"?>

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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
                                                xmlns:g="http://www.w3.org/2001/03/XPath/grammar">
	<xsl:import href="javacc.xsl"/>
	
	<xsl:template name="javacc-options">
	  <!-- xsl:apply-imports/ -->
          STATIC = false;
	   MULTI=false;
	   VISITOR=true ;     // invokes the JJTree Visitor support
	   NODE_SCOPE_HOOK=false;
	   NODE_USES_PARSER=true;
	</xsl:template>
	
	<xsl:template name="input">
		SimpleNode <xsl:choose>
			<xsl:when test="$spec='xquery'">XPath2</xsl:when>
			<xsl:when test="$spec='xpath'">XPath2</xsl:when>
			<xsl:when test="$spec='pathx1'">XPath2</xsl:when>
      <xsl:otherwise>XPath2</xsl:otherwise>
		</xsl:choose>() :
		{}
		{
		  <xsl:value-of select="g:start/@name"/>()&lt;EOF&gt;
		  { return jjtThis ; }
		}
      <xsl:if test="$spec='xpath' or $spec='pathx1'">
		SimpleNode <xsl:choose>
			<xsl:when test="$spec='xpath'">MatchPattern</xsl:when>
			<xsl:when test="$spec='pathx1'">MatchPattern</xsl:when>
      <xsl:otherwise>MatchPattern</xsl:otherwise>
		</xsl:choose>() :
		{m_isMatchPattern = true;}
		{
		  <xsl:value-of select="g:start[contains(@if,'xslt-patterns')]/@name"/>()&lt;EOF&gt;
		  { return jjtThis ; }
		}
    </xsl:if>
	</xsl:template>

  <xsl:template name="extra-parser-code"/>
	
	<xsl:template name="parser">
    <xsl:variable name="parser-class">
      <xsl:choose>
			<xsl:when test="$spec='xpath'">XPath</xsl:when>
			<xsl:when test="$spec='pathx1'">XPath</xsl:when>
      <xsl:otherwise>XPath</xsl:otherwise>
    </xsl:choose>
    </xsl:variable>
    PARSER_BEGIN(<xsl:value-of select="$parser-class"/>)

<xsl:call-template name="set-parser-package"/>
import java.io.*;		
import java.util.Stack;
import java.util.Vector;

public class <xsl:value-of select="$parser-class"/> {
      <xsl:call-template name="extra-parser-code"/>

      boolean m_isMatchPattern = false;
      boolean isStep = false;

		  Stack binaryTokenStack = new Stack();
		  
		  public Node createNode(int id) {
			  return null;
		  }
		  
		  
		  
		  public static void main(String args[])
		     throws Exception
		  {
         int numberArgsLeft = args.length;
         int argsStart = 0;
         boolean isMatchParser = false;
         if(numberArgsLeft > 0)
         {
           if(args[argsStart].equals("-match"))
           {
             isMatchParser = true;
             System.out.println("Match Pattern Parser");
             argsStart++;
             numberArgsLeft--;
           }
         }
		     if(numberArgsLeft > 0)
		    {
			try
			{
        final boolean dumpTree = true;
        if(args[0].endsWith(".xquery"))
        {
          System.out.println("Running test for: "+args[0]);
          File file = new File(args[0]);
          FileInputStream fis = new FileInputStream(file);
          XPath parser = new XPath(fis);
          SimpleNode tree = parser.XPath2();
          if(dumpTree)
            tree.dump("|") ;
        }
        else
        {
				for(int i = argsStart; i &lt; args.length; i++)
				{
					System.out.println();
					System.out.println("Test["+i+"]: "+args[i]);
					XPath parser = new XPath(new java.io.StringBufferInputStream(args[i]));
          SimpleNode tree;
          if(isMatchParser)
          {
					tree = parser.<xsl:choose>
						<xsl:when test="$spec='xquery'">XPath2</xsl:when>
						<xsl:when test="$spec='xpath'">MatchPattern</xsl:when>
						<xsl:when test="$spec='pathx1'">MatchPattern</xsl:when>
            <xsl:otherwise>XPath2</xsl:otherwise>
					</xsl:choose>();
          }
          else
          {
					tree = parser.<xsl:choose>
						<xsl:when test="$spec='xquery'">XPath2</xsl:when>
						<xsl:when test="$spec='xpath'">XPath2</xsl:when>
						<xsl:when test="$spec='pathx1'">XPath2</xsl:when>
            <xsl:otherwise>XPath2</xsl:otherwise>
					</xsl:choose>();
          }
					((SimpleNode)tree.jjtGetChild(0)).dump("|") ;
				}
				System.out.println("Success!!!!");
        }
			}
			catch(ParseException pe)
			{
				System.err.println(pe.getMessage());
			}
			return;
		   }
		    java.io.DataInputStream dinput = new java.io.DataInputStream(System.in);
		    while(true)
		    {
			  try
			  {
			      System.err.println("Type Expression: ");
			      String input =  dinput.readLine(); 
			      if(null == input || input.trim().length() == 0)
			        break;  
			      XPath parser = new XPath(new java.io.StringBufferInputStream(input));
          SimpleNode tree;
          if(isMatchParser)
          {
					tree = parser.<xsl:choose>
						<xsl:when test="$spec='xquery'">XPath2</xsl:when>
						<xsl:when test="$spec='xpath'">MatchPattern</xsl:when>
						<xsl:when test="$spec='pathx1'">MatchPattern</xsl:when>
            <xsl:otherwise>XPath2</xsl:otherwise>
					</xsl:choose>();
          }
          else
          {
					tree = parser.<xsl:choose>
						<xsl:when test="$spec='xquery'">XPath2</xsl:when>
						<xsl:when test="$spec='xpath'">XPath2</xsl:when>
						<xsl:when test="$spec='pathx1'">XPath2</xsl:when>
            <xsl:otherwise>XPath2</xsl:otherwise>
					</xsl:choose>();
          }
			      ((SimpleNode)tree.jjtGetChild(0)).dump("|") ;
			  }
			  catch(ParseException pe)
			  {
			  	System.err.println(pe.getMessage());
			  }
			  catch(Exception e)
			  {
			  	System.err.println(e.getMessage());
			  }
		    }		    
		  }
		}

    PARSER_END(<xsl:value-of select="$parser-class"/>)

	</xsl:template>
	
	<xsl:template name="action-production">
		<!-- Begin LV -->
		<xsl:if test="@prod-user-action">
				<xsl:value-of select="@prod-user-action"/>
		</xsl:if>
		<!-- End LV -->
	</xsl:template>
	
	<xsl:template name="action-production-end">
	</xsl:template>

	<xsl:template name="action-exprProduction-label">
    <xsl:if test="@node-type='void'">
      <xsl:text> #void </xsl:text>
    </xsl:if>
	</xsl:template>
	
	
	<xsl:template name="action-exprProduction">
	</xsl:template>
	
	<xsl:template name="action-exprProduction-end">
	</xsl:template>
	
	<xsl:template name="action-level">
		<xsl:param name="thisProd"/>
		<xsl:param name="nextProd"/>
		 
		<xsl:if test="@level-user-action">
				<xsl:value-of select="@level-user-action"/>
		</xsl:if>
		<!-- xsl:text>
printIndent(); System.err.println("</xsl:text>
	   <xsl:value-of select="$thisProd"/>
	   <xsl:text>");
</xsl:text -->
	</xsl:template>
	
	<xsl:template name="action-level-jjtree-label">
    <xsl:param name="label">
      <xsl:text>void</xsl:text>
    </xsl:param>
		<!-- Begin SMPG -->
    <xsl:param name="condition"></xsl:param>
		<!-- End SMPG -->

    <xsl:text> #</xsl:text>
    <xsl:value-of select="$label"/>

		<!-- Begin SMPG -->
		<xsl:if test="$condition != ''">
			<xsl:text>(</xsl:text>
			<xsl:value-of select="$condition"/>
			<xsl:text>)</xsl:text>
		</xsl:if>
		<!-- End SMPG -->
  </xsl:template>
	
	<xsl:template name="binary-action-level-jjtree-label">
    <xsl:param name="label"/>
    <xsl:param name="which" select="'unknown'"/>
    <!-- xsl:message>
      <xsl:text>In binary-action-level-jjtree-label: </xsl:text>
      <xsl:value-of select="name(.)"/>
      <xsl:text>  (</xsl:text>
      <xsl:value-of select="$which"/>
      <xsl:text>)</xsl:text>
    </xsl:message -->
    <xsl:if test="ancestor-or-self::g:binary or self::g:level/g:binary">
		<xsl:text>
		{
         try
         {
		       jjtThis.processToken((Token)binaryTokenStack.pop());
         }
         catch(java.util.EmptyStackException e)
         {
           token_source.printLinePos();
           e.printStackTrace();
           throw e;
         }
		}
		</xsl:text>
		<xsl:text> #</xsl:text><xsl:value-of select="$label"/><xsl:text>(2)</xsl:text>
    </xsl:if>
	</xsl:template>

	<xsl:template name="set-parser-package">
	</xsl:template>
	
	<xsl:template name="action-level-start">
	</xsl:template>
	
	<xsl:template name="action-level-end">
	</xsl:template>
	
	<xsl:template name="action-token-ref">
		<!-- xsl:if test="ancestor::g:binary">
		<xsl:text> #</xsl:text><xsl:value-of select="@name"/>
		</xsl:if -->
		<xsl:choose>
      <xsl:when test="not(ancestor::g:binary or ancestor-or-self::g:*/@is-binary='yes')">
        <xsl:choose>
          <xsl:when test="key('ref',@name)/self::g:token/@node-type='void'">
            <!-- Don't produce a node.  #void doesn't seem to work here, for some reason. -->
          </xsl:when>
          <xsl:when test="@node-type='void'">
            <!-- Don't produce a node.  #void doesn't seem to work here, for some reason. -->
          </xsl:when>
          <xsl:when test="key('ref',@name)/self::g:token/@node-type">
            <xsl:text>{</xsl:text>
						<xsl:if test="@token-user-action">
							<xsl:value-of select="@token-user-action"/>
						</xsl:if>
						<xsl:text>jjtThis.processToken(token);}</xsl:text>
            <xsl:text> #</xsl:text>
            <xsl:value-of select="key('ref',@name)/self::g:token/@node-type"/>
            <!-- See JTree doc for why I do the following... -->
            <xsl:if test="true() or following-sibling::*[1][self::g:zeroOrMore or self::g:oneOrMore]">
              <xsl:text>(true)</xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:when test="@node-type">
            <xsl:text>{</xsl:text>
						<xsl:if test="@token-user-action">
							<xsl:value-of select="@token-user-action"/>
						</xsl:if>
						<xsl:text>jjtThis.processToken(token);}</xsl:text>
            <xsl:text> #</xsl:text>
            <xsl:value-of select="@node-type"/>
            <!-- See JTree doc for why I do the following... -->
            <xsl:if test="true() or following-sibling::*[1][self::g:zeroOrMore or self::g:oneOrMore]">
              <xsl:text>(true)</xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>{</xsl:text>
						<xsl:if test="@token-user-action">
							<xsl:value-of select="@token-user-action"/>
						</xsl:if>
						<xsl:text>jjtThis.processToken(token);}</xsl:text>
            <xsl:text> #</xsl:text>
            <xsl:value-of select="@name"/>
            <!-- See JTree doc for why I do the following... -->
            <xsl:if test="true() or following-sibling::*[1][self::g:zeroOrMore or self::g:oneOrMore]">
              <xsl:text>(true)</xsl:text>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
				

			</xsl:when>
			<xsl:otherwise>
        <!-- xsl:message>
          <xsl:text>In binary-action-level-jjtree-label: </xsl:text>
          <xsl:value-of select="name(.)"/>
        </xsl:message -->
         {binaryTokenStack.push(token);}
			</xsl:otherwise>
		</xsl:choose>		
	</xsl:template>
	
	<!-- Begin LV -->
<xsl:template name="user-action-ref-start">
<xsl:if test="@nt-user-action-start">{<xsl:value-of select="@nt-user-action-start"/>}</xsl:if>
</xsl:template>

<xsl:template name="user-action-ref-end">
<xsl:if test="@nt-user-action-end">{<xsl:value-of select="@nt-user-action-end"/>}</xsl:if>
</xsl:template>
<!-- End LV -->
	
</xsl:stylesheet>
