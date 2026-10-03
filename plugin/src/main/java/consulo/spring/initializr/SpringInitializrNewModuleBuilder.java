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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.scratch.NewModuleBuilder;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.module.creation.scratch.NewModuleContext;
import consulo.module.creation.scratch.NewModuleContextGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.spring.initializr.icon.SpringInitializrIconGroup;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.ui.ex.wizard.WizardStep;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-04-14
 */
@ExtensionImpl(order = "after jvm")
public class SpringInitializrNewModuleBuilder implements NewModuleBuilder {
    private final SpringInitializrProjectCreator myProjectCreator;

    @Inject
    public SpringInitializrNewModuleBuilder(ProgressBuilderFactory progressBuilderFactory, NotificationService notificationService) {
        myProjectCreator = new SpringInitializrProjectCreator(progressBuilderFactory, notificationService);
    }

    @Override
    public void setupContext(NewModuleContext newModuleContext) {
        NewModuleContextGroup group = newModuleContext.findGroup("jvm");
        if (group == null) {
            return;
        }

        group.add(SpringInitializrLocalize.name(), SpringInitializrIconGroup.springboot(), new NewModuleBuilderProcessor<SpringInitializrNewModuleWizardContext>() {
            @Override
            public SpringInitializrNewModuleWizardContext createContext(boolean isNewProject) {
                return new SpringInitializrNewModuleWizardContext(isNewProject);
            }

            @Override
            public void buildSteps(Consumer<WizardStep<SpringInitializrNewModuleWizardContext>> consumer,
                                   SpringInitializrNewModuleWizardContext context) {
                consumer.accept(new SpringInitializrProjectOrModuleNameStep(context));
                consumer.accept(new SpringInitializrDependenciesStep());
            }

            @RequiredReadAction
            @Override
            public void process(SpringInitializrNewModuleWizardContext context,
                                ContentEntry contentEntry,
                                ModifiableRootModel modifiableRootModel) {
                VirtualFile projectDir = contentEntry.getFile();
                SpringInitializrGenerateRequest request = context.createRequest();
                if (projectDir == null || request == null) {
                    return;
                }

                myProjectCreator.generateAndImport(modifiableRootModel.getProject(), modifiableRootModel.getModule(), projectDir, request);
            }
        });
    }
}
