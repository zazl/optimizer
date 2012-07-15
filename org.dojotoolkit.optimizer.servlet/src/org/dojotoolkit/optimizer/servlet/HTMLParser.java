/*
    Copyright (c) 2004-2012, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.DefaultFilter;
import org.cyberneko.html.filters.Identity;
import org.cyberneko.html.filters.Writer;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class HTMLParser extends DefaultFilter {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer.servlet");
	
    private static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";
    private static final String FILTERS = "http://cyberneko.org/html/properties/filters";
    private static final String SCRIPT_TYPE = "text/javascript";
    private static final String LOCALE_PLACEHOLDER = "__localePlaceholder__";

	private HTMLConfiguration parser = null;
	private String encoding = null;
	private StringBuffer scriptBuffer = null;
	private ResourceLoader resourceLoader = null;
	private RhinoClassLoader rhinoClassLoader = null;
	private JSURLGenerator urlGenerator = null;
	private String currentURL = null;
	private String scriptURL = null;
	private Locale locale = null;
	private String path = null;
	private String contextRoot = null;
	private Map<String, Object> config = null;
	
    public HTMLParser(java.io.Writer out, 
    	String encoding, 
    	ResourceLoader resourceLoader, 
    	RhinoClassLoader rhinoClassLoader, 
    	JSOptimizer jsOptimizer, 
    	Locale locale,
    	String contextRoot,
    	String path) {
    	this.encoding = encoding; 
		this.resourceLoader = resourceLoader;
		this.rhinoClassLoader = rhinoClassLoader;
		this.locale = locale;
		this.path = path;
		this.contextRoot = contextRoot;
		parser = new HTMLConfiguration();
        parser.setFeature(AUGMENTATIONS, true);
        XMLDocumentFilter[] filters = { this, new Identity(), new HTMLWriter(out, encoding) };
        parser.setProperty(FILTERS, filters);
        urlGenerator = new JSURLGenerator(jsOptimizer, locale, contextRoot);
    }
    
    public void parse(String html) throws IOException {
        parser.parse(new XMLInputSource(null, "", null, new StringReader(html), encoding));
    }
    
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs) throws XNIException {
    	super.startDocument(locator, encoding, augs);
    }
    
    public void startElement(QName element, XMLAttributes attrs, Augmentations augs) throws XNIException {
        if (element.rawname.equalsIgnoreCase("script") && attrs != null) {
            String value = attrs.getValue("type");
            if (value != null && value.equalsIgnoreCase(SCRIPT_TYPE)) {
            	String src = attrs.getValue("src");
            	if (src != null) {
            		String url = src;
            		if (src.charAt(0) != ('/')) {
            			url = path + url;
            		}
            		if (url.startsWith(contextRoot)) {
            			url = url.substring(contextRoot.length());
            		}
            		try {
						String scriptContents = resourceLoader.readResource(url);
						if (scriptContents != null) {
							scriptURL = analyzeScript(scriptContents, resourceLoader, rhinoClassLoader, urlGenerator);
						} else {
							logger.logp(Level.SEVERE, "HTMLParser", "startElement", "Unable to locate the resource for url ["+url+"]");
						}
					} catch (IOException e) {
						logger.logp(Level.SEVERE, "HTMLParser", "startElement", "Exception on reading url ["+url+"]", e);
					}
            	} else {
            		scriptBuffer = new StringBuffer();
            	}
            }
        }
    	super.startElement(element, attrs, augs);
    }
    
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (scriptBuffer != null) {
        	scriptBuffer.append(text.ch, text.offset, text.length);
        }
        super.characters(text, augs);
    }
    
    public void endElement(QName element, Augmentations augs) throws XNIException {
        if (scriptBuffer != null) {
        	if (scriptBuffer.length() > 0) {
        		currentURL = analyzeScript(scriptBuffer.toString(), resourceLoader, rhinoClassLoader, urlGenerator);
        	}
        	scriptBuffer = null;
        }
        super.endElement(element, augs);
    }
    
    @SuppressWarnings("unchecked")
	private String analyzeScript(String scriptContents, ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, JSURLGenerator urlGenerator) {
    	String url = null;
    	ScriptAnalyzer scriptAnalyzer = new RhinoASTScriptAnalyzer(resourceLoader, rhinoClassLoader);
    	try {
    		Map<String, Object> results = scriptAnalyzer.analyze(scriptContents);
    		List<String> depList = (List<String>)results.get("dependencies");
    		if (results.containsKey("config")) {
    			config = (Map<String, Object>)results.get("config");
    		}
    		if (depList.size() > 0) {
    			String[] deps = new String[depList.size()];
    			deps = depList.toArray(deps);
    			url = urlGenerator.generateURL(deps, config);
    		}
		} catch (IOException e) {
			logger.logp(Level.SEVERE, "HTMLParser", "analyzeScript", "Exception on script analyze", e);
		}
    	return url;
    }
    
    public class HTMLWriter extends Writer {
    	StringWriter sw = null;
    	PrintWriter pw = null;
    	
    	public HTMLWriter(java.io.Writer out, String encoding) {
    		super(out, encoding);
    	}
    	
        protected void printStartElement(QName element, XMLAttributes attrs) throws XNIException {
            if (element.rawname.equalsIgnoreCase("script") && attrs != null) {
                String value = attrs.getValue("type");
                if (value != null && value.equalsIgnoreCase(SCRIPT_TYPE)) {
                	String src = attrs.getValue("src");
                	if (src != null) {
                		if (scriptURL  != null) {
                			fPrinter.println("<script type=\"text/javascript\" src=\""+scriptURL+"\"></script>");
                		}
                		super.printStartElement(element, attrs);
                	} else {
		            	sw = new StringWriter();
		            	PrintWriter save = fPrinter; 
		            	pw = new PrintWriter(sw);
		            	fPrinter = pw;
		            	super.printStartElement(element, attrs);
		            	fPrinter = save;
                	}
                } else {
                	super.printStartElement(element, attrs);
                }
            } else {
            	super.printStartElement(element, attrs);
            }
        }
        
        protected void printCharacters(XMLString text, boolean normalize) {
        	if (sw != null) {
            	PrintWriter save = fPrinter; 
            	fPrinter = pw;
        		super.printCharacters(text, normalize);
            	fPrinter = save;
        	} else {
        		super.printCharacters(text, normalize);
        	}
        }
        
        protected void printEndElement(QName element) throws XNIException {
            if (sw != null) {
            	PrintWriter save = fPrinter; 
            	fPrinter = pw;
            	super.printEndElement(element);
            	fPrinter = save;
            	if (currentURL != null) {
                	fPrinter.println("<script type=\"text/javascript\" src=\""+currentURL+"\"></script>");
                	currentURL = null;
            	}
            	String script = sw.toString();
        		if (script.contains(LOCALE_PLACEHOLDER)) {
        			script = script.replace(LOCALE_PLACEHOLDER, "'"+locale.toString().toLowerCase().replace('_', '-')+"'");
        		}
            	fPrinter.print(script);
            	sw = null;
            	pw = null;
            } else {
            	super.printEndElement(element);
            }
        }
    }
}
