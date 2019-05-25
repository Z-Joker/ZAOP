package io.git.zjoker.processor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Created by ZJoker on 2019/5/23.
 */
public class ClassProcessor extends ClassVisitor {
    private ClassNode classNode;
    public static final int api = ASM5;
    private File sourceFile;

    public ClassProcessor(int api, ClassVisitor cv, ClassNode classNode, File sourceFile) {
        super(api, cv);
        this.classNode = classNode;
        this.sourceFile = sourceFile;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }

    private MethodVisitor safeMV(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
        if (mv != null) {
            return mv;
        } else {
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = null;

        MethodNode mn = getMethodNode(access, name, desc, signature, exceptions);
        AnnotationNode threadOnAnn = withThreadOnAnn(mn);
        AnnotationNode checkPermissionAnn = withCheckPermissionAnn(mn);

        if (threadOnAnn != null) {
            ThreadSwitchWeaver.weave(cv, classNode, mn, threadOnAnn, sourceFile.getParentFile().getAbsolutePath());
            mv = super.visitMethod(access, ThreadSwitchWeaver.getProxyMethodName(name), desc, signature, exceptions);
        } else if (!CodeWeaveUtils.isStatic(access) && checkPermissionAnn != null) {
            CheckSelfPermissionWeaver.weave(cv, classNode, mn, checkPermissionAnn, sourceFile.getParentFile().getAbsolutePath());
            mv = super.visitMethod(access, CheckSelfPermissionWeaver.getProxyMethodName(name), desc, signature, exceptions);
        }

        AnnotationNode rtSupportAnn = withRTSupportAnn(mn);
        if (rtSupportAnn != null) {
            mv = new RTSupportMethodWeaver(
                    api
                    , safeMV(mv, access, name, desc, signature, exceptions)
                    , mn);
        }

        if (!CodeWeaveUtils.isStatic(access)) {
            if (CodeWeaveUtils.matchMethod(
                    name
                    , desc
                    , "onActivityResult"
                    , Type.INT_TYPE, Type.INT_TYPE, Type.getObjectType(CodeWeaveUtils.getClassPath("android.content.Intent")))) {
                mv = new OnActResultWeaver(api, safeMV(mv, access, name, desc, signature, exceptions), access, name, desc);
            } else if (CodeWeaveUtils.matchMethod(
                    name
                    , desc
                    , "onRequestPermissionsResult"
                    , Type.INT_TYPE, Type.getType(String[].class), Type.getType(int[].class))) {
                mv = new PermissionRequestResultWeaver(api, safeMV(mv, access, name, desc, signature, exceptions), access, name, desc);
            } else if (name.equals("onClick")) {//�ж���OnClickListener.onClick����
                Type[] argumentTypes = Type.getArgumentTypes(desc);
                if (argumentTypes.length == 1
                        && argumentTypes[0].equals(Type.getObjectType(CodeWeaveUtils.getClassPath("android.view.View")))) {
                    mv = new FastClickWeaver(
                            api
                            , safeMV(mv, access, name, desc, signature, exceptions)
                            , access, name, desc);
                }
            } else if (withFastClickFilter(mn) != null) {
                mv = new FastClickWeaver(
                        api
                        , safeMV(mv, access, name, desc, signature, exceptions)
                        , access, name, desc);
            }

        }


        return safeMV(mv, access, name, desc, signature, exceptions);
    }


    private MethodNode getMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
        for (int i = 0; i < classNode.methods.size(); i++) {
            MethodNode mn = classNode.methods.get(i);

            if (mn.access == access
                    && mn.name.equals(name)
                    && mn.desc.equals(desc)) {
                return mn;
            }
        }
        return null;
    }

    private AnnotationNode withRTSupportAnn(MethodNode mn) {
        return withAnn(mn, "io.git.zjoker.zaop.annotations.RTSupport");
    }

    //
    private AnnotationNode withThreadOnAnn(MethodNode mn) {
        return withAnn(mn, "io.git.zjoker.zaop.annotations.ThreadOn");
    }

    //
    private AnnotationNode withCheckPermissionAnn(MethodNode mn) {
        return withAnn(mn, "io.git.zjoker.zaop.annotations.CheckPermission");
    }

    private AnnotationNode withFastClickFilter(MethodNode mn) {
        return withAnn(mn, "io.git.zjoker.zaop.annotations.FastClickFilter");
    }

    private AnnotationNode withAnn(MethodNode mn, String className) {
        if (mn == null) {
            return null;
        }
        String classDesc = Type.getObjectType(CodeWeaveUtils.getClassPath(className)).getDescriptor();
        List<AnnotationNode> annotations = mn.invisibleAnnotations;
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(classDesc)) {
                    return annotation;
                }
            }
        }
        return null;
    }
}
