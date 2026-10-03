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

import com.google.gson.annotations.SerializedName;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Single-select option of the metadata: Boot version, Java version, language, project type or packaging.
 *
 * @param tags set for project types only - {@code build} ("maven", "gradle") and {@code dialect}
 */
public record MetadataItem(String id, String nameId, @SerializedName("default") boolean defaultValue, Map<String, String> tags) {
    public MetadataItem {
        tags = tags == null ? Map.of() : tags;
    }

    public LocalizeValue getName() {
        return MetadataText.of(nameId);
    }

    public @Nullable String getBuildSystemId() {
        return tags.get("build");
    }
}
