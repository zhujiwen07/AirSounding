
package www.cnr7.com.wmoBufr;

import java.io.FileOutputStream;
import java.io.IOException;


public class TestBits {


static void badparms( String msg) {
  prtln("Error: " + msg);
  prtln("Parms:");
  prtln("  -outfile   <stg>");
  System.exit(1);
}


public static void main( String[] args) {
  try { runit( args); }
  catch( Exception exc) {
    prtln("caught: " + exc);
    exc.printStackTrace();
    System.exit(1);
  }
}



static void runit( String[] args)
throws Exception
{
  String outfile = null;

  if (args.length % 2 != 0) badparms("parms must be key/value pairs");
  for (int iarg = 0; iarg < args.length; iarg += 2) {
    String key = args[iarg];
    String val = args[iarg+1];
    if (key.equals("-outfile")) outfile = val;
    else badparms("unknown parm: " + key);
  }

  if (outfile == null) badparms("parm not specified: -outfile");

  BitBufWriter bitBuf = new BitBufWriter();
  for (int ii = 0; ii < 32; ii++) {
    bitBuf.putBits( ii, -(ii % 2));
  }
  FileOutputStream ostm = new FileOutputStream( outfile);
  bitBuf.writeFile( ostm);
  ostm.close();
}






static void prtln( String msg) {
  System.out.println( msg);
}

} // end class
