package org.avaje.idea.typequery.plugin;

import com.google.common.io.Files;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Lookup a class file by given class name.
 *
 * @author Mario Ivankovits, mario@ops.co.at
 * @author yevgenyk - Updated 28/04/2014 for IDEA 13
 */
public class IdeaClassBytesReader implements ClassBytesReader {

  private final CompileContext compileContext;

  private final Map<String, File> compiledClasses;

  public IdeaClassBytesReader(CompileContext compileContext, Map<String, File> compiledClasses) {
    this.compileContext = compileContext;
    this.compiledClasses = compiledClasses;
  }

  @Override
  public byte[] getClassBytes(String classNamePath, ClassLoader classLoader) {
    // Try to fetch the class from the list of files that we know was compiled during this run.
    final String className = classNamePath.replace('/', '.');
    final File compiledFile = compiledClasses.get(className);
    if (compiledFile != null) {
      try {
        return Files.toByteArray(compiledFile);
      } catch (IOException e) {
        warn("Error reading file contents: " + compiledFile);
        return null;
      }
    }

    // The class wasn't compiled on this run, look it up from the project structure (or it's dependencies).
    return lookupClassBytesFallback(classNamePath);
  }

  private byte[] lookupClassBytesFallback(String classNamePath) {

    // Create a Psi compatible className
    String className = classNamePath.replace('/', '.').replace('$', '.');

    GlobalSearchScope searchScope = GlobalSearchScope.allScope(compileContext.getProject());
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(compileContext.getProject());

    PsiClass psiClass = psiFacade.findClass(className, searchScope);
    if (psiClass == null) {
      warn("Couldn't find PsiClass for class: " + className);
      return null;
    }

    VirtualFile containingFile = psiClass.getContainingFile().getVirtualFile();
    if (containingFile == null) {
      warn("Couldn't find containing file for PsiClass: " + psiClass);
      return null;
    }

    VirtualFile classFile = getClassFile(containingFile, classNamePath);
    if (classFile == null) {
      warn("Couldn't find .class file for class: " + className);
      return null;
    }

    try {
      return classFile.contentsToByteArray();
    } catch (IOException e) {
      warn("Error reading file contents: " + classFile);
      return null;
    }
  }

  private VirtualFile getClassFile(VirtualFile containingFile, String classNamePath) {

    Module module = compileContext.getModuleByFile(containingFile);
    if (module == null) {
      // File is not linked to a project module - probably from a 3rd party .jar
      return containingFile;
    }

    VirtualFile classFile = getClassFileFromModule(module, classNamePath);
    if (classFile == null) {
      warn(module.getName() + ": Couldn't find compiled file for class: " + classNamePath);
      return containingFile;
    }
    return classFile;
  }

  private VirtualFile getClassFileFromModule(Module module, String classNamePath) {

    String classNamePathWithExtension = classNamePath + ".class";

    // Search the file in the module's main output directory.
    VirtualFile requiredFile = getClassFileFromModuleMainOutput(module, classNamePathWithExtension);
    if (requiredFile != null) {
      return requiredFile;
    }

    // File is not in the module's main output directory.
    // Search the file in the module's test output directory.
    return getClassFileFromModuleTestOutput(module, classNamePathWithExtension);
  }

  private VirtualFile getClassFileFromModuleMainOutput(Module module, String classNamePathWithExtension) {

    // Search the file in the module's main output directory.
    VirtualFile outputDirectory = compileContext.getModuleOutputDirectory(module);
    if (outputDirectory == null) {
      warn(module.getName() + ": Couldn't find main output directory!");
      return null;
    }

    return outputDirectory.findFileByRelativePath(classNamePathWithExtension);
  }

  private VirtualFile getClassFileFromModuleTestOutput(Module module, String classNamePathWithExtension) {
    // Search the file in the module's test output directory.
    VirtualFile outputDirectory = compileContext.getModuleOutputDirectoryForTests(module);
    if (outputDirectory == null) {
      warn(module.getName() + ": Couldn't find test output directory!");
      return null;
    }

    return outputDirectory.findFileByRelativePath(classNamePathWithExtension);
  }

  private void warn(String message) {
    compileContext.addMessage(CompilerMessageCategory.WARNING, message, null, -1, -1);
  }
}