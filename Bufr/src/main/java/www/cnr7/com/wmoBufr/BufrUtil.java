
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.LinkedList;


/**
 * Miscellaneous utility methods.
 *
 * @author S. Sullivan
 */

class BufrUtil {


static String missingTag   = "*Missing*";
static String reservedTag  = "*Reserved*";
static String unknownTag   = "*Unknown*";
static int descBits = 16;      // num bits in a descriptor




static void checkFxy( int fval, int xval, int yval)
throws BufrException
{
  if ((fval < 0 || fval >= 4) && fval != BufrMessage.CUSTOM_FVAL)
    throwerr("bad fval: " + fval);
  if (xval < 0 || xval >= 0x40) throwerr("bad xval: " + xval);
  if (yval < 0 || yval >= 0x100) throwerr("bad yval: " + yval);
}





// Convert f,x,y to standard fxy value.

static int getFxy(
  int fval,             // 2 bits, max dec = 3
  int xval,             // 6 bits, max dec = 63
  int yval)             // 8 bits, max dec = 255
throws BufrException
{
  checkFxy( fval, xval, yval);
  return getFxyNoCheck( fval, xval, yval);
}



// Convert f,x,y to standard fxy value.
// Used only for init of BufrMessage.CUSTOM_*_FXY.

static int getFxyNoCheck(
  int fval,             // 2 bits, max dec = 3
  int xval,             // 6 bits, max dec = 63
  int yval)             // 8 bits, max dec = 255
{
  int fxy = (fval << 14) | (xval << 8) | yval;
  return fxy;
}





// Extract fval from fxy value.

static int getFval(
  int fxy)
throws BufrException
{
  // The fval is normally 2 bits, but we use larger fvals
  // for BufrMessage.CUSTOM_FVAL.
  int fval = fxy >>> 14;                     // 2 bits, max dec = 3
  int xval = (fxy >>> 8) & 0x3f;             // 6 bits, max dec = 63
  int yval = fxy & 0xff;                     // 8 bits, max dec = 255
  checkFxy( fval, xval, yval);
  return fval;
}




// Extract xval from fxy value.

static int getXval(
  int fxy)
throws BufrException
{
  int fval = (fxy >>> 14) & 0x3;
  int xval = (fxy >>> 8) & 0x3f;
  int yval = fxy & 0xff;
  checkFxy( fval, xval, yval);
  return xval;
}




// Extract yval from fxy value.

static int getYval(
  int fxy)
throws BufrException
{
  int fval = (fxy >>> 14) & 0x3;
  int xval = (fxy >>> 8) & 0x3f;
  int yval = fxy & 0xff;
  checkFxy( fval, xval, yval);
  return yval;
}



// Don't throw anything since this is called from toString()
// in various classes.

static String formatFxy( int fxy)
{
  // Don't throw anything since this is called from toString()
  String res = "*INVALID*";
  try {
    res = String.format("%01d_%02d_%03d",
      getFval( fxy), getXval( fxy), getYval( fxy));
  }
  catch( BufrException exc) {
    exc.printStackTrace();
    System.exit(1);
  }
  return res;
}




// Parse string to get fxy value.
// Format can be either "fxxyyy" or "f_xx_yyy"

static int parseFxy(
  String msg,         // error message info
  String stg)         // string to be parsed
throws BufrException
{
  boolean hasBlanks = false;
  if (stg.length() == 6) {}
  else if (stg.length() == 8) hasBlanks = true;
  else throwerr("Invalid fxy format in " + msg + ": \"" + stg + "\"");
  int ix = 0;
  int fval = parseInt( msg, stg.substring( ix, ix+1));
  ix++;
  if (hasBlanks) {
    if (stg.charAt(ix) != '_')
      throwerr("Invalid fxy format in " + msg + ": \"" + stg + "\"");
    ix++;
  }
  int xval = parseInt( msg, stg.substring( ix, ix+2));
  ix += 2;
  if (hasBlanks) {
    if (stg.charAt(ix) != '_')
      throwerr("Invalid fxy format in " + msg + ": \"" + stg + "\"");
    ix++;
  }
  int yval = parseInt( msg, stg.substring( ix, ix+3));
  ix += 3;
  if (ix != stg.length())
    throwerr("Invalid fxy format in " + msg + ": \"" + stg + "\"");
  int fxy = getFxy( fval, xval, yval);
  return fxy;
}







static int parseInt(
  String msg,         // error message info
  String stg)         // string to be parsed
throws BufrException
{
  int ires = 0;
  try {
    ires = Integer.parseInt( stg, 10);
  }
  catch( NumberFormatException exc) {
    throwerr("Invalid value for " + msg + ": \"" + stg + "\"");
  }
  return ires;
} // end parseInt




static boolean parseBoolean(
  String msg,         // error message info
  String stg)         // string to be parsed
throws BufrException
{
  boolean bres = false;
  if (stg.equals("n")) bres = false;
  else if (stg.equals("y")) bres = true;
  else throwerr("Invalid value for " + msg + ": \"" + stg + "\"");
  return bres;
} // end parseBoolean




static int parseKeyword(
  String msg,         // error message info
  String[] names,     // legal names
  boolean allow0,     // allow ires = 0
  String stg)         // string to be parsed
throws BufrException
{
  int ires = -1;
  for (int ii = 0; ii < names.length; ii++) {
    if (stg.equals( names[ii])) {
      ires = ii;
      break;
    }
  }
  if (ires == -1 || (ires == 0 && ! allow0))
    throwerr("Invalid value for " + msg + ": \"" + stg + "\"");
  return ires;
} // end parseInt





/**
 * Returns a mask having the low order numBits bits set.
 */

static int mkBitMask( int numBits) {

  int ival = 0;
  for (int ii = 0; ii < numBits; ii++) {
    ival <<= 1;
    ival |= 1;
  }
  return ival;
}



/**
 * Returns the portion of fname after the last slash.
 */

static String getFilenameTail( String fname) {
  String res = fname;
  String slash = System.getProperty("file.separator");
  int ix = res.lastIndexOf( slash);
  if (ix >= 0) res = res.substring( ix+1);
  return res;
}





/**
 * Returns the fname without the final ".suffix", if any.
 */

static String getBaseName( String fname) {
  String res = fname;
  String slash = System.getProperty("file.separator");
  int ixslash = res.lastIndexOf( slash);
  int ixdot = res.lastIndexOf( ".");
  if (ixdot > 0 && ixdot > ixslash) res = res.substring( 0, ixdot);
  return res;
}




static String getCanonicalPath( String fname)
throws BufrException
{
  String path = "";
  try {
    path = new File( fname).getCanonicalPath();
  }
  catch( IOException exc) {
    BufrUtil.prtlnexc("caught", exc);
    throwerr("caught: " + exc);
  }
  return path;
}









static boolean isPrintableBlack( int ival) {
  boolean res = false;
  if ( ival >=  33 && ival <= 126     // See: man ascii
    || ival >= 161 && ival <= 255)    // See: man iso_8859-15
    res = true;
  return res;
}



static boolean isPrintableWhite( int ival) {
  boolean res = false;
  if ( ival ==   9       // '\t'    See: man ascii
    || ival ==  10       // '\n'
    || ival ==  13       // '\r'
    || ival ==  32       // space
    || ival == 160)      // non-breakable space.  See: man iso_8859-15
    res = true;
  return res;
}


static boolean isPrintable( int ival) {
  boolean res = isPrintableBlack( ival) || isPrintableWhite( ival);
  return res;
}


/**
 * Translates whitespace and unprintables to newchar
 */
static String translateNonBlack( char newchar, String stg) {
  StringBuilder sbuf = new StringBuilder();
  for (int ii = 0; ii < stg.length(); ii++) {
    int ival = 0xff & stg.charAt(ii);
    if (BufrUtil.isPrintableBlack(ival)) sbuf.append((char) ival);
    else sbuf.append( newchar);
  }
  return sbuf.toString();
}





/**
 * Translates non-printables to newchar
 */
static String translateNonPrintable( char newchar, String stg) {
  StringBuilder sbuf = new StringBuilder();
  for (int ii = 0; ii < stg.length(); ii++) {
    int ival = 0xff & stg.charAt(ii);
    if (BufrUtil.isPrintable(ival)) sbuf.append((char) ival);
    else sbuf.append( newchar);
  }
  return sbuf.toString();
}





  
/**
 * Translates the specified chars to newchar.
 * Called by BufrMessage.getOutSpecName to get rid of slashes
 * and such in file names.
 */
static String translateChars( String oldchars, char newchar, String stg) {
  StringBuilder sbuf = new StringBuilder();
  for (int ii = 0; ii < stg.length(); ii++) {
    int ival = 0xff & stg.charAt(ii);
    char cval = (char) ival;
    if (oldchars.indexOf( cval) < 0) sbuf.append( cval);
    else sbuf.append( newchar);
  }
  return sbuf.toString();
}





static String cleanXml( String stg) {
  String res = "";
  if (stg != null) {
    res = stg;
    res = translateNonPrintable( '_', res);
    res = res.replaceAll("&", "&amp;");
    res = res.replaceAll("<", "&lt;");
    res = res.replaceAll(">", "&gt;");
    res = res.replaceAll("\"", "&quot;");
    res = res.replaceAll("\'", "&apos;");
  }
  return res;
}






static String cleanText( String stg) {
  String res = "";
  if (stg != null) {
    res = stg;
    res = translateNonPrintable( '_', res);
    res = res.replaceAll("\\\\", "\\\\\\\\");   // change \\ to \\\\
    res = res.replaceAll("\\\"", "\\\\\"");     // change \" to \\\"
  }
  return res;
}





static void validateYear(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 1900 || ival > 2100)
    throwValidateInt( "year", ival, bmsg, subsetNum, bitem);
}


static void validateYearInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 1)
    throwValidateInt( "year increment", ival, bmsg, subsetNum, bitem);
}


static void validateMonth(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 1 || ival > 12)
    throwValidateInt( "month", ival, bmsg, subsetNum, bitem);
}


static void validateMonthInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 12)
    throwValidateInt( "month increment", ival, bmsg, subsetNum, bitem);
}


static void validateDay(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 1 || ival > 31)
    throwValidateInt( "day", ival, bmsg, subsetNum, bitem);
}


static void validateDayInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 31)
    throwValidateInt( "day increment", ival, bmsg, subsetNum, bitem);
}


static void validateHour(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 23)
    throwValidateInt( "hour", ival, bmsg, subsetNum, bitem);
}


static void validateHourInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 24)
    throwValidateInt( "hour increment", ival, bmsg, subsetNum, bitem);
}


static void validateMinute(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 59)
    throwValidateInt( "minute", ival, bmsg, subsetNum, bitem);
}


static void validateMinuteInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 60)
    throwValidateInt( "minute increment", ival, bmsg, subsetNum, bitem);
}


static void validateSecond(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 59)
    throwValidateInt( "second", ival, bmsg, subsetNum, bitem);
}


static void validateSecondInc(
  int ival,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (ival < 0 || ival > 60)
    throwValidateInt( "second increment", ival, bmsg, subsetNum, bitem);
}


static void validateFloatSecond(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (dval < 0 || dval >= 60)
    throwValidateFloat( "float second", dval, bmsg, subsetNum, bitem);
}


static void validateLatitude(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (dval < -90 || dval > 90)
    throwValidateFloat( "latitude", dval, bmsg, subsetNum, bitem);
}


static void validateLatitudeInc(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (dval < -90 || dval > 90)
    throwValidateFloat( "latitude increment", dval, bmsg, subsetNum, bitem);
}


static void validateLongitude(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (dval < -180 || dval > 180)
    throwValidateFloat( "longitude", dval, bmsg, subsetNum, bitem);
}


static void validateLongitudeInc(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  if (dval < -180 || dval > 180)
    throwValidateFloat( "longitude increment", dval, bmsg, subsetNum, bitem);
}


static void validateHeight(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  // allow anything
}


static void validateHeightInc(
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  // allow anything
}


static void throwValidateInt(
  String msgParm,
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  String msg = "Validation error: " + msgParm + "\n"
    + "value in question: " + dval + "\n"
    + "inFile: \"" + bmsg.bfile.inFile + "\"\n"
    + "BUFR msg num: " + bmsg.msgNum + "\n"
    + "BUFR subset num: " + subsetNum + "\n";
  if (bitem != null) msg += "BufrItem: " + bitem + "\n";
  prtln( msg);
  throw new BufrException( msg);
}


static void throwValidateFloat(
  String msgParm,
  double dval,
  BufrMessage bmsg,
  int subsetNum,
  BufrItem bitem)
throws BufrException
{
  String msg = "Validation error: " + msgParm + "\n"
    + "value in question: " + dval + "\n"
    + "inFile: \"" + bmsg.bfile.inFile + "\"\n"
    + "BUFR msg num: " + bmsg.msgNum + "\n"
    + "BUFR subset num: " + subsetNum + "\n";
  if (bitem != null) msg += "BufrItem: " + bitem + "\n";
  prtln( msg);
  throw new BufrException( msg);
}





//xxx del:
///static String quoteTruncate( boolean isXml, int len, String stg)
///throws BufrException
///{
///  if (stg == null) throwerr("stg is null");
///  stg = truncate( len, stg);
///  return quote( isXml, stg);
///}






static String truncate( int len, String stg)
throws BufrException
{
  if (stg == null) throwerr("stg is null");
  if (len > 0 && stg.length() > len) stg = stg.substring( 0, len);
  return stg;
}





static String quote( boolean isXml, String stg)
throws BufrException
{
  if (stg == null) throwerr("stg is null");
  if (isXml) stg = cleanXml( stg);
  else stg = cleanText( stg);
  if (stg.indexOf("\"") >= 0) throwerr("String contains a quote");
  return "\"" + stg + "\"";
}





// Format the true value, rounding appropriately
// to avoid representations like "199999999" or "0.001999999"
// when the number of significant digits is small.
//
// Find the true numeric value.
// encoded = 10^scale * trueval - reference
// trueval = (encoded + reference) * 10^(-scale)
// See:
//   3.1.3.3 Table B - Classification of Elements
//   3.1.6.1 Changing Data Width, Scale, and Reference Value

static String formatTrueValue(
  int scale,          // scale from TableDesc
  int reference,      // reference from TableDesc
  int encval)         // encoded value
{
  double tmpabs;
  String res;
  double dval = encval + reference;
  if (dval == 0) res = "0";
  else {
    // Find the true num significant digits
    int trueSig = 0;
    tmpabs = Math.abs( dval);
    while (tmpabs >= 1) {
      tmpabs /= 10;
      trueSig++;
    }

    if (scale <= 0) {
      for (int ii = 0; ii < -scale; ii++) {
        dval *= 10;
      }
    }
    else dval *= Math.pow( 10, -scale);

    // Find num digits left of dec point
    int numLeft = 0;
    tmpabs = Math.abs( dval);
    while (tmpabs >= 1) {
      tmpabs /= 10;
      numLeft++;
    }

    // Find fmt = appropriate format
    double dabs = Math.abs( dval);
    String fmt;
    if (dabs >= 1.e10 && numLeft > trueSig + 5)
      fmt = "%" + trueSig + "e";                // exponential notation
    else if (scale <= 0)
      fmt = "%.0f";                            // integer
    else if (dabs >= 1.e-3) {
      if (numLeft >= trueSig) fmt = "%.0f";       // integer
      else fmt = "%." + (trueSig - numLeft) + "f";
    }
    else fmt = "%" + trueSig + "e";             // exponential notation

    //prtln("dval: " + dval
    //  + "  trueSig: " + trueSig
    //  + "  numLeft: " + numLeft
    //  + "  fmt: \"" + fmt + "\"");

    StringBuilder sbuf = new StringBuilder();
    Formatter fmtr = new Formatter( sbuf);
    fmtr.format( fmt, dval);
    res = sbuf.toString();
  } // if not 0
  return res;
} // end formatTrueValue




// Returns true if value's low order bitLen bits are all 1.

static boolean isAllOnes( int bitLen, int value) {
  int shiftlen = 32 - bitLen;
  int mask = (-1 << shiftlen) >>> shiftlen;      // bits: 000000011111

  boolean bres = false;
  if ((value ^ mask) == 0) bres = true;
  return bres;
}





static String mkIndent( int indent) {
  String res = "";
  for (int ii = 0; ii < indent; ii++) {
    res += "  ";
  }
  return res;
}









public static void main( String[] args) {
  int reference = 0;
  int encval = 333;
  for (int scale = -20; scale <= 20; scale++) {
    String stg = formatTrueValue( scale, reference, encval);
    prtln("encval: " + encval
      + "  scale: " + scale
      + "  stg: \"" + stg + "\"");
  }
}









static void throwerr( String msg)
throws BufrException
{
  throw new BufrException("BufrUtil: " + msg);
}



static void prtlnexc(
  String msg,
  Throwable exc)
{
  prtln( msg + "\n"
    + "Exception:\n"
    + formatStackTrace( exc) + "\n");
}



static String formatStackTrace(
  Throwable exc)
{
  StringWriter swtr = new StringWriter();
  PrintWriter pwtr = new PrintWriter( swtr);
  exc.printStackTrace( pwtr);
  pwtr.close();
  return swtr.toString();
}



static void prtln( String msg) {
  System.out.println( msg);
}






} // end class
