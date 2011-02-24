/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class Util {
	private static String lineSeparator = System.getProperty("line.separator");

	public static void writeLocalizations(ResourceLoader resourceLoader,  Writer w, List<Localization> localizations, Locale locale) throws IOException {
		w.write(resourceLoader.readResource("/optimizer/localization.js"));
		String localeString = locale.toString();
		String intermediateLocaleString = null;
		localeString = localeString.toLowerCase();
		if (localeString.indexOf('_') != -1) {
			localeString = localeString.replace('_', '-');
			intermediateLocaleString = localeString.substring(0, localeString.indexOf('-'));
		}
		//System.out.println("["+localeString+"]["+intermediateLocaleString+"]");
		StringBuffer sb = new StringBuffer();
		for (Localization localization : localizations) {
			//System.out.println("["+localization.bundlePackage+"]["+localization.modulePath+"]["+localization.bundleName+"]");
			String rootModule = normalizePath(localization.modulePath+'/'+localization.bundleName+".js");
			String intermediateModule = null;
			String fullModule = normalizePath(localization.modulePath+'/'+localeString+'/'+localization.bundleName+".js");
			if (intermediateLocaleString != null) {
				intermediateModule = normalizePath(localization.modulePath+'/'+intermediateLocaleString+'/'+localization.bundleName+".js");
			}
			String langId = (intermediateLocaleString == null) ? null : "'"+intermediateLocaleString+"'";
			String root = resourceLoader.readResource(rootModule);
			if (root == null) {
				root = "null";
			} else {
				root = root.replace(lineSeparator, " ");
				root = "'"+root+"'";
			}
			String lang = (intermediateModule == null) ? null : resourceLoader.readResource(intermediateModule);
			if (lang == null) {
				lang = "null";
			} else {
				lang = lang.replace(lineSeparator, " ");
				lang = "'"+lang+"'";
			}
			String langCountry = resourceLoader.readResource(fullModule);
			if (langCountry == null) {
				langCountry = "null";
			} else {
				langCountry = langCountry.replace(lineSeparator, " ");
				langCountry = "'"+langCountry+"'";
			}
			sb.append("dojo.optimizer.localization.load('"+localization.bundlePackage+"', "+langId+", '"+localeString+"', "+root+", "+lang+", "+langCountry+");\n");
		}
		w.write(sb.toString());
	}

	public static String normalizePath(String path) {
		try {
			URI uri = new URI(path);
			path = uri.normalize().getPath();
			if (path.charAt(0) != '/') {
				path = '/'+path;
			}
			return path;
		} catch (Exception e) {
			e.printStackTrace();
			return path;
		}
	}
	
}
