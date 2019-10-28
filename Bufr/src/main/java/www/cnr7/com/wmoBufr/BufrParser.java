
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

import java.util.Calendar;
import java.util.LinkedList;
import java.util.SimpleTimeZone;

//xxx ren desc to bkey
//xxx edscs to expandedBkeys
//xxx contains exBkey


/**
 * Represents the parser and the parse trees for all subsets
 * for the data within one BUFR message.
 * <p>
 * For the command line XML converter, see {@link BufrFile BufrFile}.
 * <p>
 * For overall documentation on BUFR formats and tables see
 * the {@link wmoBufr wmoBufr} overview.: <br>
 * <p>
 *
 * @author S. Sullivan
 */



//==============================================================



class BufrParser {


static class ReferenceMod {
  int fxy;
  int refVal;
  ReferenceMod( int fxy, int refVal) {
    this.fxy = fxy;
    this.refVal = refVal;
  }
} // end inner class ReferenceMod



int bugs;
BufrFile bfile;
BufrMessage bmsg;
DefDesc defRoot;        // root of the DefDesc tree
BufrItem[] rootItems;   // roots of the BufrItem trees.  One per subset.
BitBufReader dataBuf;

int numActive;          // Num subsets we handle at once.
                        // If compressed, equals bmsg.numSubsets.
                        // Else is 1.

int subsetNum;          // Used for err msgs
                        // If compressed, is -1.
                        // Else is current subset number.

// Maintain changed width, scale, or reference for
// the 2 xx yyy descriptors.
int modWidth = 0;             // delta num bits
int modScale = 0;             // delta scale
int modReferenceBits = 0;     // new reference len

// List of descriptors and their modified reference values
LinkedList<ReferenceMod> referenceModList = new LinkedList<ReferenceMod>();






// Parse the entire DefDesc tree for all subsets,
// forming new BufrItem trees in this.rootItems
// Calls BufrParser.parseMain, handles DynDefs, etc.

BufrParser(
  int bugs,
  BufrFile bfile,
  BufrMessage bmsg,
  DefDesc defRoot,
  BitBufReader dataBuf)
throws BufrException
{
  this.bugs = bugs;
  this.bfile = bfile;
  this.bmsg = bmsg;
  this.defRoot = defRoot;
  this.dataBuf = dataBuf;

  if (bmsg.flagCompressed) {
    // numActive is the num subsets we decompress concurrently.
    numActive = bmsg.numSubsets;
    subsetNum = -1;                 // used for err msgs
    rootItems = parseMain();        // CALL MAIN PARSER
  }

  else {   // else not compressed
    // numActive is the num subsets we decompress concurrently.
    numActive = 1;
    rootItems = new BufrItem[ bmsg.numSubsets];
    for (int isub = 0; isub < bmsg.numSubsets; isub++) {
      subsetNum = isub;                           // used for err msgs
      BufrItem[] tempRootItems = parseMain();     // CALL MAIN PARSER
      if (tempRootItems.length != 1)
        throwerr("invalid len for tempRootItems: " + tempRootItems.length,
          null);
      rootItems[isub] = tempRootItems[0];
    }
  }

  if (bugs >= 10) {
    for (int isub = 0; isub < rootItems.length; isub++) {
      prtln("\n===== Data contents before handleDynDefTree, for subset "
        + isub + ":");
      prtln("");
      prtln( BufrFormatter.formatBufrItemTree(
        false, rootItems[isub], bmsg, isub));
      prtln("");
    }
  }

  // Check for and handle dynDefs - dynamic definitions
  // Traverse the entire BufrItems tree.
  for (int isub = 0; isub < rootItems.length; isub++) {
    BufrItem root = rootItems[isub];
    int idef = 0;
    while (idef < root.subItems.length) {
      int incr = DynDefs.handleDynDefTree( bugs, bfile, bmsg, root, idef);
      idef += incr;
    }
  }

} // end constructor






BufrItem[] parseMain()
throws BufrException
{
  // Alloc array, size = numActive = num subsets
  BufrItem[] bufrItems = mkBufrItemsFxy( BufrMessage.CUSTOM_SUBSET_FXY);

  // Parse the entire DefDesc tree, forming a BufrItem tree under rootItem.
  for (DefDesc def : defRoot.subDefs) {
    if (bugs >= 5) prtln("BufrParser.const: def.fxy: "
      + BufrUtil.formatFxy( def.fxy));
    // Parse DefDesc subtree, getting back subtrees.
    BufrItem[] subItems = handleDef( def);
    addSubs( bufrItems, subItems);  // add each subItem to corresp bufrItem
  }

  return bufrItems;
} // end parseMain




//========================================================================




// Returns numActive BufrItems, each representing a subtree
// for one subset.
// Given a DefDesc subtree, read all data for the subtree
// and construct the matching BufrItem tree(s) of data values.

BufrItem[] handleDef(
  DefDesc def)
throws BufrException
{
  if (bugs >= 5) prtln("handleDef: entry: def.fxy: "
    + BufrUtil.formatFxy( def.fxy));
  BufrItem[] bufrItems = null;           // return value

  if (bugs >= 10) {
    prtln("\nhandleDef dataBits: " + def + "\n"
      + "  bitWidth: " + def.bitWidth + "\n"
      + "  dataBuf: " + dataBuf.formatBytes( 24));
  }

  // 0 xx yyy: simple descriptor (table B)
  if (def.fval == 0) {

    // If we are defining new reference values ...
    if (modReferenceBits != 0) {
      bufrItems = mkBufrItems( def);

      int refVal = dataBuf.getInt( modReferenceBits);
      // See section 3.1.6.1 in the Guide to WMO Table Driven Code Forms,
      // Table 3.1.6.1-1 BUFR Table C - Data Description Operators.
      // Or the document "BUFR Table C - Data description operators
      // (Edition 3, Version 13-07/11/2007)".
      // The entry for 2 03 states:
      //   "Negative reference values shall be represented by a
      //   positive integer with the left-most bit (bit 1) set to 1."

      int bitMask = 1 << (modReferenceBits - 1);
      if ((refVal & bitMask) != 0) {   // If top order bit is set
        // Use xor to turn off the high bit, then negate
        refVal = -( refVal ^ bitMask);
      }
      referenceModList.add( new ReferenceMod( def.fxy, refVal));
    }
    else {  // else simple descriptor
      BufrValue[] bvalues = handleSimpleDef( def);
      bufrItems = bvalues;
    }
  } // if fval == 0


  // 1 xx yyy: Replication or repetition
  else if (def.fval == 1) {
    if (bugs >= 10) prtln("handleDef: beg rep fxy: "
      + BufrUtil.formatFxy( def.fxy));
    bufrItems = handleRepDef( def);
  }


  // 2 xx yyy: operator
  else if (def.fval == 2) {
    if (bugs >= 10) prtln("handleDef: beg oper fxy: "
      + BufrUtil.formatFxy( def.fxy));
    bufrItems = handleOperatorDef( def);
  }

  // 3 xx yyy: sequence (table D)
  else if (def.fval == 3) {
    if (bugs >= 5) prtln("table d: " + BufrUtil.formatFxy( def.fxy));
    bufrItems = handleSequenceDef( def);
  }

  // Custom: call handleSimpleDef for associated field
  else if (def.fxy == BufrMessage.CUSTOM_ASSOCFLD_FXY) {
    BufrValue[] bvalues = handleSimpleDef( def);
    bufrItems = bvalues;
  }

  else throwerr("unknown fval", def);

  if (bugs >= 5) {
    for (int isub = 0; isub < numActive; isub++) {
      prtln("handleDef: final item for isub " + isub + ": " + bufrItems[isub]);
    }
  }

  return bufrItems;
} // end handleDef



//========================================================================




// Returns numActive BufrValues, one for each subset.

BufrValue[] handleSimpleDef(
  DefDesc def)
throws BufrException
{
  BufrValue[] bufrValues = new BufrValue[ numActive];

  if (bmsg.flagCompressed) {

    // Get bitWidth.  Add in modWidth, but not for associated fields.
    // See section 3.1.5 in the Guide to WMO Table Driven Code Forms.
    int numBits = def.bitWidth;
    if (def.fxy != BufrMessage.CUSTOM_ASSOCFLD_FXY) {
      numBits += modWidth;   // Get bitWidth
    }
    if (bugs >= 5)
      prtln("handleSimpleDef: compressMin numBits: " + numBits);
    int compressMinEnc = dataBuf.getInt( numBits);
    int compressBitLen = dataBuf.getInt( 6);

    if (compressBitLen > numBits)
      throwerr("handleSimpleDef: compressBitLen > numBits", def);

    if (bugs >= 5) {
      prtln("handleSimpleDef: compressMinEnc: " + compressMinEnc);
      prtln("handleSimpleDef: compressBitLen: " + compressBitLen);
    }

    for (int isub = 0; isub < numActive; isub++) {
      if (BufrUtil.isAllOnes( numBits, compressMinEnc)) {
        // All missing
        if (bugs >= 5) prtln("handleSimpleDef: compressed isub: " + isub
          + "  All vals are missing");
        if (compressBitLen != 0)
          throwerr("missing compressed has bitLen != 0", def);
        bufrValues[isub] = new BufrValue( def);
        bufrValues[isub].bstatus = BufrValue.BST_MISSING;
      }
      else {
        if (bugs >= 5) prtln("handleSimpleDef: compressed isub: " + isub);
        // Get compressed numeric value
        // If comressBitLen == 0, it will just repeat compressMinEnc
        bufrValues[isub] = handleSimpleDefSingle(
          isub, def, compressMinEnc, compressBitLen);
      }
    }
  } // if flagCompressed

  // Else non-compressed
  else {
    if (bugs >= 5) prtln("handleSimpleDef: not compressed");

    bufrValues[0] = handleSimpleDefSingle(
      0,     // isub
      def,
      0,     // compressMinEnc
      def.bitWidth + modWidth);   // numBits
  } // else not compressed

  if (bugs >= 5) {
    for (int isub = 0; isub < numActive; isub++) {
      prtln("handleSimpleDef: final bufrValues[" + isub
        + "]: " + bufrValues[isub]);
    }
  }
  return bufrValues;
}




//========================================================================



// Returns a single BufrValue for a single Table B descriptor.
// Handle a simple Table B descriptor.
// No loops, only 1 subset, nothing fancy.

BufrValue handleSimpleDefSingle(
  int isub,                 // subset number
  DefDesc def,
  int compressMinEnc,
  int numBits)
throws BufrException
{
  if (bugs >= 5) prtln("handleSimpleDefSingle: entry: def.fxy: "
    + BufrUtil.formatFxy( def.fxy));

  // Set up returned value
  BufrValue bufrValue = new BufrValue( def);

  // DefDesc entries are never missing ... if the were,
  // they wouldn't be in the table.

  if (def.isString) {
    bufrValue.bstatus = BufrValue.BST_OK;
    bufrValue.stringValue = dataBuf.getTrimString( numBits);
  }
    
  else if (def.isNumeric) {
    bufrValue.bstatus = BufrValue.BST_OK;

    // Get scale
    int scale = def.scale + modScale;

    // Get ref = reference value.
    // Is fxy in the list of modified references?
    ReferenceMod rmod = getReferenceMod( def.fxy);
    int ref = def.reference;
    if (rmod != null) ref = rmod.refVal;

    // Find the true numeric value.
    // encoded = 10^scale * trueval - reference
    // trueval = (encoded + reference) * 10^(-scale)
    // See:
    //   3.1.3.3 Table B - Classification of Elements
    //   3.1.6.1 Changing Data Width, Scale, and Reference Value

    int encval = 0;
    if (numBits > 0) encval = dataBuf.getInt( numBits);

    // Missing values.
    // See: Guide to WMO Table Driven Code Forms: FM 94 BUFR and FM 95 CREX
    //   Layer 3: Detailed Description of the Code Forms
    // Section 3.1.3.3, under "Data Width":
    //   "In those instances where a Table B descriptor defines an
    //    element of data in Section 4 that is missing for a given
    //    subset, then all bits for that element will be set
    //    to 1's in Section 4."

    if (BufrUtil.isAllOnes( numBits, encval)) {
      bufrValue.bstatus = BufrValue.BST_MISSING;
      bufrValue.encodedValue = encval;
    }
    else {
      // If we separate int vs double, we don't know where to get the
      // true value.

      int ival = compressMinEnc + encval;
      bufrValue.encodedValue = ival;
      bufrValue.doubleValue = (ival + ref) * (double) Math.pow( 10, -scale);
      bufrValue.stringValue = BufrUtil.formatTrueValue( scale, ref, ival);
    }
  } // if isNumeric

  else if (def.isBitFlag || def.isCode) {
    int ikey = 0;
    if (numBits > 0) ikey = dataBuf.getInt( numBits);
    if (BufrUtil.isAllOnes( numBits, ikey)) {
      bufrValue.bstatus = BufrValue.BST_MISSING;
      bufrValue.encodedValue = ikey;
    }
    else {
      ikey += compressMinEnc;
      bufrValue.encodedValue = ikey;
      bufrValue.stringValue = "" + bufrValue.encodedValue;
      StatusValue sv = bfile.tableCodeFlag.getStatusValue(  // never null
        def.fxy, def.isBitFlag, ikey, def.bitWidth, 
        bfile.tableCommon);
      bufrValue.bstatus = sv.sstatus;
      bufrValue.codeFlagMeaning = sv.value;
    }
  } // if isBitFlag or isCode

  else throwerr("unknown type", def);

  if (bufrValue == null) throwerr("bufrValue == null", def);
  return bufrValue;
} // end handleSimpleDefSingle



//========================================================================


// Handle replication or repetition
// Returns numActive BufrItems, each representing a subtree
// for one subset.

BufrItem[] handleRepDef(
  DefDesc def)
throws BufrException
{
  if (bugs >= 5) {
    prtln("handleRepDef: entry: def.fxy: "
      + BufrUtil.formatFxy( def.fxy));
  }

  int numIters = def.yval;         // Mmay be 0 for delayed rep
  boolean repetitionFlag = false;  // false: replication,  true: repetition

  // Set up the returned value.
  // Assume for now it's replication, not repetition.
  BufrItem[] bufrItems = mkBufrItems( def);

  if (numIters == 0) {       // If delayed replication ...
    if (bugs >= 5)
      prtln("handleRepDef: delayed rep countDef: " + def.countDef);

    // Next desc must be the replication or repetition factor, 0 31 yyy
    if (! def.countDef.testFxy( 0, 31, -1))
      throwerr("invalid delayed replication/repetition factor", def);

    // Recurse to handle the delayed rep count
    BufrItem[] countItems = handleDef( def.countDef);

    // Insure all the counts are the same
    BufrValue bval0 = (BufrValue) countItems[0];
    numIters = bval0.encodedValue;
    for (int isub = 0; isub < countItems.length; isub++) {
      BufrValue bval = (BufrValue) countItems[isub];
      if (bval.encodedValue != numIters)
        throwerr("countDef num iters mismatch", def);
    }

    if (bugs >= 5) {
      prtln("  handleRepDef delayed rep:");
      prtln("    countItems[0]: " + countItems[0]);
      prtln("    numIters: " + numIters);
    }
  } // if numIters == 0 (delayed rep count)

  for (int isub = 0; isub < numActive; isub++) {
    bufrItems[isub].numDescs = def.subDefs.length;
    bufrItems[isub].numIters = numIters;
  }

  if (bugs >= 5) {
    prtln("  handleRepDef:");
    prtln("    repetitionFlag: " + repetitionFlag);
    prtln("    numDescs: " + def.subDefs.length);
    prtln("    numIters: " + numIters);
  }

  int startDataPos = dataBuf.getBitPos();
  for (int ii = 0; ii < numIters; ii++) {
    BufrItem[] iterItems = mkBufrItemsFxy( BufrMessage.CUSTOM_REPGROUP_FXY);
    for (int isub = 0; isub < numActive; isub++) {
      iterItems[isub].numDescs = def.subDefs.length;
      //xxx del:
      iterItems[isub].iterNum = ii;
    }
    addSubs( bufrItems, iterItems);  // add each iterItem to corresp bufrItem

    // If repetition, reset the data position to the loop start
    // so we read the same data values again.
    if (repetitionFlag) dataBuf.setBitPos( startDataPos);

    for (DefDesc subDef : def.subDefs) {
      BufrItem[] subItems = handleDef( subDef);     // recursion
      addSubs( iterItems, subItems); // add each subItem to corresp iterItem
    }
  } // for ii < numIters

  if (bufrItems == null) throwerr("bufrItems == null", def);
  return bufrItems;
} // end handleRepDef





//========================================================================



// Returns an array of numActive BufrItems, one for each subset.
// All are OPERATORs except for "2 05 yyy", signify character,
// which returns BufrValues.
//
// These are single BufrItems, not subtrees.

BufrItem[] handleOperatorDef(
  DefDesc def)
throws BufrException
{
  if (bugs >= 5) prtln("handleOperatorDef: entry: def.fxy: "
    + BufrUtil.formatFxy( def.fxy));
  BufrItem[] bufrItems = null;

  // 2 01 yyy: Change bitWidth.
  // See section 3.1.6.1 in the Guide to WMO Table Driven Code Forms.
  // See doc on Change reference, below.
  if (def.xval == 1) {
    if (def.yval == 0) {    // modify width end
      modWidth = 0;         // modify width end
      bufrItems = mkBufrItems( def);
    }
    else {                  // modify width start
      modWidth = def.yval - 128;
      bufrItems = mkBufrItems( def);
    }
	if (bugs >= 5) prtln("handleOperatorDef: new modWidth: " + modWidth);
  }

  // 2 02 yyy: Change scale.
  // See section 3.1.6.1 in the Guide to WMO Table Driven Code Forms.
  // See doc on Change reference, below.
  else if (def.xval == 2) {
    if (def.yval == 0) {    // modify scale end
      modScale = 0;         // modify scale end
      bufrItems = mkBufrItems( def);
    }
    else {                  // modify scale start
      modScale = def.yval - 128;
      bufrItems = mkBufrItems( def);
    }
	if (bugs >= 5) prtln("handleOperatorDef: new modScale: " + modScale);
  }

  // 2 03 yyy: Change reference.
  // See section 3.1.6.1 in the Guide to WMO Table Driven Code Forms.
  // Usage:
  //    2 01 yyy     Modify bitWidth on following entries by yyy - 128.
  //    2 02 yyy     Modify scale on following entries by yyy - 128.
  //
  //    2 03 yyy     Start defs of new ref values, yyy bits each
  //    0 12 001       define new reference for temperature
  //    0 13 001       define new reference for specific humidity
  //    2 03 255     End ref definitions
  //    0 12 001       temperature using new reference
  //    0 13 001       specific humidity using new reference
  //    2 03 000     End use of modified references
  //
  //    2 02 000     Cancel modified scale
  //    2 01 000     Cancel modified bitWidth

  else if (def.xval == 3) {
    // If yval == 255: end definition of new references
    // and start use of changed references
    if (def.yval == 255) {
      modReferenceBits = 0;
      bufrItems = mkBufrItems( def);
	  if (bugs >= 5) prtln("handleOperatorDef: end ref defs; start uses");
    }
    // If yval == 0: end use of changed references
    else if (def.yval == 0) {
      if (modReferenceBits != 0) throwerr("modReferenceBits != 0", def);
      referenceModList.clear();
      bufrItems = mkBufrItems( def);
	  if (bugs >= 5) prtln("handleOperatorDef: end ref uses");
    }
    else {
      modReferenceBits = def.yval;    // start definition of new references
      bufrItems = mkBufrItems( def);
	  if (bugs >= 5) prtln("handleOperatorDef: start ref defs."
	    + "  modReferenceBits: " + modReferenceBits);
    }
  }

  // 2 04 yyy: Add associated field.
  else if (def.xval == 4) {
    // Ignore it.  We already handled it in BufrMessage.buildDef.
    bufrItems = mkBufrItems( def);
  }

  // 2 05 yyy: Signify character.  Insert data characters.
  // Returns a list of identical vals.
  else if (def.xval == 5) {
    int numBytes = def.yval;
    BufrValue[] vals = mkBufrValues( def);
    for (int isub = 0; isub < numActive; isub++) {
      vals[isub].stringValue = dataBuf.getTrimString( 8 * numBytes);
    }
    bufrItems = vals;    // return the list of identical vals
  }

  // 2 06 yyy: Signify bitWidth of single following local descriptor.
  // See section 3.1.6.5 in the Guide to WMO Table Driven Code Forms,
  // Signifying Length of Local Descriptors.
  // Ignore it, since we should have the table entry.
  else if (def.xval == 6) {
    bufrItems = mkBufrItems( def);
  }

  else throwerr("unknown operator", def);

  if (bufrItems == null) throwerr("bufrItems == null", def);
  return bufrItems;
} // end handleOperatorDef




//========================================================================

// Returns numActive BufrItems, each representing a subtree
// for one subset.

BufrItem[] handleSequenceDef(
  DefDesc def)
throws BufrException
{
  if (bugs >= 5) prtln("handleSequenceDef: entry: def.fxy: "
    + BufrUtil.formatFxy( def.fxy));
  BufrItem[] bufrItems = mkBufrItems( def);

  if (bugs >= 10) prtln("handleDef: beg seq def.fxy: "
    + BufrUtil.formatFxy( def.fxy));
  for (int isub = 0; isub < numActive; isub++) {
    bufrItems[isub].def.description = def.description;
  }
  for (DefDesc subDef : def.subDefs) {
    BufrItem[] subItems = handleDef( subDef);    // recursive
    addSubs( bufrItems, subItems);  // add each subItem to corresp bufrItem
  }
  return bufrItems;
}



//========================================================================




// Returns numActive (== num subsets) BufrItems all with fxy.

BufrItem[] mkBufrItemsFxy( int fxy)
throws BufrException
{
  DefDesc def = new DefDesc( fxy);
  if (fxy == BufrMessage.CUSTOM_SUBSET_FXY)
    def.description = "root";
  else if (fxy == BufrMessage.CUSTOM_REPGROUP_FXY)
    def.description = "repGroup";
  else throwerr("unknown fxy", def);

  BufrItem[] resItems = new BufrItem[ numActive];
  for (int ii = 0; ii < numActive; ii++) {
    resItems[ii] = new BufrItem( def);
  }
  return resItems;
}




//========================================================================


// Returns numActive (== num subsets) BufrValues all with fxy.

BufrValue[] mkBufrValues( DefDesc def)
throws BufrException
{
  BufrValue[] resValues = new BufrValue[ numActive];
  for (int ii = 0; ii < numActive; ii++) {
    resValues[ii] = new BufrValue( def);
  }
  return resValues;
}



//========================================================================


// Returns numActive (== num subsets) BufrItems all with fxy.

BufrItem[] mkBufrItems( DefDesc def)
throws BufrException
{
  BufrItem[] resItems = new BufrItem[ numActive];
  for (int ii = 0; ii < numActive; ii++) {
    resItems[ii] = new BufrItem( def);
  }
  return resItems;
}



//========================================================================



// Add each subItem to corresp bufrItem.
// If the subs elements are all null, they aren't added.
// There should be numActive of each: one per subset.

void addSubs(
  BufrItem[] parents,
  BufrItem[] subs)
throws BufrException
{
  if (parents.length != numActive)
    throwerr("addSubs: wrong len for parents", null);
  if (subs.length != numActive)
    throwerr("addSubs: wrong len for subs", null);

  for (int isub = 0; isub < numActive; isub++) {
    if (subs[isub] == null) throwerr("sub is null", null);
    parents[isub].addSub( subs[isub]);
  }
}



//========================================================================


ReferenceMod getReferenceMod( int fxy)
throws BufrException
{
  ReferenceMod res = null;
  for (ReferenceMod rmod : referenceModList) {
    if (rmod.fxy == fxy) {
      res = rmod;
      break;
    }
  }
  return res;
}



//========================================================================

void throwerr(
  String msg,
  DefDesc def)
throws BufrException
{
  String bigMsg = "\nException in BufrParser:\n"
    + msg + "\n"
    + "BufrFile: " + bfile + "\n"
    + "msgNum: " + bmsg.msgNum + "\n"
    + "numActive: " + numActive + "\n"
    + "subsetNum: " + subsetNum + "\n"
    + "dataBuf: " + dataBuf + "\n"
    + "def: " + def + "\n";
  throw new BufrException( bigMsg);
}


//========================================================================


static void prtln( String msg) {
  System.out.println( msg);
}


//========================================================================

} // end class
