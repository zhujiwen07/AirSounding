
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




/**
 * Handles dynDefs - dynamic definitions, by walking the BufrItem tree.
 *
 * @author S. Sullivan
 */


// Define a DefCateg entry (WMO table A entry):
//    VAL_STRING                  0 00 001  Table A: entry
//                                          as 24 bits (int as 3 chars).
//                                          Max existing categ num is 255.
//    VAL_STRING                  0 00 002  Table A: data category desc,
//                                          line 1 as 256 bits (32 chars)
//    VAL_STRING                  0 00 003  Table A: data category desc,
//                                          line 2 as 256 bits (32 chars)
// or possibly as:
//    VAL_STRING                  0 00 001  Table A: entry
//    SEQUENCE                    3 00 002
//      VAL_STRING                  0 00 002  Table A: data category desc,
//      VAL_STRING                  0 00 003  Table A: data category desc,
//
//
// Define a DefDesc-descriptor entry (WMO table B entry):
//    SEQUENCE                    3 00 004  sequence
//      SEQUENCE                    3 00 003  sequence
//        VAL_STRING                  0 00 010  F desc to be defined, as
//                                              8 bits (int as 1 char: "f")
//        VAL_STRING                  0 00 011  X desc to be defined, as
//                                              16 bits (int as 2 chars: "xx")
//        VAL_STRING                  0 00 012  Y desc to be defined, as
//                                              24 bits (int as 3 chars: "yyy")
//      VAL_STRING                  0 00 013  Element name, line 1
//                                            as 256 bits (32 chars)
//      VAL_STRING                  0 00 014  Element name, line 2
//                                            as 256 bits (32 chars)
//      VAL_STRING                  0 00 015  Units name
//                                            as 192 bits (24 chars)
//      VAL_STRING                  0 00 016  Units scale sign
//                                            as 8 bits (1 char)
//      VAL_STRING                  0 00 017  Units scale
//                                            as 24 bits (3 chars)
//      VAL_STRING                  0 00 018  Units reference sign
//                                            as 8 bits (1 char)
//      VAL_STRING                  0 00 019  Units reference value
//                                            as 80 bits (10 chars)
//      VAL_STRING                  0 00 020  Element data width
//                                            as 24 bits (3 chars)
//
//
// Define a DefDesc-sequence entry (WMO table D entry):
//    SEQUENCE                    3 00 003  sequence
//      VAL_STRING                  0 00 010  F desc to be defined, as
//                                            8 bits (int as 1 char: "f")
//      VAL_STRING                  0 00 011  X desc to be defined, as
//                                            16 bits (int as 2 chars: "xx")
//      VAL_STRING                  0 00 012  Y desc to be defined, as
//                                            24 bits (int as 3 chars: "yyy")
//    OPERATOR                    2 05 064  Signify character: name of sequence
//                                          yyy = 064 bytes = len of name
//    ITERATE                     1 01 000  numDescs: 1  numIters: 0
//        ITER_COUNT              0 31 001  delayed iter count
//      VAL_STRING                0 00 030  Descriptor defining sequence
//                                          as 48 bits (6 chars) "fxxyyy"
// Or:
//    SEQUENCE                    3 00 010  sequence
//      SEQUENCE                    3 00 003  sequence
//        VAL_STRING                  0 00 010  F desc to be defined
//        VAL_STRING                  0 00 011  X desc to be defined
//        VAL_STRING                  0 00 012  Y desc to be defined
//      ITERATE                     1 01 000  numDescs: 1  numIters: 0
//          ITER_COUNT              0 31 001  delayed iter count
//        VAL_STRING                0 00 030  Descriptor defining sequence
//
// Note: in rare cases, the last ITERATE above has delayed count = 0,
// meaning they are defining a sequence with no elements.





class DynDefs {



//========================================================================

// Check for and handle dynDefs - dynamic definitions
// Traverse the entire BufrItems tree.
//
// We start with bitem.subItems[isub]
//
// Returns isubIncr = the increment for isub.
// If isub + isubIncr == subItems.len, we're done with these subItems.

static int handleDynDefTree(
  int bugs,
  BufrFile bfile,
  BufrMessage bmsg,
  BufrItem bparent,
  int isub)
throws BufrException
{
  BufrItem bitem = bparent.subItems[isub];

  if (bugs >= 5) prtln("handleDynDefTree: entry: bparent: "
    + BufrUtil.formatFxy( bparent.def.fxy)
    + "  isub: " + isub + "  bitem: "
    + BufrUtil.formatFxy( bitem.def.fxy));

  int isubIncr = -1;         // return value: increment for isub


  // 0 00 001   dynCateg: dynamic table A
  // Then:
  //   0 00 002  line 1
  //   0 00 003  line 2
  // or:
  //   3 00 002, which expands to:
  //     0 00 002
  //     0 00 003

  if (bitem.testFxy( 0, 0, 1)) {    // Table A entry
    int categNum = BufrUtil.parseInt( "categNum", bitem.getStg().trim());

    BufrItem itema = bparent.subItems[isub+1];
    BufrItem suba = null;       // 0 00 002
    BufrItem subb = null;       // 0 00 003
    if (itema.testFxy( 0, 0, 2)) {
      suba = itema;
      subb = bparent.subItems[isub+2];
      isubIncr = 3;
    }
    else if (itema.testFxy( 3, 0, 2)) {
      suba = itema.getCheckSub( 0, 0, 0, 2);
      subb = itema.getCheckSub( 1, 0, 0, 3);
      isubIncr = 2;
    }
    else throwerr("unknown categ def");

    String desc = suba.getStg().trim() + " " + subb.getStg().trim();
    DefCateg dynDef = new DefCateg( DefCateg.CATEG_STANDARD, categNum, desc);
    bfile.tableCateg.addDef( true, dynDef, null);
                           // allowDups = true, rdr = null
  } // if 0 00 001


  // 3 00 002 must be preceeded by 0 00 001, so is illegal here.
  if (bitem.testFxy( 3, 0, 2))
    throwerr("bad ordering");


  // Handle 3 00 003 here if it's not a child of 3 00 004 or 3 00 010.
  // 3 00 003 is:
  //    0 00 010  fval
  //    0 00 011  xval
  //    0 00 012  yval
  else if (bitem.testFxy( 3, 0, 3)
    && (! bparent.testFxy( 3, 0, 4))
    && (! bparent.testFxy( 3, 0, 10)))
  {
    int defFval = BufrUtil.parseInt("dynDef fval",
      bitem.getCheckSub( 0, 0, 0, 10).getStg().trim());   // defined fval
    if (defFval == 1) {          // if defining a table B value
      if (bugs >= 5) prtln("call defineValue from top level 3 00 003");
      defineValue(
        bugs, bfile, bmsg,
        bitem, 0,                  // def f,x,y:  0 00 010, 0 00 011, 0 00 012
        bparent, isub+1);          // 0 00 013 through 0 00 020
      isubIncr = 9;
    }
    else if (defFval == 3) {     // if defining a table D sequence
      if (bugs >= 5) prtln("call defineSequence from top level 3 00 003");
      defineSequence(
        bugs, bfile, bmsg,
        bitem, 0,                  // def f,x,y:  0 00 010, 0 00 011, 0 00 012
        bparent, isub+1);          // 2 05 064 or 1 01 000
      isubIncr = 4;
    }
    else throwerr("unknown defFval");
  }


  // 3 00 004 is:
  //    3 00 003    define fval, xval, yval
  //    0 00 013    name line 1
  //    0 00 014    name line 2
  //    0 00 015    unit name
  //    0 00 016    scale sign
  //    0 00 017    scale
  //    0 00 018    ref sign
  //    0 00 019    ref
  //    0 00 020    bitWidth

  else if (bitem.testFxy( 3, 0, 4)) {
    if (bugs >= 5) prtln("call defineValue from top level 3 00 004");
    BufrItem ndef = bitem.getCheckSub( 0, 3, 0, 3);
    defineValue(
      bugs, bfile, bmsg,
      ndef, 0,                   // def f,x,y:  0 00 010, 0 00 011, 0 00 012
      bitem, 1);                 // 0 00 013 through 0 00 020
    isubIncr = 1;
  }


  // 3 00 010 is:
  //    3 00 003    define fval, xval, yval
  //    1 01 000    delayed replication
  //    0 31 001    rep count, 8 bits
  //    0 00 030    def sequence element

  else if (bitem.testFxy( 3, 0, 10)) {
    if (bugs >= 5) prtln("call defineSequence from top level 3 00 010");
    BufrItem ndef = bitem.getCheckSub( 0, 3, 0, 3);
    defineSequence(
      bugs, bfile, bmsg,
      ndef, 0,                   // def f,x,y:  0 00 010, 0 00 011, 0 00 012
      bitem, 1);                 // 1 01 000
    isubIncr = 1;
  }


  else if (bitem.testFxy( 0, 0, 10)) {
    throwerr("naked 0 00 010 not yet implemented");
  }


  else {
    // Recurse to subs
    int idef = 0;
    while (idef < bitem.subItems.length) {
      int incr = handleDynDefTree( bugs, bfile, bmsg, bitem, idef);
      idef += incr;
    }
    isubIncr = 1;
  }

  if (isubIncr <= 0) throwerr("isubIncr <= 0");
  return isubIncr;

} // end handleDynDefTree






static void defineValue(
  int bugs,
  BufrFile bfile,
  BufrMessage bmsg,
  BufrItem idItem,
  int idBeg,
  BufrItem defItem,
  int defBeg)
throws BufrException
{
  if (bugs >= 5) {
    prtln("defineValue entry:");
    prtln("  idItem: " + BufrUtil.formatFxy( idItem.def.fxy));
    prtln("  idBeg: " + idBeg);
    for (int ii = idBeg; ii < idItem.subItems.length; ii++) {
      prtln("    idItem.subItems[" + ii + "]: "
        + BufrUtil.formatFxy( idItem.subItems[ii].def.fxy));
    }
    prtln("  defItem: " + BufrUtil.formatFxy( defItem.def.fxy));
    prtln("  defBeg: " + defBeg);
    for (int ii = defBeg; ii < defItem.subItems.length; ii++) {
      prtln("    defItem.subItems[" + ii + "]: "
        + BufrUtil.formatFxy( defItem.subItems[ii].def.fxy));
    }
  }

  int fval = BufrUtil.parseInt("dynDef fval",
    idItem.subItems[idBeg].getCheckStg( 0, 0, 10).trim());

  int xval = BufrUtil.parseInt("dynDef xval",
    idItem.subItems[idBeg+1].getCheckStg( 0, 0, 11).trim());

  int yval = BufrUtil.parseInt("dynDef yval",
    idItem.subItems[idBeg+2].getCheckStg( 0, 0, 12).trim());
  
  int isub = defBeg;
  String description =
    defItem.subItems[isub++].getCheckStg( 0, 0, 13).trim() + " "
    + defItem.subItems[isub++].getCheckStg( 0, 0, 14).trim();

  // 0 00 015: unit
  String unit = defItem.subItems[isub++].getCheckStg( 0, 0, 15).trim();

  // 0 00 016: scale sign
  String scSign = defItem.subItems[isub++].getCheckStg( 0, 0, 16).trim();

  // 0 00 017: scale
  int scale = BufrUtil.parseInt("dynDef scale",
    defItem.subItems[isub++].getCheckStg( 0, 0, 17).trim());
  if (scSign.equals("+")) {}
  else if (scSign.equals("-")) scale = -scale;
  else throwerr("dynDef invalid scale sign");

  // 0 00 018: reference sign
  String refSign = defItem.subItems[isub++].getCheckStg( 0, 0, 18).trim();

  // 0 00 019: reference
  int reference = BufrUtil.parseInt("dynDef reference",
    defItem.subItems[isub++].getCheckStg( 0, 0, 19).trim());
  if (refSign.equals("+")) {}
  else if (refSign.equals("-")) reference = -reference;
  else throwerr("dynDef invalid reference sign");

  // 0 00 020: bitWidth
  int bitWidth = BufrUtil.parseInt("dynDef bitWidth",
    defItem.subItems[isub++].getCheckStg( 0, 0, 20).trim());

  DefDesc dynDef = new DefDesc( fval, xval, yval);
  dynDef.description = description;
  dynDef.scale = scale;
  dynDef.reference = reference;
  dynDef.bitWidth = bitWidth;
  dynDef.unit = unit;

  if (unit.equalsIgnoreCase("code table")) dynDef.isCode = true;
  else if (unit.equalsIgnoreCase("flag table")) dynDef.isBitFlag = true;
  else if (unit.equalsIgnoreCase("ccitt ia5")) dynDef.isString = true;
  else dynDef.isNumeric = true;

  // If description is empty, fill it in from the old entry, if any
  if (dynDef.description.length() == 0)
    dynDef.description = bmsg.getDescription( dynDef.fxy);

  bfile.tableDescription.addDef( true, dynDef, null);
                         // allowDups = true, rdr = null
  if (bugs >= 5) prtln("Add dynDef descriptor: " + dynDef);
 
} // end defineValue










static void defineSequence(
  int bugs,
  BufrFile bfile,
  BufrMessage bmsg,
  BufrItem idItem,
  int idBeg,                    // 0 00 010
  BufrItem defItem,
  int defBeg)                   // 2 05 064 or 1 01 000
throws BufrException
{
  if (bugs >= 5) {
    prtln("defineSequence entry:");
    prtln("  idItem: " + BufrUtil.formatFxy( idItem.def.fxy));
    prtln("  idBeg: " + idBeg);
    for (int ii = idBeg; ii < idItem.subItems.length; ii++) {
      prtln("    idItem.subItems[" + ii + "]: "
        + BufrUtil.formatFxy( idItem.subItems[ii].def.fxy));
    }
    prtln("  defItem: " + BufrUtil.formatFxy( defItem.def.fxy));
    prtln("  defBeg: " + defBeg);
    for (int ii = defBeg; ii < defItem.subItems.length; ii++) {
      prtln("    defItem.subItems[" + ii + "]: "
        + BufrUtil.formatFxy( defItem.subItems[ii].def.fxy));
    }
  }

  int fval = BufrUtil.parseInt("dynDef fval",
    idItem.subItems[idBeg].getCheckStg( 0, 0, 10).trim());

  int xval = BufrUtil.parseInt("dynDef xval",
    idItem.subItems[idBeg+1].getCheckStg( 0, 0, 11).trim());

  int yval = BufrUtil.parseInt("dynDef yval",
    idItem.subItems[idBeg+2].getCheckStg( 0, 0, 12).trim());
  
  DefDesc dynDef = new DefDesc( fval, xval, yval);


  // Next sub is iterator or optional 2 05 yyy: char name
  String desc = "";
  BufrItem itema = defItem.subItems[defBeg];
  if (itema.testFxy( 2, 5, -1)) {
    desc = itema.getStg().trim();
    itema = defItem.subItems[defBeg+1];   // get next item: iterator
  }
  dynDef.description = desc;

  itema.checkFxy( 1, 1, 0);     // iterator

  for (int iterSub = 0; iterSub < itema.subItems.length; iterSub++) {
    BufrItem repGroupItem = itema.getCheckSub( iterSub,
      BufrMessage.CUSTOM_FVAL,
      BufrMessage.CUSTOM_REPGROUP_X,
      BufrMessage.CUSTOM_REPGROUP_Y);
    String fxyStg = repGroupItem.subItems[0].getCheckStg( 0, 0, 30).trim();
    if (fxyStg.length() != 6) throwerr("invalid dynSeq row");
    int fseqval = BufrUtil.parseInt( "fseqval", fxyStg.substring(0,1));
    int xseqval = BufrUtil.parseInt( "xseqval", fxyStg.substring(1,3));
    int yseqval = BufrUtil.parseInt( "yseqval", fxyStg.substring(3,6));
    int fxy = BufrUtil.getFxy( fseqval, xseqval, yseqval);

    DefDesc seqSubDef = new DefDesc( fxy);
    // Get description from the old entry, if any
    seqSubDef.description = bmsg.getDescription( fxy);
    dynDef.addSubdef( seqSubDef);
    if (bugs >= 5) prtln("add subDef to sequential dynDef: " + seqSubDef);
  }

  bfile.tableSequence.addSequence( true, dynDef, null);
                              // allowDups = true, rdr = null
  if (bugs >= 5) prtln("Add dynDef descriptor: " + dynDef);
} // end defineSequence






static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("DynDefs: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}




} // end class







