package maximoplus.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodSkipperVisitor extends MethodVisitor implements Opcodes {
    public MethodSkipperVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitCode() {
        Label D = new Label();
        mv.visitLdcInsn("maximoplus.port");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitJumpInsn(IFNULL, D);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(D);
        mv.visitCode();

    }
}