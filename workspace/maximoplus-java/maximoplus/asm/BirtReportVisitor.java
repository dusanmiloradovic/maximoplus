package maximoplus.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BirtReportVisitor extends ClassVisitor implements Opcodes {
    public BirtReportVisitor(ClassVisitor cv) {
        super(ASM4, cv);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                exceptions);
        if ("startBatchReportImport".equals(name)) {
            return new MethodSkipperManualVisitor(mv);
        }
        return mv;
    }
}
