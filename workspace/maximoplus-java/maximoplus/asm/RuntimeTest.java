package maximoplus.asm;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class RuntimeTest {

    public static void main(String[] args) {
        try {
            URLClassLoader load = new URLClassLoader(new URL[]{new URL("file:///home/dusan/tmp")});
            load.loadClass("psdi.mbo.MboSet");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
