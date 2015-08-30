package org.avaje.idea.typequery.plugin;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maintains the per project activate flag and setup the compiler stuff appropriate
 */
@State(name = "avajeTypeQueryEnhancement", storages = {
  @Storage(id = "avajeTypeQueryEnhancement", file = StoragePathMacros.WORKSPACE_FILE)
})
public class EnhancementActionComponent implements ProjectComponent, PersistentStateComponent<EnhancementActionComponent.EnhancementState> {

  private final Project project;

  private final CompiledFileCollector compiledFileCollector;

  private final EnhancementState enhancementState;

  public EnhancementActionComponent(Project project) {
    this.project = project;
    this.compiledFileCollector = new CompiledFileCollector();
    this.enhancementState = new EnhancementState();
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "Ebean Type Query plugin";
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }

  @Override
  public void projectOpened() {
  }

  @Override
  public void projectClosed() {
    setEnabled(false);
  }

  public boolean isEnabled() {
    return enhancementState.enabled;
  }

  public void setEnabled(boolean enabled) {
    if (!this.enhancementState.enabled && enabled) {
      getCompilerManager().addCompilationStatusListener(compiledFileCollector);
    } else if (this.enhancementState.enabled && !enabled) {
      getCompilerManager().removeCompilationStatusListener(compiledFileCollector);
    }
    this.enhancementState.enabled = enabled;
  }

  private CompilerManager getCompilerManager() {
    return CompilerManager.getInstance(project);
  }

  @Nullable
  @Override
  public EnhancementState getState() {
    return enhancementState;
  }

  @Override
  public void loadState(EnhancementState ebeanEnhancementState) {
    setEnabled(ebeanEnhancementState.enabled);
    XmlSerializerUtil.copyBean(ebeanEnhancementState, this.enhancementState);
  }

  public static class EnhancementState {
    public boolean enabled;
  }
}
