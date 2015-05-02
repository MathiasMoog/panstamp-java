package me.legrange.panstamp.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * A device library that uses a directory with XML files to find device 
 * configurations. 
 * 
 * @since 1.0
 * @author Gideon le Grange https://github.com/GideonLeGrange *
 */
public final class FileLibrary extends XmlDeviceLibrary {

    public FileLibrary(File dir) {
        this.dir = dir;
    }
    
    /** Return the directory used for loading by this file loader.
     * 
     * @return The absolute directory path. 
     * @since 1.2
     */
    public String getDirectory() {
        return dir.getAbsolutePath();
    }

    @Override
    InputStream getStream(String path) {
        try {
            return new FileInputStream(dir.getAbsolutePath() + "/" + path);
        } catch (FileNotFoundException ex) {
            log.warning(String.format("File '%s' for does not exist under '%s'.", path, dir.getAbsolutePath()));
            return null;
        }
    }

    private final File dir;
    private final static Logger log = Logger.getLogger(FileLibrary.class.getName());

}
