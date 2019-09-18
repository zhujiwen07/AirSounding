package www.cnr7.com;

import org.geotools.xml.xsi.XSISimpleTypes;
import www.cnr7.com.bean.RadarStation;
import www.cnr7.com.services.RadarService;
import www.cnr7.com.services.impl.RadarServiceImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    // 高程数据路径，tif格式
    private static final String elevationPath = "E:/Project/2019/9test/data/henan/henan_1.tif";

    private static final RadarService radarService = new RadarServiceImpl();

    static {
        // 1.读取高程数据至内存
        radarService.readDemFile2Grid(new File(elevationPath));
    }

    public static void main( XSISimpleTypes.String[] args )
    {
        // 2.根据具体的雷达站点画图
        RadarStation radarStation = new RadarStation(115.63,34.407,0d,75000d);
        BufferedImage bufferedImage = radarService.drawRadarMaxElevation(radarStation);
        try {
            ImageIO.write(bufferedImage, "png", new File("E:/Project/2019/9test/test2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
