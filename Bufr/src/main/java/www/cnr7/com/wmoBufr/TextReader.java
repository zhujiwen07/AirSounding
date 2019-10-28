
// Copyright (c) 2008, UCAR (University Corporation for Atmospheric Research)
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or
// without modification, are permitted provided that the following
// conditions are met:
//     * Redistributions of source code must retain the above
//       copyright notice, this list of conditions and the
//       following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the
//       following disclaimer in the documentation and/or other
//       materials provided with the distribution.
//     * Neither the name of the UCAR nor the names of its
//       contributors may be used to endorse or promote products
//       derived from this software without specific
//       prior written permission.
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
// CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
// BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
// 
// (This is the Simplified BSD License of 2008.)


package www.cnr7.com.wmoBufr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;



/**
 * Reads a file, splits each line into tokens,
 * and returns the tokens one by one as ints or Strings.
 *
 * @author S. Sullivan
 */

class TextReader {

int bugs = 0;
String fname = null;
BufferedReader rdr;

String curLine = null;
int lineNum = 0;
Object[] curObjs = null;
int objNum = 0;

boolean atEof = false;




TextReader( int bugs, String fname)
throws BufrException
{
  this.bugs = bugs;
  this.fname = fname;
  try {
    rdr = new BufferedReader( new FileReader( fname));
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("cannot open file \"" + fname + "\": " + exc);
  }
}



void close()
throws BufrException
{
  try {
    rdr.close();
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("cannot close file \"" + fname + "\": " + exc);
  }
}





void readLine()
throws BufrException
{
  if (atEof) throwerr("read past EOF");
  try {
    while (! atEof) {
      curLine = rdr.readLine();
      lineNum++;
      if (bugs >= 10)
        prtln("readLine: curLine: " + lineNum + "  \"" + curLine + "\"");
      if (curLine == null) atEof = true;
      else {
        curLine = curLine.trim();
        if (curLine.length() > 0 && ! curLine.startsWith("#")) {
          curObjs = splitString(
            true,           // allowInt
            true,           // allowDouble
            true,           // allowString
            true,           // allowQuote
            true,           // compressFlag
            curLine);       // string to be parsed
          objNum = 0;
          break;
        }
      }
    }
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwfmt("IO error");
  }
  if (atEof) curObjs = null;
}



void setObjNum( int num) {
  objNum = num;
}



boolean atLineEnd() {
  boolean res = false;
  if (objNum == curObjs.length) res = true;
  return res;
}




int getInt()
throws BufrException
{
  if (atEof) throwfmt("at EOF");
  if (objNum >= curObjs.length) throwfmt("line too short");
  if ( ! (curObjs[objNum] instanceof Integer)) throwfmt("invalid desc line");
  int ival = ((Integer) curObjs[objNum]).intValue();
  objNum++;
  return ival;
}





String getString()
throws BufrException
{
  if (atEof) throwfmt("at EOF");
  if (objNum >= curObjs.length) throwfmt("line too short");
  if ( ! (curObjs[objNum] instanceof String)) throwfmt("invalid desc line");
  String sval = (String) curObjs[objNum];
  objNum++;
  return sval;
}






// Parse a string of the form:
//     12 alpha 33.44 "beta b"
// To get an Object[] containing:
//     Integer(12), String("alpha"), Double(33.44), String("beta b")

Object[] splitString(
  boolean allowInt,       // allow int:               12
  boolean allowDouble,    // allow double:            1.23
  boolean allowString,    // allow unquoted string:   alpha
  boolean allowQuote,     // allow quoted string:     "beta b"
  boolean compressFlag,   // replace all whitespace with a single blank
  String stg)             // string to be parsed
throws BufrException
{
  LinkedList<Object> resList = new LinkedList<Object>();
  int ipos = 0;
  while (ipos < stg.length()) {
    // Skip whitespace
    while (ipos < stg.length() && Character.isWhitespace( stg.charAt(ipos))) {
      ipos++;
    }
    if (ipos >= stg.length()) break;

    // Try a quoted string
    if (allowQuote && stg.charAt(ipos) == '\"') {
      StringBuffer sbuf = new StringBuffer();
      // Scan for ending quote
      ipos++;
      while (ipos < stg.length()) {
        char cc = stg.charAt(ipos);
        if (cc == '\"') break;
        else if (cc == '\\') {
          ipos++;
          if (ipos >= stg.length()) throwfmt("No ending quote");
          cc = stg.charAt(ipos);
          if (cc == '\\') sbuf.append('\\');
          else if (cc == 'n') sbuf.append('\n');
          else if (cc == 'r') sbuf.append('\r');
          else if (cc == 't') sbuf.append('\t');
          else if (cc == '\"') sbuf.append('\"');
          else throwfmt("Invalid backslash code");
        }
        else sbuf.append(cc);
        ipos++;
      }
      ipos++;  // skip over ending quote
      String resStg = sbuf.toString();
      if (compressFlag) resStg = compressString( resStg);
      resList.add( resStg);
    }
    else {      // else not a quoted string

      // Skip non-whitespace
      int ilim = ipos + 1;
      while (ilim < stg.length()
        && ! Character.isWhitespace( stg.charAt(ilim))) {
        ilim++;
      }

      // Try an integer
      boolean okflag = false;
      if (allowInt && ! okflag) {
        try {
          int ival = Integer.parseInt( stg.substring( ipos, ilim), 10);
          resList.add( new Integer( ival));
          okflag = true;
        }
        catch( NumberFormatException exc) { okflag = false; }
      }
  
      // Try a double
      if (allowDouble && ! okflag) {
        try {
          double dval = Double.parseDouble( stg.substring( ipos, ilim));
          resList.add( new Double( dval));
          okflag = true;
        }
        catch( NumberFormatException exc) { okflag = false; }
      }

      // Try an unquoted string
      if (allowString && ! okflag) {
        String resStg = stg.substring( ipos, ilim);
        if (compressFlag) resStg = compressString( resStg);
        resList.add( resStg);
        okflag = true;
      }

      if (! okflag) throwfmt("unrecognized item in string");
      ipos = ilim;
    } // else not a quoted string
  } // while ipos < stg.len
  Object[] resvec = resList.toArray( new Object[0]);
  return resvec;
} // end splitString




// Replaces all whitespace with a single blank

String compressString( String stgparm)
{
  String stg = stgparm.trim();
  StringBuffer sbuf = new StringBuffer();

  char cprev = 'a';       // previous character
  for (int ii = 0; ii < stg.length(); ii++) {
    char cc = stg.charAt(ii);
    if (Character.isWhitespace(cc)) {
      if (! Character.isWhitespace(cprev)) sbuf.append(' ');
    }
    else sbuf.append(cc);
    cprev = cc;
  }
  String resStg = sbuf.toString();
  return resStg;
}








int parseInt(
  String msg,         // error message info
  String stg)         // string to be parsed
throws BufrException
{
  int ires = 0;
  try {
    ires = Integer.parseInt( stg, 10);
  }
  catch( NumberFormatException exc) {
    TextReader.prtlnexc("caught", exc);
    throwfmt("Invalid value for " + msg + ": \"" + stg + "\"");
  }
  return ires;
} // end parseInt






static void prtlnexc(
  String msg,
  Throwable exc)
{
  StringWriter swtr = new StringWriter();
  PrintWriter pwtr = new PrintWriter( swtr);
  exc.printStackTrace( pwtr);
  pwtr.close();
  prtln( msg + "<br><pre>\n  Exception: "
    + swtr.toString() + "\n</pre>\n");
}


static void prtln( String msg) {
  System.out.println( msg);
}



void throwfmt( String msgparm)
throws BufrException
{
  String msg = "\nError: " + msgparm + "\n"
    + "  file: \"" + fname + "\"\n"
    + "  line number: " + lineNum + "\n"
    + "  \"" + curLine + "\"";
  throwerr( msg);
}


static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("TextReader: " + msg);
}



} // end class
