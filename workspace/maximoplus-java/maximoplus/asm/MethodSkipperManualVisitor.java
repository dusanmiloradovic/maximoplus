package maximoplus.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodSkipperManualVisitor extends MethodVisitor implements Opcodes {
    public MethodSkipperManualVisitor( MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitCode() {
        Label D = new Label();
        mv.visitLdcInsn("maximoplus.port");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);

        mv.visitJumpInsn(IFNULL, D);

        mv.visitInsn(RETURN);
        mv.visitLabel(D);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitCode();

    }
}
