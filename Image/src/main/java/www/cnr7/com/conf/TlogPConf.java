package www.cnr7.com.conf;

import java.awt.*;

/**
 * @Author zhujiwen
 * @Date 星期一 2019-08-26 9:57
 * @Version 1.0
 * @Description
 **/
public interface TlogPConf {

    /**
     * 底图
     */
    public static class BaseMap {
        // 画笔颜色
        public static final Color COLOR = new Color(255,255,255);
    }

    /**
     * 最外层矩形
     */
    public static class OuterRectangle {
        // 相对于图形宽高的比例系数
        public static final float X = 0.05f;
        public static final float Y = 0.08f;
        public static final float W = 0.8f;
        public static final float H = 0.85f;

        // 画笔颜色
        public static final Color COLOR = new Color(215,85,85);
    }

    /**
     * 内部多边形
     */
    public static class InnerPolygon{
        // 内多边形和最外层矩形的间距
        public static final int S = 2;

        /* 内多边形第三个点拐点坐标 */
        public static final float T3 = 6f;
        public static final float P3 = 240f;

        // 画笔颜色
        public static final Color COLOR = new Color(215,85,85);
    }

    /**
     * 等温线
     */
    public static class Isotherm {
        // 温度刻度值
        public static final int[] TEM = {-80,-70,-60,-50,-40,-30,-20,-10,0,10,20,30,40};

        // 画笔颜色
        public static final Color COLOR = new Color(240,180,180);

        public static final Font FONT = new Font("微软雅黑", Font.BOLD, 14);

        public static final Color FONT_COLOR = new Color(200, 85, 85);
    }

    /**
     * 对数等压线
     */
    public static class Isobar{
        public static final float MAXPRE = 210f;

        // 左边压强刻度值
        public static final int[] LEFTPRE = {40,50,60,80,100,120,140,170,185,200};

        // 右边压强刻度值
        public static final int[] RIGHTPRE = {200,250,300,400,500,600,700,850,925,1000};

        // 画笔颜色
        public static final Color COLOR = new Color(240,180,180);

        public static final Font FONT = new Font("微软雅黑", Font.BOLD, 14);

        public static final Color FONT_COLOR = new Color(200, 85, 85);
    }

    /**
     * 干绝热线
     */
    public static class DryAdiabat{
        // 等位温线温度刻度值，从下到上，从左往右
        public static final float[] TEM = {-57f,-37f,-17f,3f,23f,-75.7f,-62.7f,-50.5f,-38.2f,-25.2f,-12.2f,0.3f,15.5f,28.5f};

        // 等位温线压强刻度值，从左往右，从下到上
        public static final float[] PRE = {141.5f,103f,77.5f,59.5f,46.5f,1000f,810f,660f,545f,455f,380f,215f,321f,275f};

        // 画笔颜色
        public static final Color COLOR = new Color(240,180,180);
    }

    /**
     * 湿绝热线
     */
    public static class WetAdiabat{
        /* 湿绝热线起点坐标，控制点坐标，终点坐标 */
        public static final float[] START_TEM = {-80,-80,-80,-75.6f,-62.6f,-50.8f,-39,-29,-20.5f,-14.8f,-9.8f};
        public static final float[] START_PRE = {77.5f,59.8f,46.5f,40,40,40,40,40,40,40,40};

        public static final float[] CTRL_TEM = {-20,-20,-10,-10,-12,-20,-20,-18,-12,-8,-5};
        public static final float[] CTRL_PRE = {200,146,130,100,80,60,50,48,48,48,45};

        public static final float[] END_TEM = {-18.5f,-3.5f,7.3f,15,21.5f,25.8f,30,32.5f,35.5f,38,40};

        // 画笔颜色
        public static final Color COLOR = new Color(79,145, 108);

        // 画笔形状
        public static final Stroke STROKE = new BasicStroke(1f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,0, new float[]{6,2},0);
    }

    /**
     * 等饱和比湿线
     */
    public static class Ssh{
        // 图形刻度
        public static final float UPPER_PRE = 80f;
        public static final float[] UPPER_TEM = {-51f,-45.5f,-40f,-31.5f,-25f,-20.8f,-18f,-14f,-10f,-5.5f,-2f,3f,7f,10.5f,13.5f,16.5f};
        public static final float[] DOWN_TEM = {-43f,-37.5f,-30.8f,-22f,-15f,-10.03f,-7.5f,-2f,1f,7f,11.2f,17.5f,22f,25.5f,29f,33f};

        // 标注刻度
        public static final float PRE = 152f;
        public static final String[] VALUE = {"0.05","0.1","0.2","0.5","1","1.5","2","3","4","6","8","12","16","20","24","30"};

        // 画笔颜色
        public static final Color COLOR = new Color(107, 163,195);

        // 画笔形状
        public static final Stroke STROKE = new BasicStroke(1f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,0, new float[]{2,2},0);

        // 画笔颜色
        public static final Color COLOR10 = new Color(76, 147,195);

        // 画笔形状
        public static final Stroke STROKE10 = new BasicStroke(1.8f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,0, new float[]{2,2},0);

        public static final Font FONT = new Font("微软雅黑", Font.PLAIN, 12);

        public static final Color FONT_COLOR = new Color(107, 163,195);

        public static final Font FONT10 = new Font("微软雅黑", Font.BOLD, 12);

        public static final Color FONT_COLOR10 = new Color(76, 147,195);
    }
}
