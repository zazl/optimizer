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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class Util {
	private static String lineSeparator = System.getProperty("line.separator");
	private static Pattern commentsRegex = Pattern.compile("(\\/\\*([\\s\\S]*?)\\*\\/|\\/\\/(.*)$)", Pattern.MULTILINE);
	private static Pattern apostropheRegex = Pattern.compile("(?<!\\\\)\'", Pattern.MULTILINE);

	public static void writeLocalizations(ResourceLoader resourceLoader,  Writer w, List<Localization> localizations, Locale locale) throws IOException {
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
				root = convert(root);
			}
			String lang = (intermediateModule == null) ? null : resourceLoader.readResource(intermediateModule);
			if (lang == null) {
				lang = "null";
			} else {
				lang = convert(lang);
			}
			String langCountry = resourceLoader.readResource(fullModule);
			if (langCountry == null) {
				langCountry = "null";
			} else {
				langCountry = convert(langCountry);
			}
			sb.append("dojo.optimizer.localization.load('"+localization.bundlePackage+"', "+langId+", '"+localeString+"', "+root+", "+lang+", "+langCountry+");\n");
		}
		w.write(sb.toString());
	}

	public static void writeAMDLocalizations(ResourceLoader resourceLoader,  Writer w, List<Localization> localizations, Locale locale) throws IOException {
		String localeString = locale.toString();
		String intermediateLocaleString = null;
		localeString = localeString.toLowerCase();
		if (localeString.indexOf('_') != -1) {
			localeString = localeString.replace('_', '-');
			intermediateLocaleString = localeString.substring(0, localeString.indexOf('-'));
		}
		//System.out.println("["+localeString+"]["+intermediateLocaleString+"]");
		for (Localization localization : localizations) {
			//System.out.println("["+localization.bundlePackage+"]["+localization.modulePath+"]["+localization.bundleName+"]");
			String rootModule = normalizePath(localization.modulePath+'/'+localization.bundleName);
			String intermediateModule = null;
			String fullModule = normalizePath(localization.modulePath+'/'+localeString+'/'+localization.bundleName);
			if (intermediateLocaleString != null) {
				intermediateModule = normalizePath(localization.modulePath+'/'+intermediateLocaleString+'/'+localization.bundleName);
			}
			String root = resourceLoader.readResource('/'+rootModule+".js");
			if (root != null) {
				writeLocalization(w, root, rootModule);
			}
			String lang = (intermediateModule == null) ? null : resourceLoader.readResource('/'+intermediateModule+".js");
			if (lang != null) {
				writeLocalization(w, lang, intermediateModule);
			}
			String langCountry = resourceLoader.readResource('/'+fullModule+".js");
			if (langCountry != null) {
				writeLocalization(w, langCountry, fullModule);
			}
		}
	}
	
	public static String normalizePath(String path) {
		try {
			URI uri = new URI(path);
			path = uri.normalize().getPath();
			return path;
		} catch (Exception e) {
			e.printStackTrace();
			return path;
		}
	}
	
	private static void writeLocalization(Writer w, String content, String moduleName) throws IOException {
		w.write(content.substring(0, content.indexOf('(')+1));
		w.write("'");
		w.write(moduleName);
		w.write("',");
		w.write(content.substring(content.indexOf('(')+1));
		w.write("\n");
	}
	
	private static String convert(String messages) {
		Matcher m = commentsRegex.matcher(messages);
		messages = m.replaceAll("");
		m = apostropheRegex.matcher(messages);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "\\\\\'");
		}
		m.appendTail(sb);
		messages = sb.toString();
		messages = messages.replace(lineSeparator, " ").replace("\\\"", "\\\\\"");
		
		if (messages.indexOf('(') == -1) {
			messages = "'("+messages+")'";
		} else {
			messages = "'"+messages+"'";
		}
		return messages;
	}
}
