package www.cnr7.com;

/**
 * @Author zhujiwen
 * @Date 星期一 2019-08-26 10:05
 * @Version 1.0
 * @Description
 **/
public class Test1 {
    private int a;

    private int b;

    private Test2 test2;

    public Test1() {
        a = 1;
        b = 2;

        test2 = new Test2();
        test2.setC(3);
        test2.setD(4);
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public Test2 getTest2() {
        return test2;
    }

    public void setTest2(Test2 test2) {
        this.test2 = test2;
    }
}
