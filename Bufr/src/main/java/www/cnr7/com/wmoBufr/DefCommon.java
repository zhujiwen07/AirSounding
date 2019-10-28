
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
 * Define a common codes table.
 * <p>
 * The original document is: <br>
 * Manual on Codes <br>
 * also known as c. COMMON CODE TABLES TO BINARY AND ALPHANUMERIC CODES <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/ManualCodesGuides.html <br>
 *    select: Common Code Tables <br>
 *
 * @author S. Sullivan
 */

class DefCommon {


int tableNum;       // table number: 1 for table c-1, etc.
String title;       // never null, but may be ""
DefCommonRow[] rows = new DefCommonRow[0];



public String toString() {
  String res = null;
  res = "tableNum: " + tableNum + "  title: \"" + title + "\"\n";
  for (DefCommonRow row : rows) {
    res += "  " + row + "\n";
  }
  return res;
}





// If not found, returns null.

private DefCommonRow getRow(
  int ikey)
throws BufrException
{
  //prtln("getRow: ikey: " + ikey);
  if (ikey < 0) throwerr("ikey < 0");

  DefCommonRow row = null;
  for (int irow = 0; irow < rows.length; irow++) {
    DefCommonRow trow = rows[irow];
    if (trow.keyLo <= ikey && ikey <= trow.keyHi) {
      row = trow;
      break;
    }
  }
  return row;
}




// Never returns null.

StatusValue getStatusValue(
  int ikey)
throws BufrException
{
  StatusValue sv = new StatusValue( BufrValue.BST_OK, "");  // returned value
  DefCommonRow row = getRow( ikey);
  if (row == null)
    sv.sstatus = BufrValue.BST_UNKNOWN;
  else if (row.rowType == DefCommonRow.CMRTP_MISSING)
    sv.sstatus = BufrValue.BST_MISSING;
  else if (row.rowType == DefCommonRow.CMRTP_RESERVED)
    sv.sstatus = BufrValue.BST_RESERVED;
  else if (row.rowType == DefCommonRow.CMRTP_STANDARD)
    sv.value = row.description;
  else sv.sstatus = BufrValue.BST_UNKNOWN;
  return sv;
}





static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("DefCommon: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}



} // end class

