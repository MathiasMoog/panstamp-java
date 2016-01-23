package panstamp;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import me.legrange.panstamp.DeviceLibrary;
import me.legrange.panstamp.NetworkException;
import me.legrange.panstamp.definition.CompoundDeviceLibrary;
import me.legrange.panstamp.definition.DeviceDefinition;
import me.legrange.panstamp.xml.ClassLoaderLibrary;
import me.legrange.panstamp.xml.FileLibrary;

/**
 * Basic Tests for the Device Library
 * @author Mathias
 *
 */
public class TestDeviceLibrary {

	/**
	 * Load and test default device definition library.
     * @throws NetworkException
	 */
    @Test
    public void testDefault() throws NetworkException {
        DeviceLibrary devLib = new ClassLoaderLibrary();
        testInternal(devLib);
    }

    /**
     * Load and test device with a given path definition library.
     * Might be used to check changes in device definition files.
     * @throws NetworkException
     */
    @Test
    public void testCustom() throws NetworkException {
        String dir = "src/main/resources/devices";
        FileLibrary lib = new FileLibrary(new File(dir));
        assertNotNull(lib);
        DeviceLibrary devLib = new CompoundDeviceLibrary(lib, new ClassLoaderLibrary());
        testInternal(devLib);
    }

    /**
     * Run some tests with an given device library
     *
     * @param devLib device library, might be null
     * @throws NetworkException
     */
    private void testInternal(DeviceLibrary devLib) throws NetworkException {
        assertNotNull(devLib);
        // Manufacture 1 = panStamp, Product 1 = Dual Temperature-Humidity sensor
        assertTrue(devLib.hasDeviceDefinition(1, 1));
        DeviceDefinition devDef = devLib.getDeviceDefinition(1, 1);
        assertNotNull(devDef);
        System.out.println(" Device definition = " + devDef);
    }

}
