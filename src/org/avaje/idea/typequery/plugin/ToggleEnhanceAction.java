package org.avaje.idea.typequery.plugin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

/**
 * Action for toggling enhancement on and off
 */
public class ToggleEnhanceAction extends ToggleAction {
  @Override
  public boolean isSelected(AnActionEvent e) {
    final Project currentProject = e.getProject();
    if (currentProject != null && currentProject.hasComponent(EnhancementActionComponent.class)) {
      final EnhancementActionComponent action = currentProject.getComponent(EnhancementActionComponent.class);
      return action.isEnabled();
    }
    return false;
  }

  @Override
  public void setSelected(AnActionEvent e, boolean selected) {
    final Project currentProject = e.getProject();
    if (currentProject != null && currentProject.hasComponent(EnhancementActionComponent.class)) {
      final EnhancementActionComponent action = currentProject.getComponent(EnhancementActionComponent.class);
      action.setEnabled(selected);
    }
  }
}