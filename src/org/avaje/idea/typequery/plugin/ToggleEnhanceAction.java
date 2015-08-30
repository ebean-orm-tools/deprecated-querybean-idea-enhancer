/*
 * Copyright 2009 Mario Ivankovits
 *
 *     This file is part of Ebean-idea-plugin.
 *
 *     Ebean-idea-plugin is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Ebean-idea-plugin is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Ebean-idea-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

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