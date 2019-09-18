package www.cnr7.com.services.impl;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import www.cnr7.com.bean.RadarStation;
import www.cnr7.com.conf.RadarConf;
import www.cnr7.com.services.RadarService;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @Author zhujiwen
 * @Date 星期三 2019-09-11 14:59
 * @Version 1.0
 * @Description
 **/
public class RadarServiceImpl implements RadarService {
    // tif栅格
    private GridCoverage2D coverage;

    @Override
    public BufferedImage drawRadarMaxElevation(RadarStation radarStation) {
        // 根据雷达站点计算绘图数据
        ArrayList<Double> maxElevation = calMaxElevation(radarStation);
        /*if (radarStation.getRadius() != 0d){
            RadarConf.MaxElevation.radarRadius = radarStation.getRadius();
        }*/
        BufferedImage bufferedImage = new BufferedImage(RadarConf.MaxElevation.width, RadarConf.MaxElevation.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        drawBaseMap(graphics2D);
        drawData(graphics2D,maxElevation);
        graphics2D.dispose();
        return bufferedImage;
    }

    @Override
    public void readDemFile2Grid(File demFile) {
        if(demFile.isFile() && demFile.exists()){
            try {
                GeoTiffReader tifReader = new GeoTiffReader(demFile);
                this.coverage = tifReader.read(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 画底图
     * @param graphics2D
     */
    private void drawBaseMap(Graphics2D graphics2D){
        int fontSize = RadarConf.MaxElevation.font.getSize();
        int barWidth = (int) (RadarConf.MaxElevation.width * RadarConf.MaxElevation.barWidth);
        int barUpperH = (int) (RadarConf.MaxElevation.height * RadarConf.MaxElevation.upperH);
        int barDownH = (int) (RadarConf.MaxElevation.height * RadarConf.MaxElevation.downH);

        /*// 最小同心圆半径
        double minRadius = (barDownH - barUpperH) / (double) (RadarConf.MaxElevation.elevationValue.length-1);
        // 圆心坐标
        int circularX = (int) (barWidth + minRadius * 5 + fontSize * 3);
        int circularY = (int) (barUpperH + minRadius * 5);*/

        RadarConf.MaxElevation.minRadius = (barDownH - barUpperH) / (double) (RadarConf.MaxElevation.elevationValue.length-1);
        RadarConf.MaxElevation.circularX = (int) (barWidth + RadarConf.MaxElevation.minRadius * 5 + fontSize * 3);
        RadarConf.MaxElevation.circularY = (int) (barUpperH + RadarConf.MaxElevation.minRadius * 5);

        // 设置抗锯齿
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充画板
        graphics2D.setColor(Color.WHITE);
        graphics2D.fillRect(0,0, RadarConf.MaxElevation.width, RadarConf.MaxElevation.height);

        graphics2D.setColor(new Color(50, 50, 50));

        // 画同心圆以及直线
        for (int i = 0; i < 6; i++) {
            graphics2D.drawArc((int) (RadarConf.MaxElevation.circularX - RadarConf.MaxElevation.minRadius * i), (int) (RadarConf.MaxElevation.circularY - RadarConf.MaxElevation.minRadius * i), (int) (RadarConf.MaxElevation.minRadius * i * 2), (int) (RadarConf.MaxElevation.minRadius * i * 2), 0, 360);

            // 弧度
            double radian = Math.toRadians(i * 30);
            int sint = (int) (RadarConf.MaxElevation.minRadius * 5 * Math.sin(radian));
            int cost = (int) (RadarConf.MaxElevation.minRadius * 5 * Math.cos(radian));
            graphics2D.drawLine(RadarConf.MaxElevation.circularX + sint, RadarConf.MaxElevation.circularY + cost, RadarConf.MaxElevation.circularX - sint, RadarConf.MaxElevation.circularY - cost);
        }

        graphics2D.setFont(RadarConf.MaxElevation.font);
        graphics2D.setColor(Color.BLACK);

        // 画图例
        graphics2D.drawLine(barWidth,barUpperH,barWidth,barDownH);
        for (int i = 0; i < RadarConf.MaxElevation.elevationValue.length; i++) {
            int barH = (int) (barUpperH + RadarConf.MaxElevation.minRadius * i);
            graphics2D.drawLine(barWidth - 8,barH,barWidth,barH);
            graphics2D.drawString(RadarConf.MaxElevation.elevationValue[i],barWidth - 8 - fontSize * 2,barH + fontSize / 2);
        }

        // 画同心圆刻度
        for (int i = 0; i < 12; i++) {
            // 方位角
            int azimuth = i * 30;
            // 弧度
            double radian = Math.toRadians(azimuth);
            int sint = (int) (RadarConf.MaxElevation.minRadius * 5 * Math.sin(radian));
            int cost = (int) (RadarConf.MaxElevation.minRadius * 5 * Math.cos(radian));
            if (i<3){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (RadarConf.MaxElevation.circularY - cost - (fontSize / 2.0d * Math.cos(radian))));
            } else if (i==3){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (RadarConf.MaxElevation.circularY - cost + (fontSize / 2.0d )));
            } else if (i<6){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian))),(int) (RadarConf.MaxElevation.circularY - cost - (fontSize * Math.cos(radian))));
            } else if (i==6){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint - fontSize / 2.0d),(int) (RadarConf.MaxElevation.circularY - cost + fontSize));
            } else if (i<9){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (RadarConf.MaxElevation.circularY - cost - (fontSize * Math.cos(radian))));
            } else if (i==9){
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (RadarConf.MaxElevation.circularY - cost + (fontSize / 2.0d )));
            } else {
                graphics2D.drawString(azimuth + "°",(int) (RadarConf.MaxElevation.circularX + sint + (fontSize / 2.0d * Math.sin(radian)) - fontSize * 2),(int) (RadarConf.MaxElevation.circularY - cost - (fontSize / 2.0d * Math.cos(radian))));
            }
        }

        // 画标题
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.toRadians(270), 0, 0);
        graphics2D.setFont(RadarConf.MaxElevation.barNameFont.deriveFont(affineTransform));
        graphics2D.drawString(RadarConf.MaxElevation.barName,barWidth - 8 - fontSize * 3,(int) (barUpperH + RadarConf.MaxElevation.minRadius * 6.4));
    }

    /**
     * 根据数据绘图
     * @param graphics2D
     * @param maxElevation
     */
    private void drawData(Graphics2D graphics2D,ArrayList<Double> maxElevation){
        int[] xPoints = new int[361];
        int[] yPoints = new int[361];
        double maxRadius = 0;
        for (int i = 0; i < 360; i++) {
            if (maxElevation.get(i)<0){
                maxRadius = 0;
            } else if (maxElevation.get(i)<2.5){
                maxRadius = maxElevation.get(i) / 0.5d * RadarConf.MaxElevation.minRadius;
            } else {
                maxRadius = 5 * RadarConf.MaxElevation.minRadius;
            }
            // 弧度
            double radian = Math.toRadians(i);
            int sint = (int) (maxRadius * Math.sin(radian));
            int cost = (int) (maxRadius * Math.cos(radian));
            xPoints[i] = RadarConf.MaxElevation.circularX + sint;
            yPoints[i] = RadarConf.MaxElevation.circularY + cost;
        }
        xPoints[360] = xPoints[0];
        yPoints[360] = yPoints[0];
        Polygon polygon = new Polygon(xPoints,yPoints,xPoints.length);
        graphics2D.setStroke(new BasicStroke(3));
        graphics2D.drawPolygon(polygon);
    }

    /**
     * 计算各个方向上的最大仰角
     * @param radarStation
     * @return
     */
    private ArrayList<Double> calMaxElevation(RadarStation radarStation){
        ArrayList<Double> maxElevation = new ArrayList();
        ArrayList<Double> degressList = new ArrayList();
        int stationLength = (int) (radarStation.getRadius() / RadarConf.MaxElevation.dertDistance);
        double[] lonlat;
        double stationH = getDem(radarStation.getLon(),radarStation.getLat()) + radarStation.getHeight();

        for (int i = 0; i < 360; i++) {
            for (int j = 0; j < stationLength; j++) {
                lonlat = computerThatLonLat(radarStation.getLon(),radarStation.getLat(),i,(j+1)* RadarConf.MaxElevation.dertDistance);
                degressList.add(calElevation((j+1)* RadarConf.MaxElevation.dertDistance,getDem(lonlat[0],lonlat[1])-stationH));
            }
            maxElevation.add(Collections.max(degressList));
            degressList.clear();
        }
        return maxElevation;
    }

    /**
     * 获取经纬度点的高程数据
     * @param lon
     * @param lat
     * @return
     */
    private double getDem(double lon,double lat){
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
        DirectPosition position = new DirectPosition2D(crs, lon, lat);
        int[] results = (int[]) coverage.evaluate(position);
        results = coverage.evaluate(position, results);
        return results[0];
    }

    /**
     * 计算仰角
     * @param distance
     * @param height
     * @return
     */
    private static double calElevation(double distance,double height){
        return Math.toDegrees(Math.atan2(height,distance));
    }

    /**
     * 已知中心点经纬度，根据距离和方位角求另一点的经纬度
     * @param lon
     * @param lat
     * @param degress
     * @param distance
     */
    private static double[] computerThatLonLat(double lon, double lat, double degress, double distance) {
        double[] lonlat = new double[2];
        double alpha1 = Math.toRadians(degress);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        double tanU1 = (1 - RadarConf.Earth.f) * Math.tan(Math.toRadians(lat));
        double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
        double sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (RadarConf.Earth.a * RadarConf.Earth.a - RadarConf.Earth.b * RadarConf.Earth.b) / (RadarConf.Earth.b * RadarConf.Earth.b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double cos2SigmaM=0;
        double sinSigma=0;
        double cosSigma=0;
        double sigma = distance / (RadarConf.Earth.b * A), sigmaP = 2 * Math.PI;
        while (Math.abs(sigma - sigmaP) > 1e-12) {
            cos2SigmaM = Math.cos(2 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)
                    - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
            sigmaP = sigma;
            sigma = distance / (RadarConf.Earth.b * A) + deltaSigma;
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double lat2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
                (1 - RadarConf.Earth.f) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda = Math.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double C = RadarConf.Earth.f / 16 * cosSqAlpha * (4 + RadarConf.Earth.f * (4 - 3 * cosSqAlpha));
        double L = lambda - (1 - C) * RadarConf.Earth.f * sinAlpha
                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));

        lonlat[0] = lon+Math.toDegrees(L);
        lonlat[1] = Math.toDegrees(lat2);

        return lonlat;
    }
}
