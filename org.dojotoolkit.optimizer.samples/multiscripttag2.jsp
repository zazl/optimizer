<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.servlet.JSURLGenerator" %>
<html>
    <head>
        <title>Person Grid + Calendar using multiple script tags</title>
        <style type="text/css">
            @import "<%=request.getContextPath()%>/dojo/resources/dojo.css";
            
            #summary {
            	border-bottom:1px solid black;
            	width: 40%;
            }
        </style>
        <link type="text/css" rel="stylesheet" href="<%=request.getContextPath()%>/css/styles.css">
        <link id="themeStyles" rel="stylesheet" href="<%=request.getContextPath()%>/dijit/themes/tundra/tundra.css"/>
		<link id="themeStyles" rel="stylesheet" href="<%=request.getContextPath()%>/dojox/grid/resources/Grid.css"/>
		<link id="themeStyles" rel="stylesheet" href="<%=request.getContextPath()%>/dojox/grid/resources/tundraGrid.css"/>
		<script type="text/javascript">
			djConfig = {
				isDebug: false,
				usePlainJson: true,
				parseOnLoad: true,
				baseUrl: "<%=request.getContextPath()%>/dojo/",
				locale: "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>",
				localizationComplete: true
			};
		</script>
		<%
		    JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		    if (jsOptimizer == null) {
		    	throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		    }
		    JSURLGenerator urlGenerator = new JSURLGenerator(jsOptimizer, request.getLocale(), request.getContextPath()); 
		%>
			<script type="text/javascript" src="<%=urlGenerator.generateURL("dijit._Widget", null)%>"/></script>
			<script type="text/javascript" src="<%=urlGenerator.generateURL("dijit.Calendar", null)%>"/></script>
			<script type="text/javascript" src="<%=urlGenerator.generateURL("test.PersonGrid", null)%>"/></script>
		<script type="text/javascript">
  			dojo.require("dijit.Calendar");
  			dojo.require("test.PersonGrid");
		</script>        
		<script type="text/javascript">
	        var data = {
	            identifier: 'firstName',
	            label: 'firstName',
	            items: [
                	{
                		firstName: "John",
                		lastName: "Smtih",
                		emailAddress: "jsmith@gmail.com"
                	},
                	{
                		firstName: "Mike",
                		lastName: "Jones",
                		emailAddress: "mjones@yahoo.com"
                	},
                	{
                		firstName: "Eric",
                		lastName: "Winner",
                		emailAddress: "ewinner@gmail.com"
                	},
                	{
                		firstName: "Susan",
                		lastName: "Defranco",
                		emailAddress: "sdefranco@hotmail.com"
                	},
                	{
                		firstName: "Elizabeth",
                		lastName: "Huggings",
                		emailAddress: "ehuggins@aol.com"
                	}
	            ]
	        };
			var personStore = new dojo.data.ItemFileReadStore({data: data});
	        
		</script>        
    </head>
    <body class="tundra">
    	<div dojoType="dijit.Calendar"></div>
    	<table id="persongrid" dojoType="dojox.grid.DataGrid" store="personStore">
			<thead>
				<tr>
					<th field="firstName" width="100px">First Name</th>
					<th field="lastName" width="100px">Last Name</th>
					<th field="emailAddress" width="100px">Email Address</th>
				</tr>
			</thead>
    	</table>
    </body>
</html>
