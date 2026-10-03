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
package consulo.spring.initializr.model;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @param compatibleBootVersions Boot versions start.spring.io offers this dependency for
 */
public record DependencyItem(String id, String nameId, @Nullable String descriptionId, List<String> compatibleBootVersions) {
    public DependencyItem {
        compatibleBootVersions = compatibleBootVersions == null ? List.of() : compatibleBootVersions;
    }

    public boolean isCompatibleWith(@Nullable String bootVersion) {
        return bootVersion == null || compatibleBootVersions.contains(bootVersion);
    }

    public LocalizeValue getName() {
        return MetadataText.of(nameId);
    }

    public LocalizeValue getDescription() {
        return MetadataText.of(descriptionId);
    }
}
