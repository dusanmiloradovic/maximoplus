package maximoplus.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

public class MboClassVisitor extends ClassVisitor implements Opcodes {

    public MboClassVisitor(ClassVisitor cv) {
        super(ASM4, cv);

    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //		System.out.println("+++"+name);
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        String[] voidDescs = new String[]{"undelete", "select", "unselect"};
        if (Arrays.asList(voidDescs).contains(name)) {
            return new MboCommandNoParamMethodVisitor(name, mv);
        }
        if ("delete".equals(name) && "(J)V".equals(desc)) {
            return new MboCommandNoParamMethodVisitor(name, mv);
        }
        if ("setFlag".equals(name) && "(JZ)V".equals(desc)) {
            return new MboCommandSetFlag(mv);
        }

        if ("setFieldFlag".equals(name) && "(Ljava/lang/String;JZLpsdi/util/MXException;)V".equals(desc)) {
            return new MboCommandSetFieldFlag(mv);
        }
        if ("<init>".equals(name)) {
            return new MboInitialize(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        FieldVisitor fv = cv.visitField(ACC_PUBLIC, "uniqueMboId", "J", null,
                null);
        fv.visitEnd();

    }

}
