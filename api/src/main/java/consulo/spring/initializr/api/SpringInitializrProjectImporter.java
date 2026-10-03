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
package consulo.spring.initializr.api;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Imports a generated Spring Boot project into its build system. Implementations live in optional jars,
 * so there is no importer when the build system plugin is not installed.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SpringInitializrProjectImporter {
    /**
     * @return value of the {@code build} tag of Initializr project types: {@code maven} or {@code gradle}
     */
    String getBuildSystemId();

    /**
     * Called once the generated files are visible in the VFS.
     *
     * @param module          module the wizard created, its content root is {@code projectDir}
     * @param projectDir      holds the generated build script
     * @param rootProjectName name of the generated root project - the artifactId
     */
    @RequiredUIAccess
    void importProject(Project project, Module module, VirtualFile projectDir, String rootProjectName);
}
