package www.cnr7.com.bean;

/**
 * @Author zhujiwen
 * @Date 星期三 2019-09-11 14:39
 * @Version 1.0
 * @Description
 **/
public class RadarStation {
    // 站点名称
    private String stationName;

    // 站点编号
    private String stationNo;

    // 站点经度
    private double lon;

    // 站点纬度
    private double lat;

    // 站点离地高度
    private double height;

    // 雷达扫描半径
    private double radius;

    // 雷达仰角
    private double elevation;

    public RadarStation(double lon, double lat, double height, double radius) {
        this.lon = lon;
        this.lat = lat;
        this.height = height;
        this.radius = radius;
    }

    public RadarStation(double lon, double lat, double height, double radius, double elevation) {
        this.lon = lon;
        this.lat = lat;
        this.height = height;
        this.radius = radius;
        this.elevation = elevation;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationNo() {
        return stationNo;
    }

    public void setStationNo(String stationNo) {
        this.stationNo = stationNo;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
}
