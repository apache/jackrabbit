<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>IfPresent tag</h1>

<jcr:session>

<jcr:ifPresent item="/TestA">
	/TestA is present<br>
</jcr:ifPresent>

<jcr:ifPresent item="/john" value="false">
	/john is not present<br>
</jcr:ifPresent>

<jcr:ifPresent item="/">
	Root is present<br>
</jcr:ifPresent>

<jcr:ifPresent item="/" property="jcr:primcaryType">
	Property 'jcr:primaryType' is present in Root<br>
</jcr:ifPresent>

<jcr:ifPresent item="/" value="false">
	Root is not present<br>
</jcr:ifPresent>

</jcr:session>

