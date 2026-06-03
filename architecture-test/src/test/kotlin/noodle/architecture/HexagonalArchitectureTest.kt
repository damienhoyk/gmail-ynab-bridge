package noodle.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
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

    @Test
    fun `no lateral infrastructure-to-infrastructure dependencies across applications`() {
        val scope = Konsist.scopeFromProject()
        val applicationNames = applicationNames(scope)

        // No infrastructure file should import from another application's infrastructure.
        // Shared integration modules (noodle.<integration>.infrastructure.*) have no core
        // package and are not applications, so importing them is allowed.
        scope.files
            .filter { it.packagee?.name?.contains(".infrastructure.") == true }
            .forEach { file ->
                val appName =
                    file.packagee
                        ?.name
                        ?.split(".")
                        ?.getOrNull(1) // "noodle.<app>.infrastructure..."

                file.imports.forEach { import ->
                    val importName = import.name

                    if (importName.startsWith("noodle.") && importName.contains(".infrastructure.")) {
                        val importAppName = importName.split(".").getOrNull(1)

                        require(importAppName == appName || importAppName !in applicationNames) {
                            "File ${file.name} in $appName infrastructure imports from $importAppName infrastructure: $importName"
                        }
                    }
                }
            }
    }

    @Test
    fun `adapter classes should only import from own-app core, drivers, or integrations`() {
        val scope = Konsist.scopeFromProject()
        val applicationNames = applicationNames(scope)

        // Adapter classes should only import from own-app core, drivers, or integrations
        scope.files
            .filter { it.packagee?.name?.contains(".infrastructure.") == true }
            .forEach { file ->
                val appName =
                    file.packagee
                        ?.name
                        ?.split(".")
                        ?.getOrNull(1) // "noodle.<app>.infrastructure..."

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

                        // Reject other application's infrastructure packages. Shared integration
                        // modules have no core package, so they are not applications and are allowed.
                        if (importName.contains(".infrastructure.")) {
                            val importAppName = importSegments.getOrNull(1)
                            if (importAppName != appName && importAppName in applicationNames) {
                                require(false) {
                                    "File ${file.name} in $appName infrastructure imports from $importAppName infrastructure: $importName"
                                }
                            }
                        }
                    }
                }
            }
    }

    // An application owns a core package; shared integration modules do not. The set of
    // application names lets the rules above distinguish a forbidden cross-application
    // infrastructure import from an allowed shared-integration import.
    private fun applicationNames(scope: KoScope): Set<String> =
        scope.files
            .filter { it.packagee?.name?.contains(".core.") == true }
            .mapNotNull {
                it.packagee
                    ?.name
                    ?.split(".")
                    ?.getOrNull(1)
            }.toSet()
}
