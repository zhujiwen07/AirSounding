
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;


/**
 * Container for set of DefDesc entries (WMO table D),
 * each of which has DefDesc entries as subs.
 *
 * Each main DefDesc represents one entry in Table D.
 * The original document is:
 *   BUFR Table D - Lists of common sequences
 * available from:
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html
 *   select: Table D - BUFR. List of common sequences
 *
 * @author S. Sullivan
 */

class TableSeq {

static class TestCase {
  int fxy;
  int[] subFxys;    // fxy values
  TestCase( int fval, int xval, int yval, int[] subvals)
  {
    try { this.fxy = BufrUtil.getFxy( fval, xval, yval); }
    catch( BufrException exc) {
      BufrUtil.prtlnexc("caught", exc);
      badparms("invalid f, x, y");
    }
    subFxys = null;
    if (subvals != null) {
      if (subvals.length % 3 != 0)
        badparms("TestCase subvals len must be multiple of 3");
      this.subFxys = new int[ subvals.length / 3];
      for (int ii = 0; ii < subFxys.length; ii++) {
        try {
          subFxys[ii] = BufrUtil.getFxy(
            subvals[3*ii], subvals[3*ii+1], subvals[3*ii+2]);
        }
        catch( BufrException exc) {
          BufrUtil.prtlnexc("caught", exc);
          badparms("invalid f, x, y");
        }
      }
    }
  }
  public String toString() {
    String res = "fxy: " + BufrUtil.formatFxy( fxy)
      + "  subvals:\n";
    if (subFxys == null) res += "  subFxys: null";
    else {
      for (int ii = 0; ii < subFxys.length; ii++) {
        res += "  " + BufrUtil.formatFxy( subFxys[ii]) + "\n";
      }
    }
    return res;
  }
}  // end inner class TestCase




int versionMajor = -1;
int versionMinor = -1;
String infile;              // used only for error messages

String sectionSentinel = "defineFxy:";
String rowSentinel = "rowFxy:";

// Defined entries: int fxy -> DefDesc
HashMap< Integer, DefDesc> defMap = new HashMap< Integer, DefDesc>();

int bugs = 0;





TableSeq(
  int bugs)
{
  this.bugs = bugs;
}







int size() {
  return defMap.size();
}



void addSequence(
  boolean allowDups,
  DefDesc section,
  TextReader rdr)
throws BufrException
{
  DefDesc oldval = defMap.put( new Integer( section.fxy), section);
  if (oldval != null && ! allowDups) {
    if (rdr == null) throwerr("duplicate section: " + oldval);
    else rdr.throwfmt("duplicate section: " + oldval);
  }
}




void merge(
  TableSeq table)
throws BufrException
{
  defMap.putAll( table.defMap);
}






void read(
  boolean allowDups,
  String fname)
throws BufrException
{
  TextReader rdr = new TextReader( bugs, fname);

  // Get versionMajor, versionMinor
  rdr.readLine();
  if (rdr.atEof) rdr.throwfmt("file is empty");
  String tag = rdr.getString();
  if (! tag.equals("tableVersion")) rdr.throwfmt("tableVersion not found");
  versionMajor = rdr.getInt();
  versionMinor = rdr.getInt();
  if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

  rdr.readLine();
  while ( ! rdr.atEof) {
    DefDesc section = parseSection( bugs, rdr);
    addSequence( allowDups, section, rdr);
  }
  rdr.close();
}





DefDesc parseSection(
  int bugs,
  TextReader rdr)
throws BufrException
{
  String sentinel = rdr.getString();
  if (! sentinel.equals( sectionSentinel))
    rdr.throwfmt("invalid section start");
  int fval = rdr.getInt();
  int xval = rdr.getInt();
  int yval = rdr.getInt();
  DefDesc section = new DefDesc( fval, xval, yval);

  sentinel = rdr.getString();
  if (! sentinel.equals("title:")) rdr.throwfmt("invalid section start");
  section.description = rdr.getString();
  if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");
  if (bugs >= 5) prtln("parseSection: start section: " + section);

  // Get the rows defining the sequence
  LinkedList<DefDesc> rowList = new LinkedList<DefDesc>();
  while (true) {
    rdr.readLine();
    if (rdr.atEof) break;
    sentinel = rdr.getString();
    if (sentinel.equals(sectionSentinel)) {
      rdr.setObjNum(0);     // reset to start at curObjs
      break;
    }
    if (! sentinel.equals( rowSentinel)) rdr.throwfmt("invalid row");
    fval = rdr.getInt();
    xval = rdr.getInt();
    yval = rdr.getInt();
    DefDesc subVal = new DefDesc( fval, xval, yval);

    sentinel = rdr.getString();
    if (! sentinel.equals("desc:")) rdr.throwfmt("invalid row");
    subVal.description = rdr.getString();
    if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

    // We allow duplicate rows.  For example see 3 01 026.
    // Finally, add the row
    rowList.add( subVal);
    if (bugs >= 5) prtln("parseSection: got row: " + subVal);
  }
  section.subDefs = rowList.toArray( new DefDesc[0]);
  return section;
}




public String toString() {
  StringBuffer sbuf = new StringBuffer();
  sbuf.append("size: " + defMap.size() + "\n");
  Integer[] keys = defMap.keySet().toArray( new Integer[0]);
  Arrays.sort( keys);
  for (Integer key : keys) {
    sbuf.append( defMap.get(key) + "\n");
  }
  return sbuf.toString();
}



DefDesc getDefCopy(
  int fxy)
throws BufrException
{
  DefDesc res = defMap.get( new Integer( fxy));
  if (res != null) res = res.cloneDef();
  return res;
}





static void badparms( String msg) {
  prtln("Error: " + msg);
  System.exit(1);
}


public static void main( String[] args) {
  if (args.length != 2) badparms("wrong num parms");
  int iarg = 0;
  int bugs = Integer.parseInt( args[iarg++]);
  String fname = args[iarg++];
  try {
    boolean allowDups = false;
    TableSeq table = new TableSeq( bugs);
    table.read( allowDups, fname);
    if (bugs >= 5) prtln("main: table: " + table);


    TestCase[] tests = {
      //                       
      //            f  x   y   
      new TestCase( 3,  1, 49,
        new int[] {    // expected f x y rows
        //f  x   y
          0, 02, 111,
          0, 02, 112,
          0, 21,  62,
          0, 21,  63,
          0, 21,  65
        }),
      new TestCase( 3,  1, 50, null)    // fxy not found
    };

    for (TestCase test : tests) {
      DefDesc def = table.defMap.get( new Integer( test.fxy));
      if (bugs >= 1) {
        prtln("");
        prtln("test:   " + test);
        prtln("actual: " + def);
        if (def != null) {
          for (int isub = 0; isub < def.subDefs.length; isub++) {
            prtln("  sub: " + def.subDefs[isub]);
          }
        }
      }
      if (test.subFxys == null) {
        if (def != null) {
          prtln("mismatch: fxy: " + BufrUtil.formatFxy( test.fxy));
          prtln("  expected: null");
          prtln("  actual:   " + def);
          throwerr("main: test mismatch");
        }
      }
      else {
        if (def == null) {
          prtln("mismatch: fxy: " + BufrUtil.formatFxy( test.fxy));
          prtln("  expected: " + test);
          prtln("  actual:   null");
          throwerr("main: test mismatch");
        }
        else {
          if (test.subFxys.length != def.subDefs.length) {
            prtln("mismatch: fxy: " + BufrUtil.formatFxy( test.fxy));
            prtln("  expected: " + test);
            prtln("  actual:   " + def);
            throwerr("main: test mismatch");
          }
          else {
            for (int ii = 0; ii < test.subFxys.length; ii++) {
              if (test.subFxys[ii] != def.subDefs[ii].fxy) {
                prtln("mismatch: fxy: " + BufrUtil.formatFxy( test.fxy));
                prtln("  expected: " + test);
                prtln("  actual:   " + def);
                throwerr("main: test mismatch");
              }
            }
          }
        }
      }
    } // for test : tests
  }
  catch( BufrException exc) {
    BufrUtil.prtlnexc("caught", exc);
    prtln("main: caught: " + exc);
    System.exit(1);
  }
}





static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("TableSeq: " + msg);
}



static void prtln( String msg) {
  System.out.println( msg);
}


} // end class
