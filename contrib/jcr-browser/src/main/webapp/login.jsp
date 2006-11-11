<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<html>

<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Welcome to Jackrabbit JCR Browser</title>
<link href="<c:url value="/style.css"/>" type="text/css" rel="stylesheet">
<script src="<c:url value="/dojo/dojo.js"/>"></script>
<script>
			dojo.require("dojo.lfx.rounded");
			dojo.hostenv.writeIncludes();

			dojo.addOnLoad(function(){
				dojo.lfx.rounded({
						tl:{ radius:15 },
						tr:{ radius:10 },
						br:{ radius:15 },
						bl:{ radius:15 }
					}, 
					["roundMe"]
				);
				//dojo.lfx.rounded({}, ["roundMe3"]);
			});
		</script>

</head>

<body bgcolor="#ffffff" link="#0000FF" vlink="0000FF">


<table cellpadding="0" cellspacing="0" border="0" align="center"
	width="750">
	<tr>
		<td><img src="<c:url value="/images/jackrabbitlogo.gif"/>" width="320" height="83"
			border="0" /></td>
		<td width="100%" align="right" style="padding-left: 20;"><br />
		<h2>Welcome to JCR Browser</h2>
		</td>
	</tr>
	<tr>
		<td colspan="2">
		<hr size="1" noshade="noshade" />
		</td>
	</tr>	
	<tr>
		<td height="450" width="50%">
		<h4>Welcome</h4>
		<ul>
			<li>user: jackrabbit</li>
			<li>password: jackrabbit</li>
		</ul>
		</td>
		<td>
		<div id="roundMe">
		<form method="POST" action="j_security_check">
		<table align="center">
			<tr>
				<td>User:</td>
				<td><input type="text" name="j_username"></td>
			</tr>
			<tr>
				<td>Password:</td>
				<td><input type="password" name="j_password"></td>
			</tr>
			<tr>
				<td colspan="2" align="center">
				<button name="submit" type="submit">Sign in</button>
				</td>
			</tr>
		</table>
		</form>
		</div>
		</td>
	</tr>
	<tr>
		<td colspan="2">
		<hr size="1" noshade="noshade" />
		</td>
	</tr>
	<tr>
		<td colspan="2" align="center">
		<h6>http://jackrabbit.apache.org</h6>
		</td>
	</tr>
</table>

</body>
</html>
