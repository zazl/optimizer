package org.dojotoolkit.optimizer.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

public class AMDExcludeTest extends OptimizerTest {
	private static String depCompare = "dijit/layout/_ContentPaneResizeMixin.js dojo/html.js dijit/layout/ContentPane.js dijit/layout/AccordionPane.js dijit/layout/AccordionContainer.js "; 
	
	public AMDExcludeTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}

	protected void runTest() throws Throwable {
		URL url = Utils.findBundle(bundleContext, "org.dojotoolkit.optimizer.servlet").getResource("/requirejs.json");
		assertNotNull(url);
		
		try {
			Map<String, Object> config = Utils.loadHandlerConfig(url);
			assertNotNull(config);
			RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
			
			JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, true, config);
			JSAnalysisData  analysisData = optimizer.getAnalysisData(new String[] {"dijit/layout/TabContainer"});
			analysisData = optimizer.getAnalysisData(new String[] {"dijit/layout/AccordionContainer"}, new JSAnalysisData[] {analysisData});
			StringBuffer sb = new StringBuffer();
			for (String dependency: analysisData.getDependencies()) {
				sb.append(dependency);
				sb.append(' ');
			}
			assertEquals(depCompare, sb.toString());
		} catch (IOException e) {
			TestCase.fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
