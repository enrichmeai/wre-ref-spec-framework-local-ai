package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.PlatformRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** The framework's standards, checked on this service's classes, generated ones included. */
class PlatformRulesTest {
    @Test
    void followsThePlatformRules() {
        PlatformRules.check(new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.enrichmeai.ref.shipping"));
    }
}
