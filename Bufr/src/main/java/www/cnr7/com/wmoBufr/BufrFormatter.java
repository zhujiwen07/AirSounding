
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.SimpleTimeZone;


//xxx
// insure char value is not clobbered.  Same for localInfo.


class BufrFormatter {


// Codes for foutType for getOutSpecName
static int FOUT_DATA    = 1;
static int FOUT_REPORT  = 2;


// Tag names
static String messageTagNm = "message";          // overall BUFR message
static String hdrInfoTagNm = "hdrInfo";          // BUFR message header
static String convertInfoTagNm = "convertInfo";  // convert date, host
static String localInfoTagNm = "localInfo";      // section 2
static String dkeyInfoTagNm = "dkeyInfo";        // section 3 info: numSubsets
static String dkeysTagNm = "dkeys";
static String dkeyTagNm = "dkey";
static String expandDkeysTagNm = "expandDkeys";
static String expandDkeyTagNm = "expandDkey";

static String subsetsTagNm = "subsets";          // section 4: all subsets
static String subsetTagNm = "subset";            // section 4: one subset
static String sequenceTagNm = "sequence";        // section 4: sequence
static String loopTagNm = "loop";                // section 4: loop head
static String groupTagNm = "group";              // section 4: loop 1 iter
static String charTagNm = "char";                // section 4: char fxy
static String numTagNm = "number";               // section 4: number
static String codeTagNm = "code";                // section 4: code
static String flagTagNm = "flag";                // section 4: bit flag
static String controlTagNm = "control";          // section 4: control fxy
static String afldTagNm = "assocFld";            // section 4: associated field

// Attribute names
static String dkeyAttrNm = "dkey";
static String noteAttrNm = "note";
static String idAttrNm = "id";
static String valueAttrNm = "value";
static String unitAttrNm = "units";
static String meaningAttrNm = "meaning";
static String missingAttrNm = "missing";          // section 4: missing
static String itersAttrNm = "iters";
static String levelAttrNm = "level";

static String missingValueNm = "MISSING";         // section 4: missing



static boolean showLevel = false;

/**
 * Inner class to allow findTreeItemSub to return a pair of items.
 */
static class FindTreeResult {
  int numFound;           // num matches found in the subtree
  BufrItem foundItem;     // if numFound == numDesired, foundItem
                          // is numFound'th matched item.
                          // Otherwise foundItem is null.
  FindTreeResult( int numFound, BufrItem foundItem) {
    this.numFound = numFound;
    this.foundItem = foundItem;
  }

  public String toString() {
    String res = "numFound: " + numFound + "  foundItem: " + foundItem;
    return res;
  }
} // end inner class FindTreeResult




/**
 * For each subset, formats and writes an XML file, and formats
 * and writes a line to the report output file.
 * <p>
 * The output file names are specified by BufrFile.outSpec.<br>
 * The lines written to the report file are specified
 * by BufrFile.reportSpec.<br>
 * <p>
 * For info on the specification of file names and report output lines,
 * see {@link BufrFile#printHelpUsage Usage info in BufrFile.printHelpUsage}.
 */


//                  xml result             text result
//                  ---------              -----------
// mkFxyStartTag    <tag dkeyTagNm="..."   tag dkeyTagNm="..."
// mkOpenTagLn      \n<tag\n               tag\n
// mkStartTagLn     \n<tag>\n              \ntag\n
// mkCloseTagLn     >\n                    \n
// mkFullTagLn      />\n                   \n
// mkEndTagLn       </tag>\n               tag_end\n



static void writeAllOutput(
  boolean isXml,
  BufrFile bfile,
  BufrMessage bmsg)
throws BufrException
{
  String encodingName = "US-ASCII";

  StringBuilder sbuf = new StringBuilder();
  if (isXml) {
    sbuf.append("<?xml version=\"1.0\" encoding=\"" + encodingName
      + "\" ?>\n");
  }

  // Write documentation at start of output file.
  // If text output, we use "# ...".
  // If xml, we use "<!-- ... -->".

  String usageMsg = ""
    + "This file represents one BUFR message.\n"
    + "\n"
    + "*** INPUT FILE STRUCTURE ***\n"
    + "An input BUFR file may contain multiple BUFR messages.\n"
    + "The wmoBufr software splits each input file into separate\n"
    + "output files, one for each BUFR message.\n"
    + "\n"
    + "Each BUFR message may contain 0 or more subsets of data values.\n"
    + "A subset is a complete set of data values, for all the dkeys.\n"
    + "\n"
    + "The format of a BUFR message is:\n"
    + "  Section 0:  start sentinal == \"BUFR\"\n"
    + "  Section 1:  header info\n"
    + "  Section 2:  optional unstructured local info\n"
    + "  Section 3:  dkeys (see below) that describe the bits in section 4\n"
    + "  Section 4:  data bits corresponding to the dkeys in section 3\n"
    + "  Section 5:  end sentinal == \"7777\"\n"
    + "\n"
    + "*** What's a dkey? ***\n"
    + "dkey == descriptor == descriptor reference number == fxy code.\n"
    + "Different people call it different names, but it's just an integer\n"
    + "formatted in 3 parts (f,x,y) as f_xx_yyy.\n"
    + "A dkey in BUFR section 3 refers to published WMO tables that\n"
    + "describe how to decode the corresponding bits of section 4.\n"
    + "For example, the dkey 0_12_001 in section 3 indicates that the\n"
    + "next 12 bits of section 4 are a temperature value, encoded\n"
    + "using the units, scale and offset shown in the WMO table B.\n"
    ///+ "Some more examples:\n"
    ///+ "  f_xx_yyy    Meaning in WMO tables\n"
    ///+ "  --------    ---------------------\n"
    ///+ "  0_02_011    coded radiosonde type, 8 bits.\n"
    ///+ "  0_05_001    coded latitude, 25 bits.\n"
    ///+ "  0_12_001    coded air temperature, 12 bits.\n"
    ///+ "  1_03_010    loop over the next 3 dkeys 10 times.  Many complexities.\n"
    ///+ "  2_xx_yyy    control: special processing.  More complexities!\n"
    ///+ "  3_01_021    sequence (macro) expands to other dkeys.\n"
    ///+ "  7_xx_yyy    artificial dkey used by this software, not the WMO,\n"
    ///+ "              to represent certain structures.\n"
    + "\n"
    + "The types of dkeys in section 3 depend on the \"f\" value in f_xx_yyy.\n"
    + "\n"
    + "  f = 0  Ordinary dkeys, like dkey = 0_12_001, air temperature.\n"
    + "\n"
    + "  f = 1  Loop over the next xx dkeys.\n"
    + "         For example, 1_03_010 loops over the next 3 dkeys\n"
    + "         for 10 iterations.  Many complexities.\n"
    + "\n"
    + "  f = 2  Control codes: special processing.  Many more complexities.\n"
    + "\n"
    + "  f = 3  Sequences.  For example, dkey = 3_01_021 is the same as the\n"
    + "         two dkeys 0_05_001, 0_06_001 (latitude, longitude).\n"
    + "         Sequences can be nested.\n"
    + "\n"
    + "  f = 7  Artificial f value used by this software, not the WMO,\n"
    + "         to represent certain structures.\n"
    + "\n"
    + "*** OUTPUT FILE STRUCTURE ***\n"
    + "The output file structure is as below.\n"
    + "Depending on the -parseStage parameter, some sections may be omitted.\n"
    + "\n"
    + "<" + messageTagNm + ">                        # Overall BUFR message\n"
    + "  <" + hdrInfoTagNm + ">...</" + hdrInfoTagNm + ">         # Info from the section 1 header\n"
    + "  <" + convertInfoTagNm + ">...</" + convertInfoTagNm + "> # Info on the wmoBufr conversion\n"
    + "  <" + localInfoTagNm + ">...</" + localInfoTagNm + ">     # Local info from section 2\n"
    + "  <" + dkeyInfoTagNm + ">...</" + dkeyInfoTagNm + ">       # Overall info from section 3\n"
    + "  <" + dkeysTagNm + ">...</" + dkeysTagNm + ">             # The dkeys in section 3\n"
    + "  <" + expandDkeysTagNm + ">...</" + expandDkeysTagNm + "> # dkeys with sequences and controls expanded\n"
    + "  <" + subsetsTagNm + ">                      # 0 or more subsets.  \n"
    + "    <" + subsetTagNm + "> ... </" + subsetTagNm + ">       # subset 0 of data values\n"
    + "    <" + subsetTagNm + "> ... </" + subsetTagNm + ">       # subset 1 of data values\n"
    + "    ...                          # more subsets\n"
    + "  </" + subsetsTagNm + ">\n"
    + "</" + messageTagNm + ">\n"
    + "\n"
    + "The tags used in this file are:\n"
    + "  " + messageTagNm + "      Overall BUFR message\n"
    + "  " + hdrInfoTagNm + "      Header info from section 1\n"
    + "  " + convertInfoTagNm + "  Info on the wmoBufr conversion process\n"
    + "  " + localInfoTagNm + "    Local info from BUFR section 2\n"
    + "  " + dkeyInfoTagNm + "     General info from BUFR section 3, includes numSubsets\n"
    + "  " + dkeysTagNm + "        List of dkeys from BUFR section 3\n"
    + "  " + dkeyTagNm + "         One dkey\n"
    + "  " + expandDkeysTagNm + "  List of dkeys with all sequences and controls expanded\n"
    + "  " + expandDkeyTagNm + "   One expanded dkey\n"
    + "  " + subsetsTagNm + "      List of 0 or more subsets in section 4\n"
    + "  " + subsetTagNm + "       One subset\n"
    + "  " + sequenceTagNm + "     Dkey for a sequence (f=3)\n"
    + "  " + loopTagNm + "         Dkey for a loop (f=1) over the following descriptors\n"
    + "  " + groupTagNm + "        Dkey for one iteration of a loop\n"
    + "  " + charTagNm + "         Dkey for character data\n"
    + "  " + numTagNm + "       Dkey for a numeric value like dkey=0_12_001, temperature\n"
    + "  " + codeTagNm + "         Dkey for an integer code like dkey=0_02_011, radiosonde type\n"
    + "  " + flagTagNm + "         Dkey for bit flags like dkey=0_08_001, sounding signif\n"
    + "  " + controlTagNm + "      Any of the f=2 control codes\n"
    + "  " + afldTagNm + "     Dkey for associated field, created by dkey 2_04_yyy.\n"
    + "\n"
    + "The most common attributes used in data items are:\n"
    + "  " + dkeyAttrNm + "       The dkey, f_xx_yyy\n"
    + "  " + valueAttrNm + "       The item's value.\n"
    + "  " + missingAttrNm + "     Indicates the value is missing\n"
    + "  " + unitAttrNm + "       The units string, if available\n"
    + "  " + meaningAttrNm + "     For \"code\" and \"flag\", the string interpretation\n"
    + "              of the value, if available.\n"
    + "  " + itersAttrNm + "       For loops, the number of iterations.\n"
    + "              This is the number of \"" + groupTagNm + "\" tags within the loop.\n"
    + "  " + noteAttrNm + "        Description of the item, if available.\n";

  if (showLevel)
    usageMsg += "  " + levelAttrNm + "       nesting level for data items.\n";

  usageMsg += ""
    + "  " + idAttrNm + "          If non-zero, the " + idAttrNm + " is a unique id for each "
    + expandDkeyTagNm + ".\n"
    + "              Within a subset, each " + idAttrNm + " attribute refers to the\n"
    + "              corresponding " + expandDkeyTagNm + ".\n"
    + "\n"
    + "For documentation on the WMO BUFR format see:\n"
    + "  Guide to WMO Table-Driven Code Forms\n"
    + "  FM 94 BUFR and FM 95 CREX\n"
    + "  http://www.wmo.ch/pages/prog/www/WDM/Guides/BUFRCREXGuide-English.html\n"
    + "For documentation on WMO BUFR tables see:\n"
    + "  WMO Operational Codes TAC - BUFR - CREX - GRIB\n"
    + "  http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html\n"
    + "\n";

  mkDoc( isXml, sbuf, usageMsg);

  mkOpenTagLn( isXml, messageTagNm, sbuf);
  mkAttrIntLn( isXml, "msgNum", bmsg.msgNum, bfile.outStyle, sbuf);
  mkCloseTagLn( isXml, sbuf);

  sbuf.append( formatHeader( isXml, bfile, bmsg, encodingName));

  sbuf.append( formatConvertInfo( isXml, bfile, bmsg, encodingName));

  if (bfile.parseStage >= BufrFile.STAGE_LOCAL)
    sbuf.append( formatSection2( isXml, bmsg));

  if (bfile.parseStage >= BufrFile.STAGE_DKEY) {
    sbuf.append( formatDescInfo( isXml, bfile, bmsg, encodingName));
    sbuf.append( formatCodeList( isXml, bfile.outStyle, bmsg.fxyList, bmsg));
  }

  if (bfile.parseStage >= BufrFile.STAGE_EXPDKEY)
    sbuf.append( formatDefDescTree( isXml, bfile.outStyle, bmsg.defRoot));


  if (bfile.parseStage >= BufrFile.STAGE_DATA) {
    mkStartTagLn( isXml, subsetsTagNm, sbuf);
    // If BufrMessage.readData throws an Exception, we get parser == null.
    if (bmsg.parser == null) {
      // xxx how to signify bad data?  just leave it with no subsets.
    }
    else {
      for (int isub = 0; isub < bmsg.numSubsets; isub++) {
        BufrItem rootItem = bmsg.parser.rootItems[isub];

        // Format the subset's entire xml tree
        sbuf.append("\n");
        sbuf.append( formatBufrItemTree(
          isXml, rootItem, bmsg, isub));
      } // for isub
    } // else parser is valid
    mkEndTagLn( isXml, subsetsTagNm, 0, sbuf);
  } // if STAGE_DATA

  mkEndTagLn( isXml, messageTagNm, 0, sbuf);

  writeOutFile(             // Write output file and report line
    bfile,
    bmsg,
    encodingName,
    sbuf.toString());
} // end writeAllOutput





/**
 * Creates the XML header
 */

static String formatHeader(
  boolean isXml,
  BufrFile bfile,
  BufrMessage bmsg,
  String encodingName)
throws BufrException
{
  int outStyle = bfile.outStyle;
  // Init XML
  boolean section2Present = false;
  if (bmsg.hdrSection2Flag) section2Present = true;

  StringBuilder sbuf = new StringBuilder();

  mkOpenTagLn( isXml, hdrInfoTagNm, sbuf);

  // Section 0 header info
  mkAttrIntLn( isXml, "edition", bmsg.hdrBufrEdition, outStyle, sbuf);

  // Section 1 header info
  mkAttrIntLn( isXml, "centre", bmsg.hdrCentre, outStyle, sbuf);
  mkAttrIntLn( isXml, "subCentre", bmsg.hdrSubCentre, outStyle, sbuf);
  mkAttrIntLn( isXml, "updateSeqNum", bmsg.hdrUpdateSeqNum, outStyle, sbuf);
  mkAttrBoolLn( isXml, "section2Present", section2Present, outStyle, sbuf);
  mkAttrIntLn( isXml, "category", bmsg.hdrCategory, outStyle, sbuf);
  mkAttrLn( isXml, "categoryName", bmsg.hdrCategoryName, outStyle, sbuf);
  mkAttrIntLn( isXml, "internatSubCategory", bmsg.hdrInternatSubCategory,
    outStyle, sbuf);
  mkAttrIntLn( isXml, "localSubCategory", bmsg.hdrLocalSubCategory,
    outStyle, sbuf);
  mkAttrIntLn( isXml, "masterTableNum", bmsg.hdrMasterTableNum,
    outStyle, sbuf);
  mkAttrIntLn( isXml, "masterTableVersion", bmsg.hdrMasterTableVersion,
    outStyle, sbuf);
  mkAttrIntLn( isXml, "localTableVersion", bmsg.hdrLocalTableVersion,
    outStyle, sbuf);
  mkAttrIntLn( isXml, "year", bmsg.hdrYear, outStyle, sbuf);
  mkAttrIntLn( isXml, "month", bmsg.hdrMonth, outStyle, sbuf);
  mkAttrIntLn( isXml, "day", bmsg.hdrDay, outStyle, sbuf);
  mkAttrIntLn( isXml, "hour", bmsg.hdrHour, outStyle, sbuf);
  mkAttrIntLn( isXml, "minute", bmsg.hdrMinute, outStyle, sbuf);
  mkAttrIntLn( isXml, "second", bmsg.hdrSecond, outStyle, sbuf);

  mkFullTagLn( isXml, sbuf);
  return sbuf.toString();
}





static String formatConvertInfo(
  boolean isXml,
  BufrFile bfile,
  BufrMessage bmsg,
  String encodingName)
throws BufrException
{
  int outStyle = bfile.outStyle;
  // Init XML

  // Get convertFile = the portion of inFile after the last slash.
  String convertFile = BufrUtil.getFilenameTail( bfile.inFile);

  // Get convertPath = full inFile path.
  String convertPath = BufrUtil.getCanonicalPath( bfile.inFile);

  // Get convertUtcDateStg = time right now.
  SimpleTimeZone utctz = new SimpleTimeZone( 0, "UTC");
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  sdf.setTimeZone( utctz);
  String convertUtcDateStg = sdf.format(
    new Date( System.currentTimeMillis()));

  // Get convertHost = current hostname
  String convertHost = "";
  try {
    InetAddress hostAddr = InetAddress.getLocalHost();
    convertHost = hostAddr.getHostName();
  }
  catch (UnknownHostException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("caught: " + exc);
  }

  // Finally create the text or XML
  StringBuilder sbuf = new StringBuilder();

  mkOpenTagLn( isXml, convertInfoTagNm, sbuf);

  mkAttrLn( isXml, "convertFile", convertFile, outStyle, sbuf);
  mkAttrLn( isXml, "convertPath", convertPath, outStyle, sbuf);
  mkAttrLn( isXml, "convertHost", convertHost, outStyle, sbuf);
  mkAttrLn( isXml, "convertUtcDate", convertUtcDateStg, outStyle, sbuf);

  mkFullTagLn( isXml, sbuf);
  return sbuf.toString();
}









static String formatDescInfo(
  boolean isXml,
  BufrFile bfile,
  BufrMessage bmsg,
  String encodingName)
throws BufrException
{
  int outStyle = bfile.outStyle;
  StringBuilder sbuf = new StringBuilder();

  mkOpenTagLn( isXml, dkeyInfoTagNm, sbuf);

  // Section 3 header info
  mkAttrIntLn( isXml, "numSubsets", bmsg.numSubsets, outStyle, sbuf);
  mkAttrBoolLn( isXml, "flagObserved", bmsg.flagObserved, outStyle, sbuf);
  mkAttrBoolLn( isXml, "flagCompressed", bmsg.flagCompressed, outStyle, sbuf);

  // Misc info
  ///mkAttrIntLn( isXml, "sectionLength0", bmsg.section0.length, outStyle, sbuf);
  ///mkAttrIntLn( isXml, "sectionLength1", bmsg.section1.length, outStyle, sbuf);
  ///mkAttrIntLn( isXml, "sectionLength2", bmsg.section2.length, outStyle, sbuf);
  ///mkAttrIntLn( isXml, "sectionLength3", bmsg.section3.length, outStyle, sbuf);
  ///mkAttrIntLn( isXml, "sectionLength4", bmsg.section4.length, outStyle, sbuf);
  ///mkAttrIntLn( isXml, "sectionLength5", bmsg.section5.length, outStyle, sbuf);

  int numDesc = 0;
  if (bmsg.fxyList != null) numDesc = bmsg.fxyList.size();
  mkAttrIntLn( isXml, "numDescriptors", numDesc, outStyle, sbuf);

  mkFullTagLn( isXml, sbuf);
  return sbuf.toString();
}






/**
 * Formats the local section2 info as a printable string.
 */
static String formatSection2(
  boolean isXml,
  BufrMessage bmsg)
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  mkStartTagLn( isXml, localInfoTagNm, sbuf);
  String stg;
  if (isXml) stg = BufrUtil.cleanXml( bmsg.section2Stg);
  else stg = BufrUtil.cleanText( bmsg.section2Stg);
  sbuf.append( stg);
  sbuf.append("\n");
  mkEndTagLn( isXml, localInfoTagNm, 0, sbuf);
  return sbuf.toString();
}








static String formatCodeList(
  boolean isXml,
  int outStyle,
  FxyList fxyList,
  BufrMessage bmsg)
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  mkStartTagLn( isXml, dkeysTagNm, sbuf);
  int len = fxyList.size();
  int[] modIndents = new int[len];
  for (int ii = 0; ii < len; ii++) {
    int fxy = fxyList.getIx( ii);

    mkFxyStartTag( isXml, dkeyTagNm, fxy, modIndents[ii], sbuf);
    String desc = bmsg.getDescription( fxy);
    if (desc.length() > 0)
      mkAttrTrunc( isXml, noteAttrNm, desc, outStyle, sbuf);
    mkFullTagLn( isXml, sbuf);

    // If iteration, indent the following ones more
    int fval = BufrUtil.getFval( fxy);
    int xval = BufrUtil.getXval( fxy);
    int yval = BufrUtil.getYval( fxy);

    if (fval == 1) {            // if iteration
      if (yval == 0) xval++;    // + 1 for delayed count element
      boolean hitEnd = false;
      for (int jj = 0; jj < xval; jj++) {
        int ix = ii + 1 + jj;      // start indents after the iterate
        if (ix >= len) {
          hitEnd = true;
          break;
        }
        modIndents[ix]++;
      }
      if (bmsg.bugs >= 5 && hitEnd) {
        sbuf.append("formatCodeList: iterator goes past end of list by "
          + (ii + 1 + xval - len) + " elements\n");
      }
    } // if iteration

  } // for ii
  mkEndTagLn( isXml, dkeysTagNm, 0, sbuf);
  return sbuf.toString();
} // end formatCodeList











static void formatDefDesc(
  boolean isXml,
  int outStyle,
  DefDesc def,
  int indent,
  StringBuilder sbuf)
throws BufrException
{
  mkFxyStartTag( isXml, expandDkeyTagNm, def.fxy, indent, sbuf);

  if (def.fval == 1 && def.countDef != null) {    // if delayed rep
    mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
    mkFullTagLn( isXml, sbuf);
    formatDefDesc( isXml, outStyle, def.countDef, indent + 1, sbuf);
  }
  else {
    if (def.isNumeric && def.unit.length() > 0)
      mkAttrTrunc( isXml, unitAttrNm, def.unit, outStyle, sbuf);
    if (def.description.length() > 0)
      mkAttrTrunc( isXml, noteAttrNm, def.description, outStyle, sbuf);
    mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
    mkFullTagLn( isXml, sbuf);  // end either countDef or def
  }
}





static String formatDefDescTree(
  boolean isXml,
  int outStyle,
  DefDesc def)
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  mkStartTagLn( isXml, expandDkeysTagNm, sbuf);
  formatDefDescTreeSub( isXml, outStyle, def, 0, sbuf);    // indent = 0
  mkEndTagLn( isXml, expandDkeysTagNm, 0, sbuf);
  return sbuf.toString();
}



static void formatDefDescTreeSub(
  boolean isXml,
  int outStyle,
  DefDesc def,
  int indent,
  StringBuilder sbuf)
throws BufrException
{
  formatDefDesc( isXml, outStyle, def, indent, sbuf);

  // Recurse on the subDefs
  for (DefDesc subDef : def.subDefs) {
    formatDefDescTreeSub( isXml, outStyle, subDef, indent + 1, sbuf);
  }
}









static void formatBufrItem(
  boolean isXml,
  BufrItem bitem,
  BufrMessage bmsg,
  int isubset,
  int indent,
  StringBuilder sbuf)
throws BufrException
{
  int outStyle = bmsg.bfile.outStyle;
  DefDesc def = bitem.def;
  String description = null;
  if (def.description != null && def.description.length() > 0)
    description = def.description;

  // Handle anything that might have subItems.
  if (def.fxy == BufrMessage.CUSTOM_SUBSET_FXY      // subset root
    || def.fxy == BufrMessage.CUSTOM_REPGROUP_FXY   // repgroup
    || def.fval == 1                                // replication, repetition
    || def.fval == 3)                               // sequence
  {
    // Write the start xml tag and attributes
    if (def.fxy == BufrMessage.CUSTOM_SUBSET_FXY) {         // if subset root
      mkFxyStartTag( isXml, subsetTagNm, def.fxy, indent, sbuf);
      mkAttrInt( isXml, "msgNum", bmsg.msgNum, outStyle, sbuf);
      mkAttrInt( isXml, "setNum", isubset, outStyle, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkCloseTagLn( isXml, sbuf);
    }
    else if (def.fxy == BufrMessage.CUSTOM_REPGROUP_FXY) {  // if repgroup
      mkFxyStartTag( isXml, groupTagNm, def.fxy, indent, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkCloseTagLn( isXml, sbuf);
    }
    else if (def.fval == 1) {                               // if replication
      mkFxyStartTag( isXml, loopTagNm, def.fxy, indent, sbuf);
      mkAttrInt( isXml, itersAttrNm, bitem.numIters, outStyle, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkCloseTagLn( isXml, sbuf);
    }
    else if (def.fval == 3) {                               // if sequence
      mkFxyStartTag( isXml, sequenceTagNm, def.fxy, indent, sbuf);
      if (description != null)
        mkAttrTrunc( isXml, noteAttrNm, description, outStyle, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkCloseTagLn( isXml, sbuf);
    }
    else throwerr("unknown type.  def: " + def);

    // Recursion to subItems
    for (BufrItem subItem : bitem.subItems) {
      formatBufrItem( isXml, subItem, bmsg, isubset,
        indent + 1, sbuf);
    }

    // Now the ending xml tag
    if (def.fxy == BufrMessage.CUSTOM_SUBSET_FXY) {         // if subset root
      mkEndTagLn( isXml, subsetTagNm, indent, sbuf);
    }
    else if (def.fxy == BufrMessage.CUSTOM_REPGROUP_FXY) {  // if repgroup
      mkEndTagLn( isXml, groupTagNm, indent, sbuf);
    }
    else if (def.fval == 1) {                               // if replication
      mkEndTagLn( isXml, loopTagNm, indent, sbuf);
    }
    else if (def.fval == 3) {                               // if sequence
      mkEndTagLn( isXml, sequenceTagNm, indent, sbuf);
    }
    else throwerr("unknown type.  def: " + def);

  }  // if recursion needed

  else if (def.fval == 2) {          // if control
    mkFxyStartTag( isXml, controlTagNm, def.fxy, indent, sbuf);
    if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
    mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
    mkFullTagLn( isXml, sbuf);
  }

  // BufrValue stuff.  Yes, this could be done by having
  // a BufrValue method overload formatBufrItem.  But this way
  // is easier to maintain.
  else if (bitem instanceof BufrValue) {
    BufrValue bvalue = (BufrValue) bitem;

    if (def.isString) {
      mkFxyStartTag( isXml, charTagNm, def.fxy, indent, sbuf);
      mkAttr( isXml, valueAttrNm, bvalue.stringValue, outStyle, sbuf);
      if (description != null)
        mkAttrTrunc( isXml, noteAttrNm, description, outStyle, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkFullTagLn( isXml, sbuf);
    }
    else if (def.isNumeric || def.isCode || def.isBitFlag) {
      String tagMsg = null;
      if (def.fxy == BufrMessage.CUSTOM_ASSOCFLD_FXY) tagMsg = afldTagNm;
      else if (def.isNumeric) tagMsg = numTagNm;
      else if (def.isCode) tagMsg = codeTagNm;
      else if (def.isBitFlag) tagMsg = flagTagNm;
      else throwerr("inconsistent def type");

      mkFxyStartTag( isXml, tagMsg, def.fxy, indent, sbuf);
      if (bvalue.bstatus == BufrValue.BST_MISSING) {
        mkAttr( isXml, missingAttrNm, "true", outStyle, sbuf);
        mkAttr( isXml, valueAttrNm, missingValueNm, outStyle, sbuf);
      }
      else {
        if (def.isNumeric) {
          mkAttr( isXml, valueAttrNm, bvalue.stringValue, outStyle, sbuf);
          mkAttrTrunc( isXml, unitAttrNm, def.unit, outStyle, sbuf);
        }
        else {            // else isCode or isFlag
          mkAttr( isXml, valueAttrNm, bvalue.stringValue, outStyle, sbuf);
          if (bvalue.codeFlagMeaning != null)
            mkAttrTrunc( isXml, meaningAttrNm, bvalue.codeFlagMeaning,
              outStyle, sbuf);
        }
      }
      if (description != null)
        mkAttrTrunc( isXml, noteAttrNm, description, outStyle, sbuf);
      if (showLevel) mkAttrInt( isXml, "level", indent, outStyle, sbuf);
      mkAttrInt( isXml, idAttrNm, def.defId, outStyle, sbuf);
      mkFullTagLn( isXml, sbuf);
    }
    else throwerr("unknown BufrValue: " + bvalue);
  } // if bitem instanceof BufrValue
  else throwerr("unknown BufrItem: " + bitem);

} // end formatBufrItem





static String formatBufrItemTree(
  boolean isXml,
  BufrItem bitem,
  BufrMessage bmsg,
  int isubset)
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  formatBufrItem( isXml, bitem, bmsg, isubset, 0, sbuf);
  return sbuf.toString();
}







// Write text or XML output file.
// Write one report line to stdout, if reportSpec was specified.

static void writeOutFile(
  BufrFile bfile,
  BufrMessage bmsg,
  String encodingName,
  String xmlStg)
throws BufrException
{
  // Write text or XML output
  if (bmsg.bugs >= 1) prtln("writeOutFile:");
  String outFile = getOutSpecName(
    bfile,
    bmsg,
    FOUT_DATA,
    bfile.outSpec,
    null);     // outFile.  The #outFile# spec is only valid for reportSpec.
  if (bmsg.bugs >= 1) {
    prtln("  msgNum: " + bmsg.msgNum);
    prtln("  output spec:      \"" + bfile.outSpec + "\"");
    prtln("  output file name: \"" + outFile + "\"");
  }

  // Write output file
  try {
    if (new File(outFile).exists())
      throwerr("output file already exists: \"" + outFile + "\"");
    FileOutputStream ostm = new FileOutputStream( outFile);
    OutputStreamWriter wtr = new OutputStreamWriter( ostm, encodingName);
    if (bmsg.bugs >= 1) prtln("writeOutFile: wtr.encoding: \""
      + wtr.getEncoding() + "\"");
    wtr.write( xmlStg, 0, xmlStg.length());
    wtr.close();
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("cannot write file \"" + outFile + "\"");
  }

  // Write report line
  if (bfile.reportSpec != null) {
    String reportLine = getOutSpecName(
      bfile,
      bmsg,
      FOUT_REPORT,
      bfile.reportSpec,
      outFile);
    prtln( reportLine);
  }
} // end writeOutFile







/**
 * Creates an output file name or report line according to
 * the specifications in the parm outSpec.  Called by writeOutFile.
 * The parm outSpec may be either BufrFile.outSpec or BufrFile.reportSpec.
 * <p>
 * For info on the specification of file names and report output lines,
 * see {@link BufrFile#printHelpUsage Usage info in BufrFile.printHelpUsage}.
 */

static String getOutSpecName(
  BufrFile bfile,
  BufrMessage bmsg,
  int foutType,        // one of FOUT_*
  String outSpec,      // outSpec or reportSpec 
  String outFile)      // The #outFile# spec is only valid for reportSpec.
throws BufrException
{
  String delim = "#";

  StringBuilder outNameBuf = new StringBuilder();
  Formatter fmtr = new Formatter( outNameBuf);
  int specLen = outSpec.length();
  if (bmsg.bugs >= 5) prtln("getOutSpecName: outSpec: \"" + outSpec + "\"");

  int ipos = 0;

  while (ipos < specLen) {
    if (outSpec.startsWith( delim, ipos)) {
      ipos++;

      // Scan for ending delim of code like "#year#".
      int ix = outSpec.indexOf( delim, ipos);
      if (ix < 0) throwOutSpec("no ending \"#\"", outSpec.substring(ipos));
      String origOutCode = outSpec.substring( ipos, ix);
      ipos = ix + 1;

      String resstg = getOutSpecCode(
        bfile, bmsg, foutType, origOutCode, outFile);
      fmtr.format( "%s", resstg);
    } // if outSpec startsWith delim

    else {
      fmtr.format( "%c", outSpec.charAt(ipos));
      ipos++;
    }
  } // while ipos < specLen


  String outName = outNameBuf.toString();
  if (bmsg.bugs >= 5)
    prtln("getOutSpecName: outName: \"" + outName + "\"");
  return outName;
} // end getOutSpecName







/**
 * Translates an outSpec code like "year" to "2008".
 * The caller has stripped off the enclosing "#" chars; it was "#year#".
 * Also handles min/max len specs like "hour:02"
 * which produces "05" for hour 5.
 * See {@link BufrFile#printHelpUsage Usage info in BufrFile.printHelpUsage}.
 */

static String getOutSpecCode(
  BufrFile bfile,
  BufrMessage bmsg,
  int foutType,        // one of FOUT_*
  String origOutCode,      // original outCode, like "year"
  String outFile)      // The #outFile# spec is only valid for reportSpec.
throws BufrException
{
  if (bmsg.bugs >= 10)
    prtln("  getOutSpecCode: origOutCode: \"" + origOutCode + "\"");

  String outCode = origOutCode;
  if (outCode == null || outCode.length() == 0)
    throwOutSpec("empty outCode", origOutCode);

  // Check for leading "^".
  // Normally if if findTreeItem returns null, we throwOutSpec.
  // But if "^" is specified, we return the string "none".
  boolean allowNotFound = false;
  if (outCode.startsWith("^")) {
    allowNotFound = true;
    outCode = outCode.substring(1);
  }

  // Check outCode for leading subsetNum; init rootItem
  BufrItem rootItem = null;
  int subsetNum;
  char cc = outCode.charAt(0);
  if (cc >= '0' && cc <= '9') {
    int ix = outCode.indexOf(":");
    if (ix < 0) throwOutSpec("missing code name", origOutCode);
    String tstg = outCode.substring( 0, ix);
    outCode = outCode.substring( ix + 1);
    subsetNum = BufrUtil.parseInt("subsetNum", tstg);

    if (bmsg.parser != null
      && subsetNum >= 0
      && subsetNum < bmsg.parser.rootItems.length)
    {
      rootItem = bmsg.parser.rootItems[subsetNum];
    }
    else if (! allowNotFound) {
      throwOutSpec("Either data parse failed or bad subsetNum", origOutCode);
    }
  }
  else {                   // else no subsetNum specified
    subsetNum = 0;         // default is subset 0
    if (bmsg.parser != null && bmsg.parser.rootItems.length >= 1)
      rootItem = bmsg.parser.rootItems[subsetNum];
  }

  // Check outCode for trailing fieldMin, fieldMax
  int fieldMin = 0;
  int fieldMax = 0;
  char fieldPad = ' ';
  int ix = outCode.indexOf(":");
  if (ix >= 0) {
    int ixsv = ix;
    ix++;           // skip over ":"
    if (ix >= outCode.length()) throwOutSpec("trailing \":\"", origOutCode);
    // Get minLen
    if (outCode.charAt(ix) != ':') {    // if they didn't spec "code::maxlen"
      // Get pad character
      fieldPad = outCode.charAt(ix);
      ix++;      // skip over fieldPad
      if (fieldPad >= '1' && fieldPad <= '9')
        throwOutSpec("invalid pad char \"" + fieldPad + "\"", origOutCode);
      // get minLen
      if (ix >= outCode.length()) throwOutSpec("missing minLen", origOutCode);
      int iy = outCode.indexOf(":", ix);
      if (iy < 0) {     // if no maxLen specified
        fieldMin = BufrUtil.parseInt(
          "-report or -outSpec minLen", outCode.substring( ix));
        ix = outCode.length();
      }
      else {            // else maxLen specified
        // Get minLen and advance ix.
        fieldMin = BufrUtil.parseInt(
          "-report or -outSpec minLen", outCode.substring( ix, iy));
        ix = iy;
      }
    }

    // Get maxLen
    if (ix < outCode.length()) {    // if they specified maxLen
      if (outCode.charAt(ix) != ':')
        throwOutSpec("invalid maxLen", origOutCode);
      ix++;           // skip over ":"
      if (ix >= outCode.length()) throwOutSpec("trailing \":\"", origOutCode);
      fieldMax = BufrUtil.parseInt(
        "-report or -outSpec maxLen", outCode.substring( ix));
    }

    outCode = outCode.substring( 0, ixsv);
  }

  if (bmsg.bugs >= 10) {
    prtln("    final outCode: \"" + outCode + "\"");
    prtln("    fieldMin: " + fieldMin
      + "  fieldMax: " + fieldMax
      + "  fieldPad: \"" + fieldPad + "\"");
  }

  String resstg = null;    // resulting string

  if (outCode.equals("inFile")) {
    // Use the portion of inFile after the last slash.
    resstg = BufrUtil.getFilenameTail( bfile.inFile);
  }

  else if (outCode.equals("inFileBase")) {
    // Use the base name portion of inFile after the last slash.
    resstg = BufrUtil.getBaseName(
      BufrUtil.getFilenameTail( bfile.inFile));
  }

  else if (outCode.equals("inPath")) {
    resstg = BufrUtil.getCanonicalPath( bfile.inFile);
  }

  else if (outCode.equals("inPathBase")) {
    resstg = BufrUtil.getBaseName(
      BufrUtil.getCanonicalPath( bfile.inFile));
  }

  else if (outCode.equals("outFile")) {
    if (foutType != FOUT_REPORT) {
      throwOutSpec(
        "getOutSpecName: outCode is ok in -report but is invalid in -outSpec",
        origOutCode);
    }
    // Use the portion of outFile after the last slash.
    resstg = BufrUtil.getFilenameTail( outFile);
  }

  else if (outCode.equals("outPath")) {
    if (foutType != FOUT_REPORT) {
      throwOutSpec(
        "getOutSpecName: outCode is ok in -report but is invalid in -outSpec",
        origOutCode);
    }
    resstg = BufrUtil.getCanonicalPath( outFile);
  }

  else if (outCode.equals("year"))
    resstg = Integer.toString( bmsg.hdrYear);
  else if (outCode.equals("month"))
    resstg = Integer.toString( bmsg.hdrMonth);
  else if (outCode.equals("day"))
    resstg = Integer.toString( bmsg.hdrDay);
  else if (outCode.equals("hour"))
    resstg = Integer.toString( bmsg.hdrHour);
  else if (outCode.equals("minute"))
    resstg = Integer.toString( bmsg.hdrMinute);
  else if (outCode.equals("second"))
    resstg = Integer.toString( bmsg.hdrSecond);

  else if (outCode.equals("category"))
    resstg = Integer.toString( bmsg.hdrCategory);
  else if (outCode.equals("categoryName"))
    resstg = bmsg.hdrCategoryName;
  else if (outCode.equals("msgNum"))
    resstg = Integer.toString( bmsg.msgNum);

  else if (outCode.equals("sectionLength0"))
    resstg = Integer.toString( bmsg.section0.length);
  else if (outCode.equals("sectionLength1"))
    resstg = Integer.toString( bmsg.section1.length);
  else if (outCode.equals("sectionLength2"))
    resstg = Integer.toString( bmsg.section2.length);
  else if (outCode.equals("sectionLength3"))
    resstg = Integer.toString( bmsg.section3.length);
  else if (outCode.equals("sectionLength4"))
    resstg = Integer.toString( bmsg.section4.length);
  else if (outCode.equals("sectionLength5"))
    resstg = Integer.toString( bmsg.section5.length);

  else if (outCode.equals("numTemplates"))
    resstg = Integer.toString( bmsg.fxyList.size());


  // The following items require parsing of sections 3, 4.
  // They cannot be used if we don't parse the data.
  else {
    if (bfile.parseStage < BufrFile.STAGE_DATA) {
      throwOutSpec("This outCode is illegal when -parseStage is not \"data\"",
        origOutCode);
    }

    if (rootItem == null) {
      if (allowNotFound) resstg = "none";
      else throwOutSpec("Either parse failed or bad subsetNum", origOutCode);
    }

    else if (outCode.equals("repDescs") || outCode.equals("repIters")) {
      BufrItem item = findTreeItem(
        bmsg.bugs, 1, -1, -1, rootItem, 1);   // f, x, y, rt, ix
      if (item == null) {
        if (allowNotFound) resstg = "none";
        else resstg = null;        // causes throwOutSpec at end
      }
      else if (outCode.equals("repDescs"))
        resstg = Integer.toString(item.numDescs);
      else resstg = Integer.toString( item.numIters);
    }

    else if (outCode.startsWith("value_")) {
      int ifxy = 6;

      String dot = ".";
      // Set inum = start of ".numDesired"
      int inum = outCode.indexOf( dot, ifxy+1);
      if (inum < 0) inum = outCode.length();

      int fxy = BufrUtil.parseFxy("-report or -outSpec value fxy",
         outCode.substring( ifxy, inum));
      int fval = BufrUtil.getFval( fxy);
      if (fval != 0)
        throwOutSpec("value_fxy must have fval = 0", origOutCode);

      //xxx use origin 0?
      int numDesired = 1;
      if (inum < outCode.length()) {     // if numDesired specified
        inum++;                      // skip over dot
        if (inum >= outCode.length())
          throwOutSpec("trailing \"" + dot + "\"", origOutCode);
        numDesired = BufrUtil.parseInt("numDesired", outCode.substring( inum));
      }

      // Search the tree for the item
      BufrItem item = findTreeItem( bmsg.bugs, fxy, rootItem, numDesired);

      if (item == null) {
        if (allowNotFound) resstg = "none";
        else resstg = null;        // causes throwOutSpec at end
      }
      else {      // else we found the item
        if (! (item instanceof BufrValue))
          throwOutSpec("fxy is not a value type: "
            + BufrUtil.formatFxy( fxy), origOutCode);
        BufrValue bvalue = (BufrValue) item;
        resstg = bvalue.stringValue;
      }
    } // if outCode starts with "value"

    else throwOutSpec("unknown outFile spec", origOutCode);
  } // else outCode requires parsing of sections 3 and 4

  if (bmsg.bugs >= 5) prtln("    origOutCode: \"" + origOutCode + "\""
    + "  raw resstg:   \"" + resstg + "\"");
  if (resstg == null) throwOutSpec("outCode not found", origOutCode);

  // Handle fieldMin: minimum field length
  while (resstg.length() < fieldMin) {
    resstg = fieldPad + resstg;
  }
  // Handle fieldMax: minimum field length
  if (fieldMax > 0 && resstg.length() > fieldMax)
    resstg = resstg.substring( 0, fieldMax);

  // xxx review these translations
  // Convert all white and unprintable to underbar
  resstg = BufrUtil.translateNonBlack( '_', resstg);

  // If output is a filename, clean up: / \ '' ""
  if (foutType != FOUT_REPORT)
    resstg = BufrUtil.translateChars( "/\\\'\"", '_', resstg);

  if (bmsg.bugs >= 5) prtln("    origOutCode: \"" + origOutCode + "\""
    + "  final resstg: \"" + resstg + "\"");
  return resstg;
} // end getOutSpecCode













/**
 * Searches the BufrItem tree starting at item == rootItem
 * using a preorder depth first search
 * and returns the numDesired'th BufrItem matching fxy,
 * or null if none are found.
 * Called by getOutSpecName to extract values from a BUFR message.
 * The parm item is the root of the tree to be searched,
 * and is set to rootItem when called by getOutSpecName.
 */

static BufrItem findTreeItem(       // returns numFound
  int bugs,
  int fxy,
  BufrItem item,
  int numDesired)
throws BufrException
{
  int fval = BufrUtil.getFval( fxy);
  int xval = BufrUtil.getXval( fxy);
  int yval = BufrUtil.getYval( fxy);
  return findTreeItem( bugs, fval, xval, yval, item, numDesired);
}


static BufrItem findTreeItem(       // returns numFound
  int bugs,
  int fval,
  int xval,
  int yval,
  BufrItem item,
  int numDesired)
throws BufrException
{
  FindTreeResult res = findTreeItemSub(
    bugs, fval, xval, yval, item, numDesired, 0);
  if (bugs >= 10) {
    prtln("findTreeItem: fval: " + fval
        + "  xval: " + xval
        + "  yval: " + yval
      + "  res: " + res);
  }
  return res.foundItem;
}






/**
 * Searches the BufrItem tree starting at item
 * using a preorder depth first search.
 * Searches until matching item number <code>numDesired</code> is found.
 * <p>
 * Returns a FindTreeResult containing the number of matching
 * items found in the subtree and the numDesired'th matching
 * foundItem, if any.
 */

static FindTreeResult findTreeItemSub(       // returns numFound, foundItem
  int bugs,
  int fval,
  int xval,
  int yval,
  BufrItem item,
  int numDesired,
  int numFound)
throws BufrException
{
  BufrItem foundItem = null;
  if (item != null) {
    if (bugs >= 20) {
      prtln("  findTreeItemSub: fval: " + fval
        + "  xval: " + xval
        + "  yval: " + yval
        + "  itemFval: " + BufrUtil.formatFxy( item.def.fxy));
    }
    
    if ( item.testFxy( fval, xval, yval)) {
      numFound++;
      if (numFound == numDesired) foundItem = item;
    }
    if (foundItem == null) {
      for (BufrItem subItem : item.subItems) {
        FindTreeResult res = findTreeItemSub(
          bugs, fval, xval, yval, subItem, numDesired, numFound);
        numFound = res.numFound;
        foundItem = res.foundItem;
        if (foundItem != null) break;
      }
    }
  }
  return new FindTreeResult( numFound, foundItem);
} // end findTreeItemSub





static void mkFxyStartTag(
  boolean isXml,
  String tag,
  int fxy,
  int indent,
  StringBuilder sbuf)
throws BufrException
{
  sbuf.append( BufrUtil.mkIndent( indent));
  if (isXml) sbuf.append("<" + tag);
  else sbuf.append( tag);
  sbuf.append(" " + dkeyAttrNm + "=\"" + BufrUtil.formatFxy( fxy) + "\"");
}











static void mkOpenTagLn(
  boolean isXml,
  String tag,
  StringBuilder sbuf)
{
  sbuf.append("\n");
  if (isXml) sbuf.append("<" + tag);
  else sbuf.append( tag);
  sbuf.append("\n");
}








static void mkStartTagLn(
  boolean isXml,
  String tag,
  StringBuilder sbuf)
{
  sbuf.append("\n");
  if (isXml) sbuf.append("<" + tag + ">");
  else sbuf.append( tag);
  sbuf.append("\n");
}





static void mkCloseTagLn(
  boolean isXml,
  StringBuilder sbuf)
{
  if (isXml) {
    sbuf.append(">");
  }
  sbuf.append("\n");
}





static void mkFullTagLn(
  boolean isXml,
  StringBuilder sbuf)
{
  if (isXml) {
    sbuf.append("/>");
  }
  sbuf.append("\n");
}







static void mkEndTagLn(
  boolean isXml,
  String tag,
  int indent,
  StringBuilder sbuf)
{
  sbuf.append( BufrUtil.mkIndent( indent));
  if (isXml) sbuf.append("</" + tag + ">\n");
  else sbuf.append( tag + "_end\n");
}




static void mkAttrBase(
  boolean isXml,
  boolean isTruncate,
  String name,
  String value,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{

  if (name == null) throwerr("mkAttr: name is null");
  if (name.length() == 0) throwerr("mkAttr: name is empty");
  if (value == null) throwerr("mkAttr: value is null");
  // Allow value = "", for character strings

  if (outStyle == BufrFile.OUTSTYLE_TERSE && isTruncate) {
    // Do nothing: for TERSE mode, entirely skip attrs that
    // can be truncated.
  }
  else {
    String val = value;
    if (isTruncate) {
      int truncateLen = 0;
      if (outStyle == BufrFile.OUTSTYLE_FULL)
        truncateLen = 0;
      else if (outStyle == BufrFile.OUTSTYLE_STANDARD)
        truncateLen = 20;
      val = BufrUtil.truncate( truncateLen, val);
    }
    val = BufrUtil.quote( isXml, val);

    sbuf.append("  " + name + "=" + val);
  }
}



static void mkAttr(
  boolean isXml,
  String name,
  String value,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  mkAttrBase( isXml, false, name, value, outStyle, sbuf);
}


static void mkAttrInt(
  boolean isXml,
  String name,
  int ival,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  mkAttrBase( isXml, false, name, Integer.toString(ival), outStyle, sbuf);
}



static void mkAttrTrunc(
  boolean isXml,
  String name,
  String value,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  boolean isTruncate = false;
  if (outStyle != BufrFile.OUTSTYLE_FULL) isTruncate = true;
  mkAttrBase( isXml, isTruncate, name, value, outStyle, sbuf);
}


static void mkAttrLn(
  boolean isXml,
  String name,
  String value,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  mkAttrBase( isXml, false, name, value, outStyle, sbuf);
  sbuf.append("\n");
}



static void mkAttrIntLn(
  boolean isXml,
  String name,
  int ival,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  mkAttrBase( isXml, false, name, Integer.toString(ival), outStyle, sbuf);
  sbuf.append("\n");
}




static void mkAttrBoolLn(
  boolean isXml,
  String name,
  boolean bval,
  int outStyle,                 // one of BufrFile.OUTSTYLE_*
  StringBuilder sbuf)
throws BufrException
{
  mkAttrBase( isXml, false, name, Boolean.toString(bval), outStyle, sbuf);
  sbuf.append("\n");
}



static void mkDoc(
  boolean isXml,
  StringBuilder sbuf,
  String msg)
{
  sbuf.append("\n");
  if (isXml) sbuf.append("<!--\n");
  String[] lines = msg.split("\n");
  for (String line : lines) {
    if (! isXml) sbuf.append("#");
    sbuf.append("  ");
    sbuf.append( line);
    sbuf.append( "\n");
  }
  if (isXml) sbuf.append("-->\n");
  sbuf.append("\n");
}



static void throwOutSpec( String msg, String origTag)
throws BufrException
{
  String bigMsg = "Invalid code in -report or -outSpec:\n"
    + "  " + msg + "\n"
    + "  code: \"" + origTag + "\"";
  throwerr( bigMsg);
}



static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrFormatter: " + msg);
}




static void prtln( String msg) {
  System.out.println( msg);
}

} // end class
