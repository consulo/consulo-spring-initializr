import org.jspecify.annotations.NullMarked;

/**
 * Loaded only when the Gradle plugin is enabled, see META-INF/plugin-requires.xml.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@NullMarked
module consulo.spring.initializr.gradle.impl {
    requires consulo.spring.initializr.api;
    requires consulo.external.system.api;
}
