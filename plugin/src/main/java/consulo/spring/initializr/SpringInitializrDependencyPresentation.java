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
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Rendering shared by the dependencies step and the catalog popup.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
final class SpringInitializrDependencyPresentation {
    private SpringInitializrDependencyPresentation() {
    }

    static void renderName(TextItemPresentation presentation,
                           DependencyItem dependency,
                           @Nullable String bootVersion,
                           boolean withDescription) {
        boolean compatible = dependency.isCompatibleWith(bootVersion);
        presentation.append(dependency.getName(), compatible ? TextAttribute.REGULAR : TextAttribute.GRAYED);

        if (!compatible && bootVersion != null) {
            presentation.append(" ");
            presentation.append(SpringInitializrLocalize.dependencyNotAvailable(bootVersion), TextAttribute.ERROR);
        }
        else if (withDescription) {
            LocalizeValue description = dependency.getDescription();
            if (description.isNotEmpty()) {
                presentation.append("  ");
                presentation.append(description, TextAttribute.GRAY);
            }
        }
    }

    static Map<DependencyItem, DependencyGroupItem> categoriesOf(MetadataResult metadata) {
        Map<DependencyItem, DependencyGroupItem> categories = new HashMap<>();
        for (DependencyGroupItem group : metadata.dependencies()) {
            for (DependencyItem dependency : group.content()) {
                categories.put(dependency, group);
            }
        }
        return categories;
    }

    static LocalizeValue categoryName(Map<DependencyItem, DependencyGroupItem> categories, DependencyItem dependency) {
        DependencyGroupItem group = categories.get(dependency);
        return group == null ? LocalizeValue.empty() : group.getName();
    }
}
