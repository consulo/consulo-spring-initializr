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
import consulo.localize.LocalizeValue;
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.spring.initializr.model.MetadataItem;
import consulo.spring.initializr.model.MetadataResult;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.util.FormBuilder;
import consulo.ui.util.ShowNotifier;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-04-14
 */
public class SpringInitializrProjectOrModuleNameStep extends UnifiedProjectOrModuleNameStep<SpringInitializrNewModuleWizardContext> {
    private final SpringInitializrNewModuleWizardContext myContext;

    private @Nullable TextBox myGroupIdBox;
    private @Nullable TextBox myArtifactIdBox;
    private @Nullable TextBox myPackageNameBox;

    private @Nullable ComboBox<MetadataItem> myBootVersionBox;
    private @Nullable ComboBox<MetadataItem> myJavaVersionBox;
    private @Nullable ComboBox<MetadataItem> myLanguageBox;
    private @Nullable ComboBox<MetadataItem> myTypeBox;
    private @Nullable ComboBox<MetadataItem> myPackagingBox;

    public SpringInitializrProjectOrModuleNameStep(SpringInitializrNewModuleWizardContext context) {
        super(context);
        myContext = context;
    }

    @Override
    @RequiredUIAccess
    protected void extend(FormBuilder builder, Disposable uiDisposable) {
        myGroupIdBox = TextBox.create(myContext.getGroupId());
        builder.addLabeled(SpringInitializrLocalize.labelGroup(), myGroupIdBox);

        myArtifactIdBox = TextBox.create(myContext.getArtifactId()).withPlaceholder(SpringInitializrLocalize.placeholderArtifact());
        builder.addLabeled(SpringInitializrLocalize.labelArtifact(), myArtifactIdBox);

        myPackageNameBox = TextBox.create(myContext.getPackageName()).withPlaceholder(SpringInitializrLocalize.placeholderPackageName());
        builder.addLabeled(SpringInitializrLocalize.labelPackageName(), myPackageNameBox);

        LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(DockLayout.create(), uiDisposable);
        builder.setBottom(loadingLayout);

        ShowNotifier.once(loadingLayout, () -> loadingLayout.startLoading(SpringInitializrMetadataLoader::load, (layout, metadata) -> {
            if (metadata == null) {
                layout.center(Label.create(SpringInitializrLocalize.metadataLoadFailed()));
                return;
            }

            myContext.setMetadata(metadata);
            layout.center(buildOptions(metadata));
        }));
    }

    @RequiredUIAccess
    private Component buildOptions(MetadataResult metadata) {
        myBootVersionBox = createComboBox(metadata.bootVersions(), myContext.getBootVersion());
        myJavaVersionBox = createComboBox(metadata.javaVersions(), myContext.getJavaVersion());
        myLanguageBox = createComboBox(metadata.languages(), myContext.getLanguage());
        myPackagingBox = createComboBox(metadata.packagings(), myContext.getPackaging());
        myTypeBox = createComboBox(metadata.types(), myContext.getType());

        // two options per row - the wizard dialog is not tall enough for a row each;
        // the short labels go to the second column, the drop-downs do not shrink
        TableLayout options = TableLayout.create(StaticPosition.TOP);
        addOption(options, 0, 0, SpringInitializrLocalize.labelSpringBoot(), myBootVersionBox);
        addOption(options, 0, 2, SpringInitializrLocalize.labelJava(), myJavaVersionBox);
        addOption(options, 1, 0, SpringInitializrLocalize.labelLanguage(), myLanguageBox);
        addOption(options, 1, 2, SpringInitializrLocalize.labelType(), myTypeBox);
        addOption(options, 2, 0, SpringInitializrLocalize.labelPackaging(), myPackagingBox);
        return options;
    }

    @RequiredUIAccess
    private static void addOption(TableLayout layout, int row, int column, LocalizeValue text, ComboBox<MetadataItem> comboBox) {
        Label label = Label.create(text);
        label.setTarget(comboBox);
        layout.add(label, TableLayout.cell(row, column));
        layout.add(comboBox, TableLayout.cell(row, column + 1).fill());
    }

    @RequiredUIAccess
    private static ComboBox<MetadataItem> createComboBox(List<MetadataItem> items, @Nullable MetadataItem selected) {
        ComboBox<MetadataItem> comboBox = ComboBox.create(items);
        comboBox.setTextRenderer(item -> item == null ? LocalizeValue.empty() : item.getName());

        MetadataItem value = selected;
        if (value == null) {
            value = items.stream().filter(MetadataItem::defaultValue).findFirst().orElse(null);
        }
        if (value != null) {
            comboBox.setValue(value);
        }
        else {
            comboBox.selectFirst();
        }
        return comboBox;
    }

    @Override
    @RequiredUIAccess
    public void onStepLeave(SpringInitializrNewModuleWizardContext context) {
        // stores the name and the location
        super.onStepLeave(context);

        context.setGroupId(valueOf(myGroupIdBox));
        context.setArtifactId(valueOf(myArtifactIdBox));
        context.setPackageName(valueOf(myPackageNameBox));

        // the options are missing while the metadata is loading
        if (myBootVersionBox != null) {
            context.setBootVersion(myBootVersionBox.getValue());
        }
        if (myJavaVersionBox != null) {
            context.setJavaVersion(myJavaVersionBox.getValue());
        }
        if (myLanguageBox != null) {
            context.setLanguage(myLanguageBox.getValue());
        }
        if (myTypeBox != null) {
            context.setType(myTypeBox.getValue());
        }
        if (myPackagingBox != null) {
            context.setPackaging(myPackagingBox.getValue());
        }
    }

    private static String valueOf(@Nullable TextBox textBox) {
        return textBox == null ? "" : StringUtil.notNullize(textBox.getValue()).trim();
    }
}
