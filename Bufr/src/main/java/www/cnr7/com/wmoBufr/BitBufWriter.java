
package www.cnr7.com.wmoBufr;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;


// Buffer for writing bit streams.


class BitBufWriter {

int _ipos;       // current byte position
int _nfree;      // num free bits in byte at _ipos
byte[] _bbuf;    // byte buffer


BitBufWriter() {
  _ipos = 0;
  _nfree = 8;
  _bbuf = new byte[0];
}


public String toString() {
  String res = String.format(
    "_ipos: %d = 0x%x\n  _nfree: %d\n  _bbuf.length: %d\n",
    _ipos, _ipos, _nfree, _bbuf.length);
  return res;
}


void putBits(
  int prmLen,        // num valid bits in ival
  int prmVal)        // bits in low order end of word
{
  prtln( String.format("\nEntry: prmLen: %d  prmVal: 0x%x", prmLen, prmVal));
  int needLen = _ipos + 1 + prmLen / 8 + 1;
  if ( needLen >= _bbuf.length)
    _bbuf = Arrays.copyOf( _bbuf, 2 * needLen);

  while (prmLen > 0) {
    int ival = 0xff & _bbuf[_ipos];
    prtln("loop head");
    prtln( String.format("  _ipos: %d  _nfree: %d", _ipos, _nfree));
    prtln( String.format("  prmLen: %d  prmVal: 0x%x", prmLen, prmVal));
    prtln( String.format("  ival: 0x%x", 0xff & ival));

    int usedLen;
    if (prmLen >= _nfree) {
      // Pick bits:  start = 32-prmLen, limit = 32 - (prmLen - _nfree)
      int bitMask = prmVal << (32 - prmLen);   // clear high order bits
      bitMask >>>= (32 - _nfree);              // move bits to start of _nfree
      ival |= bitMask;
      usedLen = _nfree;
    }
    else {  // else prmLen < _nfree
      int bitMask = prmVal << (32 - prmLen);   // clear high order bits
      bitMask >>>= (32 - _nfree);              // move bits to start of _nfree
      ival |= bitMask;
      usedLen = prmLen;
    }

    _bbuf[_ipos] = (byte) ival;
    prmLen -= usedLen;
    _nfree -= usedLen;
    prtln("updated:");
    prtln( String.format("  _ipos: %d  _nfree: %d", _ipos, _nfree));
    prtln( String.format("  prmLen: %d  prmVal: 0x%x", prmLen, prmVal));
    prtln( String.format("  ival: 0x%x", 0xff & ival));
    if (_nfree == 0) {
      _ipos++;
      _nfree = 8;
      prtln("inc _ipos to: " + _ipos);
    }
  } // while PrmLen > 0
} // end putBits


void writeFile( OutputStream ostm)
throws IOException
{
  int wlen = _ipos;
  if (_nfree < 8) wlen++;
  ostm.write( _bbuf, 0, wlen);
}


static void prtln( String msg) {
  System.out.println( msg);
}

} // end class BitBufWriter  
