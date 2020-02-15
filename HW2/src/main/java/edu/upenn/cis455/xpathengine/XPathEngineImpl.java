package edu.upenn.cis455.xpathengine;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.helpers.DefaultHandler;

import test.edu.upenn.cis.stormlite.PrintBolt;

/** (MS2) Implements XPathEngine to handle XPaths.
  */
public class XPathEngineImpl implements XPathEngine {
	static Logger logger = Logger.getLogger(XPathEngineImpl.class);

	String[] xpaths = new String[0];
	int nextPos;
	char sym;
	String xpath;
	boolean ignoreSpace = true;

  public XPathEngineImpl() {
    // Do NOT add arguments to the constructor!!
  }
	
  public void setXPaths(String[] s) {
	  xpaths = s;
  }
  
  
  /**
   * Custom exception to escape the recursion
   * @author cis455
   *
   */
  private class XPathException extends Exception{
	  
	  public XPathException(String msg) {
		  super("Invalid XPath : " + msg);
	  }
	  
	  public XPathException() {
		  super("Invalid XPath");
	  }
  }
  
/**
 */
  private void error(String msg) throws XPathException {
	  throw new XPathException(msg);
  }
  
  private void error() throws XPathException {
	  throw new XPathException();
  }
  
/**
 * Reads the next symbol (ignoring the whitespace if set)
 */
  private void nextsym() {
	  logger.debug(String.format(" NextPos = %d | CurrentSym = %c", nextPos, sym));
	  if(nextPos == xpath.length()) {
		  nextPos = -1;
		  return;
	  }
	  sym = xpath.charAt(nextPos++);
	  if(ignoreSpace && Character.isWhitespace(sym)) {
		  nextsym();
	  }
  }
  
  /**
   * Checks if next few words are from a given list, used to test
   * resets the nextPos if check fails
   * @param words
   * @return true if match
   */
  private boolean nextwords(String[] words) {
	 logger.debug(String.format("Checking for String : %s", String.join(" ", words)));
	  int nextPos_backup = nextPos;
	  
	  for(String word: words) {
		  ignoreSpace = true;
		  for(int i = 0; i < word.length(); i++) {
			  nextsym();
			  ignoreSpace = false;
			  if(nextPos == -1 || sym != word.charAt(i)) {
				  ignoreSpace = true;
				  nextPos = nextPos_backup;
				  return false;
			  }		
		  }
	  }
	  ignoreSpace = true;
	  return true;
  }
  

  public boolean isValid(int i) {
	  
	  if( xpaths[i].isEmpty()) return false;
	  
	  xpath = xpaths[i];
	  nextPos = 0;
	  try {
		  nextsym();
		  axis();
		  nextsym();
		  step();
		  sym = 0;
		  nextPos = 0;
		  return true;
		  
	  } catch(XPathException e) {
//		  e.printStackTrace();
	  }

	  sym = 0;
	  nextPos = 0;
	  
	  return false;
  }
  
  private void axis() throws XPathException {
	  if(sym == '/' && nextPos > 0) {
		return;  
	  } 
	  throw new XPathException("Axis : " + sym);
  }
  

  private void step() throws XPathException {
	  nodename();
	  if(nextPos > 0) {
		  while(sym == '[') {
			  test();
			  logger.debug(String.format(" NextPos = %d | CurrentSym = %c", nextPos, sym));
			  if (sym != ']') throw new XPathException("Test in Step : " + sym);
			  nextsym();
		  }
		  logger.debug(String.format(" NextPos = %d | CurrentSym = %c", nextPos, sym));
		  if(nextPos > 0 && sym !=']') {
			  axis();
			  nextsym();
			  step();
		  }
	  }
  }
   
  
  private void nodename() throws XPathException {
	  if(Character.isAlphabetic(sym)) {
		  while((Character.isAlphabetic(sym) || Character.isDigit(sym) || sym == '-' || sym == '_' || sym == '.') && (nextPos > 0) ){
			  nextsym();
		  }
		  return;
	  }
	  throw new XPathException("Nodename : didn't find an alphabet");
  }
  
    
  private void test() throws XPathException {
	  int nextPos_backup = nextPos;
	  if(nextwords(new String[] {"text","(",")","=","\""})) text();
	  else if(nextwords(new String[] {"contains", "(", "text","(",")",",","\""})) contains();
	  else if(nextwords(new String[] {"@"})) att();
	  else {
		  nextsym();
		  step();
	  }
  }

  private void text() throws XPathException {
	  nextsym();
	  while(sym!='"' && nextPos > 0) {
		  nextsym();
	  }
	  if(sym != '"') throw new XPathException("Text : unmatched quotes");
	  nextsym();
  }
  
  private void contains() throws XPathException {
	  text();
	  if(sym != ')') throw new XPathException("Contains : unmatched paranthesis");
	  nextsym();	  
  }
  
  private void att() throws XPathException {

	  nextsym();
	  while(sym!='=' && nextPos > 0) {
		  nextsym();
	  }
	  if(sym != '=') throw new XPathException("Att : didn't find equality");
	  nextsym();
	  if(sym != '"') throw new XPathException("Att : didn't find starting quote");
	  text();
  }
  
  
  
  
  
  
  public boolean[] evaluate(Document d) { 
	  if(d == null) return null;
	  
	  boolean[] evaluation = new boolean[xpaths.length];
	  for(int i = 0; i < xpaths.length; ++i) {
		  evaluation[i] = false;
		  if(isValid(i)) {
			  logger.debug(String.format("Evaluating xpath %s", xpaths[i]));
			  evaluation[i] = evaluate((xpaths[i].indexOf('/') == 0)?xpaths[i].substring(1):xpaths[i], d.getDocumentElement());
		  }
	  }
	  return evaluation;
  }



  
  private boolean evaluate(String path, Node node) {
	  String step = "";
	  String nextStep = "";
	  
	  if(path != null) {
		  if(path.length() > 0) {
			  int i = 0;
			  boolean isquote = false;
			  for(; i < path.length(); ++i) {
				  if(path.charAt(i) == '/' && !isquote) {
					  break;
				  }else if (path.charAt(i) == '"' && path.charAt(i-1) != '\\') {
					  isquote = !isquote;
				  }
			  }
			  step = path.substring(0, i);
			  nextStep = (i < path.length())?path.substring(i + 1):"";
		  }
	  }
	  
	  logger.debug(String.format("\t Current Node = %s | Step = %s | nextStep = %s", node.getNodeName(), step, nextStep));
	  
	  // Extract out tests if any
	  ArrayList<String> tests = new ArrayList<String>();
	  if(step.indexOf('[') > 0) {
		  int leftParanthesisCounter = 0;
		  StringBuilder sb = new StringBuilder();
		  for(int i = 0; i < path.length(); ++i) {
			  if(path.charAt(i) != '[' && leftParanthesisCounter == 0) continue;
			  switch(path.charAt(i)) {
			  case '[' :
				  if(leftParanthesisCounter++ > 0)sb.append(path.charAt(i));break;
			  case ']' :
				  if(--leftParanthesisCounter == 0) {
					  tests.add(sb.toString());
//					  logger.debug("\t\t\t" + sb.toString());
					  sb = new StringBuilder();
					  break;
				  }
			  default : sb.append(path.charAt(i));
			  }
		  }
		  // remove tests from step
		  step = step.substring(0, step.indexOf('[')).trim();
	  }
	  
	  logger.debug(String.format(" \t Tests : %s | step = %s", String.join("|", tests), step));
	  
	  
	  if(!step.isEmpty()) {
		  // check if current step matches
		  if(node.getNodeName().equals(step)) {
			  int numTestsPassed = 0;
			  if(tests.size() > 0) { // Check for all tests
				  for(String test : tests) {
					  logger.debug(String.format(" \t Performing Test %s", test));
					  if(test.replaceAll(" ", "").startsWith("text()=\"")) { // text test
						  if(node.getTextContent() != null) {
							  logger.debug(String.format("\t\t Testing text() equality %s : %s", test.substring(test.indexOf("\"") + 1, test.lastIndexOf("\"")).trim(), node.getTextContent().trim()));
							  if(node.getTextContent().trim().equals(test.substring(test.indexOf("\"") + 1, test.lastIndexOf("\"")).trim())) {numTestsPassed++;logger.debug(String.format("\t\t Passed test text() : %s", test));}
						  }
						  
					  } else if(test.replaceAll(" ", "").startsWith("contains(text(),\"")){ // contains test
						  if(node.getTextContent() != null) {
							  logger.debug(String.format("\t\t Testing contains() %s : %s", test.substring(test.indexOf("\"") + 1, test.lastIndexOf("\"")).trim(), node.getTextContent().trim()));
							  if(node.getTextContent().trim().contains(test.substring(test.indexOf("\"") + 1, test.lastIndexOf("\"")).trim())) {numTestsPassed++;logger.debug(String.format("\t\t Passed test contain() : %s", test));}
						  }
						  
					  } else if(test.replaceAll(" ", "").startsWith("@")) { // attribute test
						  String attname = 	test.substring(test.indexOf("@") + 1, test.lastIndexOf("=")).trim();
						  if(node.getAttributes().getNamedItem(attname) != null) {
							  logger.debug(String.format("\t\t Testing attr() equality %s : %s", test.substring(test.indexOf("@") + 1, test.lastIndexOf("=")).trim(), node.getAttributes().getNamedItem(attname).getNodeValue().trim()));
							  if(node.getAttributes().getNamedItem(attname).getNodeValue().trim().equals(test.substring(test.indexOf("\"") + 1, test.lastIndexOf("\"")).trim())) {numTestsPassed++;logger.debug(String.format("\t\t Passed test attr() : %s", test));}
						  }						  
						  
					  } else { // step test; true if any child matches
							  NodeList children = node.getChildNodes();
							  for(int i =0; i < children.getLength(); ++i) {
								  if(evaluate(test, children.item(i))) {
									  numTestsPassed++;
									  logger.debug(String.format("\t\t Passed test step : %s", test));
									  break;
								  }
							  }
					  }
				  }
			  }
			  if(numTestsPassed == tests.size()) { // tests were successful
				  logger.debug("\t Passed tests");
				  if(nextStep.isEmpty()) return true;
				  NodeList children = node.getChildNodes();
				  for(int i =0; i < children.getLength(); ++i) {
					  logger.debug(String.format("\t\t\t Child : %s", children.item(i).getNodeName()));
					  // recursively check for next step
					  if(evaluate(nextStep, children.item(i))) {
						  logger.debug(String.format("\t\t Found success at Child : %s", children.item(i).getNodeName()));
						  return true;
					  }
				  }
			  }
		  }
		  return false;
	  }
	  
	  
	  return true;
  }
	
  
  
@Override
public boolean isSAX() {
	// TODO Auto-generated method stub
	return false;
}

@Override
public boolean[] evaluateSAX(InputStream document, DefaultHandler handler) {
	// TODO Auto-generated method stub
	return null;
}
        
}
