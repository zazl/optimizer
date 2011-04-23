<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.JSAnalysisData" %>
<!DOCTYPE html>
<html>
<head>
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
	</style>
	<style type="text/css">
    	html, body { width: 100%; height: 100%; margin: 0; overflow:hidden; }
    	#borderContainerTwo { width: 100%; height: 100%; }
	</style>
	<script type="text/javascript">
		require = {
            packages: [
                {
                    name: 'dojo',
                    location: 'dojo',
                    main:'lib/main-browser',
                    lib: '.'
                },
                {
                    name: 'dijit',
                    location: 'dijit',
                    main:'lib/main',
                    lib: '.'
                }
            ],
            paths: {
                require: 'requirejs/require'
            },
			ready: function () {
                require(['amdtest/Declarative']);
			},
			locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>"
		};
	</script>
	<%
		boolean debug = (request.getParameter("debug") == null) ? false : Boolean.valueOf(request.getParameter("debug"));
		String url = null;
		if (debug) {
			url = request.getContextPath() +"/_javascript?debug=true";
		} else {
		    JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		    if (jsOptimizer == null) {
		    	throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		    }
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {"amdtest/Declarative"});
			url = request.getContextPath() +"/_javascript?modules=amdtest/Declarative&version="+analysisData.getChecksum()+"&locale="+request.getLocale();
		}
	%>
	<script type="text/javascript" src="<%=url%>"></script>

</head>
<body class="claro">
<div dojoType="dijit.layout.BorderContainer" gutters="true" id="borderContainerTwo" liveSplitters="false">
    <div dojoType="dijit.layout.ContentPane" region="top" splitter="false">
    	<div id="title"></div>
    </div>
    <div dojoType="dijit.layout.AccordionContainer" minSize="20" style="width: 300px;" id="leftAccordion" region="leading" splitter="true">
        <div dojoType="dijit.layout.AccordionPane" title="One">
        </div>
        <div dojoType="dijit.layout.AccordionPane" title="Two">
        </div>
        <div dojoType="dijit.layout.AccordionPane" title="Three" selected="true">
        </div>
        <div dojoType="dijit.layout.AccordionPane" title="Four">
        </div>
    </div>
    <!-- end AccordionContainer -->
    <div dojoType="dijit.layout.TabContainer" region="center" tabStrip="true">
        <div dojoType="dijit.layout.ContentPane" title="Calendar tab" selected="true">
            <div dojoType="dijit.Calendar"></div>
        </div>
        <div dojoType="dijit.layout.ContentPane" title="Tree tab">
			<div dojoType="dojo.data.ItemFileReadStore" jsId="continentStore" url="<%=request.getContextPath()%>/amdtest/countries.json">
			</div>
			<div dojoType="dijit.tree.ForestStoreModel" jsId="continentModel" store="continentStore"
				query="{type:'continent'}" rootId="continentRoot" rootLabel="Continents"
				childrenAttrs="children">
			</div>
			<div dojoType="dijit.Tree" id="mytree" model="continentModel" openOnClick="true">
			</div>        
        </div>
        <div dojoType="dijit.layout.ContentPane" title="Form tab">
			<div dojoType="dijit.form.Form" id="myForm" jsId="myForm" encType="multipart/form-data" action="" method="">
    			<script type="dojo/method" event="onReset">
        			return confirm('Press OK to reset widget values');
    			</script>
    			<script type="dojo/method" event="onSubmit">
			        if (this.validate()) {
            			return confirm('Form is valid, press OK to submit');
        			} else {
            			alert('Form contains invalid data.  Please correct first');
            			return false;
        			}
        			return true;
    			</script>
    			<table style="border: 1px solid #9f9f9f;" cellspacing="10">
        			<tr>
            			<td>
                			<label for="name">Name:</label>
            			</td>
            			<td>
                			<input type="text" id="name" name="name" required="true" dojoType="dijit.form.ValidationTextBox"/>
            			</td>
        			</tr>
        			<tr>
            			<td>
                			<label for="dob">Date of birth:</label>
            			</td>
            			<td>
                			<input type="text" id="dob" name="dob" dojoType="dijit.form.DateTextBox"/>
            			</td>
        			</tr>
    			</table>
    			<button dojoType="dijit.form.Button" type=button onClick="console.log(myForm.getValues())">
        			Get Values from form!
    			</button>
    			<button dojoType="dijit.form.Button" type="submit" name="submitButton" value="Submit">
        			Submit
    			</button>
    			<button dojoType="dijit.form.Button" type="reset">
        			Reset
    			</button>
			</div>        
        </div>
        <div dojoType="dijit.layout.ContentPane" title="Stack Container tab">
			<button id="previous" onClick="dijit.byId('stackContainer').back()" dojoType="dijit.form.Button">&lt;</button>
			<span dojoType="dijit.layout.StackController" containerId="stackContainer"></span>
			<button id="next" onClick="dijit.byId('stackContainer').forward()" dojoType="dijit.form.Button">&gt;</button>
			<div dojoType="dijit.layout.StackContainer" id="stackContainer">
    			<div dojoType="dijit.layout.ContentPane" title="Questions">
        			Please answer following questions
    			</div>
    			<div dojoType="dijit.layout.ContentPane" title="Answers">
        			Here is what you should have answered :P
    			</div>
			</div>
		</div>	
    </div>
    <!-- end TabContainer -->
</div>
<!-- end BorderContainer -->
</body>
</html>