<html>
<HEAD>
</HEAD>
<BODY>

<h1>JCR taglib</h1>

<h3>Basic usage</h3>
<ol>
	<LI>
		<a href="session.jsp">SessionTag</a>. <br/>
		jrc:session  sets the boundaries of a JCR session. 
		It creates a Session and stores it in a page scope variable.
	</LI>
	<LI>
		<a href="cd.jsp">CdTag</a>. <br/>
		jrc:cd sets the current working directory. 
	</LI>	
	<LI><a href="nodes.jsp">NodesTag</a><br/>
		jrc:nodes iterates over all the traversed nodes from the given node.	
	</LI>
	<LI><a href="properties.jsp">PropertiesTag</a><br/>
		jcr:properties iterates over the properties of the given node.
	</LI>
	<LI><a href="out.jsp">OutTag</a><br/>
		jcr:out displays Node and property values through 
		the given template engine and template.	
	</LI>
	<LI><a href="set.jsp">SetTag</a><br/>
		jcr:set stores the given node or property in a 
		page context scoped variable.
	</LI>
	<LI><a href="count.jsp">CountTag</a><br/>
		jcr:counts counts the nodes returned by the given 
		Traverser and writes the value.
	</LI>
	<LI><a href="size.jsp">SizeTag</a><br/>
		jcr:size Estimates the cumulative size of the nodes returned by the given
		Traverser and writes the value.
	</LI>	
	<LI><a href="query.jsp">QueryTag</a><br/>
		jcr:query Iterates over the query result nodes. 
	</LI>		
	<LI><a href="ifPresent.jsp">IfPresentTag</a><br/>
		jcr:ifPresent Conditional tag which evaluates a node existence. 
	</LI>	
	<LI><a href="versions.jsp">versionsTag</a><br/>
		jcr:versions Iterates over the versions of the given node 
	</LI>
</ol>

<h3>Advanced usage</h3>
<ol>
	<LI>Test <a href="traversers.jsp">traversers</a> </LI>
	<LI>Test <a href="filters.jsp">filters</a> </LI>
	<LI>Test <a href="comparators.jsp">comparators</a> </LI>
</ol>
</BODY>
</html>