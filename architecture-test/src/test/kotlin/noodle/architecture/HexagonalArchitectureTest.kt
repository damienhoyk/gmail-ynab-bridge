package noodle.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class HexagonalArchitectureTest {
    @Test
    fun `core packages should not import from infrastructure or bootstrap`() {
        val scope = Konsist.scopeFromProject()

        // All files in core packages should not import infrastructure or bootstrap
        scope.files
            .filter { it.packagee?.name?.contains(".core.") == true }
            .forEach { file ->
                file.imports.forEach { import ->
                    require(
                        !import.name.contains(".infrastructure.") &&
                            !import.name.contains(".bootstrap."),
                    ) {
                        "File ${file.name} in core package imports: ${import.name}"
                    }
                }
            }
    }

    @Test
    fun `core domain should not import from core service`() {
        val scope = Konsist.scopeFromProject()

        // Files in core.domain should not import core.service
        scope.files
            .filter { it.packagee?.name?.contains(".core.domain.") == true }
            .forEach { file ->
                file.imports.forEach { import ->
                    require(!import.name.contains(".core.service.")) {
                        "File ${file.name} in domain imports service: ${import.name}"
                    }
                }
            }
    }

    @Test
    fun `nothing should import from bootstrap`() {
        val scope = Konsist.scopeFromProject()

        // No file should import from bootstrap packages
        scope.files
            .forEach { file ->
                file.imports.forEach { import ->
                    require(!import.name.contains(".bootstrap.")) {
                        "File ${file.name} imports bootstrap: ${import.name}"
                    }
                }
            }
    }

    @Disabled(
        "Known violation: gmailsync-api imports TokenInfoResponse from oauth-api. " +
            "This lateral infra→infra dependency will be resolved by the compliance refactor. " +
            "This rule is present to be enabled once that refactor removes the cross-app coupling.",
    )
    @Test
    fun `no lateral infrastructure-to-infrastructure dependencies across applications`() {
        val scope = Konsist.scopeFromProject()

        // No infrastructure file should import from another application's infrastructure
        scope.files
            .filter { it.packagee?.name?.contains(".infrastructure.") == true }
            .forEach { file ->
                val packageSegments = file.packagee?.name?.split(".") ?: emptyList()
                val appName = packageSegments.getOrNull(1) // "noodle.<app>.infrastructure..."

                file.imports.forEach { import ->
                    val importName = import.name

                    if (importName.startsWith("noodle.") && importName.contains(".infrastructure.")) {
                        val importAppSegments = importName.split(".")
                        val importAppName = importAppSegments.getOrNull(1)

                        require(importAppName == appName) {
                            "File ${file.name} in $appName infrastructure imports from $importAppName infrastructure: $importName"
                        }
                    }
                }
            }
    }

    @Disabled(
        "Known violation: gmailsync-api imports TokenInfoResponse from oauth-api's infrastructure module. " +
            "Adapter classes should only import from their own application's core, drivers (noodle.<driver>), " +
            "or integrations (noodle.<integration>.infrastructure.*) — not from another application's infrastructure package. " +
            "This violation will be resolved by the compliance refactor.",
    )
    @Test
    fun `adapter classes should only import from own-app core, drivers, or integrations`() {
        val scope = Konsist.scopeFromProject()

        // Adapter classes should only import from own-app core, drivers, or integrations
        scope.files
            .filter { it.packagee?.name?.contains(".infrastructure.") == true }
            .forEach { file ->
                val packageSegments = file.packagee?.name?.split(".") ?: emptyList()
                val appName = packageSegments.getOrNull(1) // "noodle.<app>.infrastructure..."

                file.imports.forEach { import ->
                    val importName = import.name

                    if (importName.startsWith("noodle.")) {
                        val importSegments = importName.split(".")

                        // Allow own-app core packages
                        if (importName.startsWith("noodle.$appName.core.")) {
                            return@forEach
                        }

                        // Allow drivers (noodle.<segment> where segment has no dots)
                        if (importSegments.size == 2 && importSegments[0] == "noodle") {
                            return@forEach
                        }

                        // Reject other app's infrastructure packages
                        if (importName.contains(".infrastructure.")) {
                            val importAppName = importSegments.getOrNull(1)
                            if (importAppName != appName && importAppName != null) {
                                require(false) {
                                    "File ${file.name} in $appName infrastructure imports from $importAppName infrastructure: $importName"
                                }
                            }
                        }
                    }
                }
            }
    }
}
