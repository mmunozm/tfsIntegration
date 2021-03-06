/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.ui.UpdateSettingsForm;

import javax.swing.*;
import java.util.Map;

public class UpdateConfigurable implements Configurable {

  private final Project myProject;
  private final Map<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings> myWorkspaceSettings;

  private UpdateSettingsForm myUpdateSettingsForm;

  public UpdateConfigurable(Project project, Map<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings> workspaceSettings) {
    myProject = project;
    myWorkspaceSettings = workspaceSettings;
  }

  @Override
  public void apply() throws ConfigurationException {
    myUpdateSettingsForm.apply(TFSProjectConfiguration.getInstance(myProject));
  }

  @Override
  public void reset() {
    myUpdateSettingsForm.reset(TFSProjectConfiguration.getInstance(myProject));
  }

  @Override
  public String getDisplayName() {
    return TFSBundle.message("configurable.TFSUpdateConfigurable.display.name");
  }

  @Override
  public JComponent createComponent() {
    myUpdateSettingsForm = new UpdateSettingsForm(myProject, getDisplayName(), myWorkspaceSettings);
    return myUpdateSettingsForm.getPanel();
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void disposeUIResources() {
    myUpdateSettingsForm = null;
  }

}
