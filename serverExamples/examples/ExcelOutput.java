package examples;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Map;

import maximoplus.BinaryOutput;
import maximoplus.BinaryOutputT;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;

public class HtmlOutput implements BinaryOutput {

	@Override
	public BinaryOutputT getOutput(Map params, MboSetRemote mboset, String[] columns) {
		System.out.println("MboSet is:" + mboset + ",  columns=" + columns);
		String out = "<html><head><title>Test Output</title></head><body>";
		for (Object p : params.keySet()) {
			out += ("key=" + p + ",value=" + params.get(p) + ",type=" + params.get(p).getClass()) + "<br/>";
		}
		out += "<table>";
		try {
			int count = mboset.count();
			for (int i = 0; i < count; i++) {
				MboRemote mbo = mboset.getMbo(i);
				out += "<tr>";
				for (int j = 0; j < columns.length; j++) {
					out += "<td>" + mbo.getString(columns[j]) + "</td>";
				}
				out += "</tr>";
			}
			out += "</table></body></html>";
			InputStream is = new ByteArrayInputStream(out.getBytes());
			BinaryOutputT outb = new BinaryOutputT();
			outb.setFileName("report.html");
			outb.setInputStream(is);
			outb.setContentType("text/html");
			return outb;
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MXException e) {
			e.printStackTrace();
		}
		return null;
	}
}
