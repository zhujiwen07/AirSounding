
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
 * Container for set of DefCateg entries (WMO table A).
 * See DefCateg for more info.
 *
 * @author S. Sullivan
 */

class TableCateg {

static class TestCase {
  int categNum;
  int categType;
  String description;

  TestCase( int categNum, String typeName, String description)
  throws BufrException
  {
    this.categNum = categNum;

    if (typeName.equals("standard"))
      this.categType = DefCateg.CATEG_STANDARD;
    else if (typeName.equals("reserved"))
      this.categType = DefCateg.CATEG_RESERVED;
    else throwerr("unknown type name: \"" + typeName + "\"");

    this.description = description;
  }

  public String toString() {
    String res = "tp: " + DefCateg.categNames[ categType]
      + "  categNum: " + categNum
      + "  desc: \"" + description + "\"";
    return res;
  }
}  // end inner class TestCase



int versionMajor = -1;
int versionMinor = -1;
String infile;              // used only for error messages

// Defined entries: int categNum -> DefCateg
HashMap< Integer, DefCateg> defMap = new HashMap< Integer, DefCateg>();

int bugs = 0;




TableCateg(
  int bugs)
{
  this.bugs = bugs;
}




int size() {
  return defMap.size();
}




void addDef(
  boolean allowDups,
  DefCateg def,
  TextReader rdr)
throws BufrException
{
  DefCateg oldval = defMap.put( new Integer( def.categNum), def);
  if (oldval != null && ! allowDups) {
    if (rdr == null) throwerr("duplicate def");
    else rdr.throwfmt("duplicate def");
  }
}




void merge(
  TableCateg table)
throws BufrException
{
  defMap.putAll( table.defMap);
}








void read(
  boolean allowDups,
  String fname)
throws BufrException
{
  infile = fname;
  TextReader rdr = new TextReader( bugs, infile);

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

    int keyLo = rdr.getInt();
    int keyHi = rdr.getInt();

    String typeStg = rdr.getString();
    int tp = DefCateg.CATEG_UNKNOWN;
    if (typeStg.equals("standard")) tp = DefCateg.CATEG_STANDARD;
    else if (typeStg.equals("reserved")) tp = DefCateg.CATEG_RESERVED;
    else rdr.throwfmt("unknown type");

    String desc = rdr.getString();
    if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

    for (int ii = keyLo; ii <= keyHi; ii++) {
      DefCateg def = new DefCateg( tp, ii, desc);
      addDef( allowDups, def, rdr);
    }
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



// Returns null if not found.

DefCateg getDefCopy(
  int categNum)
throws BufrException
{
  DefCateg res = defMap.get( new Integer( categNum));
  if (res != null) res = res.cloneDef();
  return res;
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
    TableCateg table = new TableCateg( bugs);
    table.read( allowDups, fname);
    if (bugs >= 5) prtln("main: table: " + table);

    TestCase[] tests = {
      //                         Expected ...
      //       categNum  categType  description
      new TestCase( 0,  "standard", "Surface data - land"),
      new TestCase( 25, "reserved", "Reserved"),
    };

    for (TestCase test : tests) {
      DefCateg def = table.getDefCopy( test.categNum);
      if (bugs >= 1) {
        prtln("");
        prtln("test:   " + test);
        prtln("actual: " + def);
      }
      if (test.description == null) {
        if (def != null) {
          prtln("mismatch: categNum: " + test.categNum);
          prtln("  expected: null");
          prtln("  actual:   " + def);
          throwerr("main: test mismatch");
        }
      }
      else {
        if (def == null) {
          prtln("mismatch: categNum: " + test.categNum);
          prtln("  expected: " + test);
          prtln("  actual:   null");
          throwerr("main: test mismatch");
        }
        else {
          if (test.categNum != def.categNum
            ||  test.categType != def.categType
            || ( ! test.description.equals( def.description)))
          {
            prtln("mismatch: categNum: " + test.categNum);
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




static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("TableCateg: " + msg);
}



static void prtln( String msg) {
  System.out.println( msg);
}


} // end class
