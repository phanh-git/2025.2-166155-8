package com.importorder.boundary;

import io.github.palexdev.materialfx.theming.CSSFragment;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.scene.Scene;

final class MaterialTheme {

    private static final CSSFragment MATERIAL_THEME = UserAgentBuilder.builder()
            .themes(MaterialFXStylesheets.DEFAULT, MaterialFXStylesheets.LEGACY)
            .setDeploy(false)
            .build();

    private MaterialTheme() {
    }

    static void apply(Scene scene) {
        MATERIAL_THEME.applyOn(scene);
    }
}
