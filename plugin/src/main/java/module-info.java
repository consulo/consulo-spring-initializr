import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-04-13
 */
@NullMarked
module consulo.spring.initializr {
    requires consulo.ide.api;
    requires consulo.module.creation.api;
    requires consulo.process.api;
    requires consulo.project.ui.api;
    requires consulo.spring.initializr.api;
    requires com.google.gson;

    opens consulo.spring.initializr.model to com.google.gson;
}