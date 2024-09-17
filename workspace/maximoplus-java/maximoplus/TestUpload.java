package maximoplus;

import psdi.mbo.MboSetRemote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;

public class TestUpload implements Upload {

    @Override
    public BinaryOutputT upload(Map uploadParameters, MboSetRemote mboset) {
        BinaryOutputT out = new BinaryOutputT();
        for (Object p : uploadParameters.keySet()) {
            System.out.println("key=" + p + ",value=" + uploadParameters.get(p) + ",type=" + uploadParameters.get(p).getClass());
        }

        Map fileData = (Map) uploadParameters.get("file");
        File tmpFile = (File) fileData.get("tempfile");
        String fileName = (String) fileData.get("filename");
        int ieUploadDelimiter = fileName.lastIndexOf("\\");//the way internet explorer report the file names
        if (ieUploadDelimiter != -1) {
            fileName = fileName.substring(ieUploadDelimiter + 1);
        }
        String docType = (String) uploadParameters.get("doctype");
        String ret = "OK";
        String dirName = null;
        File destFile = null;
        String dest = null;
        System.out.println("Uploading file " + fileName + ",tempfile=" + tmpFile);
        out.setContentType("text/plain");
        try {
            out.setInputStream(new ByteArrayInputStream("OK".getBytes("UTF8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
