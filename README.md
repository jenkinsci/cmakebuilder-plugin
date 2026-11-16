# Jenkins CMake Plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fcmakebuilder-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/cmakebuilder-plugin/job/master/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cmakebuilder.svg)](https://plugins.jenkins.io/cmakebuilder)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/cmakebuilder.svg?color=blue)](https://plugins.jenkins.io/cmakebuilder)

Build C/C++ and other [CMake](https://cmake.org/) projects in Jenkins across Freestyle and Pipeline jobs, with automatic CMake tool provisioning, CMake Presets (`--preset`), multi-configuration generator support (`--config`), and standalone `cmake`, `ctest`, and `cpack` steps.

- [Changelog](CHANGELOG.md)
- [Issue Tracker](https://github.com/jenkinsci/cmakebuilder-plugin/issues)

---

## Features

- **Pipeline & Freestyle Build Steps**: Configure, generate, and build CMake projects via `cmakeBuild` (Pipeline) or **CMake Build** (Freestyle), or invoke tools directly via `cmake`, `ctest`, and `cpack` steps.
- **CMake Presets Support (`--preset`)**: Run configure and build presets defined in `CMakePresets.json` / `CMakeUserPresets.json` (CMake 3.19+).
- **Multi-Configuration Generator Handling**: Automatically omits `-D CMAKE_BUILD_TYPE` at configure time and passes `--config <buildType>` during `cmake --build` for `Visual Studio *`, `Xcode`, `Ninja Multi-Config`, and `Green Hills MULTI`.
- **Automatic Tool Installation**: Downloads and caches official portable CMake binaries on demand from [cmake.org](https://cmake.org/files/) across Linux, macOS, and Windows agents.
- **Declarative `tools` & JCasC Support**: Configure `cmake` tool installations via Jenkins Configuration as Code (`tool: cmake:`) and reference them in Declarative `tools { cmake '...' }` blocks.

---

## Quick Start (Pipeline)

### 1. Standard Single-Config Build, Test & Package

```groovy
pipeline {
    agent any

    stages {
        stage('Configure & Build') {
            steps {
                cmakeBuild(
                    installation: 'InSearchPath',
                    generator: 'Ninja',
                    sourceDir: '.',
                    buildDir: 'build',
                    buildType: 'Release',
                    cleanBuild: true,
                    cmakeArgs: '-DBUILD_TESTING=ON',
                    steps: [
                        [withCmake: true]
                    ]
                )
            }
        }
        stage('Test') {
            steps {
                ctest(
                    installation: 'InSearchPath',
                    workingDir: 'build',
                    arguments: '--output-on-failure'
                )
            }
        }
        stage('Package') {
            steps {
                cpack(
                    installation: 'InSearchPath',
                    workingDir: 'build',
                    arguments: '-G TGZ'
                )
            }
        }
    }
}
```

### 2. Building with CMake Presets (`CMakePresets.json`)

```groovy
pipeline {
    agent any

    stages {
        stage('Preset Build') {
            steps {
                cmakeBuild(
                    installation: 'InSearchPath',
                    buildDir: '.',
                    preset: 'ci-release',
                    steps: [
                        [withCmake: true, preset: 'ci-release']
                    ]
                )
                ctest(
                    installation: 'InSearchPath',
                    workingDir: 'build/ci-release',
                    arguments: '--output-on-failure'
                )
            }
        }
    }
}
```

### 3. Multi-Configuration Generators (`Visual Studio`, `Xcode`, `Ninja Multi-Config`)

When using a multi-configuration generator, `buildType` is automatically passed as `--config <buildType>` to `cmake --build` when `withCmake: true` is set:

```groovy
cmakeBuild(
    installation: 'InSearchPath',
    generator: 'Ninja Multi-Config',
    sourceDir: 'src',
    buildDir: 'build',
    buildType: 'Release',
    cleanBuild: true,
    steps: [
        [withCmake: true, args: '--target all']
    ]
)

// Optionally build another configuration using the standalone cmake step:
cmake(
    installation: 'InSearchPath',
    workingDir: 'build',
    arguments: '--build . --config Debug'
)
```

### 4. Declarative `tools { cmake ... }` Block

If you want a configured CMake installation added directly to `PATH` for shell/bat steps as well as CMake steps, use the `tools` directive:

```groovy
pipeline {
    agent any

    tools {
        cmake 'cmake-3.31'
    }

    stages {
        stage('Build') {
            steps {
                sh 'cmake --version'
                sh 'cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release && cmake --build build'
            }
        }
    }
}
```

---

## Pipeline Step Reference

### `cmakeBuild`
- `installation` *(required)*: Name of the configured CMake tool installation (default `InSearchPath`).
- `sourceDir`: Directory containing `CMakeLists.txt` (relative to workspace). Optional when `preset` is specified.
- `buildDir`: Build output / working directory (relative to workspace).
- `generator`: CMake generator name (e.g., `Ninja`, `Ninja Multi-Config`, `Unix Makefiles`, `Visual Studio 17 2022`, `Xcode`).
- `buildType`: Build configuration (`Debug`, `Release`, `RelWithDebInfo`, `MinSizeRel`, or custom). Passed via `-D CMAKE_BUILD_TYPE=<buildType>` for single-config generators and `--config <buildType>` during `cmake --build` steps.
- `preset`: Configure preset name (`cmake --preset <name>`).
- `preloadScript`: Initial cache script passed via `-C <script>`.
- `cmakeArgs`: Additional command-line arguments passed to `cmake` at configure time.
- `cleanBuild`: When `true`, deletes `buildDir` prior to configure (safely skipped if `buildDir` equals `sourceDir`).
- `steps`: List of build tool invocations to run after configure. Each entry supports:
  - `withCmake`: Run via `cmake --build <dir>` (`true`) or invoke the detected `$CMAKE_BUILD_TOOL` directly (`false`).
  - `preset`: Build preset name (`cmake --build --preset <name>`).
  - `args`: Arguments passed to `cmake --build` or the build tool.
  - `envVars`: Newline-separated `KEY=VALUE` environment variables for the build step.

### `cmake` / `ctest` / `cpack`
- `installation` *(required)*: Name of the configured CMake tool installation.
- `workingDir`: Directory in which to run the tool (relative to workspace). For `ctest` and `cpack`, set this to your CMake build directory.
- `arguments`: Command-line arguments passed to the tool.
- `ignoredExitCodes`: Comma-separated list or ranges of non-zero exit codes to treat as success (e.g., `1,2` or `8`).

---

## Global Configuration & JCasC

To configure CMake tool installations automatically with the **Configuration as Code (JCasC)** plugin:

```yaml
tool:
  cmake:
    installations:
      - name: "InSearchPath"
      - name: "cmake-3.31"
        properties:
          - installSource:
              installers:
                - cmakeInstaller:
                    id: "3.31.6"
```

---

## Freestyle Job Configuration

1. Add a **CMake Build** step under **Build Steps** to configure and optionally run build tool invocations (`withCmake` or direct `$CMAKE_BUILD_TOOL`).
2. Optionally add **Invoke CMake**, **Invoke CTest**, or **Invoke CPack** steps (**Execute CMake / CPack / CTest**) to run standalone CMake suite tools in any working directory.

---

## Localizations

1. Chinese localization is maintained in `jenkinsci/localization-zh-cn-plugin`.
