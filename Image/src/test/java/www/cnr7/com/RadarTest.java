package www.cnr7.com;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


/**
 * @Author zhujiwen
 * @Date 星期二 2019-09-10 9:49
 * @Version 1.0
 * @Description
 **/
public class RadarTest {
    /*
	 * 大地坐标系资料WGS-84 长半径a=6378137 短半径b=6356752.3142 扁率f=1/298.2572236
	 */
    /** 长半径a=6378137 */
    private static final double a = 6378137;
    /** 短半径b=6356752.3142 */
    private static final double b = 6356752.3142;
    /** 扁率f=1/298.2572236 */
    private static final double f = 1 / 298.2572236;

    // 站点名称
    private static final String stationName = "洛阳新一代天气雷达站";
    // 站点编号
    private static final String stationNo = "Z9379";

    private static final double stationLon = 112.449388888889;

    private static final double stationLat = 34.5620833333333;


    /*// 雷达仰角
    private static double elevationAngle = 0.5d;
    // 弧度
    private static double radian = Math.toRadians(elevationAngle);*/
    // 扫描半径（单位：m）
    private static double r = 75000d;

    private static int width = 1000;

    private static int height = 800;

    private static Font font = new Font("Times New Roman", Font.PLAIN, 20);

    private static final String barName = "Max terrain angle";

    private static Font barNameFont = new Font("Times New Roman", Font.PLAIN, 20);

    private static final String demPath = "E:\\Project\\2019\\9test\\data\\henan/henan_1.tif";

    // 雷达扫描半径步长（单位：°）
    private static final int dertDegrees = 1;

    // 取点经度（单位：m）
    private static final double dertDistance = 30;

    private static GridCoverage2D coverage;

    private static ArrayList<Double> maxElevationAngle = new ArrayList();

    static {
        /**
         * 读取tif数据
         */
        try {
            File file = new File(demPath);
            GeoTiffReader tifReader = new GeoTiffReader(file);
            coverage = tifReader.read(null);
            calMaxElevation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        // 设置抗锯齿
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2D.setColor(Color.WHITE);
        graphics2D.fillRect(0,0,width,height);
        graphics2D.setColor(Color.BLACK);

        int barWidth = (int) (width * 0.25d);
        int barUpperH = (int) (height * 0.15d);
        int barDownH = (int) (height * 0.85d);
        double perH = (barDownH - barUpperH) / 10d;

        Font font1 = graphics2D.getFont();

        graphics2D.setFont(font);
        int fontSize = font.getSize();
        graphics2D.drawLine(barWidth,barUpperH,barWidth,barDownH);
        for (int i = 0; i < 11; i++) {
            int barH = (int) (barUpperH + perH * i);
            graphics2D.drawLine(barWidth - 8,barH,barWidth,barH);
            graphics2D.drawString(getElevationAngleValue(i),barWidth - 8 - fontSize * 2,barH + fontSize / 2);
        }

        /* 画同心圆 */
        graphics2D.setFont(font1);

        // 最大圆半径
        int radius = (int) (perH * 5);
        // 圆心坐标
        int circularX = barWidth + radius + fontSize * 3;
        int circularY = (int) (barUpperH + perH * 5);

        for (int i = 0; i < 6; i++) {
            graphics2D.drawArc((int) (circularX - perH * i), (int) (circularY-perH * i), (int) (perH * i * 2), (int) (perH * i * 2), 0, 360);

            // 方位角
            int azimuth = i * 30;
            // 弧度
            double radian = (azimuth / 180d) * Math.PI;
            int sint = (int) (radius * Math.sin(radian));
            int cost = (int) (radius * Math.cos(radian));
            graphics2D.drawLine(circularX + sint,circularY + cost,circularX - sint,circularY - cost);
        }

        graphics2D.setFont(font);
        for (int i = 0; i < 12; i++) {
            // 方位角
            int azimuth = i * 30;
            // 弧度
            double radian = (azimuth / 180d) * Math.PI;
            int sint = (int) (radius * Math.sin(radian));
            int cost = (int) (radius * Math.cos(radian));
            if (i<3){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (circularY - cost - (fontSize / 2.0d * Math.cos(radian))));
            } else if (i==3){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (circularY - cost + (fontSize / 2.0d )));
            } else if (i<6){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (circularY - cost - (fontSize * Math.cos(radian))));
            } else if (i==6){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint - fontSize / 2.0d),(int) (circularY - cost + fontSize));
            } else if (i<9){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (circularY - cost - (fontSize * Math.cos(radian))));
            } else if (i==9){
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (circularY - cost + (fontSize / 2.0d )));
            } else {
                graphics2D.drawString(azimuth + "°",(int) (circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (circularY - cost - (fontSize / 2.0d * Math.cos(radian))));
            }
        }

        // 添加字体旋转方向
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.toRadians(270), 0, 0);
        graphics2D.setFont(barNameFont.deriveFont(affineTransform));
        graphics2D.drawString(barName,barWidth - 8 - fontSize * 3,(int) (barUpperH + perH * 6.4));

        /* 填充假数据 */
        int[] xPoints = new int[361];
        int[] yPoints = new int[361];
        double maxRadius = 0;
        for (int i = 0; i < 360; i++) {
            if (maxElevationAngle.get(i)>0){
                maxRadius = maxElevationAngle.get(i) * perH;
            }else {
                maxRadius = 0;
            }
            // 弧度
            double radian = Math.toRadians(i);

            int sint = (int) (maxRadius * Math.sin(radian));
            int cost = (int) (maxRadius * Math.cos(radian));
            xPoints[i] = circularX + sint;
            yPoints[i] = circularY + cost;
        }
        xPoints[360] = xPoints[0];
        yPoints[360] = yPoints[0];
        Polygon polygon = new Polygon(xPoints,yPoints,xPoints.length);
        graphics2D.setStroke(new BasicStroke(2));
        graphics2D.drawPolygon(polygon);

        graphics2D.dispose();
        try {
            ImageIO.write(bufferedImage, "png", new File("E:\\Project\\2019\\9test\\" + stationNo + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getElevationAngleValue(int i){
        switch (i){
            case 0:
                return "2.5°";
            case 1:
                return "2.0°";
            case 2:
                return "1.5°";
            case 3:
                return "1.0°";
            case 4:
                return "0.5°";
            case 5:
                return "0.0°";
            case 6:
                return "0.5°";
            case 7:
                return "1.0°";
            case 8:
                return "1.5°";
            case 9:
                return "2.0°";
            case 10:
                return "2.5°";
        }
        return "";
    }

    /**
     * 获取经纬度点的高程数据
     * @param lon
     * @param lat
     * @return
     */
    private static double getDem(double lon,double lat){
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
        DirectPosition position = new DirectPosition2D(crs, lon, lat);
        int[] results = (int[]) coverage.evaluate(position);
        results = coverage.evaluate(position, results);
        return results[0];
    }

    /**
     * 获取仰角
     * @param distance
     * @param h
     * @return
     */
    private static double calElevation(double distance,double h){
        return Math.toDegrees(Math.atan2(h,distance));
    }

    /**
     * 已知中心点经纬度，根据距离和方位角求另一点的经纬度
     * @param lon
     * @param lat
     * @param brng
     * @param dist
     */
    private static double[] computerThatLonLat(double lon, double lat, double brng, double dist) {
        double[] lonlat = new double[2];
        double alpha1 = Math.toRadians(brng);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        double tanU1 = (1 - f) * Math.tan(Math.toRadians(lat));
        double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
        double sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double cos2SigmaM=0;
        double sinSigma=0;
        double cosSigma=0;
        double sigma = dist / (b * A), sigmaP = 2 * Math.PI;
        while (Math.abs(sigma - sigmaP) > 1e-12) {
            cos2SigmaM = Math.cos(2 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)
                    - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
            sigmaP = sigma;
            sigma = dist / (b * A) + deltaSigma;
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double lat2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
                (1 - f) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda = Math.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
        double L = lambda - (1 - C) * f * sinAlpha
                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));

        double revAz = Math.atan2(sinAlpha, -tmp); // final bearing

        lonlat[0] = lon+Math.toDegrees(L);
        lonlat[1] = Math.toDegrees(lat2);

        return lonlat;
    }

    /**
     * 计算各个方向上的最大仰角
     */
    private static void calMaxElevation(){
        int stationLength = (int) (r / dertDistance);
        ArrayList<Double> degressList = new ArrayList();

        double[] lonlat;
        double stationH = getDem(stationLon,stationLat);

        for (int i = 0; i < 360; i++) {
            for (int j = 0; j < stationLength; j++) {
                lonlat = computerThatLonLat(stationLon,stationLat,i,(j+1)*dertDistance);
                degressList.add(calElevation((j+1)*dertDistance,getDem(lonlat[0],lonlat[1])-stationH));
            }
            maxElevationAngle.add(Collections.max(degressList));
            degressList.clear();
        }
    }


    @Test
    public void getPoints() throws IOException {
        /*
        "","","112.449388888889","34.5620833333333","277.1"
        */

        // 从中心点开始，累计360度

        System.out.println("test");
        /*String demPath = "";


        double lon = 113.489;
        double lat = 34.96;
        DirectPosition position = new DirectPosition2D(crs, lon, lat);
        int[] results = (int[]) coverage.evaluate(position);
        results = coverage.evaluate(position, results);
        System.out.println(results[0]);*/

        /*for(int i=0;i<points.length;i++) {
            String strLonlat = points[i];
            String[] strLonlats = strLonlat.split(",");

            double lon = Double.parseDouble(strLonlats[0]),
                    lat = Double.parseDouble(strLonlats[1]);

            DirectPosition position = new DirectPosition2D(crs, lon, lat);
            int[] results = (int[]) coverage.evaluate(position);
            results = coverage.evaluate(position, results);
            Map map = new HashMap();
            map.put("lon", lon);
            map.put("lat", lon);
            map.put("dem", results[0]);
            list.add(JSONObject.toJSONString(map));
            response.getWriter().println(JSONArray.toJSONString(list));
        }*/
    }
}
