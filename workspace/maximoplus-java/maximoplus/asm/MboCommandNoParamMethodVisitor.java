package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboCommandNoParamMethodVisitor extends MethodVisitor implements Opcodes {

    private String command;

    public MboCommandNoParamMethodVisitor(String command, MethodVisitor mv) {
        super(ASM4, mv);
        this.command = command;
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitLdcInsn(command);
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "commandMbo",
                    "(Lpsdi/mbo/MboRemote;JLjava/lang/String;)V",false);
        }
        mv.visitInsn(opcode);
    }
}
