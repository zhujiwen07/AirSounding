
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
 * Extends BufrItem: represents a simple BUFR Table B descriptor.
 * No iterators, no fancy stuff: Just the data.
 *
 * @author S. Sullivan
 */

class BufrValue extends BufrItem {

// These must be ordered from worst to best.
// See code in DefCodeFlag.getStatusValue().
static int BST_UNKNOWN   = 0;
static int BST_MISSING   = 1;
static int BST_RESERVED  = 2;
static int BST_OK        = 3;
static String[] bstatusNames = {"UNK", "MISS", "RSRV", "OK"};

int bstatus;         // one of BST_*

int encodedValue;    // The encoded int value as found in the file.
                     // Valid if isNumeric or isBitFlag or isCode.

double doubleValue;  // numeric value

String stringValue;  // Valid for string, numeric, bitFlag, code.
                     // Never null.
                     // string: value
                     // numeric: converted value
                     // bitFlag, code: encoded value

String codeFlagMeaning;   // The meaning for code or bitFlag.




BufrValue(
  DefDesc def)
{
  super( def);
  bstatus = BST_OK;
  stringValue = "";
}



// toString: see BufrItem.toString()



static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrValue: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}




} // end class

