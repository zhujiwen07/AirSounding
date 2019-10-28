
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
 * Defines a row in an entry for the auxiliary Codes and Flags table.
 * <p>
 * The original document is: <br>
 *   CODE TABLES AND FLAG TABLES ASSOCIATED WITH BUFR/CREX TABLE B <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html <br>
 *   select: Definition of Code and Flag Tables associated with Table B <br>
 *
 * @author S. Sullivan
 */

class DefCodeFlagRow {


static int CFRTP_MISSING   = 1;
static int CFRTP_RESERVED  = 2;
static int CFRTP_STANDARD  = 3;
static String[] typeNames = {"UNKNOWN", "MISSING", "RESERVED", "STANDARD"};


int keyLo;            // keyLo <= key <= keyHi
int keyHi;            // keyLo <= key <= keyHi
int rowType;          // one of CFRTP_*
String description;   // never null, but may be ""


public String toString() {
  String res = "keyLo: " + keyLo
    + "  keyHi: " + keyHi
    + "  rowType: " + typeNames[rowType];
  if (rowType == CFRTP_STANDARD) res += "  \"" + description + "\"";
  return res;
}





// Never returns null.

StatusValue getStatusValue(
  boolean bitFlag)
throws BufrException
{
  StatusValue sv = new StatusValue( BufrValue.BST_OK, "");  // returned value

  if (rowType == CFRTP_MISSING)
    sv.sstatus = BufrValue.BST_MISSING;
  else if (rowType == CFRTP_RESERVED)
    sv.sstatus = BufrValue.BST_RESERVED;
  else if (rowType == DefCodeFlagRow.CFRTP_STANDARD)
    sv.value = description;
  else sv.sstatus = BufrValue.BST_UNKNOWN;
  return sv;
}


static void prtln( String msg) {
  System.out.println( msg);
}


} // end class

