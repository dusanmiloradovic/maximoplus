package maximoplus.asm;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CommonUtilVisitor extends ClassVisitor implements Opcodes {
    public CommonUtilVisitor(ClassVisitor cv) {
        super(ASM4, cv);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                exceptions);
        if ("getAppServerPorts".equals(name)) {
            return new MethodSkipperVisitor(mv);
        }
        return mv;
    }
}
