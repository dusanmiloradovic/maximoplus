package examples;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import maximoplus.BinaryOutputT;
import maximoplus.Upload;
import psdi.mbo.MboSetRemote;

public class DefaultUpload implements Upload{

    @Override
	public BinaryOutputT upload(Map uploadParameters, MboSetRemote mboset) {
	BinaryOutputT out = new BinaryOutputT();
	for (Object p:uploadParameters.keySet()){
	    System.out.println("key="+p+",value="+uploadParameters.get(p)+",type="+uploadParameters.get(p).getClass());
	}
	Map fileData = (Map) uploadParameters.get("file");
	File tmpFile = (File) fileData.get("tempfile");
	String fileName = (String) fileData.get("filename");
	String userHome = System.getProperty( "user.home" );
	String dest = userHome+"/"+fileName;
	String ret ="OK";
	try {
		InputStream inF = new FileInputStream(tmpFile);
		OutputStream outF = new FileOutputStream(dest);
		byte[] buf = new byte[1024];
		int len;
		while ((len = inF.read(buf)) > 0) {
		    outF.write(buf, 0, len);
		}
		inF.close();
		outF.close();
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		ret="Error";
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		ret="Error";
	} 
	out.setContentType("text/plain"); //for JSON
	out.setInputStream(new ByteArrayInputStream(ret.getBytes()));
	return out;
    }

}
