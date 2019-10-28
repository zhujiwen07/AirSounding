
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
 * Defines an entry in TableCateg (WMO BUFR Table A).
 * <p>
 * The original document is: <br>
 *   BUFR TABLE RELATIVE TO SECTION 1 (Version 13-07/11/2007) <br>
 * available from: <br>
 *   http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html <br>
 *   select: Table A - BUFR. Data Category <br>
 *
 * @author S. Sullivan
 */

class DefCateg {


static int CATEG_UNKNOWN  = 0;    
static int CATEG_STANDARD = 1;    
static int CATEG_RESERVED = 2;    

static String[] categNames = {
  "UNKNOWN",
  "STANDARD",
  "RESERVED",
};


int categType;    // one of CATEG_*
int categNum;
String description;



DefCateg( int categType, int categNum, String description) {
  this.categType = categType;
  this.categNum = categNum;
  this.description = description;
}





// Used to get a clone by TableCateg.
// Not needed since we never modify DefCateg, but kept
// to be like DefDesc.cloneDef.

DefCateg cloneDef() {
  DefCateg res = new DefCateg( categType, categNum, description);
  return res;
}




public String toString() {
  String res = "type: " + categNames[categType]
    + "  categNum: " + categNum
    + "  description: \"" + description + "\"";
  return res;
}



} // end class

