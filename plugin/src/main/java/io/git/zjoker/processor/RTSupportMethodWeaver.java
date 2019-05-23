package io.git.zjoker.processor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static io.git.zjoker.processor.CodeWeaveUtils.CLASS_PATH_ZAOP;
import static io.git.zjoker.processor.CodeWeaveUtils.getClassPath;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;

public class RTSupportMethodWeaver extends MethodVisitor {
    private MethodNode methodNode;

    public RTSupportMethodWeaver(int i, MethodVisitor mv, MethodNode methodNode) {
        super(i, mv);
        this.methodNode = methodNode;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        //NonNull
        weaveNonNullCheck();
    }

    private void weaveNonNullCheck() {
        List<AnnotationNode>[] allInvisibleParameterAnnotations
                = methodNode.invisibleParameterAnnotations;
        if (allInvisibleParameterAnnotations == null) {
            return;
        }

        //????????@Nonnull?????
        List<LocalVariableNode> localVariables = methodNode.localVariables;
        int paramCount = Type.getArgumentTypes(methodNode.desc).length;
        List<Integer> nonnullParamIndex = new ArrayList<>();
        for (int i = 0; i < allInvisibleParameterAnnotations.length; i++) {
            List<AnnotationNode> parameterAnnotation = allInvisibleParameterAnnotations[i];

            if (parameterAnnotation == null) continue;

            for (int j = 0; j < parameterAnnotation.size(); j++) {
                AnnotationNode annotationNode = parameterAnnotation.get(j);
                if (annotationNode != null
                        && CodeWeaveUtils.getDesc("android.support.annotation.NonNull").equals(annotationNode.desc)) {
                    nonnullParamIndex.add(i);
                }
            }
        }

        //????????????
        if (!nonnullParamIndex.isEmpty()) {
            int nextVarIndex = paramCount;
            boolean isStatic = CodeWeaveUtils.isStatic(methodNode.access);
            if (!isStatic) {//?????????????this??index????????????¦Ë
                nextVarIndex++;
            }

            //??????????????list
            mv.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
            mv.visitInsn(DUP);
            CodeWeaveUtils.newObj(mv, Type.getInternalName(ArrayList.class), CodeWeaveUtils.getMethodDesc(void.class));
            mv.visitVarInsn(ASTORE, nextVarIndex);

            String paramEntityCP = CodeWeaveUtils.getClassPath("io.git.zjoker.zaop.utils.ParamEntity");

            for (int i = 0; i < nonnullParamIndex.size(); i++) {
                Integer paramIndex = nonnullParamIndex.get(i);
                if (!isStatic) {//?????????????this??index????????????¦Ë
                    paramIndex++;
                }
                String paramName = localVariables.get(paramIndex).name;
                mv.visitVarInsn(ALOAD, nextVarIndex);
                mv.visitTypeInsn(NEW, paramEntityCP);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(paramName);
                mv.visitVarInsn(ALOAD, paramIndex);
                CodeWeaveUtils.newObj(mv, paramEntityCP, CodeWeaveUtils.getMethodDesc(void.class, String.class, Object.class));
                CodeWeaveUtils.callPublicInstanceMethod(mv, CodeWeaveUtils.getClassPath(List.class), "add", CodeWeaveUtils.getMethodDesc(boolean.class, Object.class), true);
                mv.visitInsn(POP);
            }

            mv.visitVarInsn(ALOAD, nextVarIndex);
            CodeWeaveUtils.callStaticMethod(mv, CLASS_PATH_ZAOP, "checkNonnull", CodeWeaveUtils.getMethodDesc(void.class, List.class));

        }
    }

}
