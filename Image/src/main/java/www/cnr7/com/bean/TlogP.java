package www.cnr7.com.bean;

import www.cnr7.com.conf.TlogPConf;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;

/**
 * @Author zhujiwen
 * @Date 星期六 2019-08-24 22:05
 * @Version 1.0
 * @Description
 */
public class TlogP {
    // 图片宽
    private int width;

    // 图片高
    private int height;

    // 最外层矩形
    private OuterRectangle outerRectangle;

    // 内多边形
    private InnerPolygon innerPolygon;

    // 等温线
    private Isotherm isotherm;

    // 等压线
    private Isobar isobar;

    // 干绝热线
    private DryAdiabat dryAdiabat;

    // 湿绝热线
    private WetAdiabat wetAdiabat;

    // 等饱和比湿线
    private Ssh ssh;

    // BufferedImage对象
    private BufferedImage bufferedImage;

    // 画笔
//    private Graphics graphics;
    private Graphics2D graphics2D;

    // 内多边形的6个角点坐标，从左上角开始，顺时针方向
    private int x1;
    private int y1;
    private int x2;
    private int y2;
    private int x3;
    private int y3;
    private int x4;
    private int y4;
    private int x5;
    private int y5;
    private int x6;
    private int y6;

    // 单位等温线宽度
    private float isothermWidth;

    // 总压强对数值差值
    private float totleLogP;

    // 总压强高度
    private int preHeight;

    public TlogP(int width, int height) {
        this.width = width;
        this.height = height;
        this.bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        this.graphics = bufferedImage.getGraphics();
        this.graphics2D = (Graphics2D) bufferedImage.getGraphics();
        // 设置抗锯齿
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        init();
    }

    /**
     * 类对象初始化
     */
    private void init(){
        /* 初始化最外层矩形 */
        this.outerRectangle = new OuterRectangle();
        outerRectangle.setX( Math.round( width * TlogPConf.OuterRectangle.X ) );
        outerRectangle.setY( Math.round( height * TlogPConf.OuterRectangle.Y ) );
        outerRectangle.setWidth( Math.round( width * TlogPConf.OuterRectangle.W ) );
        outerRectangle.setHeight( Math.round( height * TlogPConf.OuterRectangle.H ) );
        outerRectangle.setColor( TlogPConf.OuterRectangle.COLOR );

        /* 初始化内多边形 */
        this.innerPolygon = new InnerPolygon();
        innerPolygon.setSpace( TlogPConf.InnerPolygon.S );
        innerPolygon.setTem3( TlogPConf.InnerPolygon.T3 );
        innerPolygon.setPre3( TlogPConf.InnerPolygon.P3 );
        innerPolygon.setColor( TlogPConf.InnerPolygon.COLOR );

        /* 初始化等温线 */
        this.isotherm = new Isotherm();
        isotherm.setColor( TlogPConf.Isotherm.COLOR );
        isotherm.setFont( TlogPConf.Isotherm.FONT );
        isotherm.setFontColor( TlogPConf.Isotherm.FONT_COLOR );

        /* 初始化等压线 */
        this.isobar = new Isobar();
        isobar.setColor( TlogPConf.Isobar.COLOR );
        isobar.setFont( TlogPConf.Isobar.FONT );
        isobar.setFontColor( TlogPConf.Isobar.FONT_COLOR );

        /* 初始化干绝热线 */
        this.dryAdiabat = new DryAdiabat();
        dryAdiabat.setColor( TlogPConf.DryAdiabat.COLOR );

        /* 初始化湿绝热线 */
        this.wetAdiabat = new WetAdiabat();
        wetAdiabat.setColor( TlogPConf.WetAdiabat.COLOR );
        wetAdiabat.setStroke( TlogPConf.WetAdiabat.STROKE );

        /* 初始化等饱和比湿线 */
        this.ssh = new Ssh();
        ssh.setColor( TlogPConf.Ssh.COLOR );
        ssh.setStroke( TlogPConf.Ssh.STROKE );
        ssh.setFont( TlogPConf.Ssh.FONT );
        ssh.setFontColor( TlogPConf.Ssh.FONT_COLOR );
        ssh.setColor10( TlogPConf.Ssh.COLOR10 );
        ssh.setStroke10( TlogPConf.Ssh.STROKE10 );
        ssh.setFont10( TlogPConf.Ssh.FONT10 );
        ssh.setFontColor10( TlogPConf.Ssh.FONT_COLOR10 );
    }

    /**
     * 画图相关参数计算
     */
    private void calParams(){
        isothermWidth = ( outerRectangle.getWidth() - 2 * innerPolygon.getSpace() ) / (float) ( TlogPConf.Isotherm.TEM[TlogPConf.Isotherm.TEM.length - 1] - TlogPConf.Isotherm.TEM[0] );
        totleLogP = (float) Math.log( TlogPConf.Isobar.MAXPRE / TlogPConf.Isobar.LEFTPRE[0] );
        preHeight = outerRectangle.getHeight() - 2 * innerPolygon.getSpace();

        x1 = outerRectangle.getX() + innerPolygon.getSpace();
        y1 = outerRectangle.getY() + innerPolygon.getSpace();
        x2 = getWidthPix(TlogPConf.InnerPolygon.T3);
        y2 = y1;
        x3 = x2;
        y3 = getHeightPix(TlogPConf.InnerPolygon.P3);
        x4 = outerRectangle.getX() + outerRectangle.getWidth() - innerPolygon.getSpace();
        y4 = y3;
        x5 = x4;
        y5 = outerRectangle.getY() + outerRectangle.getHeight() - innerPolygon.getSpace();
        x6 = x1;
        y6 = y5;
    }

    /**
     * 画TlogP底图
     */
    public void drawTlogPBaseMap(){
        this.calParams();
        this.fillRect();
        this.drawOuterRectangle();
        this.drawIsotherm();
        this.drawIsobar();
        this.drawDryAdiabat();
        this.drawWetAdiabat();
        this.drawSsh();
        this.drawInnerPolygon();
    }

    /**
     * 填充画板底色
     */
    private void fillRect(){
        graphics2D.setColor( TlogPConf.BaseMap.COLOR );
        graphics2D.fillRect(0, 0, width, height);
    }

    /**
     * 最外层矩形
     */
    private void drawOuterRectangle(){
        graphics2D.setColor(this.outerRectangle.getColor());
        graphics2D.drawRect(this.outerRectangle.getX(), this.outerRectangle.getY(), this.outerRectangle.getWidth(), this.outerRectangle.getHeight());
    }

    /**
     * 内部多边形
     */
    private void drawInnerPolygon(){
        int[] xPoints = {x1,x2,x3,x4,x5,x6,x1};
        int[] yPoints = {y1,y2,y3,y4,y5,y6,y1};
        Polygon polygon = new Polygon(xPoints,yPoints,xPoints.length);
        graphics2D.setColor(this.innerPolygon.getColor());
        graphics2D.drawPolygon(polygon);
    }

    /**
     * 等温线
     */
    private void drawIsotherm(){
        graphics2D.setColor(this.isotherm.getColor());
        for (int i = 1; i < TlogPConf.Isotherm.TEM.length - 1; i++) {
            int widthPix = getWidthPix(TlogPConf.Isotherm.TEM[i]);
            if (TlogPConf.Isotherm.TEM[i]>= TlogPConf.InnerPolygon.T3){
                graphics2D.drawLine(widthPix,y3,widthPix,y5);
            } else {
                graphics2D.drawLine(widthPix,y1,widthPix,y5);
            }
        }

        /* 等温线刻度值 */
        Font font = graphics2D.getFont();
        graphics2D.setFont(this.isotherm.getFont());
        graphics2D.setColor(this.isotherm.getFontColor());
        int fontSize = this.isotherm.getFont().getSize();
        int upperHeight = this.outerRectangle.getY() - 1;
        int downHeight = this.outerRectangle.getY() + this.outerRectangle.getHeight() + fontSize;
        for (int i = 0; i < TlogPConf.Isotherm.TEM.length; i++) {
            if (i<8){
                graphics2D.drawString(TlogPConf.Isotherm.TEM[i]+ "", (getWidthPix(TlogPConf.Isotherm.TEM[i]) - fontSize), upperHeight);
                graphics2D.drawString(TlogPConf.Isotherm.TEM[i]+ "", (getWidthPix(TlogPConf.Isotherm.TEM[i]) - fontSize),downHeight);
            }else {
                graphics2D.drawString(TlogPConf.Isotherm.TEM[i]+ "", (int)(getWidthPix(TlogPConf.Isotherm.TEM[i]) - fontSize / 2.0f),upperHeight);
                graphics2D.drawString(TlogPConf.Isotherm.TEM[i]+ "", (int)(getWidthPix(TlogPConf.Isotherm.TEM[i]) - fontSize / 2.0f),downHeight);
            }
        }
        graphics2D.drawString("℃",(int)(getWidthPix(TlogPConf.Isotherm.TEM[TlogPConf.Isotherm.TEM.length - 1]) + fontSize * 1.5f),downHeight);
        graphics2D.setFont(font);
    }

    /**
     * 对数压强线
     */
    private void drawIsobar(){
        graphics2D.setColor(this.isobar.getColor());
        for (int i = 1; i < TlogPConf.Isobar.LEFTPRE.length; i++) {
            int heightPix = getHeightPix(TlogPConf.Isobar.LEFTPRE[i]);
            graphics2D.drawLine(x6,heightPix,x5,heightPix);
        }

        /* 压强线刻度值 */
        Font font = graphics2D.getFont();
        graphics2D.setFont(this.isobar.getFont());
        graphics2D.setColor(this.isobar.getFontColor());
        int fontSize = this.isobar.getFont().getSize();
        int leftWidth = (int) (this.outerRectangle.getX() - fontSize * 1.3f);
        int leftWidth1 = (int) (this.outerRectangle.getX() - fontSize * 2.0f);
        int rightWidth = this.outerRectangle.getX() + this.outerRectangle.getWidth() + 1;

        for (int i = 1; i < TlogPConf.Isobar.LEFTPRE.length; i++) {
            if(i<4){
                graphics2D.drawString(TlogPConf.Isobar.LEFTPRE[i] + "", leftWidth, (int) (getHeightPix(TlogPConf.Isobar.LEFTPRE[i]) + fontSize / 2.0f));
            }else {
                graphics2D.drawString(TlogPConf.Isobar.LEFTPRE[i] + "", leftWidth1, (int) (getHeightPix(TlogPConf.Isobar.LEFTPRE[i]) + fontSize / 2.0f));
            }
            graphics2D.drawString(TlogPConf.Isobar.RIGHTPRE[i] + "", rightWidth, (int) (getHeightPix(TlogPConf.Isobar.RIGHTPRE[i]) + fontSize / 2.0f));
        }
        graphics2D.drawString(TlogPConf.Isobar.LEFTPRE[0] + "", leftWidth, (int) (this.outerRectangle.getY() + fontSize / 1.5f));
        graphics2D.drawString(TlogPConf.Isobar.RIGHTPRE[0] + "", rightWidth, (int) (this.outerRectangle.getY() + fontSize / 1.5f));
        graphics2D.drawString("hPa",this.outerRectangle.getX()-fontSize*2,this.outerRectangle.getY() - fontSize);
        graphics2D.setFont(font);
    }

    /**
     * 干绝热线
     */
    private void drawDryAdiabat(){
        graphics2D.setColor(this.dryAdiabat.getColor());
        for (int i = 0; i < 14; i++) {
            if (i<5){
                graphics2D.drawLine(x1,getHeightPix(TlogPConf.DryAdiabat.PRE[i]),getWidthPix(TlogPConf.DryAdiabat.TEM[i]),y6);
            } else if (i<11){
                graphics2D.drawLine(getWidthPix(TlogPConf.DryAdiabat.TEM[i]),y1,x5,getHeightPix(TlogPConf.DryAdiabat.PRE[i]));
            } else if (i==11){
                graphics2D.drawLine(getWidthPix(TlogPConf.DryAdiabat.TEM[i]),y1,x2,getHeightPix(TlogPConf.DryAdiabat.PRE[i]));
            } else {
                graphics2D.drawLine(getWidthPix(TlogPConf.DryAdiabat.TEM[i]),y3,x5,getHeightPix(TlogPConf.DryAdiabat.PRE[i]));
            }
        }
    }

    /**
     * 湿绝热线
     */
    private void drawWetAdiabat(){
        Stroke stroke = graphics2D.getStroke();
        graphics2D.setColor(this.wetAdiabat.getColor());
        graphics2D.setStroke(this.wetAdiabat.getStroke());

        graphics2D.drawLine(x6,getHeightPix(200),getWidthPix(-77.5f),y6);
        graphics2D.drawLine(x6,getHeightPix(141.5f),getWidthPix(-57f),y6);
        graphics2D.drawLine(x6,getHeightPix(103f),getWidthPix(-37.2f),y6);

        QuadCurve2D curve = null;

        for (int i = 0; i < TlogPConf.WetAdiabat.START_TEM.length; i++) {
            curve = new QuadCurve2D.Double(getWidthPix(TlogPConf.WetAdiabat.START_TEM[i]),getHeightPix(TlogPConf.WetAdiabat.START_PRE[i]),
                    getWidthPix(TlogPConf.WetAdiabat.CTRL_TEM[i]),getHeightPix(TlogPConf.WetAdiabat.CTRL_PRE[i]),
                    getWidthPix(TlogPConf.WetAdiabat.END_TEM[i]),y6);
            graphics2D.draw(curve);
        }

        graphics2D.setStroke(stroke);
    }

    /**
     * 等饱和比湿线
     */
    private void drawSsh(){
        Stroke stroke = graphics2D.getStroke();
        graphics2D.setColor(this.ssh.getColor());
        graphics2D.setStroke(this.ssh.getStroke());

        int upperPre = getHeightPix(TlogPConf.Ssh.UPPER_PRE);

        for (int i = 0; i < TlogPConf.Ssh.UPPER_TEM.length; i++) {
            if (i==10)continue;
            graphics2D.drawLine(getWidthPix(TlogPConf.Ssh.UPPER_TEM[i]),upperPre,getWidthPix(TlogPConf.Ssh.DOWN_TEM[i]),y6);
        }
        graphics2D.setColor(this.ssh.getColor10());
        graphics2D.setStroke(this.ssh.getStroke10());
        graphics2D.drawLine(getWidthPix(TlogPConf.Ssh.UPPER_TEM[10]),upperPre,getWidthPix(TlogPConf.Ssh.DOWN_TEM[10]),y6);

        /* 等饱和比湿线刻度 */
        Font font = graphics2D.getFont();
        // 添加字体旋转方向
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.toRadians(350), 0, 0);
        graphics2D.setFont(this.ssh.getFont().deriveFont(affineTransform));
        graphics2D.setColor(this.ssh.getFontColor());

        int fontSize = this.ssh.getFont().getSize();
        int fontHeight = getHeightPix(TlogPConf.Ssh.PRE);
//        graphics2D.setComposite(AlphaComposite.getInstance(1));
        /*graphics2D.setClip(getWidthPix(TlogPConf.Ssh.UPPER_TEM[0]),
                            (int)(fontHeight-fontSize*1.2),
                        (getWidthPix(TlogPConf.Ssh.DOWN_TEM[TlogPConf.Ssh.DOWN_TEM.length-1]) - getWidthPix(TlogPConf.Ssh.UPPER_TEM[0])),
                            (int)(fontSize*1.4));*/
        /*graphics2D.clipRect(getWidthPix(TlogPConf.Ssh.UPPER_TEM[0]),
                (int)(fontHeight-fontSize*1.2),
                (getWidthPix(TlogPConf.Ssh.DOWN_TEM[TlogPConf.Ssh.DOWN_TEM.length-1]) - getWidthPix(TlogPConf.Ssh.UPPER_TEM[0])),
                (int)(fontSize*1.4));*/

        /*graphics2D.clipRect(100,100,200,200);*/
        /*Shape shape = graphics2D.getClip();
        graphics2D.clip(shape);*/

        for (int i = 0; i < TlogPConf.Ssh.VALUE.length; i++) {
            if (i==10){
                continue;
            }else if (i<6 && i!=4){
                graphics2D.drawString(TlogPConf.Ssh.VALUE[i]+"",(int) ((getWidthPix(TlogPConf.Ssh.UPPER_TEM[i]) + getWidthPix(TlogPConf.Ssh.DOWN_TEM[i])) / 2.0f),fontHeight);
            }else {
                graphics2D.drawString(TlogPConf.Ssh.VALUE[i]+"",(int) ((getWidthPix(TlogPConf.Ssh.UPPER_TEM[i]) + getWidthPix(TlogPConf.Ssh.DOWN_TEM[i])  + fontSize) / 2.0f),fontHeight);
            }
        }

        graphics2D.setFont(font);
        graphics2D.setStroke(stroke);
    }

    public void dispose(){
        graphics2D.dispose();
    }

    /**
     * 根据温度获取宽度像素点
     * @param tem
     * @return
     */
    private int getWidthPix(float tem){
        return Math.round( ( tem - TlogPConf.Isotherm.TEM[0] ) * isothermWidth + x1 );
    }

    /**
     * 根据压强获取高度像素点
     * @param pre
     * @return
     */
    private int getHeightPix(float pre){
        return (int) Math.round( ( Math.log( pre / getMinPre( pre ) ) / totleLogP ) * preHeight + y1);
    }

    /**
     * 获取最小压强刻度
     * @param pre
     * @return
     */
    private int getMinPre(float pre){
        if (pre > 200){
            return TlogPConf.Isobar.RIGHTPRE[0];
        }
        return TlogPConf.Isobar.LEFTPRE[0];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public OuterRectangle getOuterRectangle() {
        return outerRectangle;
    }

    public void setOuterRectangle(OuterRectangle outerRectangle) {
        this.outerRectangle = outerRectangle;
    }

    public InnerPolygon getInnerPolygon() {
        return innerPolygon;
    }

    public void setInnerPolygon(InnerPolygon innerPolygon) {
        this.innerPolygon = innerPolygon;
    }

    public Isotherm getIsotherm() {
        return isotherm;
    }

    public void setIsotherm(Isotherm isotherm) {
        this.isotherm = isotherm;
    }

    public Isobar getIsobar() {
        return isobar;
    }

    public void setIsobar(Isobar isobar) {
        this.isobar = isobar;
    }

    public DryAdiabat getDryAdiabat() {
        return dryAdiabat;
    }

    public void setDryAdiabat(DryAdiabat dryAdiabat) {
        this.dryAdiabat = dryAdiabat;
    }

    public WetAdiabat getWetAdiabat() {
        return wetAdiabat;
    }

    public void setWetAdiabat(WetAdiabat wetAdiabat) {
        this.wetAdiabat = wetAdiabat;
    }

    public Ssh getSsh() {
        return ssh;
    }

    public void setSsh(Ssh ssh) {
        this.ssh = ssh;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }
}
