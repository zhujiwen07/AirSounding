package www.cnr7.com.bean;

import java.awt.*;

/**
 * @Author zhujiwen
 * @Date 星期六 2019-08-24 22:08
 * @Version 1.0
 * @Description 等压线
 */
public class Isobar {
    // 线的颜色
    private Color color;

    // 等温线字体
    private Font font;

    // 字体颜色
    private Color fontColor;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }
}
