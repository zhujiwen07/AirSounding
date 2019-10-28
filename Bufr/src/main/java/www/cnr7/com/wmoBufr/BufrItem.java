
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
import java.util.LinkedList;



/**
 * Represents a parsed BUFR descriptor - essentially a parsed
 * form of the DefDesc.
 * Like the DefDescs, the BufrItems form a tree.
 * For example, an iteration item contains all the items in the loop.
 * <p>
 * Extended by: BufrValue
 *
 * @author S. Sullivan
 */

class BufrItem {

static String MISS_MSG = "MISS";

DefDesc def;

BufrItem[] subItems = new BufrItem[0];

int numDescs;        // used by replication and repetition
                     // for num descriptors.
                     // Always 1 for repetition.
                     // Note this is the number of items in our subItems,
                     // which may be less than our xval.  The xval
                     // includes the total number of nested descriptors
                     // for nested loops.

int numIters;        // used by replication and repetition
                     // for num iterations

//xxx del:
int iterNum;         // used by CUSTOM_X_REPGROUP for the loop index


//xxx set these.
BufrItem parentItem; // parent in the BufrItem tree.  null for root.
int subIndex;        // index in our parent's subItems.  -1 for root.






BufrItem( DefDesc def)
{
  this.def = def;
}




public String toString() {
  String res = "fxy: " + BufrUtil.formatFxy( def.fxy);
  if (def.fval == 1) {             // if replication or repetition
    res += "  numDescs: " + numDescs
      + "  numIters: " + numIters;
  }

  res += "  desc: \"" + def.description + "\"";

  // BufrValue stuff.  Yes, this could be done by having
  // a BufrValue method overload formatItem.  But this way
  // is easier to maintain.
  if (this instanceof BufrValue) {
    BufrValue bvalue = (BufrValue) this;
    res += "  status: \"" + BufrValue.bstatusNames[bvalue.bstatus] + "\"";
    res += "  unit: \"" + def.unit + "\"";
    res += "  val: \"" + bvalue.stringValue + "\"";
  }
  return res;
}





boolean testFxy( int fv, int xv, int yv)
throws BufrException
{
  boolean bres = true;
  if (
       (fv != -1 && def.fval != fv)
    || (xv != -1 && def.xval != xv)
    || (yv != -1 && def.yval != yv))
  {
    bres = false;
  }
  return bres;
}





BufrItem getCheckSub( int isub, int fv, int xv, int yv)
throws BufrException
{
  if (isub < 0 || isub >= subItems.length)
    throwerr("getCheckSub: invalid isub");
  BufrItem subItem = subItems[isub];
  subItem.checkFxy( fv, xv, yv);
  return subItem;
}






void checkFxy( int fv, int xv, int yv)
throws BufrException
{
  if (! testFxy( fv, xv, yv)) {
    throwerr("checkFxy: fxy mismatch.  expected: "
      + BufrUtil.formatFxy( BufrUtil.getFxy( fv, xv, yv))
      + "  found: " + BufrUtil.formatFxy( def.fxy));
  }
}






String getCheckStg( int fv, int xv, int yv)
throws BufrException
{
  checkFxy( fv, xv, yv);
  return getStg();
}




String getStg()
throws BufrException
{
  if (! (this instanceof BufrValue))
    throwerr("getStg: not a BufrValue");
  BufrValue bval = (BufrValue) this;
  return bval.stringValue;
}





void addSub( BufrItem item) {
  int oldLen = subItems.length;
  subItems = Arrays.copyOf( subItems, oldLen + 1);
  subItems[oldLen] = item;
}




static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrItem: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}




} // end class
