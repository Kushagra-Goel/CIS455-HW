package test.edu.upenn.cis455;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunAllTests extends TestCase 
{
	

    protected PrintWriter out;
  public static Test suite() 
  {
    try {
      Class[]  testClasses = {
        /* TODO: Add the names of your unit test classes here */
        // Class.forName("your.class.name.here") 
      };   
      
      return new TestSuite(testClasses);
    } catch(Exception e){
      e.printStackTrace();
    } 
    
    return null;
  }
//public void testBench()  {
//	String url = "https://www.google.com:80/lol/lol2/lol2.html";
//	URLInfo urlInfo = new URLInfo(url);
//
//	System.out.println(urlInfo.getHostName());
//	System.out.println(urlInfo.getProtocol());
//	System.out.println(urlInfo.getPortNo());
//	System.out.println(urlInfo.getFilePath());
//
//}
//
//public void testXPathEngineImplIsValid()  {
//	
//	Map<String, Boolean> xpaths = new LinkedHashMap<String, Boolean>(){{
//		put("/foo/bar/xyz", true);
//		put("/foo/bar[@att=\"123\"]", true);
//		put("/xyz/abc[contains(text(),\"someSubstring\")]", true);
//		put("/a/b/c[text()=\"theEntireText\"]", true);
//		put("/blah[anotherElement]", true);
//		put("/this/that[something/else]", true);
//		put("/d/e/f[foo[text()=\"something\"]][bar]", true);
//		put("/a/b/c[text() = \"white Spaces Should Not Matter\"]",true);
//		put("/a/b/c[contains(text() , \"white Spaces Should Not Matter\")]",true);
//		put("",false);
//		put("a/b/c",false);
//		put("/*",false);
//		put("/:",false);
//		put("/x[y]",true);
//		put("/x[y[z]]",true);
//		put("/x[y][z]",true);
//		put("/x[y[text() = \"white Spaces Should Not Matter\"]][z]",true);
//		put("/x[y[contains(text() , \"white Spaces Should Not Matter\")]][z]",true);
//	}};
//	String[] xpaths_String = new String[xpaths.keySet().size()];
//	xpaths.keySet().toArray(xpaths_String);
//	
//	
//	XPathEngineImpl xpel = new XPathEngineImpl();
//	
//	xpel.setXPaths(xpaths_String);
//	
//	for(int i = 0; i < xpaths_String.length; ++i) {
////		System.out.println(String.format(" >>>>>>  %b : %s", xpel.isValid(i), xpaths_String[i]));
//		assertEquals(xpaths.get(xpaths_String[i]).booleanValue(), xpel.isValid(i));
//	}
//}
//
//
//
//public void testXPathEngineImplEvaluateToy() throws ParserConfigurationException, SAXException, IOException  {
//	
//	Map<String, Boolean> xpaths = new LinkedHashMap<String, Boolean>(){{
//		put("/note", true);
//		put("/note/to", true);
//		put("/note/from", true);
//		put("/note/to/from", false);
//		put("/note[heading][with]", true);
//		put("/note/to[@specie = \"Human\"]", true);
//		put("/note[to[@specie = \"Human\"]]", true);
//		put("/note[to[@species = \"Human\"]]", false);
//		put("/note/to[@specie = \"Cat\"]", false);
//	}};
//	String[] xpaths_String = new String[xpaths.keySet().size()];
//	xpaths.keySet().toArray(xpaths_String);
//	XPathEngineImpl xpel = new XPathEngineImpl();
//	xpel.setXPaths(xpaths_String);
//	
//	
//	String testXML = "<note>" + 
//			"<to specie = \"Human\">Tove</to>" 
//			+ "<from specie = \"Dog\">Jani</from>" 
//			+ "<with >Love</with>"
//			+ "<heading>"
//			+ "Reminder"
//			+ "</heading>" + 
//			"<body>Dont forget me this weekend!</body>" + 
//			"</note>";
//	
//	
//	DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//	InputSource is = new InputSource(new StringReader(testXML));
//	Document doc = documentBuilder.parse(is);
//	
////	out = new PrintWriter(new OutputStreamWriter(System.out, "UTF8"));
////
////	print(doc);
//	
//	
//	boolean[] evaluation = xpel.evaluate(doc); 
//
//	
//	for(int i = 0; i < xpaths_String.length; ++i) {
////		System.out.println(String.format("Checking xpath %s", xpaths[i]));
////		System.out.println(String.format(" >>>>>>  %b : %s", evaluation[i], xpaths_String[i]));
//		assertEquals(xpaths.get(xpaths_String[i]).booleanValue(), evaluation[i]);
//	}
//	
//	
//	
//}
//
//
//
//
//
//public void testXPathEngineImplEvaluate() throws ParserConfigurationException, SAXException, IOException  {
//	
//	Map<String, Boolean> xpaths = new LinkedHashMap<String, Boolean>(){{
//		put("/rss", true);
//		put("/rss/NotAChannel", false);
//		put("/rss/channel", true);
//		put("/rss/channel/title", true);
//		put("/rss/channel/title[text() = \"NYT > Sports\"]", true);
//		put("/rss/channel/title[text() = \"NYT < Sports\"]", false);
//		put("/rss/channel/title[contains(text(), \"Sports\")]", true);
//		put("/rss/channel/title[contains(text(), \"> Sports\")]", true);
//		put("/rss/channel/title[contains(text(), \"< Sports\")]", false);
//		put("/rss/channel/item[guid[@isPermaLink = \"true\"]]", true);
//		put("/rss/channel/item/guid[@isPermaLink = \"true\"]", true);
//		put("/rss/channel/item[guid[@isPermaLin = \"true\"]]", false);
//		put("/rss/channel/item[guid[@isPermaLink = \"false\"]]", false);
//		put("/rss/channel/item[category[contains(text(), \"Coronavirus\")]]", true);
//	}};
//	String[] xpaths_String = new String[xpaths.keySet().size()];
//	xpaths.keySet().toArray(xpaths_String);
//	XPathEngineImpl xpel = new XPathEngineImpl();
//	xpel.setXPaths(xpaths_String);
//	
//	
//	DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();	
//	InputSource is = new InputSource(new InputStreamReader(new FileInputStream(new File("src/main/java/test/edu/upenn/cis455/Sports.xml"))));
//	Document doc = documentBuilder.parse(is);
//	
////	out = new PrintWriter(new OutputStreamWriter(System.out, "UTF8"));
////	print(doc);
//	
//	boolean[] evaluation = xpel.evaluate(doc); 
//
//	
//	for(int i = 0; i < xpaths_String.length; ++i) {
////		System.out.println(String.format(" >>>>>>  %b : %s", evaluation[i], xpaths_String[i]));
//		assertEquals(xpaths.get(xpaths_String[i]).booleanValue(), evaluation[i]);
//	}
//	
//	
//	
//}
///** Prints the specified node, recursively. */
//public void print(Node node) {
//
//    // is there anything to do?
//    if ( node == null ) {
//        return;
//    }
//
//    int type = node.getNodeType();
//    switch ( type ) {
//    // print document
//    case Node.DOCUMENT_NODE: {
//            NodeList children = node.getChildNodes();
//            for ( int iChild = 0; iChild < children.getLength(); iChild++ ) {
//                print(children.item(iChild));
//            }
//            out.flush();
//            break;
//        }
//
//        // print element with attributes
//    case Node.ELEMENT_NODE: {
//            out.print('<');
//            out.print(node.getNodeName());
//            Attr attrs[] = sortAttributes(node.getAttributes());
//            for ( int i = 0; i < attrs.length; i++ ) {
//                Attr attr = attrs[i];
//                out.print(' ');
//                out.print(attr.getNodeName());
//                out.print("=\"");
//                out.print(normalize(attr.getNodeValue()));
//                out.print('"');
//            }
//            out.print('>');
//            NodeList children = node.getChildNodes();
//            if ( children != null ) {
//                int len = children.getLength();
//                for ( int i = 0; i < len; i++ ) {
//                    print(children.item(i));
//                }
//            }
//            break;
//        }
//
//        // handle entity reference nodes
//    case Node.ENTITY_REFERENCE_NODE: {
//            out.print('&');
//            out.print(node.getNodeName());
//            out.print(';');
//            break;
//        }
//
//        // print cdata sections
//    case Node.CDATA_SECTION_NODE: {
//            out.print("<![CDATA[");
//            out.print(node.getNodeValue());
//            out.print("]]>");
//            break;
//        }
//
//        // print text
//    case Node.TEXT_NODE: {
//            out.print(normalize(node.getNodeValue()));
//            break;
//        }
//
//        // print processing instruction
//    case Node.PROCESSING_INSTRUCTION_NODE: {
//            out.print("<?");
//            out.print(node.getNodeName());
//            String data = node.getNodeValue();
//            if ( data != null && data.length() > 0 ) {
//                out.print(' ');
//                out.print(data);
//            }
//            out.println("?>");
//            break;
//        }
//    }
//
//    if ( type == Node.ELEMENT_NODE ) {
//        out.print("</");
//        out.print(node.getNodeName());
//        out.print('>');
//    }
//
//    out.flush();
//
//} // print(Node)
//
//
///** Returns a sorted list of attributes. */
//protected Attr[] sortAttributes(NamedNodeMap attrs) {
//
//    int len = (attrs != null) ? attrs.getLength() : 0;
//    Attr array[] = new Attr[len];
//    for ( int i = 0; i < len; i++ ) {
//        array[i] = (Attr)attrs.item(i);
//    }
//    for ( int i = 0; i < len - 1; i++ ) {
//        String name  = array[i].getNodeName();
//        int    index = i;
//        for ( int j = i + 1; j < len; j++ ) {
//            String curName = array[j].getNodeName();
//            if ( curName.compareTo(name) < 0 ) {
//                name  = curName;
//                index = j;
//            }
//        }
//        if ( index != i ) {
//            Attr temp    = array[i];
//            array[i]     = array[index];
//            array[index] = temp;
//        }
//    }
//
//    return(array);
//
//} // sortAttributes(NamedNodeMap):Attr[]
//
///** Normalizes the given string. */
//protected String normalize(String s) {
//    StringBuffer str = new StringBuffer();
//
//    int len = (s != null) ? s.length() : 0;
//    for ( int i = 0; i < len; i++ ) {
//        char ch = s.charAt(i);
//        switch ( ch ) {
//        case '<': {
//                str.append("&lt;");
//                break;
//            }
//        case '>': {
//                str.append("&gt;");
//                break;
//            }
//        case '&': {
//                str.append("&amp;");
//                break;
//            }
//        case '"': {
//                str.append("&quot;");
//                break;
//            }
//        case '\'': {
//                str.append("&apos;");
//                break;
//            }
//        case '\r':
//        case '\n': {
//                // else, default append char
//            }
//        default: {
//                str.append(ch);
//            }
//        }
//    }
//
//    return(str.toString());
//
//} // normalize(String):String


}
