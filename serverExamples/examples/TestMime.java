package examples;

import java.io.File;
import javax.activation.MimetypesFileTypeMap;

class TestMime {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));

        File f = new File(System.getProperty("java.home"), "lib");
        f = new File(f, "mime.types");
        System.out.println(f.exists() + " \t - " +f);

        f = new File(System.getProperty("user.home"), ".mime.types");
        System.out.println(f.exists() + " \t - " +f);

        MimetypesFileTypeMap mfm = new MimetypesFileTypeMap();
        System.out.println(mfm.getContentType("a.docx"));
        System.out.println(mfm.getContentType("a.pdf"));
        System.out.println(mfm.getContentType("a.pptx"));
        System.out.println(mfm.getContentType("a.java"));
        System.out.println(mfm.getContentType("a.htm"));
    }
}
