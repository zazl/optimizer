package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.osgi.OSGiResourceLoader;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

public abstract class OptimizerTest extends TestCase {
	protected ResourceLoader resourceLoader = null;
	protected JSOptimizerFactory factory = null;
	protected BundleContext bundleContext = null;
	protected String[] ids = null;

	public OptimizerTest(BundleContext bundleContext, String[] ids) {
		this.bundleContext = bundleContext;
		this.ids = ids;
	}

	protected void setUp() throws Exception {
		resourceLoader = new OSGiResourceLoader(bundleContext, ids, null);
	}
}
