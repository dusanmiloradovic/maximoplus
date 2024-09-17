package maximoplus;

import psdi.mbo.MboRemote;
import psdi.util.MXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
In React Native the easiest way to  open PDF of the other dcocument is iusing the Linking class. THe issue is
that it opens the link in the browser and loses the cookie . This will create the publicly accessible
temp file, and returns the file name
 */
public class DoclinksRedirectDownload extends DoclinksDownload implements BinaryOutput {
    @Override
    protected BinaryOutputT getOutputFromDocInfo(MboRemote docInfo) throws MXException, IOException {
        String urlName = docInfo.getString("urlname");
        String urlType = docInfo.getString("urltype");
        String fileName = urlName.substring(urlName.lastIndexOf("/") + 1);
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if ("FILE".equals(urlType)) {
            String s = "temp" + System.currentTimeMillis() + "." + extension;
            String destfileURL = System.getProperty("user.dir") + File.separator + "public" + File.separator + s;
            File sourceFile = new File(urlName);
            File destFile = new File(destfileURL);
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            InputStream is = new ByteArrayInputStream(s.getBytes());
            BinaryOutputT outb = new BinaryOutputT();
            outb.setFileName(fileName);
            outb.setInputStream(is);
            outb.setContentType("text/plain");//just return the url
            removeTempFile(destFile);
            return outb;
        } else {
            InputStream is = new ByteArrayInputStream(urlName.getBytes());
            // InputStream is=new FileInputStream(new
            // File("/home/dusan/resume.docx"));
            BinaryOutputT outb = new BinaryOutputT();
            outb.setFileName(fileName);
            outb.setInputStream(is);
            outb.setContentType("text/plain");//just return the url
            return outb;
        }

    }

    /*
    We don't need the temp file user already opened it in the app, delete it after 30 mins
     */
    private void removeTempFile(File file){
        final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
        final Runnable deleted=new Runnable() {
            @Override
            public void run() {
                file.delete();
            }
        };
        scheduledService.schedule(deleted,30L, TimeUnit.MINUTES);

    }
}
