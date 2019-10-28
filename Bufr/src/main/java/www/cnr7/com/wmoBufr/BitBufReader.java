
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
 * Provides bit-oriented access to an array of bytes.
 * The constructor takes an array of bytes; callers can then
 * get back strings of bits as integers.
 * <p>
 * Maintains a pointer to the current bit position,
 * {@link #bitPos bitPos}, that is automatically incremented
 * on some kinds of retrievals.
 *
 * @author S. Sullivan
 */

class BitBufReader {

static int BBTP_DATA = 1;
static int BBTP_DESC = 2;
static String[] typeNames = {"UNKNOWN", "DATA", "DESC" };

int bugs = 0;
int bufType;             // one of BBTP_*
byte[] byteBuf;          // the data
int offsetInFile;        // for debugging, the offset in the BUFR file
int offsetInMessage;     // for debugging, the offset in the BUFR message

int totBits;             // total num bits = 8 * byteBuf.len
int bitPos;              // bit position = 8 * byte position


/**
 * Creates the buffer.
 * @param bugs   The debug level.  Normally 0.
 * @param bufType  Either:
 * <ul>
 *   <li> {@link #BBTP_DATA BBTP_DATA}: the buffer contains
 *      generic bit oriented data
 *   <li> {@link #BBTP_DESC BBTP_DESC}: the buffer contains
 *      BUFR descriptor codes.
 * </ul>
 * @param byteBuf  The data to be accessed.  <code>byteBuf</code>
 *   is not copied or altered.
 */

BitBufReader(
  int bugs,
  int bufType,
  byte[] byteBuf,          // the data
  int offsetInFile,        // for debugging, the offset in the BUFR file
  int offsetInMessage)     // for debugging, the offset in the BUFR message
{
  this.bugs = bugs;
  this.bufType = bufType;
  this.byteBuf = byteBuf;
  this.offsetInFile = offsetInFile;
  this.offsetInMessage = offsetInMessage;

  totBits = 8 * byteBuf.length;
  bitPos = 0;
}



public String toString() {
  String res = String.format("bufType: %s\n"
    + "  total bytes: %d = 0x%x\n"
    + "  totBits: %d\n"
    + "  bitPos: %d  which is bit %d  within byte %d = 0x%x\n",
    typeNames[bufType],
    byteBuf.length, byteBuf.length,
    totBits,
    bitPos, bitPos % 8, bitPos / 8, bitPos / 8);
  return res;
}



/** Returns current bit position */
int getBitPos() {
  return bitPos;
}



/** Sets current bit position */
void setBitPos( int bitPos) {
  this.bitPos = bitPos;
}


/** Returns buffer length in bytes */
int getByteLength() {
  return byteBuf.length;
}



int getRemainBitLen() {
  return totBits - bitPos;
}




/**
 * Returns true if:
 * <ul>
 * <li>  for BBTP_DATA: bitPos >= totBits
 * <li>  for BBTP_DESC: bitPos > totBits - BufrUtil.descBits
 * </ul>
 */

boolean atEof()
throws BufrException
{
  boolean bres = false;
  if (bitPos > totBits) throwerr("bitPos > totBits");
  if (bufType == BBTP_DATA) {
    if (bitPos >= totBits) bres = true;
  }
  else if (bufType == BBTP_DESC) {
    if (bitPos > totBits - BufrUtil.descBits) bres = true;
  }
  return bres;
}





String formatBytes(
  int nbytes)
throws BufrException
{
  StringBuilder sbuf = new StringBuilder();
  int offset = bitPos / 8;
  sbuf.append( String.format("bitPos: %d  which is bit %d"
    + " within byte %d = 0x%x\n",
    bitPos, bitPos % 8, offset, offset));

  offset = offsetInFile + bitPos / 8;
  sbuf.append( String.format("Byte offset in file: %d = 0x%x\n",
    offset, offset));

  offset = offsetInMessage + bitPos / 8;
  sbuf.append( String.format("Byte offset in message: %d = 0x%x\n",
    offset, offset));

  int ilim = Math.min( totBits, bitPos + nbytes*8);
  if (ilim < totBits && ilim % 8 != 0)
    ilim = ilim + 8 - ilim % 8;    // round up to byte boundary

  int ipos = 8 * (bitPos / 8);     // round down to byte boundary
  while (ipos < ilim) {
    sbuf.append( String.format("  %4d=x%04x:  ", ipos/8, ipos/8));
    for (int jj = 0; jj < 6*8; jj++) {
      if (ipos >= ilim) break;

      char cc;
      if (ipos < bitPos) cc = '_';
      else {
        byte bt = byteBuf[ipos/8];
        int ival = (bt >> (7 - ipos%8)) & 1;
        if (ival == 0) cc = '0';
        else cc = '1';
      }
      if (ipos%8 == 0) sbuf.append("  ");
      sbuf.append( cc);
      ipos++;
    }
    sbuf.append("\n");
  }
  return sbuf.toString();
}









/**
 * Convenience method to return a descriptor.
 * Returns the next BufrUtil.descBits.
 * Increments bitPos.
 */
int getDesc()
throws BufrException
{
  if (bufType != BBTP_DESC) throwerr("bufType != BBTP_DESC");
  int fxy = getInt( BufrUtil.descBits);
  return fxy;
}



/**
 * Convenience to return a descriptor at a specific bitPos,
 * without incrementing bitPos.
 */
int getDescNoInc( int ipos)
throws BufrException
{
  if (bufType != BBTP_DESC) throwerr("bufType != BBTP_DESC");
  int svBitPos = bitPos;
  bitPos = ipos;
  int fxy = getDesc();
  bitPos = svBitPos;              // restore bitPos
  return fxy;
}



/**
 * Checks that the next descriptor has the specified
 * fval, xval, yval.
 * Does not increment bitPos.
 */
void checkNextDesc(
  int fval,
  int xval,
  int yval)
throws BufrException
{
  int fxy = getDescNoInc( bitPos);
  int descFval = BufrUtil.getFval( fxy);
  int descXval = BufrUtil.getXval( fxy);
  int descYval = BufrUtil.getYval( fxy);
  if (descFval != fval
    || descXval != xval
    || descYval != yval)
  {
    throwerr("next desc mismatch");
  }
}



// Not used:
// /**
//  * Retrieves the next desBits and converts to a double
//  * using:<br>
//  *  dblValue = (bitVal + reference) / 10**scale <br>
//  * Increments bitPos.
//  */
// double getDouble(
//   int desBits,
//   int scale,
//   long reference)
// throws BufrException
// {
//   if (bufType != BBTP_DATA) throwerr("bufType != BBTP_DATA");
// 
//   // encoded = 10^scale * true - reference
//   // true = (encoded + reference) / 10^scale
// 
//   long ival = getInt( desBits);
// 
//   int absScale = Math.abs( scale);
//   double pwr = 1.0;
//   for (int ii = 0; ii < absScale; ii++) {
//     pwr *= 10.0;
//   }
//   double dval = ival + reference;
//   if (scale >= 0) dval /= pwr;
//   else dval *= pwr;
//   if (bugs >= 15) prtln("getDouble: final dval: " + dval);
//   return dval;
// }






/**
 * Retrieves the next desBits and returns as an int.
 * Increments bitPos.
 */
int getInt( int desBits)
throws BufrException
{
  if (bugs >= 15) prtln("\ngetInt: type: " + typeNames[bufType]
    + "  desBits: " + desBits);
  if (desBits <= 0) throwerr("desBits <= 0");
  if (desBits > 64-8) throwerr("desBits too big");

  if (bufType == BBTP_DATA) {}
  else if (bufType == BBTP_DESC) {
    if (desBits != BufrUtil.descBits) throwerr("wrong len for BBTP_DESC");
  }
  else throwerr("invalid bufType");

  if (bitPos + desBits > totBits)
    throwerr("request goes past EOF.  bitPos: " + bitPos
      + "  desBits: " + desBits + "  totBits: " + totBits);

  // Caution: actual shift amount = (requested shift amount) % 64.
  // Another Java weirdness.

  int svBitPos = bitPos;
  int nv = 0;        // num valid bits acquired so far
  long lval = 0;     // output value
  while (nv < desBits) {
    lval <<= 8;
    if (nv == 0) {          // If first time
      int numValid = 8 - bitPos % 8;    // num bits not yet used in next byte
      lval = 0xff & byteBuf[bitPos/8];  // get next byte
      int shiftLen = 64 - numValid;     // shift to clear old bits
      lval = (lval << shiftLen) >>> shiftLen;
      bitPos += numValid;
      nv += numValid;
    }
    else {
      lval |= (0xff & byteBuf[bitPos/8]);
      bitPos += 8;
      nv += 8;
    }
    if (bugs >= 20) {
      prtln( String.format("getInt: loop end: nv: %d  lval: 0x%x", nv, lval));
    }
  }
  bitPos = svBitPos + desBits;

  // Shift out the excess bits in lval
  lval >>>= (nv - desBits);
  if (bugs >= 15) {
    prtln( String.format(
      "getInt: initial bitPos: %d  which is bit %d  within byte %d = 0x%x",
        svBitPos, svBitPos % 8, svBitPos / 8, svBitPos / 8));
    prtln( String.format(
      "getInt: final bitPos: %d  which is bit %d  within byte %d = 0x%x",
        bitPos, bitPos % 8, bitPos / 8, bitPos / 8));
    prtln( String.format("getInt: final lval: %d = 0x%x", lval, lval));
  }
  if (desBits > 31) throwerr("desBits too big.  desBits: " + desBits
    + "  lval: " + lval);
  int ival = (int) lval;      // truncate.
  return ival;
}



/**
 * Retrieves the next desBits and returns the trimmed String.
 * Never returns null.
 * Increments bitPos.
 */

String getTrimString(
  int desBits)
throws BufrException
{
  String trimStg = getRawString( desBits).trim();
  return trimStg;
}





/**
 * Retrieves the next desBits and returns as a String.
 * Never returns null.
 * Increments bitPos.
 */
String getRawString(
  int desBits)
throws BufrException
{
  if (bufType != BBTP_DATA) throwerr("bufType != BBTP_DATA");
  StringBuffer sbuf = new StringBuffer();
  if (desBits <= 0) throwerr("desBits <= 0");
  if (desBits % 8 != 0) throwerr("desBits not a multiple of 8");

  if (bitPos + desBits > totBits)
    throwerr("request goes past EOF.  bitPos: " + bitPos
      + "  desBits: " + desBits + "  totBits: " + totBits);

  if (bitPos % 8 == 0) {   // if aligned on byte boundary
    int gotBits = 0;
    while (true) {
      sbuf.append( (char) byteBuf[bitPos/8]);
      bitPos += 8;
      gotBits += 8;
      if (gotBits >= desBits) break;
    }
  }
  else {      // else not aligned on byte boundaries
    int shiftLen = 8 - bitPos % 8;     // num valid bits in current byte
    for (int ii = 0; ii < desBits/8; ii++) {
      int ival = (0xff & byteBuf[bitPos/8]) << 8;
      bitPos += 8;
      ival |= (0xff & byteBuf[bitPos/8]);
      ival >>>= shiftLen;
      sbuf.append( (char) (ival & 0xff));
    }
  }
  return sbuf.toString();
}





/**
 * Print usage info for main() test driver.
 */
static void badparms( String msg) {
  prtln("\nError: " + msg);
  System.exit(1);
}




/**
 * Test driver.
 */
public static void main( String[] args) {

  try {
    int bugs = 0;
    if (args.length % 2 != 0) badparms("args must be key/value pairs");
    for (int iarg = 0; iarg < args.length - 1; iarg += 2) {
      String key = args[iarg];
      String val = args[iarg+1];
      if (key.equals("-d")) bugs = BufrUtil.parseInt( key, val);
      else badparms("unknown key: \"" + key + "\"");
    }

    prtln("main: bugs: " + bugs);
    byte[] testbuf = new byte[100];
    for (int ii = 0; ii < testbuf.length; ii++) {
      testbuf[ii] = (byte) 0xff;
    }
    testbuf[1] = 0;
    for (int ii = 0; ii < 10; ii++) {
      prtln( String.format("testbuf[%d]: 0x%02x", ii, 0xff & testbuf[ii]));
    }

    BitBufReader bitBuf;
    bitBuf = new BitBufReader( bugs, BBTP_DATA, testbuf, 0, 0);
    for (int desBits = 1; desBits < 10; desBits++) {
      prtln("formatBytes: " + bitBuf.formatBytes(8));
      int ival = bitBuf.getInt( desBits);
      prtln("desBits: " + desBits + "  ival: " + ival);
    }

    testbuf = new byte[] {'a', 'b', 'c', 'd', 'e', 'f'};
    bitBuf = new BitBufReader( bugs, BBTP_DATA, testbuf, 0, 0);
    String alignStg = bitBuf.getRawString( 2*8);
    prtln("main: alignStg: \"" + alignStg + "\"");

    // Use "ab" shifted 1 bit right
    testbuf = new byte[] { (byte)0xb0, (byte)0xb1, 0, 0, 0, 0};
    bitBuf = new BitBufReader( bugs, BBTP_DATA, testbuf, 0, 0);
    String shiftStg = bitBuf.getRawString( 2*8);
    prtln("main: shiftStg: \"" + shiftStg + "\"");
  }
  catch( BufrException exc) {
    BufrUtil.prtlnexc("caught", exc);
    System.exit(1);
  }
}




static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BitBufReader: " + msg);
}


static void prtln( String msg) {
  System.out.println( msg);
}



} // end class
