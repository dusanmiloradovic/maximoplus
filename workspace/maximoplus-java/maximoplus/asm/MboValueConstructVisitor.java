package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboValueConstructVisitor extends MethodVisitor implements Opcodes {

    public MboValueConstructVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);

            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboValue", "currentValue", "Lpsdi/util/MaxType;");
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "updateMbo",
                    "(Lpsdi/mbo/MboValue;JLpsdi/util/MaxType;)V",false);
        }
        mv.visitInsn(opcode);
    }
}
