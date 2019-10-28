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
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.LinkedList;



/**
 * Represents a file of BUFR messages and provides
 * conversion to XML.
 * <p>
 * This is the main driver for parsing BUFR files and converting to XML.
 * <p>
 * <b>To see the command line usage info, issue:</b><br>
 * <code> java -cp someClassPath wmoBufr.BufrFile </code>
 * <p>
 * For overall documentation on BUFR formats and tables see
 * the {@link wmoBufr wmoBufr} overview.: <br>
 * <p>
 * For more info also see {@link BufrMessage BufrMessage}. <br>
 * <p>
 * A BUFR file contains multiple BUFR messages.
 * There may be local information or garbage before and after
 * each message.  We have to scan for the starting sentinel "BUFR"
 * to find the start of the next message.
 * <p>
 * Each BUFR message is composed of the following sections:<br>
 * Octets are numbered starting with bit 1.<br>
 * Bits are numbered starting with bit 1.<br>
 * <ul>
 *
 * <li> Section 0: Indicator section.  Starts with the 4 byte
 *     string "BUFR", as a sentinel.
 *
 * <li> Section 1: Indentification section.
 *     Gives date of message, originating center, BUFR table version,
 *     data category (surface, buoy, satelite, etc).
 *     Octet 8 bit 1 indicates if section 2 is present.
 *
 * <li> Section 2: Optional local information.  Can be anything
 *     but usually is character strings.  Only present if section 1
 *     bit 8 is set.
 *
 * <li> Section 3: Data description section.  This section consists of
 *     a list of raw descriptors. The descriptors, also known as fxy values,
 *     are 16 bits and have the form: <br>
 *     fval   2 bits <br>
 *     xval   6 bits <br>
 *     yval   8 bits <br>
 *     The fxy values refer to entries in various WMO tables.
 *     Here I call them the "raw" descriptors as I haven't yet
 *     processed them.
 *     <p>
 *     Octets 5 - 6 indicate the number of subsets.  The descriptors
 *     in section 3 are reprocessed numSubsets times, each time
 *     processing more of the data of section 4.
 *     <p>
 *     Octet 7 bit 2 indicates that section 4 is in "compressed data" form.
 *     This software does not support "compressed data" format.
 *     I have never seen this flag set, but the code checks for
 *     it and throws a BufrException if it's set.
 *
 * <li> Section 4: Data section.  This section contains binary
 *     data to be interpreted according to the descriptors in section 3.
 *
 * <li> Section 5: End section.  Consists solely of the 4 byte string "7777",
 *     denoting the end of the BUFR message.
 * </ul>
 *
 * @author S. Sullivan
 */



  



class BufrFile {


// Inner class
static class TableSource {
  int versionMajor;
  int versionMinor;
  String dirName;

  TableSource(
    int versionMajor,
    int versionMinor,
    String dirName)
  {
    this.versionMajor = versionMajor;
    this.versionMinor = versionMinor;
    this.dirName = dirName;
  }

  public String toString() {
    String res = "versionMajor: " + versionMajor
      + "  versionMinor: " + versionMinor
      + "  dirName: " + dirName;
    return res;
  }
} // end inner class TableSource




static String programVersion = "1.5.1";

static int STAGE_UNKNOWN   = 0;           // parseStage values
static int STAGE_HEADER    = 1;           // parse headers
static int STAGE_LOCAL     = 2;           // section 2: local info
static int STAGE_DKEY      = 3;           // parse raw descriptors
static int STAGE_EXPDKEY   = 4;           // parse expanded descriptors
static int STAGE_DATA      = 5;           // parse data
static String[] stageNames = {
  "unknown", "hdr", "localInfo", "dkey", "expdkey", "data"};


static int OUTFORMAT_UNKNOWN = 0;         // outFormat values
static int OUTFORMAT_TEXT    = 1;
static int OUTFORMAT_XML     = 2;
static String[] outFormatNames = {
  "unknown", "text", "xml"};


static int OUTSTYLE_UNKNOWN  = 0;         // outStyle values
static int OUTSTYLE_TERSE    = 1;
static int OUTSTYLE_STANDARD = 2;
static int OUTSTYLE_FULL     = 3;
static String[] outStyleNames = {
  "unknown", "terse", "standard", "full"};




int tableBugs = 0;                    // debug level for parsing spec tables
int dataBugs = 0;                     // debug level for parsing user data

int parseStage = STAGE_UNKNOWN;       // One of STAGE_*
int outFormat = OUTFORMAT_UNKNOWN;    // OUTFORMAT_*: xml or text
int outStyle = OUTSTYLE_UNKNOWN;      // OUTSTYLE_*: standard or full

int tableVersionMajor;                // major table version
int tableVersionMinor;                // minor table version
TableCateg tableCateg = null;         // Categories: WMO BUFR table A.
TableCodeFlag tableCodeFlag = null;   // Codes and flags: WMO codes and flags
                                      //   associated with BUFR table B.
TableCommon tableCommon = null;       // WMO table of common codes
TableDesc tableDescription = null;    // WMO BUFR table B
TableSeq tableSequence = null;        // WMO BUFR table D


boolean validateFlag;                 // Default = false.
                                      // If true, validate header contents
                                      // and data contents such as
                                      // date fields and lat/lon

boolean forceFlag;                    // Default = false.
                                      // If true, continue even after
                                      // errors parsing section 4.

String inFile = null;                 // current input BUFR data file

String outSpec = null;                // output name spec for text or xml files

String reportSpec = null;             // spec for report lines, like outSpec

FileInputStream istm;                 // The input BUFR data file
int fileOffset;                       // current offset in istm, for debug

String errorMsgs = "";                // Normally "".  If forceFlag,
                                      // errorMsgs is a concat of error
                                      // msgs we encountered.

/**
 * Prints error message and exits with rc 1.
 */
static void badparms( String msg) {
  prtln("");
  prtln("Error: " + msg);
  prtln("");
  prtln("For usage info use parameters: -help usage");
  prtln("For copyright info use parameters: -help copyright");
  prtln("");
  System.exit(1);
}


/**
 * Prints usage information
 */
static void printHelpUsage() {

  prtln("");
  prtln("Types:");
  prtln("  stg   means a string");
  prtln("  y/n   means either y or n, for a boolean");
  prtln("  int   means an non-negative integer");
  prtln("");
  prtln("Parameter     Type  Description");
  prtln("----------    ---   ---------------");
  prtln("-help         stg   usage: print this message");
  prtln("                    copyright: print copyright message");
  prtln("");
  prtln("-debugTable   int   debug level for parsing the tables");
  prtln("");
  prtln("-debugData    int   debug level for parsing the input file");
  prtln("");
  prtln("-parseStage   stg   one of: hdr localInfo dkey expdkey data");
  prtln("                    Controls what parts of the BUFR message");
  prtln("                    are parsed and what parts of the output");
  prtln("                    files are created:");
  prtln("");
  prtln("         -parseStage         Sections produced");
  prtln("         ------------        -----------------");
  prtln("         hdr                 hdrInfo, convertInfo");
  prtln("         localInfo           as above and add: localInfo");
  prtln("         dkey                as above and add: dkeys");
  prtln("         expdkey             as above and add: expdkeys");
  prtln("         data                as above and add: subsets");
  prtln("");
  prtln("-outFormat    stg   one of: text xml");
  prtln("                    controls the format of the output files");
  prtln("");
  prtln("-outStyle     stg   one of: terse standard full");
  prtln("                    controls the amount of information in each");
  prtln("                    item of the output file.");
  prtln("");
  prtln("-allowDups    y/n   allow duplicate entries in tables");
  prtln("");
  prtln("-tabledir_m.n stg   directory containing formatted BUFR tables.");
  prtln("                    m is the major version; n is the minor version.");
  prtln("                    See \"Note on tables\" below");
  prtln("");
  prtln("-codeFlag     stg   input code flag table file name (may be repeated)");
  prtln("                    See \"Note on tables\" below");
  prtln("");
  prtln("-common       stg   input common codes table file name (may be repeated)");
  prtln("                    See \"Note on tables\" below");
  prtln("");
  prtln("-description  stg   input description table file name (may be repeated)");
  prtln("                    See \"Note on tables\" below");
  prtln("");
  prtln("-sequence     stg   input sequence table file name (may be repeated)");
  prtln("                    See \"Note on tables\" below");
  prtln("");
  prtln("-validate     stg   y/n  Default = y.");
  prtln("                    If y, validate header contents");
  prtln("                    and data contents such as");
  prtln("                    date fields and lat/lon.");
  prtln("");
  prtln("-force        stg   y/n  default = n.");
  prtln("                    If y, continue to the next message");
  prtln("                    after errors parsing section 4.");
  prtln("                    If false, quit on error.");
  prtln("");
  prtln("-outFormat    stg   text or xml");
  prtln("");
  prtln("-outStyle     stg   standard or full");
  prtln("");
  prtln("-inFile       stg   input data file.  May be repeated.  Example:");
  prtln("                    -inFile june.bufr -inFile july.bufr");
  prtln("");
  prtln("-inFiles      stg   space separated list of input files.  Example:");
  prtln("                    -inFiles \"june.bufr july.bufr\"");
  prtln("");
  prtln("-inList       stg   name of a file whose lines contain the");
  prtln("                    input file names.  Leading and trailing");
  prtln("                    white space are ignored.");
  prtln("                    Lines that are blank or that start with");
  prtln("                    \"#\", possibly with leading white space,");
  prtln("                    are treated as comments.");
  prtln("");
  prtln("-inListDir    stg   directory name to be prepended to the names");
  prtln("                    in inList.");
  prtln("                    For example, if inDir is \"/home/test/data\"");
  prtln("                    and the inList file contains:");
  prtln("                      june.bufr");
  prtln("                      july.bufr");
  prtln("                    then the files retrieved will be:");
  prtln("                      /home/test/data/june.bufr");
  prtln("                      /home/test/data/july.bufr");
  prtln("                    Default: \"\"");
  prtln("");
  prtln("-outSpec      stg   output name specification for XML files.");
  prtln("                    Multiple -outSpec values are concatenated.");
  prtln("                    There is a separate xml output file for each");
  prtln("                    BUFR subset.  So if a file contains two");
  prtln("                    messages, each with three subsets, there will");
  prtln("                    six output files.");
  prtln("                    Output files are never over-written.");
  prtln("                    See \"Output and report specifications\" below");
  prtln("");
  prtln("-report       stg   output name specification for report lines");
  prtln("                    to be written to stdout.");
  prtln("                    Multiple -report values are concatenated.");
  prtln("                    See \"Output and report specifications\" below");
  prtln("");
  prtln("");
  prtln("Note on tables:");
  prtln("  A directory containing formatted BUFR tables");
  prtln("  having major version m and minor version n");
  prtln("  is specified as: -tabledir_m.n");
  prtln("  Example:");
  prtln("  The version 13 tables would be specified as");
  prtln("    -tabledir_13.0 myTableDir");
  prtln("");
  prtln("  BufrFile will look within the specified tabledir");
  prtln("  for the following five files:");
  prtln("");
  prtln("    categTab.formatted            # categories - BUFR table A");
  prtln("    codeFlagTab.formatted         # BUFR table of codes and flags");
  prtln("    commonTab.formatted           # WMO common codes");
  prtln("    descTab.formatted             # descriptors - BUFR table B");
  prtln("    seqTab.formatted              # sequences - BUFR table D");
  prtln("");
  prtln("  There may be multiple -tabledir specifications.");
  prtln("  They must all have the same major version number.");
  prtln("  The minor version number of the i'th tabledir");
  prtln("  must be >= that of the (i-1)'th tabledir.");
  prtln("  ");
  prtln("  Each of the above tables must be found in SOME");
  prtln("  tabledir directory.  But if there are multiple");
  prtln("  tabledir specifications, some of the directories");
  prtln("  might contain fewer than all five of the files.");
  prtln("");
  prtln("  If there are multiple table files of the same type,");
  prtln("  all the specified files are used.");
  prtln("  If the same entry is found in multiple files,");
  prtln("  the last entry is used.");
  prtln("");
  prtln("");
  prtln("");
  prtln("Output and report specifications");
  prtln("");
  prtln("********************************************");
  prtln("* Although an input file may contain many BUFR messages,");
  prtln("* each BUFR message is written to a separate output file.");
  prtln("* To insure the output files have unique names,");
  prtln("* the -outSpec parameter should contain the code #msgNum#");
  prtln("*   and at least one of the codes:");
  prtln("*   #inFile#, #inFileBase#, #inPath#, #inPathBase#");
  prtln("* Example:");
  prtln("*   -outspec 'outDir/in_#inFileBase#_msg_#msgNum#.xml'");
  prtln("********************************************");
  prtln("");
  prtln("If the -outSpec or -report parameter is");
  prtln("specified more than once, the multiple values are concatenated.");
  prtln("");
  prtln("For example, Example A and Example B below are equivalent");
  prtln("(the \\ character indicates line continuation):");
  prtln("Example A:");
  prtln("  -outspec 'outDir/in_#inFile#_msg_#msgNum#.xml'");
  prtln("Example B:");
  prtln("  -outspec 'outDir/' \\");
  prtln("  -outspec 'in_#inFile#' \\");
  prtln("  -outspec '_msg_#msgNum#' \\");
  prtln("  -outspec '.xml'");
  prtln("");
  prtln("");
  prtln("");
  prtln("");
  prtln("");
  prtln("");
  prtln("The -outSpec and -report parameters");
  prtln("can contain codes as follows.");
  prtln("");
  prtln("Each code has one of the following structure, where []");
  prtln("indicates optional items:");
  prtln("    #[^][subsetNum:]code[:pad minLen][:maxLen]#");
  prtln("Some examples:");
  prtln("  #year#             insert the year from the BUFR message header");
  prtln("  #month:02#         insert the month from the BUFR message header,");
  prtln("                     padding with \"0\" to a min len of 2.");
  prtln("  #categoryName:_10:15  insert the category name from the");
  prtln("                     BUFR message header,");
  prtln("                     padding with \"_\" to a min len of 10,");
  prtln("                     and truncating if over 15 characters.");
  prtln("  #repIters#         insert the num iterations of the first loop in subset 0");
  prtln("  #repIters:02#      as above, and also format it like %02d");
  prtln("  #3:repIters:02#    as above, but get the num iters from");
  prtln("                     subset 3 instead of subset 0");
  prtln("  #^3:repIters:02#   as above, and if there is no subset 3 or");
  prtln("                       no loop in subset 3, insert the word");
  prtln("                       \"none\" instead of indicating an error.");
  prtln("");
  prtln("The minLen/maxLen specify the minimum/maximum output field");
  prtln("lengths.  The default values are 0, meaning use whatever length");
  prtln("the item requires.");
  prtln("");
  prtln("If minLen must be preceeded by a pad character, typically");
  prtln("blank or 0.");
  prtln("");
  prtln("For outSpec, all blanks and unprintables resulting");
  prtln("from substition of #code# items are converted to underbar.");
  prtln("");
  prtln("Valid codes are:");
  prtln("");
  prtln("  #inFile#     insert input file name without leading directories");
  prtln("               Any slashes or backslashes are converted to _");
  prtln("");
  prtln("  #inFileBase# insert input file name without leading directories,");
  prtln("               after stripping off any \".suffix\"");
  prtln("               Any slashes or backslashes are converted to _");
  prtln("");
  prtln("  #inPath#     insert full input file name.");
  prtln("               Any slashes or backslashes are converted to _");
  prtln("");
  prtln("  #inPathBase# insert full input file name");
  prtln("               after stripping off any \".suffix\"");
  prtln("               Any slashes or backslashes are converted to _");
  prtln("");
  prtln("  #outFile#    insert output file name without leading directories");
  prtln("               (only for -report, not for -outSpec)");
  prtln("");
  prtln("  #outPath#    insert full output file name");
  prtln("               (only for -report, not for -outSpec)");
  prtln("");
  prtln("  #year#       insert year      from the BUFR message header");
  prtln("                                (BUFR section 1 - identification)");
  prtln("  #month#      insert month     from the BUFR message header");
  prtln("  #day#        insert day       from the BUFR message header");
  prtln("  #hour#       insert hour      from the BUFR message header");
  prtln("  #minute#     insert minute    from the BUFR message header");
  prtln("  #second#     insert second    from the BUFR message header");
  prtln("");
  prtln("  #category#   insert category from the BUFR message header");
  prtln("  #categoryName# insert category name from the BUFR message header");
  prtln("");
  prtln("  #msgNum#     insert message number");
  prtln("               (there can be multiple messages in a file)");
  prtln("");
  prtln("  #sectionLength0# insert the length in bytes of the BUFR message");
  prtln("               section 0 - the indicator section.");
  prtln("               Section 0 is always 8 bytes long.");
  prtln("");
  prtln("  #sectionLength1# insert the length in bytes of the BUFR message");
  prtln("               section 1 - the identification section.");
  prtln("");
  prtln("  #sectionLength2# insert the length in bytes of the BUFR message");
  prtln("               section 2 - the optional local information section.");
  prtln("");
  prtln("  #sectionLength3# insert the length in bytes of the BUFR message");
  prtln("               section 3 - the data description section.");
  prtln("");
  prtln("  #sectionLength4# insert the length in bytes of the BUFR message");
  prtln("               section 4 - the data section.");
  prtln("");
  prtln("  #sectionLength5# insert the length in bytes of the BUFR message");
  prtln("               section 5 - the end section.");
  prtln("               Section 5 is always 4 bytes long.");
  prtln("");
  prtln("  #numTemplates#  insert the number of descriptors, before");
  prtln("          expanding sequences or iterations");
  prtln("");
  prtln("  #repDescs#   insert the number of descriptors in the first");
  prtln("          found replication or repetition");
  prtln("");
  prtln("  #repIters#   insert the number of iterations in the first");
  prtln("          found replication or repetition");
  prtln("");
  prtln("  #value_f_xx_yyy.k#  Search the tree for the k'th item");
  prtln("             having fxy=fxxyyy");
  prtln("             and insert the associated string value");
  prtln("             The first is k = 1.");
  prtln("");
}





public String toString() {
  String res = "inFile: \"" + inFile + "\"\n"
    + "  outSpec: \"" + outSpec + "\"\n";
  return res;
}



/**
 * Main driver for parsing BUFR files and converting to XML.
 * <p>
 * <b>To see the command line usage info, issue:</b><br>
 * <code> java -cp someClassPath wmoBufr.BufrFile </code>
 * <p>
 * Internal logic:
 * <pre>
 *    Get command line parms.
 *    
 *    Read table files and build tables.
 *    If there are multiple tables of a given type (say multiple
 *    description tables), merge them.  If there are duplicate
 *    entries, only the last one processed is used.
 *    The table types are:
 *      description   WMO BUFR table B
 *      sequence      WMO BUFR table D
 *      codeFlag      WMO codes and flags associated with BUFR table B.
 *      common        WMO table of common codes
 *
 *    Call {@link #BufrFile BufrFile} to create the BufrFile object.
 *    Call {@link #readFully readFully} to read
 *      the entire inFile and convert all the BUFR messages to XML,
 *      sending the output to files as specified in outSpec.
 * </pre>
 */

public static void main( String[] args) {
  try {
    mainPgm( args);
  }
  catch( BufrException exc) {
    BufrUtil.prtlnexc("caught", exc);
    System.exit(1);
  }
}






static void mainPgm( String[] args)
throws BufrException
{
  int tableBugs = 0;
  int dataBugs = 0;

  int parseStage = STAGE_UNKNOWN;
  int outFormat = OUTFORMAT_UNKNOWN;
  int outStyle = OUTSTYLE_UNKNOWN;

  int tableVersionMajor = -1;
  int tableVersionMinor = -1;
  boolean allowDups = false;
  String helpStg = null;
  String tabCategName = null;
  String tabCodeFlagName = null;
  String tabCommonName = null;
  String tabDescriptionName = null;
  String tabSequenceName = null;
  boolean validateFlag = false;
  boolean forceFlag = false;
  LinkedList<String> inFileList = new LinkedList<String>();
  String inList = null;
  String inListDir = null;

  String outSpec = null;
  String reportSpec = null;

  String tabledirKey = "-tabledir_";
  LinkedList<TableSource> tableSourceList = new LinkedList<TableSource>();

  if (args.length % 2 != 0) badparms("args must be key/value pairs");
  for (int iarg = 0; iarg < args.length - 1; iarg += 2) {
    String key = args[iarg];
    String val = args[iarg+1];

    if (key.equals("-help")) helpStg = val;

    else if (key.equals("-debugTable"))
      tableBugs = BufrUtil.parseInt( key, val);

    else if (key.equals("-debugData"))
      dataBugs = BufrUtil.parseInt( key, val);

    else if (key.equals("-parseStage"))
      parseStage = BufrUtil.parseKeyword( "stage", stageNames, false, val);

    else if (key.equals("-outFormat")) {
      outFormat = BufrUtil.parseKeyword(
        "outFormat", outFormatNames, false, val);
    }

    else if (key.equals("-outStyle"))
      outStyle = BufrUtil.parseKeyword( "outStyle", outStyleNames, false, val);

    else if (key.equals("-allowDups"))
      allowDups = BufrUtil.parseBoolean( key, val);

    else if (key.startsWith( tabledirKey)) {            // "-tabledir_maj.min"
      String vkey = key.substring( tabledirKey.length());  // "maj.min"
      int ix = vkey.indexOf(".");
      if (ix <= 0 || ix >= vkey.length() - 1)
        badparms("invalid table spec: " + key);
      int major = BufrUtil.parseInt("table major version",
        vkey.substring( 0, ix));
      int minor = BufrUtil.parseInt("table minor version",
        vkey.substring( ix+1));
      if (tableSourceList.size() == 0) {
        tableVersionMajor = major;
        tableVersionMinor = minor;
      }
      if (major != tableVersionMajor)
        badparms("tabledir major version != previous specified major version"
          + "  for tabledir: " + key);
      if (minor < tableVersionMinor)
        badparms("tabledir minor version < previous specified minor version"
          + "  for tabledir: " + key);
      tableVersionMinor = minor;     // use the most recent version

      TableSource tsource = new TableSource( major, minor, val);
      tableSourceList.add( tsource);
    }

    else if (key.equals("-validate"))
      validateFlag = BufrUtil.parseBoolean( "validate", val);

    else if (key.equals("-force"))
      forceFlag = BufrUtil.parseBoolean( "force", val);

    else if (key.equals("-inFile")) inFileList.add( val);

    else if (key.equals("-inFiles")) {
      String[] toks = val.split("\\s+");
      for (String tok : toks) {
        if (tok.length() > 0) inFileList.add( tok);
      }
    }

    else if (key.equals("-inList")) inList = val;

    else if (key.equals("-inListDir")) inListDir = val;

    else if (key.equals("-outSpec")) {
      if (outSpec == null) outSpec = "";
      outSpec += val;
    }

    else if (key.equals("-report")) {
      if (reportSpec == null) reportSpec = "";
      reportSpec += val;
    }

    else badparms("unknown key: \"" + key + "\"");
  } // for iarg

  if (inList != null) {
    try {
      String slash = System.getProperty("file.separator");

      BufferedReader rdr = new BufferedReader( new FileReader( inList));
      while (true) {
        String line = rdr.readLine();
        if (line == null) break;
        line = line.trim();
        if (line.length() > 0 && ! line.startsWith("#"))
          if (inListDir != null) {
            String inDir = inListDir;
            if (! inDir.endsWith(slash)) inDir += slash;
            line = inDir + line;
          }
          inFileList.add( line);
      }
      rdr.close();
    }
    catch( IOException exc) {
      BufrUtil.prtlnexc("caught", exc);
      throwerr("could not open input file \"" + inList + "\"");
    }
  }


  if (helpStg != null) {
    if (helpStg.equals("usage")) printHelpUsage();
    else if (helpStg.equals("copyright")) prtln( COPYRIGHT.copyright);
    else badparms("unknown help request");
  }
  else {
    TableSource[] tableSources = tableSourceList.toArray( new TableSource[0]);
    String[] inFiles = inFileList.toArray( new String[0]);

    processFiles(
      tableBugs,
      dataBugs,
      parseStage,
      outFormat,
      outStyle,
      tableVersionMajor,
      tableVersionMinor,
      allowDups,
      helpStg,
      tableSources,
      validateFlag,
      forceFlag,
      inFiles,
      outSpec,
      reportSpec);
  }
} // mainPgm







static void processFiles(
  int tableBugs,
  int dataBugs,
  int parseStage,
  int outFormat,
  int outStyle,
  int tableVersionMajor,
  int tableVersionMinor,
  boolean allowDups,
  String helpStg,
  TableSource[] tableSources,
  boolean validateFlag,
  boolean forceFlag,
  String[] inFiles,
  String outSpec,
  String reportSpec)
throws BufrException
{
  if (parseStage == STAGE_UNKNOWN)
    badparms("parameter not found: -parseStage");
  if (outFormat == OUTFORMAT_UNKNOWN)
    badparms("parameter not found: -outFormat");
  if (outStyle == OUTSTYLE_UNKNOWN)
    badparms("parameter not found: -outStyle");

  // Build the tables to pass to BufrFile constructor
  TableCateg tabCateg = new TableCateg( tableBugs);
  TableCodeFlag tabCodeFlag = new TableCodeFlag( tableBugs);
  TableCommon tabCommon = new TableCommon( tableBugs);
  TableDesc tabDesc = new TableDesc( tableBugs);
  TableSeq tabSeq = new TableSeq( tableBugs);

  for (TableSource tsource : tableSources) {
    String tag;
    String fname;
    String tpath;
    File tfile;
    String slash = System.getProperty("file.separator");
    String verstg = tsource.versionMajor + "." + tsource.versionMinor;

    tag = "categTab";
    fname = tsource.dirName + slash + tag + ".formatted";
    tfile = new File(fname);
    tpath = BufrUtil.getCanonicalPath( fname);
    if (tfile.exists()) {
      TableCateg table = new TableCateg( tableBugs);
      table.read( allowDups, fname);
      if (table.versionMajor != tsource.versionMajor
        || table.versionMinor != tsource.versionMinor)
        badparms("table version mismatch for tabledir: "
          + tsource.dirName + "\n" + "  table file: " + tpath);
      tabCateg.merge( table);
      if (tableBugs >= 1) {
        prtln("  Found version " + verstg + " " + tag
          + " at: " + tpath + "  size: " + table.size());
      }
    }
    else {
      if (tableBugs >= 0)
        prtln("Not found: version " + verstg + " " + tag + " at: " + tpath);
    }

    tag = "codeFlagTab";
    fname = tsource.dirName + slash + tag + ".formatted";
    tfile = new File(fname);
    tpath = BufrUtil.getCanonicalPath( fname);
    if (tfile.exists()) {
      TableCodeFlag table = new TableCodeFlag( tableBugs);
      table.read( allowDups, fname);
      if (table.versionMajor != tsource.versionMajor
        || table.versionMinor != tsource.versionMinor)
        badparms("table version mismatch for tabledir: "
          + tsource.dirName + "\n" + "  table file: " + tpath);
      tabCodeFlag.merge( table);
      if (tableBugs >= 1) {
        prtln("  Found version " + verstg + " " + tag
          + " at: " + tpath + " size: " + table.size());
      }
    }
    else {
      if (tableBugs >= 0)
        prtln("Not found: version " + verstg + " " + tag + " at: " + tpath);
    }

    tag = "commonTab";
    fname = tsource.dirName + slash + tag + ".formatted";
    tfile = new File(fname);
    tpath = BufrUtil.getCanonicalPath( fname);
    if (tfile.exists()) {
      TableCommon table = new TableCommon( tableBugs);
      table.read( allowDups, fname);
      if (table.versionMajor != tsource.versionMajor
        || table.versionMinor != tsource.versionMinor)
        badparms("table version mismatch for tabledir: "
          + tsource.dirName + "\n" + "  table file: " + tpath);
      tabCommon.merge( table);
      if (tableBugs >= 1) {
        prtln("  Found version " + verstg + " " + tag
          + " at: " + tpath + " size: " + table.size());
      }
    }
    else {
      if (tableBugs >= 0)
        prtln("Not found: version " + verstg + " " + tag + " at: " + tpath);
    }

    tag = "descTab";
    fname = tsource.dirName + slash + tag + ".formatted";
    tfile = new File(fname);
    tpath = BufrUtil.getCanonicalPath( fname);
    if (tfile.exists()) {
      TableDesc table = new TableDesc( tableBugs);
      table.read( allowDups, fname);
      if (table.versionMajor != tsource.versionMajor
        || table.versionMinor != tsource.versionMinor)
        badparms("table version mismatch for tabledir: "
          + tsource.dirName + "\n" + "  table file: " + tpath);
      tabDesc.merge( table);
      if (tableBugs >= 1) {
        prtln("  Found version " + verstg + " " + tag
          + " at: " + tpath + " size: " + table.size());
      }
    }
    else {
      if (tableBugs >= 0)
        prtln("Not found: version " + verstg + " " + tag + " at: " + tpath);
    }

    tag = "seqTab";
    fname = tsource.dirName + slash + tag + ".formatted";
    tfile = new File(fname);
    tpath = BufrUtil.getCanonicalPath( fname);
    if (tfile.exists()) {
      TableSeq table = new TableSeq( tableBugs);
      table.read( allowDups, fname);
      if (table.versionMajor != tsource.versionMajor
        || table.versionMinor != tsource.versionMinor)
        badparms("table version mismatch for tabledir: "
          + tsource.dirName + "\n" + "  table file: " + tpath);
      tabSeq.merge( table);
      if (tableBugs >= 1) {
        prtln("  Found version " + verstg + " " + tag
          + " at: " + tpath + " size: " + table.size());
      }
    }
    else {
      if (tableBugs >= 0)
        prtln("Not found: version " + verstg + " " + tag + " at: " + tpath);
    }
  } // for each TableSource


  // If we're going to expand descriptors, we need the tables.
  if (parseStage >= STAGE_EXPDKEY) {
    if (tabCateg.size() == 0)
      badparms("category table not specified or is empty");
    if (tabCodeFlag.size() == 0)
      badparms("codeflag table not specified or is empty");
    if (tabCommon.size() == 0)
      badparms("common table not specified or is empty");
    if (tabDesc.size() == 0)
      badparms("description table not specified or is empty");
    if (tabSeq.size() == 0)
      badparms("sequence table not specified or is empty");
  }
  if (inFiles == null || inFiles.length == 0)
    badparms("no input file specified");
  if (outSpec == null) badparms("outSpec not specified");
  // reportSpec may be null

  // Process each input file
  String errorMsgs = "";
  for (String inFile : inFiles) {
    if (dataBugs >= 1)
      prtln("BufrFile: begin input file: \"" + inFile + "\"");

    BufrFile bfile = new BufrFile(
      tableBugs,
      dataBugs,
      parseStage,
      outFormat,
      outStyle,
      tableVersionMajor,
      tableVersionMinor,
      validateFlag,
      forceFlag,
      tabCateg,
      tabCodeFlag,
      tabCommon,
      tabDesc,
      tabSeq,
      inFile,
      outSpec,
      reportSpec);

    // Read the entire inFile and convert all the BUFR messages to XML,
    // sending the output to files as specified in outSpec.
    bfile.readFully();

    bfile.close();
    errorMsgs += bfile.errorMsgs;
  } // for each inFile

  if (errorMsgs.length() > 0) {
    String msg = "\nErrors encountered:\n" + errorMsgs;
    prtln(msg);
    System.err.println(msg);
    System.exit(1);
  }
} // processFiles





/**
 * Constructor - does nothing except set instance variables.
 */

BufrFile(
  int tableBugs,                 // debug level for parsing spec tables
  int dataBugs,                  // debug level for parsing user data
  int parseStage,                // one of STAGE_*
  int outFormat,                 // OUTFORMAT_*: xml or text
  int outStyle,                  // OUTSTYLE_*: standard or full
  int tableVersionMajor,         // major table version
  int tableVersionMinor,         // minor table version
  boolean validateFlag,          // If true, validate dates, lat/lons, etc.
  boolean forceFlag,             // If true, continue after parse error
  TableCateg tableCateg,         // Categories: WMO BUFR table A.
  TableCodeFlag tableCodeFlag,   // Codes and flags: WMO codes and flags
                                 //   associated with BUFR table B.
  TableCommon tableCommon,       // WMO table of common codes
  TableDesc tableDescription,    // WMO BUFR table B
  TableSeq tableSequence,        // WMO BUFR table D
  String inFile,                 // input BUFR data file
  String outSpec,                // output name spec for XML files
  String reportSpec)             // report spec, like outSpec
throws BufrException
{
  this.tableBugs = tableBugs;
  this.dataBugs = dataBugs;
  this.parseStage = parseStage;
  this.outFormat = outFormat;
  this.outStyle = outStyle;
  this.tableVersionMajor = tableVersionMajor;
  this.tableVersionMinor = tableVersionMinor;
  this.validateFlag = validateFlag;
  this.forceFlag = forceFlag;
  this.tableCateg = tableCateg;
  this.tableCodeFlag = tableCodeFlag;
  this.tableCommon = tableCommon;
  this.tableDescription = tableDescription;
  this.tableSequence = tableSequence;
  this.inFile = inFile;
  this.outSpec = outSpec;
  this.reportSpec = reportSpec;

  try {
    istm = new FileInputStream( inFile);
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("could not open input file \"" + inFile + "\"");
  }
  fileOffset = 0;
}



/**
 * Reads the entire inFile and converts all the BUFR messages to XML,
 * writing the output to files as specified in outSpec.
 * Internal logic:
 * <pre>
 *     For each imsg:         // for each BUFR message in inFile
 *       For each subset:     // for each subset of the message
 *         Format and write a separate XML file
 *         Write a report line
 * </pre>
 */

void readFully()
throws BufrException
{
  for (int imsg = 0; ; imsg++) {
    if (dataBugs >= 1)
      prtln("\n========== BufrFile: begin read message: " + imsg);
    BufrMessage bmsg = BufrMessage.readBufrMessage( dataBugs, this, imsg);
    if (bmsg == null) break;              // if EOF, break

    boolean isXml = false;
    if (outFormat == OUTFORMAT_XML) isXml = true;
    BufrFormatter.writeAllOutput( isXml, this, bmsg);     
    if (dataBugs >= 1) prtln("BufrFile: end read message: " + imsg);
  }
}





int readBytes(
  byte[] inbuf,
  int offset,
  int rlen)
throws BufrException
{
  int numRead = 0;
  try {
    numRead = istm.read( inbuf, offset, rlen);
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("i/o error: " + exc);
  }
  fileOffset += numRead;
  return numRead;
}



/**
 * Closes the input file.  Called by main.
 */
void close()
throws BufrException
{
  try {
    istm.close();
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("could not close input file");
  }
}





static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrMessage: " + msg);
}



static void prtln( String msg) {
  System.out.println( msg);
}



} // end class
