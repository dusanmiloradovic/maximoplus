package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetMethodVisitorAddMboToSet extends MethodVisitor implements
        Opcodes {

    boolean addOnVectorCalled = false;
    boolean insertElementAtCalled = false;
    private String desc;

    public MboSetMethodVisitorAddMboToSet(MethodVisitor mv, String desc) {
        super(ASM4, mv);
        this.desc = desc;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc) {
        //postoje dva metoda koja se zovu addmbo, treba da posaljem razlicite poruke u razlicitim slucajevima
        if (owner.equals("java/util/Vector") && name.equals("addElement")) {
            addOnVectorCalled = true;
            insertElementAtCalled = false;
        }

        if (owner.equals("java/util/Vector") && name.equals("insertElementAt")) {
            addOnVectorCalled = false;
            insertElementAtCalled = true;
        }

        super.visitMethodInsn(opcode, owner, name, desc);
    }

//	@Override
//	public void visitInsn(int instrCode) {
//
//		if (instrCode == RETURN) {
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitVarInsn(ALOAD, 0);
//			mv.visitMethodInsn(INVOKEVIRTUAL, "psdi/mbo/MboSet", "getNewId",
//					"()J");
//			mv.visitFieldInsn(PUTFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
//			
//			if (addOnVectorCalled){
////				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////				mv.visitLdcInsn("#############################################zovem add");
////				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//				mv.visitVarInsn(ALOAD, 1);
//				mv.visitVarInsn(ALOAD, 0);
//				mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboSet", "mboVec", "Ljava/util/Vector;");
//				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "size",
//						"()I");
//				mv.visitInsn(I2L);
//				mv.visitVarInsn(ALOAD, 1);
//				mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
//			
//				mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "createMbo",
//						"(Lpsdi/mbo/Mbo;JJ)V");
////				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////				mv.visitLdcInsn("#############################################zavrsio sa pozivom");
////				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//			}
//			
//			if (insertElementAtCalled){
////				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////				mv.visitLdcInsn("#############################################zovem add");
////				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//				mv.visitVarInsn(ALOAD, 1);
//				mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
//				
//				mv.visitVarInsn(ALOAD, 1);
//				mv.visitVarInsn(ILOAD, 2);
//				mv.visitInsn(I2L);
//				mv.visitVarInsn(ALOAD, 1);
//				mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
//				
//				mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "createMbo",
//						"(Lpsdi/mbo/Mbo;JJ)V");
////				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////				mv.visitLdcInsn("#############################################zavrsio sa pozivom");
////				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//			}
//		}
//		mv.visitInsn(instrCode);
//	}

    @Override
    public void visitCode() {
        mv.visitCode();

        if ("(Lpsdi/mbo/Mbo;)V".equals(desc)) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboSet", "mboVec", "Ljava/util/Vector;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "size",
                    "()I",false);
            mv.visitInsn(I2L);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "addMbo",
                    "(Lpsdi/mbo/MboRemote;JJ)V",false);
        } else {
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(I2L);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "addMbo",
                    "(Lpsdi/mbo/MboRemote;JJ)V",false);
        }


    }


}
