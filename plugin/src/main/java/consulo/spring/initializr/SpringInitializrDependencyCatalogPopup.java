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

import consulo.localize.LocalizeValue;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.spring.initializr.model.DependencyGroupItem;
import consulo.spring.initializr.model.DependencyItem;
import consulo.spring.initializr.model.MetadataResult;
import consulo.ui.CheckBox;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Length;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TableItemRenderer;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Searchable catalog of all dependencies, checking one adds it to the step's table at once.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
final class SpringInitializrDependencyCatalogPopup {
    private final MetadataResult myMetadata;
    private final @Nullable String myBootVersion;
    private final MutableFlatDataModel<DependencyItem> mySelected;
    private final Map<DependencyItem, DependencyGroupItem> myCategories;

    SpringInitializrDependencyCatalogPopup(MetadataResult metadata, @Nullable String bootVersion, MutableFlatDataModel<DependencyItem> selected) {
        myMetadata = metadata;
        myBootVersion = bootVersion;
        mySelected = selected;
        myCategories = SpringInitializrDependencyPresentation.categoriesOf(metadata);
    }

    @RequiredUIAccess
    JBPopup show(AnActionEvent e) {
        MutableFlatDataModel<DependencyItem> catalog = FlatDataModel.of(filter(""));

        Table<DependencyItem> table = Table.create(catalog);
        table.setShowHeader(false);

        table.addColumn(LocalizeValue.empty(), this::isSelected)
            .setWidth(40)
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(DependencyItem dependency) {
                    return CheckBox.create(LocalizeValue.empty(), isSelected(dependency));
                }

                @RequiredUIAccess
                @Override
                public void commit(DependencyItem dependency, @Nullable Boolean value) {
                    setSelected(dependency, Boolean.TRUE.equals(value));
                    catalog.update(dependency);
                }

                @Override
                public boolean isEditable(DependencyItem dependency) {
                    return dependency.isCompatibleWith(myBootVersion);
                }
            })
            .setRenderer(ComponentItemRenderer.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()))
            ));

        TableItemRenderer<DependencyItem, DependencyItem> nameRender =
            (presentation, item, dependency) -> SpringInitializrDependencyPresentation.renderName(presentation, dependency, myBootVersion, true);
        table.addColumn(LocalizeValue.empty(), dependency -> dependency).setRenderer(nameRender);

        TableItemRenderer<DependencyItem, DependencyItem> categoryRender = (presentation, item, dependency) ->
            presentation.append(SpringInitializrDependencyPresentation.categoryName(myCategories, dependency), TextAttribute.GRAY);
        table.addColumn(LocalizeValue.empty(), dependency -> dependency).setWidth(170).setRenderer(categoryRender);

        table.addDoubleClickListener(event -> {
            DependencyItem dependency = event.getValue();
            if (dependency != null && dependency.isCompatibleWith(myBootVersion)) {
                setSelected(dependency, !isSelected(dependency));
                catalog.update(dependency);
            }
        });

        TextBox searchBox = TextBox.create().withPlaceholder(SpringInitializrLocalize.dependenciesSearchPlaceholder());
        searchBox.addValueListener(event -> catalog.replaceAll(filter(StringUtil.notNullize(event.getValue()))));

        DockLayout content = DockLayout.create();
        content.top(searchBox);
        content.center(ScrollableLayout.create(table));
        // the unified popup takes the size of its content
        content.setWidth(Length.ofFont(30));
        content.setHeight(Length.ofFont(18));

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, searchBox)
            .setTitle(SpringInitializrLocalize.dependenciesPopupTitle())
            .setResizable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .createPopup();
        popup.showUnderneathOf(e);
        return popup;
    }

    private boolean isSelected(DependencyItem dependency) {
        return mySelected.indexOf(dependency) >= 0;
    }

    @RequiredUIAccess
    private void setSelected(DependencyItem dependency, boolean selected) {
        if (selected == isSelected(dependency)) {
            return;
        }

        if (selected) {
            mySelected.add(dependency);
        }
        else {
            mySelected.remove(dependency);
        }
    }

    private List<DependencyItem> filter(String text) {
        String filter = text.trim().toLowerCase(Locale.ROOT);

        List<DependencyItem> result = new ArrayList<>();
        for (DependencyGroupItem group : myMetadata.dependencies()) {
            for (DependencyItem dependency : group.content()) {
                if (filter.isEmpty() || matches(dependency, group, filter)) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    private static boolean matches(DependencyItem dependency, DependencyGroupItem group, String filter) {
        return dependency.id().contains(filter)
            || dependency.getName().get().toLowerCase(Locale.ROOT).contains(filter)
            || dependency.getDescription().get().toLowerCase(Locale.ROOT).contains(filter)
            || group.getName().get().toLowerCase(Locale.ROOT).contains(filter);
    }
}
