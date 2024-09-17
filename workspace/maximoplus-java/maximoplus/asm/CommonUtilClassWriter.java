package maximoplus.asm;

import org.objectweb.asm.ClassWriter;

public class CommonUtilClassWriter extends ClassWriter {
    public CommonUtilClassWriter(int i) {
        super(i);
    }

    @Override
    protected String getCommonSuperClass(String s, String s1) {

        if ("psdi/server/MXServer".equals(s) && "psdi/server/MXServerRemote".equals(s1)){
            return "psdi/server/MXServerRemote";
        }
        return super.getCommonSuperClass(s, s1);

    }
}
