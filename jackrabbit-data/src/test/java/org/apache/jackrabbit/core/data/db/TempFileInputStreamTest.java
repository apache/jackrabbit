package org.apache.jackrabbit.core.data.db;

import java.io.File;

import junit.framework.TestCase;

public class TempFileInputStreamTest extends TestCase {

    private File tmp;
    private TempFileInputStream in;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmp = File.createTempFile("test", ".bin");
        in = new TempFileInputStream(tmp);
    }

    public void testFileIsDeletedWhenStreamIsClosed() throws Exception {
        assertTrue(tmp.exists());
        in.close();
        assertFalse(tmp.exists());
    }

}
