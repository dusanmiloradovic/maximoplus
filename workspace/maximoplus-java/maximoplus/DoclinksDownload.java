package maximoplus;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Map;

public class DoclinksDownload implements BinaryOutput {

    @Override
    public BinaryOutputT getOutput(Map params, MboSetRemote mboset, String[] columns) {
        //The mboset has to be doclinks
        //	String docInfoId = ((String) params.get("docinfoid")).replace(",", "");
        // because all that is read on the client side are the strings
        try {
            synchronized (mboset) {
                //				MboRemote mbo = mboset.getMbo(0);
                //we are passing the full mboset ref from client now, it has to take the current doclinks mbo
                MboRemote mbo = mboset.getMbo();
                MboSetRemote diSet = mbo.getMboSet("docinfo");
                MboRemote docInfo = diSet.getMbo(0);
                return getOutputFromDocInfo(docInfo);

            }

        } catch (RemoteException e) {

            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (MXException e) {

            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected BinaryOutputT getOutputFromDocInfo(MboRemote docInfo) throws MXException, IOException {
        String urlName = docInfo.getString("urlname");
        String urlType = docInfo.getString("urltype");
        String fileName = urlName.substring(urlName.lastIndexOf("/") + 1);
        if ("FILE".equals(urlType)) {
            File srcFile = new File(urlName);
            InputStream is = new FileInputStream(srcFile);
            urlName = urlName.replace('\\', '/');
            String docinfoFileName = docInfo.getString("urlname");
            docinfoFileName = docinfoFileName.substring(docinfoFileName.lastIndexOf("/") + 1);
            String mimeType = SimpleMimeMapper.getMimeType(docinfoFileName);

            //String mimeType = new MimetypesFileTypeMap().getContentType(srcFile);
            BinaryOutputT outb = new BinaryOutputT();
            outb.setFileName(fileName);
            outb.setContentType(mimeType);
            outb.setInputStream(is);
            return outb;
        } else {
            String redirectHTML = "<!DOCTYPE HTML>" + "			  <html lang='en-US'>" + "			  <head>" + "			      <meta charset='UTF-8'>"
                    + "			      <meta http-equiv='refresh' content='1;url=" + urlName + "'>" + "			      <script language='javascript'>"
                    + "			          window.location.href = '" + urlName + "'" + "			      </script>"
                    + "			      <title>Page Redirection</title>" + "			  </head>" + "			  <body>"
                    + "			  If you are not redirected automatically, please click the link to continue to the <a href='" + urlName + "'>"
                    + docInfo.getString("description") + "</a>" + "			  </body>" + "			  </html>";
            InputStream is = new ByteArrayInputStream(redirectHTML.getBytes());
            // InputStream is=new FileInputStream(new
            // File("/home/dusan/resume.docx"));
            BinaryOutputT outb = new BinaryOutputT();
            outb.setFileName(fileName);
            outb.setInputStream(is);
            outb.setContentType("text/html");
            return outb;
        }

    }

}
