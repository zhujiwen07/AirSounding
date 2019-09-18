package www.cnr7.com.bean;

import java.awt.*;

/**
 * @Author zhujw
 * @Date 2019-07-02 14:38
 * @Version 1.0
 * @Description 内多边形
 **/
public class InnerPolygon {
    // 内多边形和最外层矩形的间距
    private int space;

    /* 内多边形第三个点拐点坐标 */
    private float tem3;
    private float pre3;

    // 线的颜色
    private Color color;

    public int getSpace() {
        return space;
    }

    public void setSpace(int space) {
        this.space = space;
    }

    public float getTem3() {
        return tem3;
    }

    public void setTem3(float tem3) {
        this.tem3 = tem3;
    }

    public float getPre3() {
        return pre3;
    }

    public void setPre3(float pre3) {
        this.pre3 = pre3;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
