
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


/**
 * Container for set of DefDesc entries (WMO table B).
 * See DefDesc for more info.
 *
 * @author S. Sullivan
 */

class TableDesc {

static class TestCase {
  int fxy;
  int scale;
  int reference;
  int bitWidth;
  String description;
  String unit;

  TestCase( String typeName, int fval,
    int xval, int yval, int scale, int reference,
    int bitWidth, String description, String unit)
  throws BufrException
  {
    try { this.fxy = BufrUtil.getFxy( fval, xval, yval); }
    catch( BufrException exc) {
      BufrUtil.prtlnexc("caught", exc);
      badparms("invalid f, x, y");
    }
    this.scale = scale;
    this.reference = reference;
    this.bitWidth = bitWidth;
    this.description = description;
    this.unit = unit;
  }

  public String toString() {
    String res = "fxy: " + BufrUtil.formatFxy( fxy)
      + "  desc: \"" + description + "\""
      + "  scale: " + scale
      + "  reference: " + reference
      + "  width: " + bitWidth
      + "  unit: \"" + unit + "\"";
    return res;
  }
}  // end inner class TestCase




int versionMajor = -1;
int versionMinor = -1;
String infile;              // used only for error messages

// Defined entries: int fxy -> DefDesc
HashMap< Integer, DefDesc> defMap = new HashMap< Integer, DefDesc>();

int bugs = 0;




TableDesc(
  int bugs)
{
  this.bugs = bugs;
}




int size() {
  return defMap.size();
}




void addDef(
  boolean allowDups,
  DefDesc def,
  TextReader rdr)
throws BufrException
{
  DefDesc oldval = defMap.put( new Integer( def.fxy), def);
  if (oldval != null && ! allowDups) {
    if (rdr == null) throwerr("duplicate def");
    else rdr.throwfmt("duplicate def");
  }
}




void merge(
  TableDesc table)
throws BufrException
{
  Integer[] keys = table.defMap.keySet().toArray( new Integer[0]);
  Arrays.sort( keys);
  for (Integer key : keys) {
    DefDesc oldval = defMap.get( key);
    DefDesc newval = table.defMap.get( key);
    if (oldval != null) prtln("Warning: old DefDesc: " + oldval + "\n"
      + "  is being replaced by new DefDesc: " + newval + "\n");
    defMap.put( key, newval);
  }
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

  while (true) {
    rdr.readLine();
    if (rdr.atEof) break;

    int fval = rdr.getInt();
    int xval = rdr.getInt();
    int yval = rdr.getInt();
    int fxy = BufrUtil.getFxy( fval, xval, yval);

    String typeStg = rdr.getString();
    DefDesc def = new DefDesc( fxy);
    if (typeStg.equals("reserved")) def.isReserved = true;
    else if (typeStg.equals("string")) def.isString = true;
    else if (typeStg.equals("numeric")) def.isNumeric = true;
    else if (typeStg.equals("code")) def.isCode = true;
    else if (typeStg.equals("flag")) def.isBitFlag = true;
    else rdr.throwfmt("unknown typeStg: \"" + typeStg + "\"");

    if (! typeStg.equals("reserved")) {
      def.scale = rdr.getInt();
      def.reference = rdr.getInt();
      def.bitWidth = rdr.getInt();
      def.description = rdr.getString();
      def.unit = rdr.getString();
    }
    if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

    addDef( allowDups, def, rdr);
  }
  rdr.close();
}


public String toString() {
  StringBuffer sbuf = new StringBuffer();
  sbuf.append("size: " + defMap.size() + "\n");
  Integer[] keys = defMap.keySet().toArray( new Integer[0]);
  Arrays.sort( keys);
  for (Integer key : keys) {
    sbuf.append(defMap.get(key) + "\n");
  }
  return sbuf.toString();
}






static void badparms( String msg) {
  prtln("Error: " + msg);
  prtln("Parms:  bugs  tableFileName");
  System.exit(1);
}


public static void main( String[] args) {
  if (args.length != 2) badparms("wrong num parms");
  int iarg = 0;
  int bugs = Integer.parseInt( args[iarg++]);
  String fname = args[iarg++];
  try {
    boolean allowDups = false;
    TableDesc table = new TableDesc( bugs);
    table.read( allowDups, fname);
    if (bugs >= 5) prtln("main: table: " + table);

    TestCase[] tests = {
      //                         Expected ...
      //            f  x   y     scale  ref  width
      //  Desc       Unit
      new TestCase( "valueNumeric", 0, 13,   6,  -1,  -40,  16,
        "Mixing heights", "M"),
      new TestCase( "UNKNOWN",      0, 13, 150,   0,    0,   0,
        null,             null),
    };

    for (TestCase test : tests) {
      DefDesc def = table.defMap.get( new Integer( test.fxy));
      if (bugs >= 1) {
        prtln("");
        prtln("test:   " + test);
        prtln("actual: " + def);
      }
      if (test.description == null) {
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
          if (test.fxy != def.fxy
            || test.scale != def.scale
            || test.reference != def.reference
            || test.bitWidth != def.bitWidth
            || ( ! test.description.equals( def.description))
            || ( ! test.unit.equals( def.unit)))
          {
            prtln("mismatch: fxy: " + BufrUtil.formatFxy( test.fxy));
            prtln("  expected: " + test);
            prtln("  actual:   " + def);
            throwerr("main: test mismatch");
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



DefDesc getDefCopy(
  int fxy)
throws BufrException
{
  DefDesc res = defMap.get( new Integer( fxy));
  if (res != null) res = res.cloneDef();
  return res;
}




static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("TableDesc: " + msg);
}



static void prtln( String msg) {
  System.out.println( msg);
}


} // end class
