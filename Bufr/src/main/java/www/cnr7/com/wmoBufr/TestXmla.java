
package www.cnr7.com.wmoBufr;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class TestXmla {


static void badparms( String msg) {
  prtln("Error: " + msg);
  prtln("Parms:");
  prtln("  -infile   <stg>");
  System.exit(1);
}


public static void main( String[] args) {
  try { runit( args); }
  catch( Exception exc) {
    prtln("caught: " + exc);
    exc.printStackTrace();
    System.exit(1);
  }
}



static void runit( String[] args)
throws Exception
{

  String infile = null;

  if (args.length % 2 != 0) badparms("parms must be key/value pairs");
  for (int iarg = 0; iarg < args.length; iarg += 2) {
    String key = args[iarg];
    String val = args[iarg+1];
    if (key.equals("-infile")) infile = val;
    else badparms("unknown parm: " + key);
  }

  if (infile == null) badparms("parm not specified: -infile");
  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  DocumentBuilder parser = factory.newDocumentBuilder();
  File fin = new File( infile);
  Document doc = parser.parse( fin);

  prtln( "doc: " + formatNode(doc));
  StringBuilder sbuf = new StringBuilder();
  formatXmlTree( 0, doc, sbuf);
  prtln("tree: " + sbuf.toString());
}


static void formatXmlTree( int indent, Node node, StringBuilder sbuf) {
  for (int ii = 0; ii < indent; ii++) {
    sbuf.append( "  ");
  }
  sbuf.append( formatNode( node));
  sbuf.append( "\n");
  Node subNode = node.getFirstChild();
  while (subNode != null) {
    formatXmlTree( indent+1, subNode, sbuf);
    subNode = subNode.getNextSibling();
  }
}



static String formatNode( Node node) {
  int itp = node.getNodeType();
  String res = "type: ";
  if (itp == Node.ATTRIBUTE_NODE)
    res += "ATTRIBUTE_NODE";
  else if (itp == Node.CDATA_SECTION_NODE)
    res += "CDATA_SECTION_NODE";
  else if (itp == Node.COMMENT_NODE)
    res += "COMMENT_NODE";
  else if (itp == Node.DOCUMENT_FRAGMENT_NODE)
    res += "DOCUMENT_FRAGMENT_NODE";
  else if (itp == Node.DOCUMENT_NODE)
    res += "DOCUMENT_NODE";
  else if (itp == Node.DOCUMENT_POSITION_CONTAINED_BY)
    res += "DOCUMENT_POSITION_CONTAINED_BY";
  else if (itp == Node.DOCUMENT_POSITION_CONTAINS)
    res += "DOCUMENT_POSITION_CONTAINS";
  else if (itp == Node.DOCUMENT_POSITION_DISCONNECTED)
    res += "DOCUMENT_POSITION_DISCONNECTED";
  else if (itp == Node.DOCUMENT_POSITION_FOLLOWING)
    res += "DOCUMENT_POSITION_FOLLOWING";
  else if (itp == Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC)
    res += "DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC";
  else if (itp == Node.DOCUMENT_POSITION_PRECEDING)
    res += "DOCUMENT_POSITION_PRECEDING";
  else if (itp == Node.DOCUMENT_TYPE_NODE)
    res += "DOCUMENT_TYPE_NODE";
  else if (itp == Node.ELEMENT_NODE)
    res += "ELEMENT_NODE";
  else if (itp == Node.ENTITY_NODE)
    res += "ENTITY_NODE";
  else if (itp == Node.ENTITY_REFERENCE_NODE)
    res += "ENTITY_REFERENCE_NODE";
  else if (itp == Node.NOTATION_NODE)
    res += "NOTATION_NODE";
  else if (itp == Node.PROCESSING_INSTRUCTION_NODE)
    res += "PROCESSING_INSTRUCTION_NODE";
  else if (itp == Node.TEXT_NODE)
    res += "TEXT_NODE";
  else res += "UNKNOWN TYPE";

  res += "  name: " + node.getNodeName();
  String value = node.getNodeValue();
  if (value != null) res += "  value.trim(): \"" + value.trim() + "\"";
  NamedNodeMap nmap = node.getAttributes();
  if (nmap != null) {
    for (int ii = 0; ii < nmap.getLength(); ii++) {
      Node attr = nmap.item(ii);
      res += "  attr: \"" + attr.getNodeName()
        + "\" \"" + attr.getNodeValue() + "\"";
    }
  }
  return res;
}




static void prtln( String msg) {
  System.out.println( msg);
}

} // end class
