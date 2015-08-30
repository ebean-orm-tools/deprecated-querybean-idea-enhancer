package org.avaje.idea.typequery.plugin;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ActionRunner;
import org.avaje.ebean.typequery.agent.AgentManifestReader;
import org.avaje.ebean.typequery.agent.Transformer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This task actually hand all successfully compiled classes over to the Ebean Type Query agent
 * which performs it's enhancement.
 */
public class TypeQueryEnhancementTask {

  private static final int DEBUG = 2;

  private final CompileContext compileContext;

  private final Map<String, File> compiledClasses;

  public TypeQueryEnhancementTask(CompileContext compileContext, Map<String, File> compiledClasses) {
    this.compileContext = compileContext;
    this.compiledClasses = compiledClasses;
  }

  public void process() {
    try {
      ActionRunner.runInsideWriteAction(
        new ActionRunner.InterruptibleRunnable() {
          @Override
          public void run() throws Exception {
            doProcess();
          }
        }
      );
    } catch (Exception e) {
      e.printStackTrace();
      String msg = Arrays.toString(e.getStackTrace());
      compileContext.addMessage(CompilerMessageCategory.ERROR, e.getClass().getName() + ":" + e.getMessage() + msg, null, -1, -1);
    }
  }


  /**
   * Find the type query manifest files externally to the agent as classLoader getResources does
   * not work for the agent when run in the IDEA plugin.
   *
   * @return The packages containing type query beans (this is required for the enhancement).
   */
  private Set<String> findManifests() {

    Project project = compileContext.getProject();
    GlobalSearchScope searchScope = GlobalSearchScope.allScope(compileContext.getProject());

    AgentManifestReader manifestReader = new AgentManifestReader();

    PsiFile[] files = FilenameIndex.getFilesByName(project, "ebean-typequery.mf", searchScope);
    for (int i = 0; i <files.length ; i++) {
      manifestReader.addRaw(files[i].getText());
    }
    return manifestReader.getPackages();
  }

  private void doProcess() throws IOException, IllegalClassFormatException {

    Set<String> packages = findManifests();
    compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Ebean type query enhancement started with packages:"+packages, null, -1, -1);

    IdeaClassBytesReader classBytesReader = new IdeaClassBytesReader(compileContext, compiledClasses);
    IdeaClassLoader classLoader = new IdeaClassLoader(Thread.currentThread().getContextClassLoader(), classBytesReader);

    final Transformer transformer = new Transformer("debug=" + DEBUG, classLoader, packages);

    transformer.setLogout(new PrintStream(new ByteArrayOutputStream()) {
      @Override
      public void print(String message) {
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
      }

      @Override
      public void println(String message) {
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
      }
    });

    ProgressIndicator progressIndicator = compileContext.getProgressIndicator();
    progressIndicator.setIndeterminate(true);
    progressIndicator.setText("Ebean type query enhancement");

    InputStreamTransform isTransform = new InputStreamTransform(transformer, classLoader);


    for (Entry<String, File> entry : compiledClasses.entrySet()) {
      String className = entry.getKey();
      File file = entry.getValue();
      progressIndicator.setText2(className);

      byte[] transformed = isTransform.transform(className, file);
      if (transformed != null) {
        VirtualFile outputFile = VfsUtil.findFileByIoFile(file, true);
        if (outputFile == null) {
          compileContext.addMessage(CompilerMessageCategory.ERROR, "Ebean type query - outputFile not found writing " + className, null, -1, -1);
        } else {
          outputFile.setBinaryContent(transformed);
        }
      }
    }

    compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Ebean type query enhancement complete!", null, -1, -1);
  }
}
