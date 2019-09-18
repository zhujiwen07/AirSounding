package www.cnr7.com;

import www.cnr7.com.bean.TlogP;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        TlogP tlogP = new TlogP(825,895);
        tlogP.drawTlogPBaseMap();
        tlogP.dispose();
        try {
            ImageIO.write(tlogP.getBufferedImage(), "png", new File("E:/Project/2019/9test/4.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
