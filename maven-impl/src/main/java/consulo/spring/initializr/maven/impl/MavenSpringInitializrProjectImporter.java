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
package consulo.spring.initializr.maven.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.module.Module;
import consulo.project.Project;
import consulo.spring.initializr.api.SpringInitializrProjectImporter;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class MavenSpringInitializrProjectImporter implements SpringInitializrProjectImporter {
    @Override
    public String getBuildSystemId() {
        return "maven";
    }

    @RequiredUIAccess
    @Override
    public void importProject(Project project, Module module, VirtualFile projectDir, String rootProjectName) {
        VirtualFile pom = projectDir.findChild("pom.xml");
        if (pom == null) {
            return;
        }

        // the same as the Maven new module wizard: an incremental update right after the first registration
        // finds nothing changed and leaves the module unconfigured, a forced one imports it.
        // Maven matches modules by pom location: pom.xml sits in the content root of the wizard's module, so it is reused
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
        manager.addManagedFilesOrUnignoreNoUpdate(List.of(pom));
        manager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
    }
}
