package www.cnr7.com;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() throws IOException {
        NetcdfFile netcdfFile = NetcdfFile.open("D:\\Projects\\2019\\Test9\\data\\bufr/sn.0028.bin");
        System.out.println(netcdfFile.getDetailInfo());
    }
}
