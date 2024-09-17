package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboValueSetFlagVisitor extends MethodVisitor implements Opcodes {

    public MboValueSetFlagVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {
            //prvi argument f-je je mbo
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboValue", "mbo", "Lpsdi/mbo/Mbo;");
            //drugi argument je mboid
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboValue", "mbo", "Lpsdi/mbo/Mbo;");
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
            //treci je konstanta sa imenom komande
            mv.visitLdcInsn("setfieldflag");
            //cetvrti je array sa imenom polja, flegom i vrednoscu flega
            mv.visitInsn(ICONST_3);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);

            //evo imena polja
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboValue", "mbovalueinfo", "Lpsdi/mbo/MboValueInfo;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "psdi/mbo/MboValueInfo", "getName", "()Ljava/lang/String;",false);

            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_1);
            //evo flega
            mv.visitVarInsn(LLOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_2);
            //i na kraju vrednost flega
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",false);
            mv.visitInsn(AASTORE);

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "commandMboWithParam",
                    "(Lpsdi/mbo/MboRemote;JLjava/lang/String;[Ljava/lang/Object;)V",false);
        }
        mv.visitInsn(opcode);
    }

}
