package maximoplus.asm;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

public class TestASM {

    public MbboSet thisMboSet;
    public long uniqueMboId;
    long fieldd = 0;
    Vector mboVec;
    Mbbo mbbo;
    private AtomicLong counter = new AtomicLong(6);

    public TestASM() {

    }

    public static void djosa(Mbbo m, long uniqueMboId2) {
        // TODO Auto-generated method stub

    }

    public MbboSet getThisMboSet() {
        return thisMboSet;
    }

    public void mmm(Mbbo mm) {
        uniqueMboId = getThisMboSet().getNewId();
    }

    public void testCastPass(Mbbo mo, String i) {
        counter = new AtomicLong();

    }

    public void sendCommand(Mbbo m, String command, Object[] args) {
    }

    public void sendt(Mbbo m, long l, boolean n) {
    }

    public void sada(Mbbo ff, long t, long s, boolean n) {
        System.out.println("#############################################zoveseadd");

    }

    public void generateMbboInstance() {
        Mbbo m = getMbboInstance();
        djosa(m, m.uniqueMboId);

    }

    private Mbbo getMbboInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    interface MbboRemote {
    }

    static class Hook {
        static void setMboCurrIndex(Mbbo curr, long pozicija, long uniqueid) {
        }
    }

    class Mbbo implements MbboRemote {
        public long uniqueMboId;

    }

    class MbboSet {
        MbboRemote currMbo;
        int currIndex;

        MbboRemote getMboInstance(MbboSet ms) {
            return new Mbbo();
        }

        public long getNewId() {
            return counter.incrementAndGet();
        }

        public void proba() {
            Hook.setMboCurrIndex((Mbbo) currMbo, (long) currIndex, (currMbo == null) ? -1 : ((Mbbo) currMbo).uniqueMboId);
        }
    }


}
