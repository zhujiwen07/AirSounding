package www.cnr7.com.services;

import www.cnr7.com.bean.RadarStation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * @Author zhujiwen
 * @Date 星期三 2019-09-11 14:56
 * @Version 1.0
 * @Description
 **/
public interface RadarService {
    /**
     * 画雷达站点的最大仰角图
     * @param radarStation
     * @return
     */
    public BufferedImage drawRadarMaxElevation(RadarStation radarStation);

    /**
     * 读取站点csv数据
     * @param file
     * @param charset
     * @return
     */
    public List<RadarStation> readCsvFile2List(File file, String charset);

    /**
     * 读取高程数据
     * @param demFile
     */
    public void readDemFile2Grid(File demFile);
}
