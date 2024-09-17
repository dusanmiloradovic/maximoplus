package maximoplus;

import psdi.mbo.MboSetRemote;

import java.util.Map;

public interface BinaryOutput {

    public BinaryOutputT getOutput(Map params, MboSetRemote moboset, String[] columns);
}
