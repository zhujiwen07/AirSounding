
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

import java.util.ArrayList;


/**
 * Represents a list of raw fxy descriptors.  Used in BufrMessage.
 *
 * @author S. Sullivan
 */

class FxyList {


ArrayList<Integer> list;
int curPos = 0;


FxyList() {
  list = new ArrayList<Integer>();
}


public String toString() {
  String res = "curPos: " + curPos
    + "  size: " + list.size() + "\n";
  for (int ii = 0; ii < list.size(); ii++) {
    int fxy = list.get(ii).intValue();
    res += "    " + ii + "  fxy: " + BufrUtil.formatFxy( fxy) + "\n";
  }
  return res;
}


int size() {
  return list.size();
}


int getPos() {
  return curPos;
}


void incPos( int incr)
throws BufrException
{
  if (curPos + incr < 0 || curPos + incr > list.size())
    throwerr("invalid increment.  curPos: " + curPos + "  incr: " + incr);
  curPos += incr;
}


void addVal( int ival) {
  list.add( new Integer( ival));
}


int getIx( int ix) {
  int ival = list.get(ix).intValue();
  return ival;
}


int getCurInc()
throws BufrException
{
  int ival = getCurNoInc();
  curPos++;
  return ival;
}


int getCurNoInc()
throws BufrException
{
  if (curPos < 0 || curPos >= list.size())
    throwerr("invalid curPos: " + curPos);
  int ival = list.get(curPos).intValue();
  return ival;
}


int getCurIncCheck( int fval, int xval, int yval)
throws BufrException
{
  int ival = getCurInc();
  int fv = BufrUtil.getFval( ival);
  int xv = BufrUtil.getXval( ival);
  int yv = BufrUtil.getYval( ival);
  if (fv != fval || xv != xval || yv != yval)
    throwerr("getCurIncChk: fxy mismatch.  expected: "
      + BufrUtil.formatFxy( BufrUtil.getFxy( fval, xval, yval))
      + "  found: " + BufrUtil.formatFxy( ival));
  return ival;
}






boolean hasMore() {
  boolean bres = false;
  if (curPos < list.size()) bres = true;
  return bres;
}






void insertRemains( FxyList newList)
throws BufrException
{
  int ix = curPos;
  while (newList.hasMore()) {
    list.add( ix++, newList.getCurInc());
  }
}







static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("FxyList: " + msg);
}





static void prtln( String msg) {
  System.out.println( msg);
}




} // end class
