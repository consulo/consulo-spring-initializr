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
package consulo.spring.initializr;

import consulo.application.progress.ProgressBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.spring.initializr.api.SpringInitializrProjectImporter;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the project into the content root of the wizard's module, then hands it to the build system importer.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class SpringInitializrProjectCreator {
    private static final Logger LOG = Logger.getInstance(SpringInitializrProjectCreator.class);

    private final ProgressBuilderFactory myProgressBuilderFactory;
    private final NotificationService myNotificationService;

    public SpringInitializrProjectCreator(ProgressBuilderFactory progressBuilderFactory, NotificationService notificationService) {
        myProgressBuilderFactory = progressBuilderFactory;
        myNotificationService = notificationService;
    }

    /**
     * Returns at once - the wizard calls this before the module is committed, generation and import run afterwards.
     */
    @RequiredUIAccess
    public void generateAndImport(Project project, Module module, VirtualFile projectDir, SpringInitializrGenerateRequest request) {
        Path outputDir = projectDir.toNioPath();

        myProgressBuilderFactory.newProgressBuilder(project, SpringInitializrLocalize.progressGenerating())
            .execute(project.getUIAccess(), () -> Coroutine
                .first(CodeExecution.<Object, SpringInitializrProjectGenerator.Result>apply(
                    input -> SpringInitializrProjectGenerator.generate(request, outputDir)
                ))
                // another process wrote the files, the importers look for them in the VFS
                .then(CompletableFutureStep.<SpringInitializrProjectGenerator.Result, SpringInitializrProjectGenerator.Result>await(
                    result -> refresh(projectDir, result)
                ))
                // not from the refresh callback - it runs in a write action, where importers must not start
                .then(UIAction.<SpringInitializrProjectGenerator.Result, SpringInitializrProjectGenerator.Result>apply(result -> {
                    onGenerated(project, module, projectDir, request, result);
                    return result;
                })))
            .whenComplete((result, error) -> {
                if (error != null) {
                    LOG.error("Spring Boot project generation failed", error);
                }
            });
    }

    private static CompletableFuture<SpringInitializrProjectGenerator.Result> refresh(VirtualFile projectDir,
                                                                                    SpringInitializrProjectGenerator.Result result) {
        if (result.error() != null) {
            return CompletableFuture.completedFuture(result);
        }

        CompletableFuture<SpringInitializrProjectGenerator.Result> future = new CompletableFuture<>();
        projectDir.refresh(true, true, () -> future.complete(result));
        return future;
    }

    @RequiredUIAccess
    private void onGenerated(Project project,
                             Module module,
                             VirtualFile projectDir,
                             SpringInitializrGenerateRequest request,
                             SpringInitializrProjectGenerator.Result result) {
        LocalizeValue error = result.error();
        if (error != null) {
            myNotificationService.newError(SpringInitializrNotificationGroupContributor.GROUP)
                .title(SpringInitializrLocalize.notificationGenerationFailedTitle())
                .content(error)
                .notify(project);
            return;
        }

        if (!result.warnings().isEmpty()) {
            myNotificationService.newWarn(SpringInitializrNotificationGroupContributor.GROUP)
                .title(SpringInitializrLocalize.notificationGenerationWarningsTitle())
                .content(LocalizeValue.of(String.join("\n", result.warnings())))
                .notify(project);
        }

        importProject(project, module, projectDir, request, result.artifactId());
    }

    @RequiredUIAccess
    private void importProject(Project project, Module module, VirtualFile projectDir, SpringInitializrGenerateRequest request, String artifactId) {
        String buildSystemId = request.buildSystemId();
        if (project.isDisposed() || buildSystemId == null) {
            return;
        }

        SpringInitializrProjectImporter importer = project.getApplication()
            .getExtensionPoint(SpringInitializrProjectImporter.class)
            .findFirstSafe(it -> buildSystemId.equals(it.getBuildSystemId()));
        if (importer == null) {
            myNotificationService.newInfo(SpringInitializrNotificationGroupContributor.GROUP)
                .title(SpringInitializrLocalize.notificationNoImporterTitle())
                .content(SpringInitializrLocalize.notificationNoImporterContent(StringUtil.capitalize(buildSystemId)))
                .notify(project);
            return;
        }

        importer.importProject(project, module, projectDir, artifactId);
    }
}
