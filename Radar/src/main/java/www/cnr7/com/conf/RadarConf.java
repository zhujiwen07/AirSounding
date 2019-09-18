package www.cnr7.com.conf;

import java.awt.*;

/**
 * @Author zhujiwen
 * @Date 星期三 2019-09-11 15:03
 * @Version 1.0
 * @Description
 **/
public interface RadarConf {

    public static class Img {
        // 图片宽度
        public static int width = 1000;
        // 图片高度
        public static int height = 800;
        // 每个半径上取值步长（单位：米）
        public static double dertDistance = 30.0d;
        // 刻度所在画板的宽度
        public static double barWidth = 0.25d;
        // 刻度所在画板的上高度
        public static double upperH = 0.15d;
        // 刻度所在画板的下高度
        public static double downH = 0.85d;
        // 数字字体
        public static Font font = new Font("Times New Roman", Font.PLAIN, 20);
        // 标题
        public static String barName = "Max terrain angle";
        // 标题字体
        public static Font barNameFont = new Font("Times New Roman", Font.PLAIN, 20);
        // 刻度值
        public static final String[] elevationValue = {"2.5°","2.0°","1.5°","1.0°","0.5°","0.0°","0.5°","1.0°","1.5°","2.0°","2.5°"};

        // 同心圆圆心坐标
        public static int circularX = 0;
        public static int circularY = 0;
        // 同心圆最小半径
        public static double minRadius = 0d;
    }

    /**
     * 大地坐标系资料WGS-84 长半轴a=6378137 短半轴b=6356752.3142 扁率f=1/298.2572236
     */
    public static class Earth {
        /** 长半轴a=6378137 */
        public static final double a = 6378137;
        /** 短半轴b=6356752.3142 */
        public static final double b = 6356752.3142;
        /** 扁率f=1/298.2572236 */
        public static final double f = 1 / 298.2572236;
    }
}
