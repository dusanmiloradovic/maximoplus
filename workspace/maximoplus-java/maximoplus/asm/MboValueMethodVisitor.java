package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboValueMethodVisitor extends MethodVisitor implements Opcodes {

    public MboValueMethodVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            //mv.visitMethodInsn(INVOKEVIRTUAL, "psdi/mbo/MboValue", "getMbo", "Lpsdi.mbo.Mbo;");
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboValue", "mbo", "Lpsdi/mbo/Mbo;");
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "updateMbo",
                    "(Lpsdi/mbo/MboValue;JLpsdi/util/MaxType;)V",false);
        }
        mv.visitInsn(opcode);
    }

}
