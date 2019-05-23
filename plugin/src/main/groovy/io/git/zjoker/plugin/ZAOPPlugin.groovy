package io.git.zjoker.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import io.git.zjoker.processor.CheckSelfPermissionWeaver
import io.git.zjoker.processor.ClassProcessor
import io.git.zjoker.processor.CodeWeaveUtils
import io.git.zjoker.processor.ThreadSwitchWeaver
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES

class ZAOPPlugin extends Transform implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println "test" + project.rootDir.absolutePath
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "ZAOP"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }


    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {

        println '//===============asm visit start===============//'

        def startTime = System.currentTimeMillis()
        ThreadSwitchWeaver.clearInnerClassIndex()
        CheckSelfPermissionWeaver.clearInnerClassIndex()
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->

                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        def name = file.name
                        if (name.endsWith(".class") && !name.startsWith("R\$") &&
                                !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {

                            println file.absolutePath + ' is changing...'

                            ClassNode classNode = getClassNode(file.bytes);
                            processClass(classNode, file)
                        }
                    }
                }

                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)


                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                def dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000

        println "plugin cost $cost secs"
        println '//===============asm visit end===============//'
    }

    private ClassNode getClassNode(byte[] bytes) {
        ClassReader cr = null;
        try {
            cr = new ClassReader(bytes);
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            cr.accept(classNode, EXPAND_FRAMES);
            return classNode;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] processClass(ClassNode classNode, File sourceFile) {
        FileOutputStream fos = null;
        try {
            ClassReader cr = new ClassReader(sourceFile.bytes);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

            ClassProcessor processor = new ClassProcessor(Opcodes.ASM5, cw, classNode, sourceFile);

            cr.accept(processor, EXPAND_FRAMES);

            byte[] code = cw.toByteArray();

            fos = new FileOutputStream(sourceFile.absolutePath);
            fos.write(code);
            fos.close();
            return code;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CodeWeaveUtils.close(fos);
        }
        return null;
    }
}