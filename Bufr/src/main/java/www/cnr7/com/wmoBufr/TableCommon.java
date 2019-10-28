
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
 * Container for set of DefCommon entries (WMO common codes).
 * See DefCommon for more info.
 *
 * @author S. Sullivan
 */

class TableCommon {

static class TestCase {
  int tableNum;
  int ikey;
  int tstatus;
  String value;

  TestCase( int tableNum, int ikey, int tstatus, String value)
  {
    this.tableNum = tableNum;
    this.ikey = ikey;
    this.tstatus = tstatus;
    this.value = value;
  }

  public String toString() {
    String res = "tableNum: " + tableNum
      + "  ikey: " + ikey
      + "  tstatus: " + BufrValue.bstatusNames[tstatus]
      + "  value: \"" + value + "\"";
    return res;
  }
}  // end inner class TestCase



int versionMajor = -1;
int versionMinor = -1;
String infile;              // used only for error messages

String sectionSentinel = "commonCodeTable:";

// Defined entries: int tableNum -> DefCommon
HashMap< Integer, DefCommon> defMap = new HashMap< Integer, DefCommon>();

int bugs = 0;




TableCommon(
  int bugs)
{
  this.bugs = bugs;
}



int size() {
  return defMap.size();
}




void merge(
  TableCommon table)
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
    DefCommon section = parseSection( bugs, allowDups, rdr);
    DefCommon oldval = defMap.put( new Integer( section.tableNum), section);
    if (oldval != null && ! allowDups) rdr.throwfmt("duplicate section");
  }
  rdr.close();
}


DefCommon parseSection(
  int bugs,
  boolean allowDups,
  TextReader rdr)
throws BufrException
{
  String sentinel = rdr.getString();
  if (! sentinel.equals( sectionSentinel))
    rdr.throwfmt("invalid section start");
  DefCommon section = new DefCommon();
  section.tableNum = rdr.getInt();
  section.title = rdr.getString();
  if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");
  if (bugs >= 5) prtln("parseSection: start section: " + section);

  // Get the rows defining the sequence
  LinkedList<DefCommonRow> rowList = new LinkedList<DefCommonRow>();
  while (true) {
    rdr.readLine();
    if (rdr.atEof) break;
    sentinel = rdr.getString();
    if (sentinel.equals(sectionSentinel)) {
      rdr.setObjNum(0);     // reset to start at curObjs
      break;
    }
    DefCommonRow row = new DefCommonRow();

    if (! sentinel.equals("keyLo:")) rdr.throwfmt("invalid row");
    row.keyLo = rdr.getInt();

    sentinel = rdr.getString();
    if (! sentinel.equals("keyHi:")) rdr.throwfmt("invalid row");
    row.keyHi = rdr.getInt();

    sentinel = rdr.getString();
    if (! sentinel.equals("rowType:")) rdr.throwfmt("invalid row");
    String type = rdr.getString();
    if (type.equals("missing"))
      row.rowType = DefCommonRow.CMRTP_MISSING;
    else if (type.equals("reserved"))
      row.rowType = DefCommonRow.CMRTP_RESERVED;
    else if (type.equals("standard"))
      row.rowType = DefCommonRow.CMRTP_STANDARD;
    else rdr.throwfmt("unknown type");

    if (row.rowType == DefCommonRow.CMRTP_STANDARD) {
      sentinel = rdr.getString();
      if (! sentinel.equals("desc:")) rdr.throwfmt("invalid row");
      row.description = rdr.getString();
    }
    if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

    // Insure the row's key range doesn't overlap a previous row
    if (! allowDups) {
      for (DefCommonRow trow : rowList) {
        if (trow.keyHi >= row.keyLo && trow.keyLo <= row.keyHi)
          rdr.throwfmt("duplicate key range in row");
      }
    }

    // Finally, add the row
    rowList.add( row);
    if (bugs >= 5) prtln("parseSection: got row: " + row);
  }
  section.rows = rowList.toArray( new DefCommonRow[0]);
  return section;
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










StatusValue getStatusValue(
  int tableNum,
  int ikey)
throws BufrException
{
  StatusValue sv = new StatusValue( BufrValue.BST_OK, "");  // returned value
  DefCommon comdef = defMap.get( new Integer( tableNum));
  if (comdef == null) sv.sstatus = BufrValue.BST_UNKNOWN;
  else sv = comdef.getStatusValue( ikey);
  return sv;
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
    TableCommon table = new TableCommon( bugs);
    table.read( allowDups, fname);
    if (bugs >= 5) prtln("main: table: " + table);

    TestCase[] tests = {
      //            tableNum  ikey  Expected status, value
      new TestCase(    1,     88,   BufrValue.BST_OK, "Oslo"),
      new TestCase(    2,     16,   BufrValue.BST_OK, "Elin (Austria)"),
      new TestCase(    4,      5,   BufrValue.BST_OK, "Sippican MK-12"),
      new TestCase(    5,    120,   BufrValue.BST_OK, "ADEOS"),
      new TestCase(    7,     12,   BufrValue.BST_RESERVED, ""),
      new TestCase(    7,     26,   BufrValue.BST_RESERVED, ""),
      new TestCase(    7,     59,   BufrValue.BST_OK, "Other problems"),
      new TestCase(   11,     36,   BufrValue.BST_OK, "Bangkok"),
      new TestCase(    1,    999,   BufrValue.BST_UNKNOWN, ""), // no such key
      new TestCase(   99,      1,   BufrValue.BST_UNKNOWN, ""), // no such table
    };

    for (TestCase test : tests) {
      BufrValue bvalue = new BufrValue( new DefDesc( 0));
      StatusValue sv = table.getStatusValue( test.tableNum, test.ikey);
      if (bugs >= 1) {
        prtln("");
        prtln("  expected: " + test);
        prtln("  actual: " + BufrValue.bstatusNames[ sv.sstatus]
          + "  \"" + sv.value + "\"");
      }
      if (sv.sstatus != test.tstatus || ! sv.value.equals( test.value))
        throwerr("main: test mismatch");
    }
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
  throw new BufrException("TableCommon: " + msg);
}




static void prtln( String msg) {
  System.out.println( msg);
}


} // end class
