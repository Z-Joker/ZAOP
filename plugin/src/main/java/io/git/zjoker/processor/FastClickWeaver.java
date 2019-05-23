package io.git.zjoker.processor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import static io.git.zjoker.processor.CodeWeaveUtils.CLASS_PATH_ZAOP;
import static io.git.zjoker.processor.CodeWeaveUtils.getClassPath;

public class FastClickWeaver extends AdviceAdapter {
    private boolean isFastClickAllowed;

    public FastClickWeaver(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getObjectType(
                getClassPath("io.git.zjoker.zaop.annotations.FastClickAllowed")).getDescriptor()
                .equals(desc)) {
            isFastClickAllowed = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        if (!isFastClickAllowed) {
            //֯���ֹ��������Ĵ���
            weaveFastClickCode(mv);
        }
    }

    private void weaveFastClickCode(MethodVisitor mv) {
        CodeWeaveUtils.callStaticMethod(
                mv
                , CLASS_PATH_ZAOP
                , "isFastClick"
                , CodeWeaveUtils.getMethodDesc(boolean.class));
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);
        mv.visitInsn(RETURN);
        mv.visitLabel(l1);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }
}
