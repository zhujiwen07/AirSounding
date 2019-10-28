
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



/**
 * Defines a table in the set of auxiliary Codes and Flags tables.
 * <p>
 * The original document is: <br>
 *   CODE TABLES AND FLAG TABLES ASSOCIATED WITH BUFR/CREX TABLE B <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html <br>
 *   select: Definition of Code and Flag Tables associated with Table B <br>
 *
 * @author S. Sullivan
 */

class DefCodeFlag {

static int CFTP_BITFLAG   = 1;
static int CFTP_CODEVALUE = 2;
static int CFTP_REFERRAL  = 3;
static String[] cfTypeNames = {"UNKNOWN", "BITFLAG", "CODEVALUE",
  "REFERRAL"};

int fxy;          // the fxy code
int cfType;       // one of CFTP_*

int referralTableNum;   // if CFTP_REFERRAL, common code table number
int bitMissingValue;    // numeric value of for missing value

DefCodeFlagRow[] rows = new DefCodeFlagRow[0];





// If not found, returns null.

private DefCodeFlagRow getRow(
  int ikey)
throws BufrException
{
  //prtln("getRow: ikey: " + ikey);
  if (cfType == CFTP_REFERRAL) throwerr("cannot query REFERRAL table");
  if (ikey < 0) throwerr("ikey < 0");

  DefCodeFlagRow row = null;
  for (int irow = 0; irow < rows.length; irow++) {
    DefCodeFlagRow trow = rows[irow];
    if (trow.keyLo <= ikey && ikey <= trow.keyHi) {
      row = trow;
      break;
    }
  }
  return row;
}




void merge(
  int bugs,
  DefCodeFlag newdef)
throws BufrException
{
  if (newdef.cfType != cfType) throwerr("cfType mismatch for:\n"
    + "  old def: " + this + "\n"
    + "  new def: " + newdef + "\n");
  DefCodeFlagRow[] resRows = new DefCodeFlagRow[
    rows.length + newdef.rows.length];

  // Merge rows and newdef.rows into resRows.
  int iold = 0;    // index rows
  int inew = 0;    // index newdef.rows
  int ires = 0;    // index resRows
  while (true) {
    // Pick the next row from old (this.rows) element if it's <= new;
    // otherwise pick the next row from newdef.rows.
    boolean pickNew = false;
    if (iold >= rows.length) {
      if (inew >= newdef.rows.length) break;
      else pickNew = true;
    }
    else if (inew >= newdef.rows.length) {
      if (iold >= rows.length) break;
    }
    else if (newdef.rows[inew].keyLo < rows[iold].keyLo) pickNew = true;
    else if (newdef.rows[inew].keyLo == rows[iold].keyLo
      && newdef.rows[inew].keyHi < rows[iold].keyHi) pickNew = true;

    DefCodeFlagRow nextRow;
    if (pickNew) nextRow= newdef.rows[inew++];
    else nextRow = rows[iold++];

    // If nextRow overlaps a reserved region of the previous
    // row, reduce the reserved keyHi to avoid overlap.
    // And vice versa.

    if (ires > 0 && nextRow.keyLo <= resRows[ires-1].keyHi) {

      // If the previous row is reserved,
      // replace it or decrease its keyHi.
      if (resRows[ires-1].rowType == DefCodeFlagRow.CFRTP_RESERVED) {
        if (nextRow.keyLo < resRows[ires-1].keyLo)
          throwerr("merge: bad resRows order");
        if (nextRow.keyLo == resRows[ires-1].keyLo) {
          // We know from the merge order that
          //   nextRow.keyHi >= resRows[ires-1].keyHi
          resRows[ires-1] = nextRow;  // replace previous row; don't inc ires
        }
        else {     // nextRow.keyLo > resRows[ires-1].keyLo
          resRows[ires-1].keyHi = nextRow.keyLo - 1;  // decrease keyHi
          resRows[ires++] = nextRow;
        }
      }

      // Else if the nextRow is reserved,
      // skip it or increase its keyLo.
      else if (nextRow.rowType == DefCodeFlagRow.CFRTP_RESERVED) {
        if (nextRow.keyLo < resRows[ires-1].keyLo)
          throwerr("merge: bad resRows order");
        if (nextRow.keyHi > resRows[ires-1].keyHi) {
          // Add new row and increase its keyLo
          nextRow.keyLo = resRows[ires-1].keyHi + 1;
          resRows[ires++] = nextRow;
        }
        // else don't add nextRow
      }

      else throwerr("merge: overlapping keys for:\n"
        + "  old def: " + this + "\n"
        + "  new def: " + newdef + "\n");
    } // if ires > 0 && conflict
  } // while true
  rows = Arrays.copyOfRange( resRows, 0, ires);  // update our rows

  // Check there is no overlap in key ranges
  for (int ii = 0; ii < rows.length - 1; ii++) {
    if (rows[ii].keyHi >= rows[ii+1].keyLo) {
      throwerr("merge: overlapping keys for:\n"
        + "  old def: " + this + "\n"
        + "  new def: " + newdef + "\n");
    }
  }
}







/**
 * Returns the StatusValue corresponding to ikey.
 * We use bitFlag only as a check: <br>
 *    cfType==CFTP_BITFLAG  <==> bitflag == true <br>
 * Gets the matching CodeFlagRow from rows. <br>
 * <p>
 * If CDTP_CODEVALUE, returns the StatusValue from that row. <br>
 * <p>
 * If CFTP_BITFLAG, looks up the CodeFlagRows for each for each set
 * bit.  The bits are numbered "---- 87654321".
 * For example, if ikey = 4 = binary"100", bit 3 is set.
 * <p>
 * We look up row 3 and concat it's desc to the return value.
 * The return value finally is "desc; desc; desc..."
 * corresponding to the set bits.
 * <p>
 * We need the bitWidth so we can determine the location
 * of bit 1, the leftmost bit in the field.
 * <p>
 * Never returns null.
 */

StatusValue getStatusValue(
  boolean bitFlag,
  int ikey,
  int bitWidth)
throws BufrException
{
  if (cfType == CFTP_REFERRAL) throwerr("cannot query REFERRAL table");
  if (bitWidth <= 0) throwerr("bitWidth <= 0");
  if (ikey < 0) throwerr("ikey < 0");

  StatusValue sv = new StatusValue( BufrValue.BST_OK, "");  // returned value

  if (cfType == CFTP_CODEVALUE) {
    if (bitFlag) throwerr("bitFlag disagrees with cfType");
    DefCodeFlagRow row = getRow( ikey);
    if (row == null) sv.sstatus = BufrValue.BST_UNKNOWN;
    else sv = row.getStatusValue( false);    // bitFlag = false
  }

  else if (cfType == CFTP_BITFLAG) {
    // See:
    // Guide to WMO Table Driven Code Forms: FM 94 BUFR and FM 95 CREX
    //   Layer 1: Basic Aspects of BUFR and CREX, and
    //   Layer 2: Layout, Functionality and Application of BUFR and CREX
    // Section 2.1.1: Sections of a BUFR Message
    // para 2, page L2-2
    //
    // "Bit positions within octets are referred to as bit 1 to bit 8,
    //  where bit 1 is the most significant, leftmost, or high order bit. An
    //  octet with only bit 8 set would have the integer value 1."

    if (! bitFlag) throwerr("bitFlag disagrees with cfType");
    if (ikey == bitMissingValue)
      sv.sstatus = BufrValue.BST_MISSING;
    else {
      sv.value = "";
      for (int bitNum = 1; bitNum <= bitWidth; bitNum++) {
        // Get a 1 bit in position = bitNum.
        // First bit is number 1, at shift offset 7 for an 8 bit field.
        //    -----12345678   bit position
        //         76543210   shift length
        int ival = 1 << (bitWidth - bitNum);
        if ((ikey & ival) != 0) {
          DefCodeFlagRow row = getRow( bitNum);

          // If any bit row is not found, the entire result is UNKNOWN.
          if (row == null) {
            sv.sstatus = BufrValue.BST_UNKNOWN;
            break;
          }
          else {
            StatusValue tmpsv = row.getStatusValue( true);  // bitFlag = true
            // If any bit row is unknown or missing or reserved,
            // that sets the entire result.
            // The BufrValue.BST_* values are ordered from worst to best.
            if (tmpsv.sstatus < sv.sstatus) sv.sstatus = tmpsv.sstatus;
            if (sv.value.length() > 0) sv.value += "; ";
            sv.value += row.description;
          }
        }
      } // for bitNum
    }
  } // if cfType == CFTP_BITFLAG

  else throwerr("invalid cfType");

  if (sv.sstatus != BufrValue.BST_OK) sv.value = "";
  return sv;
} // end getStatusValue





public String toString() {
  String res = "fxy: " + BufrUtil.formatFxy( fxy)
    + "  cfType: " + cfTypeNames[cfType] + "\n";
  if (cfType == CFTP_REFERRAL)
    res += "  referralTableNum: " + referralTableNum + "\n";
  else {
    if (cfType == CFTP_BITFLAG)
      res += "  bitMissingValue: " + bitMissingValue + "\n";
    for (DefCodeFlagRow row : rows) {
     res += "  " + row + "\n";
    }
  }
  return res;
}



static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("DefCodeFlag: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}


} // end class

