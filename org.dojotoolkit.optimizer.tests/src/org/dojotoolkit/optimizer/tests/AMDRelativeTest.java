package org.dojotoolkit.optimizer.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

public class AMDRelativeTest extends OptimizerTest {
	private static String depCompare = "test/amd/relative/subdir/F.js test/amd/relative/E.js test/amd/relative/subdir/anothersubdir/D.js test/amd/relative/C.js test/amd/relative/subdir/B.js test/amd/relative/A.js ";
	
	public AMDRelativeTest(BundleContext bundleContext, String[] ids) {
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
			JSAnalysisData  analysisData = optimizer.getAnalysisData(new String[] {"test/amd/relative/A"});
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
