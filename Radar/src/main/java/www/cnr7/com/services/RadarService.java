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
    public BufferedImage drawRadarMaxElevation(RadarStation radarStation, boolean drawDetails);

    /**
     * 画雷达等效半径图
     * @param radarStation
     * @return
     */
    public BufferedImage drawEquivalentRadius(RadarStation radarStation, boolean drawDetails);

    /**
     * 读取高程数据
     * @param demFile
     */
    public void readDemFile2Grid(File demFile);
}
