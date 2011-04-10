package org.dojotoolkit.optimizer.tests;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private static final String[] dojoIds = {
		"org.dojotoolkit.dojo",
		"org.dojotoolkit.compressor.uglifyjs",
		"org.json",
		"org.uglifyjs",
		"org.dojotoolkit.server.util.js",
		"org.requirejs",
		"org.dojotoolkit.optimizer.amd",
		"org.dojotoolkit.optimizer"
	};
	
	private static final String[] ids = {
		"org.dojotoolkit.optimizer.tests",
		"org.dojotoolkit.compressor.uglifyjs",
		"org.json",
		"org.uglifyjs",
		"org.dojotoolkit.server.util.js",
		"org.requirejs",
		"org.dojotoolkit.optimizer.amd",
		"org.dojotoolkit.optimizer"
	};
	
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		new Thread(new TestsRunner()).start();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}
	
	
	public class TestsRunner implements Runnable {
		public void run() {
			TestSuite suite = new TestSuite();
			if (System.getProperty("jsHandlerType") != null && System.getProperty("jsHandlerType").equals("syncloader")) {
				suite.addTest(new RhinoSyncLoaderOptimizerTest(context, dojoIds));
				suite.addTest(new V8SyncLoaderOptimizerTest(context, dojoIds));
			} else if (System.getProperty("jsHandlerType") != null && System.getProperty("jsHandlerType").equals("amd")) {
				suite.addTest(new RhinoAMDOptimizerTest(context, dojoIds));
				suite.addTest(new V8AMDOptimizerTest(context, dojoIds));
				suite.addTest(new RhinoAMDCircularDependencyTest(context, ids));
				suite.addTest(new V8AMDCircularDependencyTest(context, ids));
				suite.addTest(new RhinoAMDRelativeTest(context, ids));
				suite.addTest(new V8AMDRelativeTest(context, ids));
				suite.addTest(new RhinoAMDErrorTest(context, ids));
				suite.addTest(new V8AMDErrorTest(context, ids));
			}
			TestRunner.run(suite);
		}
	}
}
