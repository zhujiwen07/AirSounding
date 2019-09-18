package www.cnr7.com;

import org.junit.Test;
import www.cnr7.com.bean.RadarStation;
import www.cnr7.com.services.RadarService;
import www.cnr7.com.services.impl.RadarServiceImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Author zhujiwen
 * @Date 星期四 2019-09-12 10:25
 * @Version 1.0
 * @Description
 **/
public class RadarServiceTest {
    // 雷达站点数据路径
    private static final String stationPath = "E:/Project/2019/9test/data/henan/leida.csv";
    // 高程数据路径
    private static final String elevationPath = "E:/Project/2019/9test/data/henan/henan_1.tif";

    private static final RadarService radarService = new RadarServiceImpl();

    static {
        // 1.读取高程数据至内存，tif格式数据
        radarService.readDemFile2Grid(new File(elevationPath));
    }

    @Test
    public void test(){
        // 2.读取雷达站点基础经纬度数据
        List<RadarStation> radarStations = radarService.readCsvFile2List(new File(stationPath), "GBK");
        // 3.根据雷达站点绘制雷达最大仰角图
        for (RadarStation radarStation : radarStations) {
            // 设置雷达扫描半径
            radarStation.setRadius(75000d);
            BufferedImage bufferedImage = radarService.drawRadarMaxElevation(radarStation);
            try {
                ImageIO.write(bufferedImage, "png", new File("E:/Project/2019/9test/" + radarStation.getStationNo() + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据经纬度以及雷达扫描半径绘图
     */
    @Test
    public void getImgByLoc(){
        // 输入四参数，经度、纬度、雷达扫描半径（单位：米）
        double lon = 115.63;
        double lat = 34.407;
        double height = 5d;
        double radius = 75000d;

        // 2.已知四参数 new RadarStation 对象
        RadarStation radarStation = new RadarStation(lon,lat,height,radius);
        // 3.根据雷达站点绘制雷达最大仰角图
        BufferedImage bufferedImage = radarService.drawRadarMaxElevation(radarStation);
        try {
            ImageIO.write(bufferedImage, "png", new File("E:/Project/2019/9test/test1.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
