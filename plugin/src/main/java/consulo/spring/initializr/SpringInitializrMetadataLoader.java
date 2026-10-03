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

import com.google.gson.Gson;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.spring.initializr.model.MetadataResult;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;

/**
 * @author VISTALL
 * @since 2026-04-14
 */
public final class SpringInitializrMetadataLoader {
    private static final Logger LOG = Logger.getInstance(SpringInitializrMetadataLoader.class);

    // copied from src/main/dist into the plugin directory
    private static final String METADATA_FILE = "spring-initializr-metadata.json";

    private SpringInitializrMetadataLoader() {
    }

    public static File getMetadataFile() {
        return new File(PluginManager.getPluginPath(SpringInitializrMetadataLoader.class), METADATA_FILE);
    }

    public static @Nullable MetadataResult load() {
        File file = getMetadataFile();
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            return new Gson().fromJson(reader, MetadataResult.class);
        }
        catch (Exception e) {
            LOG.error("Failed to load Spring Initializr metadata from " + file, e);
            return null;
        }
    }
}
