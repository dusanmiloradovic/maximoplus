package maximoplus;

import psdi.mbo.MboSetRemote;

import java.util.Map;

public interface Upload {
    public BinaryOutputT upload(Map uploadParameters, MboSetRemote mboset);
}
