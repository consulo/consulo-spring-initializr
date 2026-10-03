/*
 * Copyright 2013-2026 consulo.io
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
package consulo.spring.initializr.gradle.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModuleWithNameAlreadyExistsException;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.spring.initializr.api.SpringInitializrProjectImporter;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collections;

/**
 * Links the generated project to Gradle and imports it, like importing an existing Gradle project does.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class GradleSpringInitializrProjectImporter implements SpringInitializrProjectImporter {
    // id of consulo.gradle.GradleConstants#SYSTEM_ID - Gradle classes are not exported, the manager is looked up by id
    private static final String GRADLE_SYSTEM_ID = "GRADLE";

    @Override
    public String getBuildSystemId() {
        return "gradle";
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void importProject(Project project, Module module, VirtualFile projectDir, String rootProjectName) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(GRADLE_SYSTEM_ID);
        if (manager == null) {
            return;
        }
        ProjectSystemId systemId = manager.getSystemId();

        // Gradle matches modules by name, the wizard's module is reused only if it carries the root project name
        renameModule(project, module, rootProjectName);

        String projectPath = ExternalSystemApiUtil.toCanonicalPath(projectDir.getPath());
        AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, systemId);
        if (settings.getLinkedProjectSettings(projectPath) == null) {
            ExternalProjectSettings projectSettings = ExternalSystemApiUtil.getSettingsConfigurableFactoryStrict(systemId).createProjectSettings();
            projectSettings.setExternalProjectPath(projectPath);
            settings.linkProject(projectSettings);
        }

        // suppresses the "unlinked Gradle project" notification
        project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);

        ExternalSystemProjectRefresher.getInstance().refreshProject(project, systemId, projectPath, new ExternalProjectRefreshCallback() {
            @Override
            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
                if (externalProject == null) {
                    return;
                }

                ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
                    @RequiredUIAccess
                    @Override
                    public void execute() {
                        ProjectDataManager projectDataManager = project.getApplication().getInstance(ProjectDataManager.class);
                        ProjectRootManager.getInstance(project).mergeRootsChangesDuring(
                            () -> projectDataManager.importData(externalProject.getKey(), Collections.singleton(externalProject), project, true)
                        );
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage, @Nullable String errorDetails) {
                // the refresher reports failures itself
            }
        }, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
    }

    @RequiredUIAccess
    private static void renameModule(Project project, Module module, String newName) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        if (module.isDisposed() || module.getName().equals(newName) || moduleManager.findModuleByName(newName) != null) {
            return;
        }

        ModifiableModuleModel modifiableModel = moduleManager.getModifiableModel();
        try {
            modifiableModel.renameModule(module, newName);
        }
        catch (ModuleWithNameAlreadyExistsException e) {
            modifiableModel.dispose();
            return;
        }
        WriteAction.runAndWait(modifiableModel::commit);
    }
}
