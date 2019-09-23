package www.cnr7.com;

import org.junit.Test;
import www.cnr7.com.bean.RadarStation;
import www.cnr7.com.conf.RadarConf;
import www.cnr7.com.services.RadarService;
import www.cnr7.com.services.impl.RadarServiceImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
        // 2.根据具体的雷达站点画图
        RadarStation radarStation = new RadarStation(115.63,34.407,2d,230000d);
        /*BufferedImage bufferedImage = radarService.drawRadarMaxElevation(radarStation,true);
        try {
            ImageIO.write(bufferedImage, "png", new File("E:/Project/2019/9test/最大仰角图.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        radarStation.setElevation(0.5);
        BufferedImage bufferedImage1 = radarService.drawEquivalentRadius(radarStation,true);

        try {
            ImageIO.write(bufferedImage1, "png", new File("E:/Project/2019/9test/离地高度分布模拟图.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 2.读取雷达站点基础经纬度数据
        /*List<RadarStation> radarStations = radarService.readCsvFile2List(new File(stationPath), "GBK");
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
        }*/
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
        BufferedImage bufferedImage = radarService.drawRadarMaxElevation(radarStation,false);
        try {
            ImageIO.write(bufferedImage, "png", new File("E:/Project/2019/9test/test1.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 画高度-面积指数图
     */
    @Test
    public void drawHeightAreaIndex(){

    }

    private void getMaxHeight(RadarStation radarStation){
        /*ArrayList<Double> maxElevation = new ArrayList();
        ArrayList<Double> degressList = new ArrayList();
        int stationLength = (int) (radarStation.getRadius() / RadarConf.MaxElevation.dertDistance);
        double[] lonlat;
        double stationH = getDem(radarStation.getLon(),radarStation.getLat()) + radarStation.getHeight();

        for (int i = 0; i < 360; i++) {
            for (int j = 0; j < stationLength; j++) {
                lonlat = computerThatLonLat(radarStation.getLon(),radarStation.getLat(),i,(j+1)*RadarConf.MaxElevation.dertDistance);
                degressList.add(calElevation((j+1)*RadarConf.MaxElevation.dertDistance,getDem(lonlat[0],lonlat[1])-stationH));
            }
            maxElevation.add(Collections.max(degressList));
            degressList.clear();
        }*/
    }
}
