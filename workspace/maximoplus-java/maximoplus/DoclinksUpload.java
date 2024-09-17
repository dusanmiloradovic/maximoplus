package maximoplus;

import psdi.app.doclink.DocinfoRemote;
import psdi.app.doclink.DoclinkServiceRemote;
import psdi.mbo.Mbo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.server.MXServer;
import psdi.util.MXException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Map;

public class DoclinksUpload implements Upload {

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
        try {
            DoclinkServiceRemote doclinkService = (DoclinkServiceRemote) MXServer.getMXServer().lookup("DOCLINK");
            dirName = getDoclinksDir(mboset, docType, doclinkService);
            File destDir = new File(dirName);
            destDir.mkdirs();
            int dotPosition = fileName.lastIndexOf(".");
            String baseFileName = fileName.substring(0, dotPosition);
            String fileExtension = fileName.substring(dotPosition + 1);
            dest = dirName + "/" + fileName;
            destFile = new File(dest);

            if (destFile.exists()) {
                int rand = (int) (Math.random() * 10e9);
                dest = dirName + "/" + baseFileName + Integer.toString(rand) + "." + fileExtension;
                destFile = new File(dest);
            }
        } catch (RemoteException e1) {
            throw new RuntimeException(e1);
        } catch (MXException e1) {
            throw new RuntimeException(e1);
        }


        try (InputStream inF = new FileInputStream(tmpFile);
             OutputStream outF = new FileOutputStream(destFile);
             //AUTO closeable statements
        ) {

            byte[] buf = new byte[1024];
            int len;
            while ((len = inF.read(buf)) > 0) {
                outF.write(buf, 0, len);
            }

            // The first part of the upload is finished, now the reference
            // should be inserted in doclinks to the document. I will try to use
            // the standard from maximo. It has to be tested in Maximo 6 and
            // Maximo 7.

            MboRemote mbo = mboset.getMbo();
            long keyValue = mbo.getUniqueIDValue();

            String keyColumn = mboset.getMboSetInfo().getUniqueIDName();
            String tableName = mboset.getMboSetInfo().getObjectName();
            // doclinkService.addDocinfoAndLinks(dest, fileName, null, docType,
            // "FILE", tableName, keyColumn, new
            // String[]{Long.toString(keyValue)}, true,
            // mboset.getApp(),mboset.getUserInfo());
            // the previous line doesn't work, so lets try with different
            // approach approach

            MboSetRemote docInfoSet = mbo.getMboSet("$newdocinfo$", "docinfo", "1=0");
            DocinfoRemote newDocInfo = (DocinfoRemote) docInfoSet.add();
            newDocInfo.setValue("newurlname", dest);
            // newDocInfo.addDocinfoAndLinks( dest, fileName, null, docType,
            // "FILE", tableName, keyColumn, new
            // String[]{Long.toString(keyValue)}, true, mboset.getApp());
            // this also has a bug in maximo, fixed code is here
            addDocinfoAndLinks(newDocInfo, dest, fileName, null, docType, "FILE", tableName, keyColumn,
                    new String[]{Long.toString(keyValue)}, true, mboset.getApp());
            // docInfoSet.save();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (MXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        out.setContentType("text/plain"); // for JSON
        out.setInputStream(new ByteArrayInputStream(ret.getBytes()));
        return out;
    }

    public String getDoclinksDir(MboSetRemote mboset, String docType, DoclinkServiceRemote doclinkService) throws MXException,
            RemoteException {
        String dirName = doclinkService.getDefaultFilePath(docType, mboset.getUserInfo());
        if (dirName == null || "".equals(dirName)) {
            // the followinng line works just with maximo 7, you can extend the
            // class and include
            // it in subclass, if you use maximo 7
            // MXServer.getMXServer().getProperty("mxe.doclink.doctypes.defpath");
            //			dirName = System.getProperty("user.home") + "/doclinks";
            String dir = System.getProperty("mxe.doclink.doctypes.defpath");
            if (dir != null && !"".equals(dir)) {
                return dir;
            }
            MXServer inst = MXServer.getMXServer();
            //to preserve backward compatibility with Maximo 6, there is no such method there
            try {
                Method m = inst.getClass().getMethod("getProperty", String.class);
                if (m != null) {

                    dir = (String) m.invoke(inst, "mxe.doclink.doctypes.defpath");
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                System.out.println(e);
            }
            if (dir == null || "".equals(dir)) {
                if (System.getProperty("os.name").startsWith("Windows")) {
                    dir = "c:/doclinks";
                } else {
                    dir = "/doclinks";
                }
            }
            return dir;
        }
        return dirName;
    }

    public void addDocinfoAndLinks(DocinfoRemote di, String urlname, String description, String[] urlparam, String doctype, String urltype,
                                   String keytable, String keycolumn, String[] keyvalue, boolean forceAutokey, String application) throws MXException,
            RemoteException {

        ((Mbo) di).getMboValue("document").autoKey();

        di.setValue("urltype", urltype);
        di.setValue("urlname", urlname);
        di.setValue("doctype", doctype);
        di.setValue("description", description);
        di.setValue("application", application, 2L);

        di.validate();

        addDoclinks(di, doctype, keytable, keycolumn, keyvalue);
    }

    public void addDoclinks(DocinfoRemote di, String doctype, String keytable, String keycolumn, String[] keyvalue) throws MXException,
            RemoteException {
        if ((keytable == null) || (keytable.equals("")) || (keyvalue == null) || (keyvalue.length < 1)) {
            return;
        }

        if ((doctype == null) || (doctype.equals(""))) {
            doctype = di.getString("doctype");
        }
        MboSetRemote linkSet = di.getMboSet("DOCLINKS");

        for (int xx = 0; xx < keyvalue.length; xx++) {
            int yy = 0;
            MboRemote testMbo = null;
            boolean linkExists = false;

            while ((testMbo = linkSet.getMbo(yy)) != null) {
                if ((testMbo.getString("doctype").equals(doctype)) && (testMbo.getString("ownertable").equals(keytable))
                        && (testMbo.getMboValueData("ownerid").getDataAsObject().toString().equals(keyvalue[xx]))) {
                    linkExists = true;
                    break;
                }

                yy++;
            }

            if (!linkExists) {
                MboRemote linkMbo = linkSet.add();

                linkMbo.setValue("doctype", doctype);
                linkMbo.setValue("ownertable", keytable);

                linkMbo.setValue("ownerid", keyvalue[xx]);
                linkMbo.setValue("docinfoid", di.getLong("docinfoid"), 11L);
                linkMbo.validate();
            }
        }
    }

}
