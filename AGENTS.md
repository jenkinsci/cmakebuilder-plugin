# AGENTS.md — Jenkins CMake Plugin (`cmakebuilder`)

## Overview
- **Plugin ID**: `cmakebuilder`
- **Repository**: `jenkinsci/cmakebuilder-plugin`
- **Java Package**: `hudson.plugins.cmake`
- **Jenkins Baseline**: `2.492.3` (`org.jenkins-ci.plugins:plugin:6.x`)

Provides CMake, CTest, and CPack build steps and Pipeline steps (`cmakeBuild`, `cmake`, `ctest`, `cpack`), along with automatic CMake tool installation (`CmakeTool`, `CmakeInstaller`), CMake `--preset` support, and multi-config generator (`Visual Studio *`, `Xcode`, `Ninja Multi-Config`) `buildType` handling.

---

## Build, Test & Verification Commands

### Prerequisites
- **JDK**: Java 17 (recommended for local verification with SpotBugs `4.7.x` / `4.9.x`) or Java 21
- **Maven**: Apache Maven `3.9+`
- **Optional**: Local `cmake`, `ctest`, and `cpack` binaries on `PATH` (integration tests in `*BuildTest` automatically skip via `CmakeNotInstalledIgnoreCondition.assumeCmakeInstalled()` if `cmake` is not installed).

### Core Maven Commands
```bash
# Full build, unit/form tests, InjectedTest, SpotBugs, and Enforcer checks
mvn clean verify

# Run unit tests quickly (skipping slow InjectedTest Jelly scan during development)
mvn test -Dtest=!InjectedTest

# Run a specific test class
mvn test -Dtest=CmakeBuilderTest
mvn test -Dtest=CmakeBuilderFormRoundTripTest

# Run SpotBugs and Maven Enforcer checks directly
mvn spotbugs:check enforcer:enforce

# Package the .hpi plugin artifact without running tests
mvn clean package -DskipTests

# Start a local Jenkins instance at http://localhost:8080/jenkins
mvn hpi:run
```

---

## Architecture & Source Layout

- `src/main/java/hudson/plugins/cmake/`
  - `CmakeBuilder.java` & `AbstractCmakeBuilder.java` — Freestyle `Builder` generating and building CMake projects (`installationName`, `generator`, `buildDir`, `sourceDir`, `buildType`, `preset`, `cmakeArgs`, `preloadScript`, `cleanBuild`, `steps`).
  - `CmakeBuilderStep.java` — Pipeline `SimpleBuildStep` (`cmakeBuild`) mirroring `CmakeBuilder` for declarative and scripted Pipelines.
  - `BuildToolStep.java` — Nested build tool invocation step supporting both direct build-tool invocation (`make`, `ninja`) and `cmake --build <dir> [--preset <preset>] [--config <buildType>]` (`withCmake: true`).
  - `CToolBuilder.java`, `AbstractToolStep.java`, `CMakeStep.java`, `CTestStep.java`, `CPackStep.java` — Standalone Freestyle and Pipeline steps (`cmake`, `ctest`, `cpack`).
  - `LaunchUtils.java` — Constructs `ArgumentListBuilder` commands for `cmake` configure calls (`buildCMakeCall`), `cmake --build` calls (`buildBuildToolCallWithCmake`), and multi-config generator detection (`isMultiConfigGenerator`).
  - `CmakeTool.java` & `CmakeInstaller.java` — Global tool installation (`@Symbol("cmake")`) and downloadable archive installer from `cmake.org` (`@Symbol("cmakeInstaller")`) supporting Declarative `tools { cmake ... }` and JCasC.
- `src/main/resources/` & `src/main/webapp/html/help/`
  - `lib/cmake/common-fields.jelly`, `hudson/plugins/cmake/*/config.jelly` — Freestyle and Snippet Generator UI definitions.
  - `src/main/webapp/html/help/builder/*.html` & `src/main/webapp/html/help/tool/*.html` — Field help documentation.
- `src/test/java/hudson/plugins/cmake/` (JUnit 5 / `@WithJenkins`; JUnit 4 imports banned via `ban-junit4-imports.skip=false`)
  - `CmakeBuilderTest.java`, `CmakeBuilderFormRoundTripTest.java`, `CToolBuilderFormRoundTripTest.java`, `BuildToolStepTest.java`, `IntSetTest.java`, `CmakeToolSymbolAndCascTest.java`, `CmakeBuilderBuildTest.java`, `CmakeBuilderBuildStepTest.java`, `CToolBuilderBuildTest.java`.

---

## Coding, Remoting & Security Guardrails

1. **Jenkins Remoting & Stream Flushing**:
   - Always use `hudson.FilePath` and `hudson.Launcher` for workspace directory creation and tool invocation on agents.
   - Always flush `listener.getLogger().flush()` after `Launcher.ProcStarter.join()` completes so build log output is never truncated on macOS/Unix agents.
2. **Multi-Config vs Single-Config Generators**:
   - Single-config generators (`Unix Makefiles`, `Ninja`, etc.) use `-D CMAKE_BUILD_TYPE=<buildType>` at configure time.
   - Multi-config generators (`Visual Studio *`, `Xcode`, `Ninja Multi-Config`, `Green Hills MULTI`) must omit `-D CMAKE_BUILD_TYPE` during generation and pass `--config <buildType>` during `cmake --build` (`LaunchUtils.isMultiConfigGenerator`).
3. **UI & Dependency Standards**:
   - Never use raw emojis in UI templates or strings, and never use native browser `alert()`, `confirm()`, or `prompt()`.
   - Keep BOM/parent POM updates separate from feature/bugfix PRs unless required by an API change.
