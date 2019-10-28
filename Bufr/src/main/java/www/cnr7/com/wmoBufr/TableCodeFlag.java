
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
 * Container for set of DefCodeFlag entries (codes and flags
 * associated with WMO table B entries).
 * See DefCodeFlag for more info.
 *
 * @author S. Sullivan
 */

class TableCodeFlag {

static class TestCase {
  int fxy;
  boolean bitFlag;
  int bitWidth;
  int ikey;
  int tstatus;
  String value;

  TestCase( int fval, int xval, int yval,
    int ibitFlag, int bitWidth, int ikey,
    int tstatus, String value)
  {
    try { this.fxy = BufrUtil.getFxy( fval, xval, yval); }
    catch( BufrException exc) {
      BufrUtil.prtlnexc("caught", exc);
      badparms("invalid f, x, y");
    }
    if (ibitFlag == 0) this.bitFlag = false;
    else this.bitFlag = true;
    this.bitWidth = bitWidth;
    this.ikey = ikey;
    this.tstatus = tstatus;
    this.value = value;
  }

  public String toString() {
    String res = String.format(
      "fxy: %s  bitWidth: %d  ikey: %d = 0x%x  tstatus: %s  value: \"%s\"",
      BufrUtil.formatFxy( fxy),
      bitWidth, ikey, ikey,
      BufrValue.bstatusNames[tstatus],
      value);
    return res;
  }
}  // end inner class TestCase



int versionMajor = -1;
int versionMinor = -1;
String infile;              // used only for error messages

String sectionSentinel = "sectionFxy:";

// Defined entries: int fxy -> DefCodeFlag
HashMap< Integer, DefCodeFlag> defMap = new HashMap< Integer, DefCodeFlag>();

int bugs = 0;





TableCodeFlag(
  int bugs)
{
  this.bugs = bugs;
}




int size() {
  return defMap.size();
}



void merge(
  TableCodeFlag table)
throws BufrException
{
  Integer[] keys = table.defMap.keySet().toArray( new Integer[0]);
  Arrays.sort( keys);
  for (Integer key : keys) {
    DefCodeFlag oldval = defMap.get( key);
    DefCodeFlag newval = table.defMap.get( key);
    if (oldval == null) defMap.put( key, newval);
    else oldval.merge( bugs, newval);
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

  rdr.readLine();
  while ( ! rdr.atEof) {
    DefCodeFlag section = parseSection( bugs, allowDups, rdr);
    DefCodeFlag oldval = defMap.put( new Integer( section.fxy), section);
    if (oldval != null && ! allowDups) rdr.throwfmt("duplicate section");
  }
  rdr.close();
}



DefCodeFlag parseSection(
  int bugs,
  boolean allowDups,
  TextReader rdr)
throws BufrException
{
  String sentinel = rdr.getString();
  if (! sentinel.equals( sectionSentinel))
    rdr.throwfmt("invalid section start");
  DefCodeFlag section = new DefCodeFlag();
  int fval = rdr.getInt();
  int xval = rdr.getInt();
  int yval = rdr.getInt();
  section.fxy = BufrUtil.getFxy( fval, xval, yval);

  sentinel = rdr.getString();
  if (! sentinel.equals("type:")) rdr.throwfmt("invalid row");
  String type = rdr.getString();
  if (type.equals("bitflag"))
    section.cfType = DefCodeFlag.CFTP_BITFLAG;
  else if (type.equals("codevalue"))
    section.cfType = DefCodeFlag.CFTP_CODEVALUE;
  else if (type.equals("referral"))
    section.cfType = DefCodeFlag.CFTP_REFERRAL;
  else rdr.throwfmt("unknown type");

  if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");
  if (bugs >= 5) prtln("parseSection: start section: " + section);

  if (section.cfType == DefCodeFlag.CFTP_BITFLAG
    || section.cfType == DefCodeFlag.CFTP_CODEVALUE)
  {
    parseStandardSection( bugs, allowDups, section, rdr);
  }
  else if (section.cfType == DefCodeFlag.CFTP_REFERRAL) {
    parseReferralSection( bugs, section, rdr);
  }
  else rdr.throwfmt("unknown type");

  return section;
}



void parseStandardSection(
  int bugs,
  boolean allowDups,
  DefCodeFlag section,
  TextReader rdr)
throws BufrException
{
  // Get the rows defining the sequence
  LinkedList<DefCodeFlagRow> rowList = new LinkedList<DefCodeFlagRow>();
  while (true) {
    rdr.readLine();
    if (rdr.atEof) break;
    String sentinel = rdr.getString();
    if (sentinel.equals(sectionSentinel)) {
      rdr.setObjNum(0);     // reset to start at curObjs
      break;
    }
    DefCodeFlagRow row = new DefCodeFlagRow();

    if (! sentinel.equals("keyLo:")) rdr.throwfmt("invalid row");
    row.keyLo = rdr.getInt();

    sentinel = rdr.getString();
    if (! sentinel.equals("keyHi:")) rdr.throwfmt("invalid row");
    row.keyHi = rdr.getInt();

    sentinel = rdr.getString();
    if (! sentinel.equals("rowType:")) rdr.throwfmt("invalid row");
    String type = rdr.getString();
    if (type.equals("missing"))
      row.rowType = DefCodeFlagRow.CFRTP_MISSING;
    else if (type.equals("reserved"))
      row.rowType = DefCodeFlagRow.CFRTP_RESERVED;
    else if (type.equals("standard"))
      row.rowType = DefCodeFlagRow.CFRTP_STANDARD;
    else rdr.throwfmt("unknown type");

    if (row.rowType == DefCodeFlagRow.CFRTP_STANDARD) {
      sentinel = rdr.getString();
      if (! sentinel.equals("desc:")) rdr.throwfmt("invalid row");
      row.description = rdr.getString();
    }
    if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");

    // Do not add BITFLAG missing values to the rowList, since
    // for BITFLAG fields the missing values are denoted by,
    // for example,
    //    keyLo: -4  keyHi: -4  rowType: missing
    // where the negative num means "all 4 bits set".
    // For BITFLAG fields, use the bitFlagMissingValue to test for missing.

    if (section.cfType == DefCodeFlag.CFTP_BITFLAG && row.keyLo < 0) {
      // The keyLo==keyHi value is negative, for example -4
      // means if "all 4" bits are set, it's a missing value.
      if (row.keyLo != row.keyHi) throwerr("neg keyLo != keyHi");
      int numBits = -row.keyLo;
      section.bitMissingValue = BufrUtil.mkBitMask( numBits);
    }
    else {
      // Insure the row's key range doesn't overlap a previous row
      if (! allowDups) {
        for (DefCodeFlagRow trow : rowList) {
          if (trow.keyHi >= row.keyLo && trow.keyLo <= row.keyHi)
            rdr.throwfmt("duplicate key range in row");
        }
      }

      // Finally, add the row
      rowList.add( row);
    }

    if (bugs >= 5) prtln("parseSection: got row: " + row);
  }
  section.rows = rowList.toArray( new DefCodeFlagRow[0]);
}





void parseReferralSection(
  int bugs,
  DefCodeFlag section,
  TextReader rdr)
throws BufrException
{
  rdr.readLine();
  String sentinel = rdr.getString();
  if (! sentinel.equals("referralTable:")) rdr.throwfmt("invalid referral");
  section.referralTableNum = rdr.getInt();
  String unused = rdr.getString();    // referralNote
  if (! rdr.atLineEnd()) rdr.throwfmt("invalid line format");
  rdr.readLine();
}







StatusValue getStatusValue(
  int fxy,
  boolean bitFlag,
  int ikey,
  int bitWidth,
  TableCommon commonTable)
throws BufrException
{
  StatusValue sv = new StatusValue( BufrValue.BST_OK, "");  // returned value

  DefCodeFlag cfdef = defMap.get( new Integer( fxy));
  if (cfdef == null) sv.sstatus = BufrValue.BST_UNKNOWN;
  else if (cfdef.cfType == DefCodeFlag.CFTP_REFERRAL)
    sv = commonTable.getStatusValue( cfdef.referralTableNum, ikey);
  else sv = cfdef.getStatusValue( bitFlag, ikey, bitWidth);
  return sv;
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
  System.exit(1);
}


public static void main( String[] args) {
  if (args.length != 2) badparms("wrong num parms");
  int iarg = 0;
  int bugs = Integer.parseInt( args[iarg++]);
  String fname = args[iarg++];
  try {
    boolean allowDups = false;
    TableCodeFlag table = new TableCodeFlag( bugs);
    table.read( allowDups, fname);
    if (bugs >= 5) prtln("main: table: " + table);

    TestCase[] tests = {
      //                       bit bit
      //            f  x  y    flg wdth ikey   Expected status, value
      new TestCase( 0, 2, 143,  0,  7,  5,     BufrValue.BST_OK, "Ehmet"),
      new TestCase( 0, 2, 143,  0,  7,  30,    BufrValue.BST_RESERVED, ""),
      new TestCase( 0, 2, 143,  0,  7,  127,   BufrValue.BST_MISSING, ""),

      new TestCase( 0, 2, 159,  1,  8,  0x80,  BufrValue.BST_OK, "Temperature inconsistency"),
      new TestCase( 0, 2, 159,  1,  8,  0x40,  BufrValue.BST_MISSING, ""), 
      new TestCase( 0, 2, 159,  1,  8,  0x20,  BufrValue.BST_OK, "Redundancy channel"),
      new TestCase( 0, 2, 159,  1,  8,  0xc0,  BufrValue.BST_MISSING, ""),
      new TestCase( 0, 2, 159,  1,  8,  0xa0,  BufrValue.BST_OK, "Temperature inconsistency; Redundancy channel"),

      new TestCase( 0, 2, 159,  1,  8,  0x44,  BufrValue.BST_MISSING, ""),
      new TestCase( 0, 2, 159,  1,  8,  0x46,  BufrValue.BST_MISSING, ""),
      new TestCase( 0, 2, 159,  1,  8,  0xff,  BufrValue.BST_MISSING, ""),
      new TestCase( 0, 2, 159,  1,  8,  0x01,  BufrValue.BST_UNKNOWN, ""),  // bit 8 not defined
      new TestCase( 0, 2, 153,  1,  8,  1,     BufrValue.BST_UNKNOWN, ""),  // no such fxy
    };

    for (TestCase test : tests) {
      StatusValue sv = table.getStatusValue(
        test.fxy,
        test.bitFlag,
        test.ikey,
        test.bitWidth,
        null);        // commonTable
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
  throw new BufrException("TableCodeFlag: " + msg);
}





static void prtln( String msg) {
  System.out.println( msg);
}


} // end class


