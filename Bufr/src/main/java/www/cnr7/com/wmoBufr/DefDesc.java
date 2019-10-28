
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
 * Define an entry in either TableDesc (WMO Table B)
 * or TableSeq (WMO Table D)
 * <p>
 * For WMO Table B, the original document is: <br>
 *   BUFR TABLES RELATIVE TO SECTION 3 (Version 13) (updated 20 December 2007) <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html <br>
 *   select: Table B - BUFR/CREX. Classification and definition <br>
 *           of data elements <br>
 * <p>
 * For WMO Table D, the original document is: <br>
 * The original document is: <br>
 *   BUFR Table D - Lists of common sequences <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html <br>
 *   select: Table D - BUFR. List of common sequences <br>
 *
 * @author S. Sullivan
 */

class DefDesc {

int fxy;             // the fxy code
int fval;            // fval part of fxy
int xval;            // xval part of fxy
int yval;            // yval part of fxy

boolean isReserved = false;
boolean isString = false;
boolean isNumeric = false;
boolean isBitFlag = false;
boolean isCode = false;

int totalRawDescs;   // total num of raw descriptors that were read
                     // in this subtree.
                     // If it's part of a defined sequence, it's 0.
                     // If this is fval == 0 or 2 or 3, and not
                     // part of a defined sequence, the value is 1.
                     // If it's an iteration, not part of a defined
                     // sequence, the value is > 1.

String description = "";  // never null, but may be ""

// The subDefs are the definitions within this one
// in the current BufrMessage.
// For a sequence, the subDefs are the DefDescs in the BufrMessage sequence.
// For an iteration, the subDefs are the DefDescs, including
// the delayed repetition factor, in the BufrMessage sequence.

DefDesc[] subDefs = new DefDesc[0];

DefDesc countDef = null;   // delayed rep count

// encoded = 10^scale * true - reference
// true = (encoded + reference) / 10^scale

int scale;
int reference;

// Num bits in field.
// decimal digits = ceil( bits / log2(10))
int bitWidth;

String unit = "";     // never null, but may be ""

int defId = 0;        // for debugging and printing only




DefDesc( int fxy)
throws BufrException
{
  this.fxy = fxy;
  fval = BufrUtil.getFval( fxy);
  xval = BufrUtil.getXval( fxy);
  yval = BufrUtil.getYval( fxy);
}




DefDesc( int fval, int xval, int yval)
throws BufrException
{
  this.fval = fval;
  this.xval = xval;
  this.yval = yval;
  this.fxy = BufrUtil.getFxy( fval, xval, yval);
}




// Used to get a clone by TableDesc and TableSeq.
// We modify the defs by setting defId in DefDesc.setTreeDefId.

DefDesc cloneDef()
throws BufrException
{
  DefDesc res = new DefDesc( fxy);
  res.isReserved = isReserved;
  res.isString = isString;
  res.isNumeric = isNumeric;
  res.isBitFlag = isBitFlag;
  res.isCode = isCode;
  res.description = description;
  res.scale = scale;
  res.reference = reference;
  res.bitWidth = bitWidth;
  res.unit = unit;
  res.subDefs = new DefDesc[ subDefs.length];
  for (int ii =0; ii < subDefs.length; ii++) {
    res.subDefs[ii] = subDefs[ii];
  }
  return res;
}






public String toString() {
  String res = "fxy: " + BufrUtil.formatFxy( fxy)
    + "  defId: " + defId;
  return res;
}






boolean testFxy( int fv, int xv, int yv)
throws BufrException
{
  boolean bres = true;
  if (
       (fv != -1 && fval != fv)
    || (xv != -1 && xval != xv)
    || (yv != -1 && yval != yv))
  {
    bres = false;
  }
  return bres;
}




// Append one new def to the tail of subDefs.

void addSubdef( DefDesc newDef) {
  int oldLen = subDefs.length;
  subDefs = Arrays.copyOf( subDefs, oldLen + 1);
  subDefs[oldLen] = newDef;
}




// Append new defs to the tail of subDefs.

void addSubdefs( DefDesc[] newDefs) {
  int oldLen = subDefs.length;
  subDefs = Arrays.copyOf( subDefs, oldLen + newDefs.length);
  for (int ii = 0; ii < newDefs.length; ii++) {
    subDefs[ oldLen + ii] = newDefs[ii];
  }
}




void clearSubDefs() {
  subDefs = new DefDesc[0];
}





// Recursively set defId with preorder depth first search.

int setTreeDefId( int id) {
  this.defId = id;
  id++;
  if (countDef != null)
    id = countDef.setTreeDefId( id);
  for (DefDesc def : subDefs) {
    id = def.setTreeDefId( id);
  }
  return id;
}


} // end class

