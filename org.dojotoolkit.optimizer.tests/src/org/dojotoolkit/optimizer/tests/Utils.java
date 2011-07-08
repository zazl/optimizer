/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import org.dojotoolkit.json.JSONParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Utils {
	public static Bundle findBundle(BundleContext bundleContext, String symbolicName) {
		Bundle[] bundles = bundleContext.getBundles();
		Bundle bundle = null;
		
		for (Bundle b : bundles) {
			if (b.getSymbolicName().equals(symbolicName)) {
				bundle = b;
				break;
			}
		}
		return bundle;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> loadHandlerConfig(URL handlerConfigURL) throws IOException {
		Map<String, Object> handlerConfig = null;
		InputStream is = null;
		Reader r = null;
		try {
			is = handlerConfigURL.openStream();
			r = new BufferedReader(new InputStreamReader(is));
			handlerConfig = (Map<String, Object>)JSONParser.parse(r);
		} finally {
			if (is != null) { try { is.close(); } catch (IOException e) {}}
		}
		return handlerConfig;
	}

}
