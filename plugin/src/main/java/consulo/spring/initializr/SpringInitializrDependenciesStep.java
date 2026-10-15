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

import consulo.disposer.Disposable;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.spring.initializr.model.DependencyGroupItem;
import consulo.spring.initializr.model.DependencyItem;
import consulo.spring.initializr.model.MetadataItem;
import consulo.spring.initializr.model.MetadataResult;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TableItemRenderer;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.LabeledLayoutStyle;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-14
 */
public class SpringInitializrDependenciesStep implements WizardStep<SpringInitializrNewModuleWizardContext> {
    private @Nullable MutableFlatDataModel<DependencyItem> mySelected;
    private @Nullable JBPopup myCatalogPopup;

    @Override
    @RequiredUIAccess
    public Component getComponent(SpringInitializrNewModuleWizardContext context, Disposable uiDisposable) {
        MetadataResult metadata = context.getMetadata();
        if (metadata == null) {
            return Label.create(SpringInitializrLocalize.metadataLoadFailed());
        }

        MetadataItem bootVersionItem = context.getBootVersion();
        String bootVersion = bootVersionItem == null ? null : bootVersionItem.id();
        Map<DependencyItem, DependencyGroupItem> categories = SpringInitializrDependencyPresentation.categoriesOf(metadata);

        // the wizard asks for a new component each time the step is shown, the selection lives in the context
        MutableFlatDataModel<DependencyItem> selected = FlatDataModel.of(context.getSelectedDependencies());
        mySelected = selected;

        Table<DependencyItem> table = Table.create(selected);

        TableItemRenderer<DependencyItem, DependencyItem> nameRender =
            (presentation, item, dependency) -> SpringInitializrDependencyPresentation.renderName(presentation, dependency, bootVersion, false);
        table.addColumn(SpringInitializrLocalize.dependenciesColumnName(), dependency -> dependency).setRenderer(nameRender);

        TableItemRenderer<DependencyItem, DependencyItem> categoryRender = (presentation, item, dependency) ->
            presentation.append(SpringInitializrDependencyPresentation.categoryName(categories, dependency), TextAttribute.GRAY);
        table.addColumn(SpringInitializrLocalize.dependenciesColumnCategory(), dependency -> dependency).setWidth(220).setRenderer(categoryRender);

        Component decorated = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    // each catalog keeps its own rows, a second one stacked over the first would show stale checks
                    JBPopup shown = myCatalogPopup;
                    if (shown != null && shown.isVisible()) {
                        return;
                    }
                    myCatalogPopup = new SpringInitializrDependencyCatalogPopup(metadata, bootVersion, selected).show(e);
                }
            })
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();

        LabeledLayout layout = LabeledLayout.create(SpringInitializrLocalize.dependenciesTitle(), decorated);
        layout.addStyle(LabeledLayoutStyle.NO_INDENT);
        return layout;
    }

    @Override
    public void onStepLeave(SpringInitializrNewModuleWizardContext context) {
        MutableFlatDataModel<DependencyItem> selected = mySelected;
        if (selected == null) {
            return;
        }

        Set<DependencyItem> dependencies = context.getSelectedDependencies();
        dependencies.clear();
        for (DependencyItem dependency : selected) {
            dependencies.add(dependency);
        }
    }
}
