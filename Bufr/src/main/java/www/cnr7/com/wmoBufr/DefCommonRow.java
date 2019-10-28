
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
 * Defines an entry in a common codes table.
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

class DefCommonRow {


static int CMRTP_MISSING   = 1;
static int CMRTP_RESERVED  = 2;
static int CMRTP_STANDARD  = 3;
static String[] typeNames = {"UNKNOWN", "MISSING", "RESERVED", "STANDARD"};


int keyLo;            // keyLo <= key <= keyHi
int keyHi;            // keyLo <= key <= keyHi
int rowType;          // one of CMRTP_*
String description;   // never null, but may be ""


public String toString() {
  String res = "keyLo: " + keyLo
    + "  keyHi: " + keyHi
    + "  rowType: " + typeNames[rowType];
  if (rowType == CMRTP_STANDARD) res += "  \"" + description + "\"";
  return res;
}


} // end class

