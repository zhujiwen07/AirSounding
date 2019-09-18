package www.cnr7.com;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void test()
    {
        Test1 test1 = new Test1();
        System.out.println(test1.getTest2().getC());
        Test2 test2 = test1.getTest2();
        test2.setC(1);
        System.out.println(test1.getTest2().getC());
    }
}
