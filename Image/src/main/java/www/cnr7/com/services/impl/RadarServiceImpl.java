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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public BufferedImage drawRadarMaxElevation(RadarStation radarStation, boolean drawDetails) {
        // 根据雷达站点计算绘图数据
        ArrayList<Double> maxElevation = calMaxElevation(radarStation);

        BufferedImage bufferedImage = new BufferedImage(RadarConf.MaxElevation.width, RadarConf.MaxElevation.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        // 设置抗锯齿
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        drawBaseMap(graphics2D);
        drawData(graphics2D,maxElevation);
        if (drawDetails){
            drawMaxElevationDetails(graphics2D,radarStation);
        }
        graphics2D.dispose();
        return bufferedImage;
    }

    @Override
    public BufferedImage drawEquivalentRadius(RadarStation radarStation, boolean drawDetails) {
        // 根据雷达站点数据计算等效半径
        double[] radius = calEquivalentRadius(radarStation, calCoverageArea(radarStation));

        BufferedImage bufferedImage = new BufferedImage(RadarConf.EquivalentRadius.width, RadarConf.EquivalentRadius.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        // 设置抗锯齿
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        drawCircles(graphics2D,radarStation,radius);
        drawEquivalentRadiusTitle(graphics2D);

        if (drawDetails){
            drawEquivalentRadiusDetails(graphics2D,radarStation);
            drawToolBarWithDetails(graphics2D);
        }else {
            drawToolBar(graphics2D);
        }
        graphics2D.dispose();
        return bufferedImage;
    }

    @Override
    public List<RadarStation> readCsvFile2List(File file, String charset) {
        List<RadarStation> radarStations = new ArrayList<>();
        if(charset == null){
            charset = "UTF-8";
        }
        if(file.isFile() && file.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),charset));
                String line;
                while (( line = reader.readLine()) != null) {
                    if (line.contains(",\"LON\",\"LAT\","))continue;
                    String[] strings = line.replaceAll("\"","").split(",");
                    /*RadarStation radarStation = new RadarStation();
                    radarStation.setStationName(strings[0]);
                    radarStation.setStationNo(strings[1]);
                    radarStation.setLon(Double.parseDouble(strings[2]));
                    radarStation.setLat(Double.parseDouble(strings[3]));*/
                    radarStations.add(new RadarStation(Double.parseDouble(strings[2]),Double.parseDouble(strings[3]),0d,75000d));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return radarStations;
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
     * 画同心圆
     * @param graphics2D
     * @param radius
     */
    private void drawCircles(Graphics2D graphics2D, RadarStation radarStation, double[] radius){
        // 填充画板
        graphics2D.setColor(Color.WHITE);
        graphics2D.fillRect(0,0, RadarConf.EquivalentRadius.width, RadarConf.EquivalentRadius.height);
        RadarConf.EquivalentRadius.circularX = RadarConf.EquivalentRadius.width / 2;
        RadarConf.EquivalentRadius.circularY = RadarConf.EquivalentRadius.height / 2;
        RadarConf.EquivalentRadius.maxRadius = RadarConf.EquivalentRadius.height / 2.0d - RadarConf.EquivalentRadius.height * 0.1d;
        /*for (int i = 0; i < radius.length; i++) {
            System.out.println(radius[i] / 1000);
        }*/
        for (int i = radius.length - 1; i >= 0; i--) {
            graphics2D.setColor(RadarConf.EquivalentRadius.toolBar[i]);
            int r = (int) Math.round(radius[i] / radarStation.getRadius() * RadarConf.EquivalentRadius.maxRadius);
            graphics2D.fillOval(RadarConf.EquivalentRadius.circularX - r,RadarConf.EquivalentRadius.circularY - r,2*r,2*r);
        }
        /*for (int i = radius.length - 1; i >= 0; i--) {
            graphics2D.setColor(RadarConf.EquivalentRadius.toolBar[i]);
            int r = (int) Math.round(radius[i] / radarStation.getRadius() * RadarConf.EquivalentRadius.maxRadius);
            graphics2D.fillOval(RadarConf.EquivalentRadius.circularX,RadarConf.EquivalentRadius.circularY,r,r);
        }*/
    }

    /**
     * 画色标盘
     * @param graphics2D
     */
    private void drawToolBar(Graphics2D graphics2D){
        int perW = (int) (RadarConf.EquivalentRadius.width * 0.8d / 9d);
        double h = RadarConf.EquivalentRadius.height * 0.02d;
        double leftUpperX =  RadarConf.EquivalentRadius.width * 0.1d;
        double leftUpperY =  RadarConf.EquivalentRadius.height * 0.92d;
        int fontH = (int)(RadarConf.EquivalentRadius.height * 0.94d + 2 + RadarConf.EquivalentRadius.font.getSize());
        graphics2D.setFont(RadarConf.EquivalentRadius.font);
        for (int i = 0; i < RadarConf.EquivalentRadius.toolBar.length; i++) {
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawRect((int)(leftUpperX + perW * i),(int)(leftUpperY),(int)(perW),(int)(h));
            if (i == 8){
                graphics2D.drawString("≥9km",(int)(leftUpperX + perW * i + perW * 0.4),fontH);
            }else {
                graphics2D.drawString((i+1)+"",(int)(leftUpperX + perW * i + perW * 0.4),fontH);
            }
            graphics2D.setColor(RadarConf.EquivalentRadius.toolBar[i]);
            graphics2D.fillRect((int)(leftUpperX + perW * i) + 1,(int)(leftUpperY) + 1,(int)(perW-1),(int)(h-1));
        }
    }

    /**
     * 画色标盘
     * @param graphics2D
     */
    private void drawToolBarWithDetails(Graphics2D graphics2D){
        int perW = (int) (RadarConf.EquivalentRadius.width * 0.6d / 9d);
        double h = RadarConf.EquivalentRadius.height * 0.02d;
        double leftUpperX =  RadarConf.EquivalentRadius.width * 0.35d;
        double leftUpperY =  RadarConf.EquivalentRadius.height * 0.92d;
        int fontH = (int)(RadarConf.EquivalentRadius.height * 0.94d + 2 + RadarConf.EquivalentRadius.font.getSize());
        graphics2D.setFont(RadarConf.EquivalentRadius.font);
        for (int i = 0; i < RadarConf.EquivalentRadius.toolBar.length; i++) {
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawRect((int)(leftUpperX + perW * i),(int)(leftUpperY),(int)(perW),(int)(h));
            if (i == 8){
                graphics2D.drawString("≥9 km",(int)(leftUpperX + perW * i + perW * 0.4),fontH);
            }else {
                graphics2D.drawString((i+1)+"",(int)(leftUpperX + perW * i + perW * 0.4),fontH);
            }
            graphics2D.setColor(RadarConf.EquivalentRadius.toolBar[i]);
            graphics2D.fillRect((int)(leftUpperX + perW * i) + 1,(int)(leftUpperY) + 1,(int)(perW-1),(int)(h-1));
        }
    }

    /**
     * 画标题
     * @param graphics2D
     */
    private void drawEquivalentRadiusTitle(Graphics2D graphics2D){
        int fontSize = RadarConf.EquivalentRadius.titleFont.getSize();
        graphics2D.setColor(Color.BLACK);
        graphics2D.setFont(RadarConf.EquivalentRadius.titleFont);
        int x = (int) ((RadarConf.EquivalentRadius.width - RadarConf.EquivalentRadius.title.length() * fontSize) / 2.0d);
        int y = (int) (RadarConf.EquivalentRadius.height * 0.1d);
        graphics2D.drawString(RadarConf.EquivalentRadius.title,x,y - fontSize);
    };

    /**
     * 画最大仰角的详情
     * @param graphics2D
     * @param radarStation
     */
    private void drawMaxElevationDetails(Graphics2D graphics2D, RadarStation radarStation){
        String details = radarStation.toMaxElevationString();
        int fontSize = RadarConf.MaxElevation.detailsFont.getSize();
        graphics2D.setColor(Color.BLACK);
        graphics2D.setFont(RadarConf.MaxElevation.detailsFont);
        int x = (int) ((RadarConf.MaxElevation.width - 26 * fontSize) / 2.0d);
        int y = (int) (RadarConf.MaxElevation.height * 0.1d);
        graphics2D.drawString(details,x,y - fontSize);
    }

    /**
     * 画离地高度分布模拟图详情
     * @param graphics2D
     * @param radarStation
     */
    private void drawEquivalentRadiusDetails(Graphics2D graphics2D, RadarStation radarStation){
        int fontSize = RadarConf.EquivalentRadius.detailsFont.getSize();
        graphics2D.setColor(Color.BLACK);
        graphics2D.setFont(RadarConf.EquivalentRadius.detailsFont);
        graphics2D.drawString("雷达仰角：" + radarStation.saveOneBit(radarStation.getElevation(),1) + "°",5,RadarConf.EquivalentRadius.height - (fontSize + 2) * 4);
        graphics2D.drawString("雷达高度：" + radarStation.getHeight() + "m",5,RadarConf.EquivalentRadius.height - (fontSize + 2) * 3);
        graphics2D.drawString("扫描半径：" + radarStation.getRadius()/1000d + "km" ,5,RadarConf.EquivalentRadius.height - (fontSize + 2) * 2);
        graphics2D.drawString("坐标（" + radarStation.saveOneBit(radarStation.getLon(),2) + "," + radarStation.saveOneBit(radarStation.getLat(),2) + "）",5,RadarConf.EquivalentRadius.height - (fontSize + 2) * 1);
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
     * 计算覆盖面积（公式：Math.pow(j+1,2) - Math.pow(j,2)）
     * @param radarStation
     * @return
     */
    private int[][] calCoverageArea(RadarStation radarStation){
        int stationLength = (int) (radarStation.getRadius() / RadarConf.MaxElevation.dertDistance);
        double[] lonlat;
        double stationH = getDem(radarStation.getLon(),radarStation.getLat()) + radarStation.getHeight();
        double tanValue = Math.tan(Math.toRadians(radarStation.getElevation()));
        // 离地高度
        double height = 0;
        int[][] area = new int[360][9];
        for (int i = 0; i < 360; i++) {
            for (int j = 0; j < stationLength; j++) {
                lonlat = computerThatLonLat(radarStation.getLon(),radarStation.getLat(),i,(j+1)* RadarConf.MaxElevation.dertDistance);
                height = (j+1) * RadarConf.MaxElevation.dertDistance * tanValue + stationH - getDem(lonlat[0],lonlat[1]);
                if (height < 0){
                    break;
                } else if (height < 1000){
                    area[i][0] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 2000){
                    area[i][1] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 3000){
                    area[i][2] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 4000){
                    area[i][3] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 5000){
                    area[i][4] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 6000){
                    area[i][5] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 7000){
                    area[i][6] += (Math.pow(j+1,2) - Math.pow(j,2));
                } else if (height < 8000){
                    area[i][7] += (Math.pow(j+1,2) - Math.pow(j,2));
                }  else {
                    area[i][8] += (Math.pow(j+1,2) - Math.pow(j,2));
                }
            }
            for (int j = 1; j < 9; j++) {
                area[i][j] += area[i][j-1];
            }
        }
        return area;
    }

    /**
     * 根据高度- 面积指数计算等效半径（单位：米）
     * @param radarStation
     * @param areas
     */
    private double[] calEquivalentRadius(RadarStation radarStation, int[][] areas){
//        double tanValue = Math.tan(Math.toRadians(radarStation.getElevation()));
//        ArrayList radius = new ArrayList();
        double[] cr = new double[9];
        // 矩阵转置求和
        for (int i = 0; i < 9; i++) {
            double totalArea = 0;
            // 累加求和
            for (int j = 0; j < 360; j++) {
                totalArea += areas[j][i];
            }
            /*double radius = (i+1) / tanValue;
            double rate = (squrR * RadarConf.MaxElevation.dertDistance * RadarConf.MaxElevation.dertDistance) / (360 * radius *1000 * radius * 1000);
            double Rh = Math.sqrt(radius * radius * rate);*/
            // 等效半径（单位：KM）
            cr[i] = Math.sqrt(totalArea / 360d) * RadarConf.MaxElevation.dertDistance;
//            radius.add(Math.sqrt(totalArea / 360d) * RadarConf.MaxElevation.dertDistance / 1000d);
        }
        return cr;
    }

    /**
     * 获取经纬度点的高程数据
     * @param lon
     * @param lat
     * @return
     */
    private double getDem(double lon,double lat){
        double h = 0d;
        try {
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
            DirectPosition position = new DirectPosition2D(crs, lon, lat);
            int[] results = (int[]) coverage.evaluate(position);
            results = coverage.evaluate(position, results);
            return results[0];
        } catch (Exception e){
            return h;
        }
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

//        double revAz = Math.atan2(sinAlpha, -tmp); // final degress

        lonlat[0] = lon+Math.toDegrees(L);
        lonlat[1] = Math.toDegrees(lat2);

        return lonlat;
    }
}
