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

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.spring.initializr.localize.SpringInitializrMetadataLocalize;
import org.jspecify.annotations.Nullable;

/**
 * Metadata carries localize keys of {@link SpringInitializrMetadataLocalize} instead of text.
 */
public final class MetadataText {
    private MetadataText() {
    }

    public static LocalizeValue of(@Nullable String localizeKey) {
        if (localizeKey == null) {
            return LocalizeValue.empty();
        }
        return LocalizeKey.of(SpringInitializrMetadataLocalize.ID, localizeKey, 0).getValue();
    }
}
