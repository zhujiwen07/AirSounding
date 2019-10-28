
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
import java.util.Formatter;



/**
 * Represents one message within a file of BUFR messages.
 * <p>
 * For the command line XML converter, see {@link BufrFile BufrFile}.
 * <p>
 * For overall documentation on BUFR formats and tables see
 * the {@link wmoBufr wmoBufr} overview.: <br>
 * <p>
 * <pre>
 *
 * Iteration and Sequence structure
 * 
 * BUFR files are a concatenation of BUFR messages.
 * There may be junk before, between, and after the individual
 * BUFR messages.  We simply scan for the "BUFR" sentinel
 * of section 0, denoting the start of a BUFR message.
 * 
 * BUFR messages contain two parts:
 *   descriptors
 *   data
 * The descriptors, also known as fxy values,
 * are 16 bits and have the form:
 *   fval   2 bits
 *   xval   6 bits
 *   yval   8 bits
 * The fval may have the values:
 *   0   standard descriptor.  Look it up in WMO Table B.
 *   1   iteration.  xval is the number of following descriptors
 *       to replicate; yval is the number of iterations.
 *       If yval is 0, the iteration descriptor must be followed
 *       immediately by a delayed iteration count (0 31 yyy).
 *       The delayed count descriptor isn't counted in xval.
 *   2   operator: change bitWidth, scale, reference, etc.
 *   3   sequence definition.  Look it up in WMO Table D.
 * 
 * Indentation denotes tree structure.
 * Most BUFR messages are simple, like:
 *   StdDesc (fxy = 0 xx yyy) standard  descriptor
 *   StdDesc
 *   StdDesc
 *   Iterate (fxy = 1 03 000) replication the next 3 descriptors
 *     IterCount (fxy = 0 31 yyy)  = delayed iteration count
 *     StdDesc  = iter sub 0
 *     StdDesc  = iter sub 1
 *     StdDesc  = iter sub 2
 *   StdDesc
 *   StdDesc
 *   StdDesc
 *   Sequence (fxy = 3 xx yyy) defined sequence containing 2 elements
 *     StdDesc = sequence definition element 0
 *     StdDesc = sequence definition element 1
 *   StdDesc
 *   StdDesc
 *   StdDesc
 *   StdDesc
 *   ...
 * 
 * Sometimes an iteration may be nested within a sequence definition,
 * like:
 *   StdDesc
 *   StdDesc
 *   Sequence (fxy = 3 xx yyy) defined sequence containing 6 elements
 *     StdDesc = seq def element 0
 *     Iterate = seq def element 1 (fxy = 1 03 000) rep 3 descriptors
 *       IterCount = seq def element 2 (fxy = 0 31 yyy)  = delayed iter count
 *       StdDesc   = seq def element 3 = iter sub 0
 *       StdDesc   = seq def element 4 = iter sub 1
 *       StdDesc   = seq def element 5 = iter sub 2
 *   StdDesc
 *   StdDesc
 * 
 * Or a sequence may be nested within an iteration.  In this case
 * the nested sequence counts as 1 in the iteration xval count.
 *   StdDesc
 *   StdDesc
 *   Iterate = seq def element 1 (fxy = 1 03 000) rep 3 descriptors
 *     IterCount = seq def element 2 (fxy = 0 31 yyy)  = delayed iter count
 *     StdDesc   = iter sub 0
 *     Sequence  = iter sub 1 = (fxy = 3 xx yyy) defined sequence with 2 eles
 *       StdDesc   = seq def element 0
 *       StdDesc   = seq def element 1
 *     StdDesc   = iter sub 2
 *   StdDesc
 * 
 * 
 * However, some BUFR messages contain iterations that run
 * past the end of a sequence definition.
 *   StdDesc
 *   StdDesc
 *   Sequence (fxy = 3 xx yyy) defined sequence containing 3 elements
 *     StdDesc   = seq def element 0
 *     Iterate   = seq def element 1, (fxy = 1 02 000) rep 2 descs ***
 *     IterCount = seq def element 2, (fxy = 0 31 yyy) delayed rep count
 *   StdDesc = iter sub 0
 *   StdDesc = iter sub 1
 * 
 * 
 * 
 * We handle this by moving the trailing iteration elements
 * out of the sequence def and up a level, restructuring it to be:
 * 
 *   StdDesc
 *   StdDesc
 *   Sequence (fxy = 3 xx yyy) defined sequence containing 1 elements
 *     StdDesc   = seq def element 0
 *   Iterate   = (fxy = 1 02 000) rep the next 2 descs
 *   IterCount = (fxy = 0 31 yyy) delayed rep count
 *   StdDesc = iter sub 0
 *   StdDesc = iter sub 1
 *
 * The restructuring is done in BuildDef.
 * </pre>
 *
 * @author S. Sullivan
 */





class BufrMessage {

static int CUSTOM_FVAL = 7;

static int CUSTOM_SUBSET_X = 0;
static int CUSTOM_SUBSET_Y = 3;
static int CUSTOM_SUBSET_FXY = BufrUtil.getFxyNoCheck(
  CUSTOM_FVAL, CUSTOM_SUBSET_X, CUSTOM_SUBSET_Y);

static int CUSTOM_REPGROUP_X = 1;
static int CUSTOM_REPGROUP_Y = 0;
static int CUSTOM_REPGROUP_FXY = BufrUtil.getFxyNoCheck(
  CUSTOM_FVAL, CUSTOM_REPGROUP_X, CUSTOM_REPGROUP_Y);

static int CUSTOM_ASSOCFLD_X = 4;
static int CUSTOM_ASSOCFLD_Y = 0;
static int CUSTOM_ASSOCFLD_FXY = BufrUtil.getFxyNoCheck(
  CUSTOM_FVAL, CUSTOM_ASSOCFLD_X, CUSTOM_ASSOCFLD_Y);


byte[] section0 = new byte[0];
byte[] section1 = new byte[0];
byte[] section2 = new byte[0];
byte[] section3 = new byte[0];
byte[] section4 = new byte[0];
byte[] section5 = new byte[0];

int fileOffset0;      // file offset of section 0
int fileOffset1;      // file offset of section 1
int fileOffset2;      // file offset of section 2
int fileOffset3;      // file offset of section 3
int fileOffset4;      // file offset of section 4
int fileOffset5;      // file offset of section 5



// General info

int bugs;                 // debug level
BufrFile bfile;           // the file containing this message
int msgNum;               // message number within this file
int totalMsgPos = 0;      // total bytes read in this msg
FxyList fxyList = null;
BufrParser parser = null;



// Section 0 info

int hdrMsgLen;
int hdrBufrEdition;


// Section 1 info


int hdrSubCentre;
int hdrCentre;
int hdrUpdateSeqNum;
int hdrFlags1;               // Flag bits
boolean hdrSection2Flag;     // is section2 present
int hdrCategory;             // See table A
String hdrCategoryName;      // never null; see table A
int hdrInternatSubCategory;
int hdrLocalSubCategory;

// For WMO FM 94 BUFR weather data this is 0.
// For IOC FM 94 BUFR oceanographic data, this is 10.
int hdrMasterTableNum;

int hdrMasterTableVersion;   // major table version
int hdrLocalTableVersion;
int hdrYear;                 // including century
int hdrMonth;
int hdrDay;
int hdrHour;
int hdrMinute;
int hdrSecond;


// Section 2 info
String section2Stg;


// Section 3 info
int sec3Len;
int numSubsets;
boolean flagObserved;
boolean flagCompressed;


// Section 4 info
int sec4Len;
DefDesc defRoot = null;

// Current associated fields
// For example:
// 3 03 026:
//   0 07 004 Pressure
//   0 08 003 Vertical significance
//   2 04 007 Add associated field of 7 bits
//   0 31 021 Additional field significance       6 bits code table
//   0 12 001 Temperature                         7 bits assoc + 12 bits temp
//   2 04 000 Cancel the added associated field
AssocField[] assocFields = new AssocField[0];


/**
 * Constructor is private: internal use only.
 * The constructor is called by readBufrMessage.
 * We use this method so readBufrMessage can scan the file
 * and return null if no further messages are found.
 */

private BufrMessage(
  int bugs,
  BufrFile bfile,
  int msgNum)
{
  this.bugs = bugs;
  this.bfile = bfile;
  this.msgNum = msgNum;
}



public String toString() {
  String res = "inFile: " + bfile.inFile + "\n";
  res += "  msgNum: " + msgNum + "\n";
  return res;
}





/**
 * Scans bfile for the next BUFR message.
 * If we reach EOF before finding the BUFR sentinel, returns null.
 * Otherwise reads and parses the entire message.
 */

static BufrMessage readBufrMessage(
  int bugs,
  BufrFile bfile,
  int msgNum)
throws BufrException
{
  if (bugs >= 1) prtstd("readBufrMessage: entry");
  byte[] sentinel = "BUFR".getBytes();
  int sentinelLen = sentinel.length;

  // Scan for sentinel "BUFR"
  boolean foundit = scanForSentinel( bugs, sentinel, bfile);

  // Read the message
  BufrMessage bmsg = null;
  if (foundit) {
    bmsg = new BufrMessage( bugs, bfile, msgNum);
    bmsg.totalMsgPos = sentinelLen;
    bmsg.readData( sentinel);    // Read and parse sections 0 through 5
  }
  if (bugs >= 1) {
    if (bmsg == null)
      prtstd("BufrMessage.readBufrMessage: return bmsg = null");
    else prtstd("BufrMessage.readBufrMessage: return bmsg with numSubsets: "
      + bmsg.numSubsets);
  }
  return bmsg;
} // end readBufrMessage






/**
 * Scans bfile for the sentinel "BUFR", skipping over
 * garbage until it's found.
 * Returns true if found it.
 */

static boolean scanForSentinel(
  int bugs,
  byte[] sentinel,
  BufrFile bfile)
throws BufrException
{
  int sentinelLen = sentinel.length;
  boolean foundit = false;
  byte[] senbuf = new byte[ sentinelLen];

  int numSkipped = 0;
  int senValid = 0;          // num valid bytes in senbuf
  while (true) {
    foundit = false;
    if (senValid == sentinelLen) {
      // Does sentinel == sentinelId?
      foundit = true;
      for (int ii = 0; ii < sentinelLen; ii++) {
        if (senbuf[ii] != sentinel[ii]) {
          foundit = false;
          break;
        }
      }
      if (foundit) break;          // break if found sentinel
      // Shift senbuf down one
      for (int ii = 0; ii < sentinelLen - 1; ii++) {
        senbuf[ii] = senbuf[ii+1];
      }
      senValid--;
    }
    int numRead = bfile.readBytes( senbuf, senValid, 1);
    if (numRead != 1) {
      foundit = false;
      break;
    }
    senValid++;
    numSkipped++;
  } // while true

  if (bugs >= 1 && foundit && numSkipped > 0)
    prtstd("scanForSentinel: numSkipped: " + numSkipped);
  return foundit;
}







/**
 * Reads and parses sections 0 through 5 of the BUFR message.
 * If we get an exception parsing section 4, set parser == null.
 */

private void readData( byte[] sentinel)
throws BufrException
{
  // Handle section 0 (start identifier)
  if (bugs >= 1) prtln("\n===== section 0");
  int sentinelLen = sentinel.length;
  fileOffset0 = bfile.fileOffset - sentinel.length;  // bk up to start
  byte[] temp0 = readSection( 8 - sentinelLen);      // Read 4 bytes
  section0 = new byte[ sentinelLen + temp0.length];
  System.arraycopy( sentinel, 0, section0, 0, sentinelLen);
  System.arraycopy( temp0, 0, section0, sentinelLen, temp0.length);
  parseStartSentinel( sentinel);

  // Read and parse section 1 (header info)
  if (bfile.parseStage >= BufrFile.STAGE_HEADER) {
    if (bugs >= 1) prtln("\n===== section 1");
    fileOffset1 = bfile.fileOffset;
    section1 = readSection( 0);
    parseHeader();
  }

  // Read and parse section 2 (local info)
  if (bfile.parseStage >= BufrFile.STAGE_LOCAL) {
    if (hdrSection2Flag) {          // Is section 2 present
      if (bugs >= 1) prtln("\n===== section 2");
      fileOffset2 = bfile.fileOffset;
      section2 = readSection( 0);
      parseLocalInfo();
    }
    else {
      if (bugs >= 1) prtln("\n===== section 2 not present");
    }
  }

  // Read and parse section 3 (descriptors)
  if (bfile.parseStage >= BufrFile.STAGE_DKEY) {
    if (bugs >= 1) prtln("\n===== section 3");
    fileOffset3 = bfile.fileOffset;
    section3 = readSection( 0);
    parseDesc();
  }

  // Expand the descriptors from section 3
  if (bfile.parseStage >= BufrFile.STAGE_EXPDKEY) {
    parseEdesc();
  }

  // Read and parse section 4 (data)
  // If exception, set parser = null.
  if (bfile.parseStage >= BufrFile.STAGE_DATA) {
    if (bugs >= 1) prtln("\n===== section 4");
    fileOffset4 = bfile.fileOffset;
    section4 = readSection( 0);
    try {
      // Parse all the data
      parseData();
    }
    catch( BufrException exc) {
      prtln("\n");
      String msg = "=============== EXCEPTION ==================\n\n"
        + "Problem parsing section 4 for message number: " + msgNum + "\n"
        + "  within file: " + bfile.inFile + "\n"
        + "  Exception: " + exc + "\n\n"
        + BufrUtil.formatStackTrace( exc) + "\n";
      bfile.errorMsgs += msg;
      parser = null;
      prtln(msg);
      if ( !bfile.forceFlag) throwerr("Parse error; see stdout");
    }
  }

  // Handle section 5 (tail identifier)
  if (bfile.parseStage >= BufrFile.STAGE_DATA) {
    if (bugs >= 1) prtln("\n===== section 5");
    fileOffset5 = fileOffset0 + hdrMsgLen - 4;
    if (bfile.fileOffset != fileOffset5) {
      throwerr("tail offset mismatch.  expected fileOffset5: " + fileOffset5
        + "  found: " + bfile.fileOffset);
    }
    section5 = readSection( 4);
    parseEndSentinel();
    // This is essentially the same check as just above.
    if (totalMsgPos != hdrMsgLen) throwerr("msgLen mismatch");
  }

} // end readData








/**
 * Parses BUFR message section 0.
 */

void parseStartSentinel( byte[] sentinel)
throws BufrException
{
  BitBufReader bitBuf = new BitBufReader(
    bugs, BitBufReader.BBTP_DATA, section0, fileOffset0, 0);

  int sentinelLen = sentinel.length;
  String chkSent = bitBuf.getRawString( sentinelLen * 8);
  if (! chkSent.equals( new String( sentinel)))
    throwerr("sentinel mismatch");
  hdrMsgLen = bitBuf.getInt( 3*8);
  hdrBufrEdition = bitBuf.getInt( 1*8);

  // Insure we're at the end of the section
  if (bitBuf.getBitPos() != 8*8) throwerr("sec0 len mismatch");
  if (bugs >= 1) {
    prtln("\n===== parse section 0: ");
    prtln( String.format("section length: %d = 0x%x",
      4 + section0.length, 4 + section0.length));   // add in sentinel: +4
    prtln("hdrMsgLen: " + hdrMsgLen);
    prtln("hdrBufrEdition: " + hdrBufrEdition);
    prtln("");
  }
}







/**
 * Parses BUFR message section 1.
 */

void parseHeader()
throws BufrException
{
  if (hdrBufrEdition == 2 || hdrBufrEdition == 3 || hdrBufrEdition == 4)
    parseHeaderEdition234();
  else throwerr("unknown edition number: " + hdrBufrEdition);
}




void parseHeaderEdition234()
throws BufrException
{

  BitBufReader bitBuf = new BitBufReader(
    bugs, BitBufReader.BBTP_DATA, section1,
    fileOffset1, fileOffset1 - fileOffset0);

  int sectionLen = bitBuf.getInt( 3*8);
  if (sectionLen != bitBuf.getByteLength())
    throwerr("section length mismatch");

  // For WMO FM 94 BUFR weather data this is 0.
  // For IOC FM 94 BUFR oceanographic data, this is 10.
  hdrMasterTableNum = bitBuf.getInt( 1*8);

  if (hdrBufrEdition == 2) {
    hdrCentre = bitBuf.getInt( 2*8);     // orgin centre: code table 0 01 031
  }
  else if (hdrBufrEdition == 3) {
    hdrSubCentre = bitBuf.getInt( 1*8);  // sub-centre:   code table 0 01 034
    hdrCentre = bitBuf.getInt( 1*8);     // orgin centre: code table 0 01 033
  }
  else if (hdrBufrEdition == 4) {
    hdrCentre = bitBuf.getInt( 2*8);    // orgin centre: common code table C-11
    hdrSubCentre = bitBuf.getInt( 2*8); // sub-centre:   common code table C-12
  }
  else throwerr("unknown edition number: " + hdrBufrEdition);

  hdrUpdateSeqNum = bitBuf.getInt( 1*8);

  hdrFlags1 = bitBuf.getInt( 1*8);
  hdrSection2Flag = false;    // Is section 2 present
  if ((hdrFlags1 & 0x80) != 0) hdrSection2Flag = true;

  hdrCategory = bitBuf.getInt( 1*8);      // see Table A

  hdrCategoryName = BufrUtil.unknownTag;
  try {
    DefCateg cdef = bfile.tableCateg.getDefCopy( hdrCategory);
    if (cdef != null) hdrCategoryName = cdef.description;
  }
  catch( BufrException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("caught: " + exc);
  }

  if (hdrBufrEdition == 2 || hdrBufrEdition == 3)
    hdrInternatSubCategory = 0;
  else if (hdrBufrEdition == 4)
    hdrInternatSubCategory = bitBuf.getInt( 1*8);
  hdrLocalSubCategory = bitBuf.getInt( 1*8);
  hdrMasterTableVersion = bitBuf.getInt( 1*8);
  hdrLocalTableVersion = bitBuf.getInt( 1*8);

  if (hdrBufrEdition == 2 || hdrBufrEdition == 3) {
    // See section 3.1.1.3 in the Guide to WMO Table Driven Code Forms.
    int year = bitBuf.getInt( 1*8);
    if (year <= 50) hdrYear = 2000 + year;
    else hdrYear = 1900 + year;  // includes 1900 + 100 = 2000, spec in manual
  }
  else if (hdrBufrEdition == 4) {
    hdrYear = bitBuf.getInt( 2*8);
  }

  hdrMonth = bitBuf.getInt( 1*8);
  hdrDay = bitBuf.getInt( 1*8);
  hdrHour = bitBuf.getInt( 1*8);
  hdrMinute = bitBuf.getInt( 1*8);
  if (hdrBufrEdition == 2 || hdrBufrEdition == 3) hdrSecond = 0;
  else if (hdrBufrEdition == 4) hdrSecond = bitBuf.getInt( 1*8);

  if (bitBuf.getBitPos() % 8 != 0) throwerr("sec1 len error");
  int localLen = bitBuf.getByteLength() - bitBuf.getBitPos() / 8;

  if (bugs >= 1) {
    prtln("\n===== parse section 1:");
    prtln("  hdrBufrEdition: " + hdrBufrEdition);
    prtln("  hdrCategory: " + hdrCategory);
    prtln("  hdrYear: " + hdrYear + "  hdrMonth: " + hdrMonth
      + "  hdrDay: " + hdrDay);
    prtln("  hdrHour: " + hdrHour + "  hdrMinute: " + hdrMinute
      + "  hdrSecond: " + hdrSecond);
    prtln("\n===== Header:");
    prtln( BufrFormatter.formatHeader( false, bfile, this, null));
      // isXml = false, encodingName = null
    prtln("");
    prtln( BufrFormatter.formatConvertInfo( false, bfile, this, null));
      // isXml = false, encodingName = null
  }

  // See categTab.formatted
  if (hdrCategory != 0    // surface data - land
    && hdrCategory != 7)  // synoptic features
  {
    prtln("BufrMessage: unusual hdrCategory: " + hdrCategory);
  }

  if (bfile.validateFlag) {
    BufrUtil.validateYear( hdrYear, this, 0, null);  // msg, subset, BufrItem
    BufrUtil.validateMonth( hdrMonth, this, 0, null);
    BufrUtil.validateDay( hdrDay, this, 0, null);
    BufrUtil.validateHour( hdrHour, this, 0, null);
    BufrUtil.validateMinute( hdrMinute, this, 0, null);
    BufrUtil.validateSecond( hdrSecond, this, 0, null);
  }
} // end parseHeaderEdition234






/**
 * Parses BUFR message section 2.
 * Section 2 is local data, meaning it's not defined by
 * the BUFR standard.
 * It could contain anything, but often is strings so
 * we just print the strings.
 */

void parseLocalInfo()
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  for (int ipos = 4; ipos < section2.length; ipos++) {
    int ival = 0xff & section2[ipos];
    if (BufrUtil.isPrintable( ival)) sbuf.append((char) ival);
    else sbuf.append('#');
  }
  section2Stg = sbuf.toString();

  if (bugs >= 1) {
    prtln("\n===== parse section 2:");
    prtln( String.format("section length: %d = 0x%x",
      section2.length, section2.length));
    prtln("section2Stg: \"" + section2Stg + "\"");
  }
} // end parseLocalInfo





/**
 * Parses BUFR message section 3 (descriptors).
 */

void parseDesc()
throws BufrException
{
  BitBufReader descBuf = new BitBufReader(
    bugs, BitBufReader.BBTP_DATA, section3,
    fileOffset3, fileOffset3 - fileOffset0);

  sec3Len = descBuf.getInt( 3 * 8);
  if (sec3Len != section3.length) throwerr("sec3Len != section3.length");
  int unused_a = descBuf.getInt( 1*8);

  numSubsets = descBuf.getInt( 2*8);

  int flags = descBuf.getInt( 1*8);     // observed flags
  if ((flags & 0x80) != 0) flagObserved = true;
  else flagObserved = false;
  if ((flags & 0x40) != 0) flagCompressed = true;
  else flagCompressed = false;

  if (bugs >= 1) {
    prtln("\n===== parse section 3:");
    prtln( String.format("section length: %d = 0x%x",
      section3.length, section3.length));
  }

  // From here on, allow only descriptors in descBuf
  descBuf.bufType = BitBufReader.BBTP_DESC;

  // Create a list of all the raw descriptors.
  fxyList = new FxyList();
  while (! descBuf.atEof()) {
    int fxy = descBuf.getDesc();
    fxyList.addVal( fxy);
  }

  if (bugs >= 1) {
    prtln("\n===== Misc info:");
    prtln( BufrFormatter.formatDescInfo( false, bfile, this, null));
      // isXml = false, encodingName = null

    prtln("\n===== descriptors (fxyList):");
    prtln( BufrFormatter.formatCodeList(
      false, bfile.outStyle, fxyList, this));
    // isXml = false
    prtln("");
  }
} // end parseDesc



/**
 * Expand the descriptor tree from section 3.
 */

void parseEdesc()
throws BufrException
{
  // Create tree of DefDesc, with root defRoot.

  // Insure data version number is compatible with table version.
  // Major versions <= 13 allow backward compatibility;
  // versions >= 14 do not.
  String errmsg = null;
  if (bfile.tableVersionMajor <= 13) {
    if (hdrMasterTableVersion > bfile.tableVersionMajor)
      errmsg = "major";
    else if (hdrMasterTableVersion == bfile.tableVersionMajor
      && hdrLocalTableVersion > bfile.tableVersionMinor)
      errmsg = "minor";
  }
  else {     // else tableVersionMajor >= 14.  No backward compatibility.
    if (hdrMasterTableVersion != bfile.tableVersionMajor)
      errmsg = "major";
    else if (hdrLocalTableVersion > bfile.tableVersionMinor)
      errmsg = "minor";
  }

  if (errmsg != null) {
    throwerr("\nIncompatible " + errmsg + " version numbers:\n"
      + "  table version: major: " + bfile.tableVersionMajor
      + "  minor: " + bfile.tableVersionMinor + "\n"
      + "  data  version: major: " + hdrMasterTableVersion
      + "  minor: " + hdrLocalTableVersion + "\n");
  }

  // Create tree of DefDesc
  defRoot = new DefDesc( CUSTOM_SUBSET_FXY);
  while (fxyList.hasMore()) {
    DefDesc[] subDefs = buildDef( null, fxyList);   // parentList = null
    defRoot.addSubdefs( subDefs);
  }

  // Set defId in all tree vertices
  defRoot.setTreeDefId( 1);

  // Print expanded descriptor tree
  if (bugs >= 1) {
    prtln("\n===== Expanded Descriptors:");
    prtln( BufrFormatter.formatDefDescTree( false, bfile.outStyle, defRoot));
    // isXml = false
    prtln("");
  }
} // end parseEdesc







/**
 * Parses BUFR message section 4 (data) using the descriptors
 * from section 3.
 */
void parseData()
throws BufrException
{
  if (bugs >= 1) {
    prtln("\n===== parse section 4:");
    prtln( String.format("section length: %d = 0x%x",
      section4.length, section4.length));
  }

  // Parse the data
  BitBufReader dataBuf = new BitBufReader(
    bugs, BitBufReader.BBTP_DATA, section4,
    fileOffset4, fileOffset4 - fileOffset0);

  // Read past dataBuf header (section 4)
  sec4Len = dataBuf.getInt( 3 * 8);
  if (sec4Len != section4.length) throwerr("sec4Len != section4.length");
  int unused_b = dataBuf.getInt( 1*8);

  // Parse all subsets.
  // Calls BufrParser.parseMain, handles DynDefs, etc.
  parser = new BufrParser( bugs, bfile, this, defRoot, dataBuf);

  if (bugs >= 2) {
    for (int isub = 0; isub < numSubsets; isub++) {
      BufrItem rootItem = parser.rootItems[isub];

      // Format the subset's entire xml tree
      prtln("\n===== Data contents of message: " + msgNum
	    + "  subset: " + isub
		+ "  in file: " + bfile.inFile);
      prtln("");
      prtln( BufrFormatter.formatBufrItemTree(
        false, rootItem, this, isub));
      prtln("");
    } // for isub
  }

  // Insure we have < 2 bytes remaining at the end
  int remLen = dataBuf.getRemainBitLen();
  if (remLen >= 16) throwerr("remLen >= 16");

} // end parseData










/**
 * Parses BUFR message section 5.
 */

void parseEndSentinel()
throws BufrException
{
  if (bugs >= 1) {
    prtln("\n===== parse section 5:");
    prtln( String.format("section length: %d = 0x%x",
      section5.length, section5.length));
  }
  BitBufReader bitBuf = new BitBufReader(
    bugs, BitBufReader.BBTP_DATA, section5,
    fileOffset5, fileOffset5 - fileOffset0);

  String endStg = bitBuf.getRawString(4*8);
  if (! endStg.equals("7777")) throwerr("sec5 end sentinel mismatch");

  // Insure we're at the end of the section
  if (bitBuf.getBitPos() != 4*8) throwerr("sec5 len mismatch");
}















/**
 * Reads a section (section number 0, 1, 2, 3, 4, or 5) of a BUFR message.
 */

byte[] readSection(
  int sectionLen)     // if 0, section len = first 3 bytes
throws BufrException
{
  if (bugs >= 1) {
    prtln( String.format("readSection entry.  sectionLen: %d = 0x%x\n"
      + "  totalMsgPos: %d = 0x%x  fileOffset: %d = 0x%x",
      sectionLen, sectionLen,
      totalMsgPos, totalMsgPos,
      bfile.fileOffset, bfile.fileOffset));
  }
  int readLen = sectionLen;
  if (readLen == 0) readLen = 3;
  byte[] inbuf = new byte[readLen];
  int numRead = bfile.readBytes( inbuf, 0, inbuf.length);
  if (numRead != readLen) throwerr("file too short");
  totalMsgPos += numRead;

  // If we just read the section length, read the rest of the section.
  if (sectionLen == 0) {
    BitBufReader bitBuf = new BitBufReader(
      bugs, BitBufReader.BBTP_DATA, inbuf, 0, 0);
    sectionLen = bitBuf.getInt( 3*8);
    if (bugs >= 1)
      prtln("readSection.  read sectionLen: " + sectionLen);

    // Make full buffer and copy in the bytes we already read
    byte[] svbuf = inbuf;
    inbuf = new byte[sectionLen];
    System.arraycopy( svbuf, 0, inbuf, 0, readLen);

    // Read rest of the section
    numRead = bfile.readBytes( inbuf, readLen, sectionLen - readLen);
    if (numRead != sectionLen - readLen) throwerr("file too short");
    totalMsgPos += numRead;
  }
  if (bugs >= 1)
    prtln("readSection exit.  totalMsgPos: " + totalMsgPos);

  // Print hex and binary notation of the section
  if (bugs >= 20) {
    StringBuilder sbuf = new StringBuilder();
    Formatter fmtr = new Formatter(sbuf);
    for (int ii = 0; ii < inbuf.length; ii++) {
      int ival = 0xff & inbuf[ii];
      char cval = (char) ival;
      if (! BufrUtil.isPrintable(cval)) cval = '#';
      fmtr.format("ii: %3d  dec: %3d  hex: %02x", ii, ival, ival);
      int imask = (1 << 7);
      fmtr.format("  ");
      for (int jj = 0; jj < 8; jj++) {
        int jval = 0;
        if ((imask & ival) != 0) jval = 1;
        fmtr.format(" %d", jval);
        imask >>>= 1;
      }
      fmtr.format("\n");
    }
    prtln( sbuf.toString());
  }
  return inbuf;
} // end readSection








/**
 * Builds a DefDesc from the next available fxy in fxyList.
 * If the next fxy is an iteration, recurses to create
 * an iteration DefDesc containing all the loop fxy values.
 * <p>
 * See doc at top of this file on restructuring badly formed
 * BUFR messages.
 */

DefDesc[] buildDef(
  FxyList parentList,    // The containing FxyList.
                         // For example the list containing the rep start.
                         // The only reason we need parentList is in
                         // case we have to restructure a badly formed
                         // repetition.

  FxyList curList)       // The current FxyList we get fxy values from.
                         // For example the items within a repetition.

throws BufrException
{
  int fxy = curList.getCurInc();
  int fval = BufrUtil.getFval( fxy);
  int xval = BufrUtil.getXval( fxy);
  int yval = BufrUtil.getYval( fxy);

  if (bugs >= 10) {
    prtln("buildDef entry: fxy: " + BufrUtil.formatFxy( fxy));
    //prtln("buildDef entry: curList: " + curList);
  }

  DefDesc[] resDefs = null;



  if (fval == 0) {       // ordinary table B descriptor

    // For each active associated fields:
    //   Insert a new assocField element before this element.
    //
    // See section 3.1.6.3 in the Guide to WMO Table Driven Code Forms.
    // Associated field bits are prefixes to the actual data.
    //
    // However, no table C operators apply to "0 31 yyy".
    // See http://www.wmo.ch/pages/prog/www/WDM/Guides/Guide-binary-1B.html
    // A Guide to the code form FM-94 BUFR
    // Section 5.3:
    //   "Note that the associated fields are not prefixed
    //   onto the data described by 0 31 YYY descriptor.
    //   This is a general rule: none of the Table C operators
    //   are applied to any of the Table B, Class 31 descriptors."

    if (xval == 31) {
      // Just handle the descriptor with no associated fields
      resDefs = new DefDesc[1];
      resDefs[0] = findDefDesc( fxy);      // ordinary descriptor
    }
    else {
      int flen = assocFields.length;
      resDefs = new DefDesc[ flen + 1];
      for (int ifld = 0; ifld < flen; ifld++) {
        AssocField afld = assocFields[ifld];
        resDefs[ifld] = new DefDesc( CUSTOM_ASSOCFLD_FXY);
        resDefs[ifld].isCode = true;
        resDefs[ifld].description = "Assoc fld, bits=" + afld.numBits;
        resDefs[ifld].scale = 0;
        resDefs[ifld].reference = 0;
        resDefs[ifld].bitWidth = afld.numBits;
      }
      // Finally handle the descriptor
      resDefs[flen] = findDefDesc( fxy);       // ordinary descriptor
    }
  }


  else if (fval == 1) {         // iteration: read included items
    int totDescs = xval;
    if (yval == 0) totDescs++;  // for delayed count

    // If the iteration extends beyond the end of curList,
    // restructure the tree.
    // Move the entire iteration of fxy values, from
    // the repetition start to the end of curList,
    // up to the parentList.
    if (curList.getPos() + totDescs > curList.size()) {
      if (bugs >= 1) {
        prtln("buildDef: promoting iteration to parentList.");
        prtln("  curList: " + curList);
        prtln("  parentList: " + parentList);
      }
      if (parentList == null)
        throwerr("iteration extends past end of root FxyList");
      curList.incPos(-1);      // back up to the iteration desc
      parentList.insertRemains( curList);
      // We're returning nothing since we pushed it all up to the parent.
      resDefs = new DefDesc[0];
    }

    // Else the iteration is completely contained in
    // the current curList.
    else {
      if (yval == 0) {
        if (! curList.hasMore())
          throwerr("no count for delayed replication");
        int countFxy = curList.getCurNoInc();  // no inc: we handle it below
        int countFval = BufrUtil.getFval( countFxy);
        int countXval = BufrUtil.getXval( countFxy);
        int countYval = BufrUtil.getYval( countFxy);
        if (countFval != 0 || countXval != 31)
          throwerr("invalid count for delayed replication");
      }

      resDefs = new DefDesc[1];
      resDefs[0] = new DefDesc( fxy);
      if (yval == 0) {
        DefDesc[] tdescs = buildDef( parentList, curList);
        if (tdescs.length != 1) throwerr("bad tdescs size");
        resDefs[0].countDef = tdescs[0];
      }

      FxyList subList = new FxyList();
      for (int ii = 0; ii < xval; ii++) {
        subList.addVal( curList.getCurInc());
      }
      while (subList.hasMore()) {
        DefDesc[] subDefs = buildDef( curList, subList);
        resDefs[0].addSubdefs( subDefs);
      }
    }
  } // if fval == 1

  else if (fval == 2) {         // operator
    resDefs = new DefDesc[1];
    resDefs[0] = new DefDesc( fxy);
    resDefs[0].description = getDescription( fxy);

    // If start associated field, add to assocFields.
    if (xval == 4) {
      int numBits = yval;
      if (numBits == 0) {               // cancel
        if (assocFields.length == 0)
          throwerr("handleOperatorDef: assocFields is already empty");
        // Delete the last element
        assocFields = Arrays.copyOf( assocFields, assocFields.length - 1);
      }
      else {
        int oldLen = assocFields.length;
        assocFields = Arrays.copyOf( assocFields, oldLen + 1);
        assocFields[oldLen] = new AssocField( numBits);
      }
    }
    else if (xval == 5)         // signify character
      resDefs[0].isString = true;
  }

  else if (fval == 3) {         // sequence: get expansion
    resDefs = new DefDesc[1];
    resDefs[0] = buildSequenceDef( curList, fxy);
  }

  if (resDefs == null)
      throwerr("resDefs is null for fxy: " + BufrUtil.formatFxy( fxy));

  if (bugs >= 10) {
    for (int ii = 0; ii < resDefs.length; ii++) {
      prtln("buildDef exit: resDefs[" + ii + "]:\n" + resDefs[ii]);
    }
  }
  return resDefs;
} // end buildDef







DefDesc buildSequenceDef(
  FxyList curList,
  int fxy)
throws BufrException
{
  DefDesc resDef = findDefDesc( fxy);
  // Replace each sequence element with its expansion
  FxyList subList = new FxyList();
  for (DefDesc subDef : resDef.subDefs) {
    subList.addVal( subDef.fxy);
  }
  if (bugs >= 10) {
    prtln("buildSequenceDef: sequence: fxy: " + BufrUtil.formatFxy( fxy));
    prtln("  subList: " + subList);
  }
  resDef.clearSubDefs();  // clear, then add expansions
  while (subList.hasMore()) {
    DefDesc[] subDefs = buildDef( curList, subList);  // parentList, list
    resDef.addSubdefs( subDefs);
  }
  return resDef;
} // end buildSequenceDef








/**
 * Retrieves a DefDesc from either:
 *   bfile.tableDescription == WMO BUFR table B.
 *   bfile.tableSequence == WMO BUFR table D.
 */

DefDesc findDefDesc( int fxy)
throws BufrException
{
  int fval = BufrUtil.getFval( fxy);
  int xval = BufrUtil.getXval( fxy);
  int yval = BufrUtil.getYval( fxy);
  DefDesc resDef = null;

  if (fval == 0) {              // ordinary descriptor
    resDef = bfile.tableDescription.getDefCopy( fxy);
    if (resDef == null)
      throwerr("findDefDesc: unknown fxy: " + BufrUtil.formatFxy( fxy));
  } // if fval == 0
  else if (fval == 3) {         // sequence: get expansion
    resDef = bfile.tableSequence.getDefCopy( fxy);
    if (resDef == null)
      throwerr("findDefDesc: unknown fxy: " + BufrUtil.formatFxy( fxy));
    if (resDef.description.length() == 0) resDef.description = "sequence";
  }
  else throwerr("findDefDesc only handles fval 0 and 3");

  if (resDef == null)
    throwerr("findDefDesc: unknown fxy: " + BufrUtil.formatFxy( fxy));
  return resDef;
}



/**
 * Returns the description of the given fxy, or "" if not found. <br>
 * If fval == 0, the description comes from bfile.tableDescription. <br>
 * If fval == 3, the description comes from bfile.tableSequence. <br>
 * Otherwise, for iterators and such, generate an appropriate description. <br>
 */

String getDescription( int fxy)
throws BufrException
{
  int fval = BufrUtil.getFval( fxy);
  int xval = BufrUtil.getXval( fxy);
  int yval = BufrUtil.getYval( fxy);

  String desc = "";
  if (fval == 0) {
    DefDesc def = bfile.tableDescription.getDefCopy( fxy);
    if (def != null) desc = def.description;
  }

  else if (fval == 1) {
    desc = "iterate  numDescs = " + xval + "  numIters: ";
    if (yval == 0) desc += "delayed";
    else desc += yval;
  }

  else if (fval == 2) {
    if (xval == 1) {
      if (yval == 0) desc = "Change bitWidth end";
      else desc = "Change bitWidth start";
    }
    else if (xval == 2) {
      if (yval == 0) desc = "Change scale end";
      else desc = "Change scale start";
    }
    else if (xval == 3) {
      if (yval == 255) desc = "Change reference end defs";
      else if (yval == 0) desc = "Change reference end use";
      else desc = "Change reference start defs";
    }
    else if (xval == 5) desc = "Signify character";

    else if (xval == 6) desc = "Spec data width for next 1 item";
  }

  else if (fval == 3) {
    DefDesc def = bfile.tableSequence.getDefCopy( fxy);
    if (def != null) desc = def.description;
  }
  return desc;
} // end getDescription




static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrMessage: " + msg);
}



static void prtln( String msg) {
  System.out.println( msg);
}


static void prtstd( String msg) {
  System.out.println( msg);
}


} // end class
