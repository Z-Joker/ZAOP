package io.git.zjoker.processor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class PermissionRequestResultWeaver extends AdviceAdapter {
    public PermissionRequestResultWeaver(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        Type[] methodArgs = Type.getArgumentTypes(methodDesc);
        CodeWeaveUtils.aloadThis(mv);
        CodeWeaveUtils.aloadVars(mv, methodArgs,1);
        CodeWeaveUtils.callStaticMethod(
                mv
                , CodeWeaveUtils.getClassPath("io.git.zjoker.zaop.utils.PermissionRequestBridge")
                , "handleResult"
                , CodeWeaveUtils.getMethodDesc(boolean.class, Object.class, int.class, String[].class, int[].class));
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);
        mv.visitInsn(RETURN);
        mv.visitLabel(l1);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }
}
