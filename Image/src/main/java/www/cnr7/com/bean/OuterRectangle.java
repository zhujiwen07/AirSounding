package www.cnr7.com.bean;

import java.awt.*;

/**
 * @Author zhujw
 * @Date 2019-07-02 14:23
 * @Version 1.0
 * @Description 最外层矩形
 **/
public class OuterRectangle {
    // 矩形左上角点到左边界的距离
    private int x;

    // 矩形左上角点到上边界的距离
    private int y;

    // 矩形的宽
    private int width;

    // 矩形的高
    private int height;

    // 线的颜色
    private Color color;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
