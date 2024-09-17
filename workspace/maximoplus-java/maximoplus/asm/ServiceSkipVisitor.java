package maximoplus.asm;


import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ServiceSkipVisitor extends MethodVisitor implements Opcodes {
    public ServiceSkipVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    public void visitCode() {
        //    mv.visitCode();
        Label l2 = new Label();


        mv.visitVarInsn(ALOAD, 2);
        mv.visitLdcInsn("OPTIMIZATION");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",false);
        mv.visitJumpInsn(IFEQ, l2);


        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(ICONST_0);

        mv.visitInsn(IRETURN);
        mv.visitEnd();


        mv.visitLabel(l2);

        mv.visitCode();
        mv.visitEnd();


    }
}
