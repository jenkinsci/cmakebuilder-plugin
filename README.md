[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fcmakebuilder-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/cmakebuilder-plugin/job/master/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cmakebuilder.svg)](https://plugins.jenkins.io/cmakebuilder)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/cmakebuilder-plugin.svg?label=changelog)](https://github.com/jenkinsci/cmakebuilder-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/cmakebuilder.svg?color=blue)](https://plugins.jenkins.io/cmakebuilder)

## About this plugin

This plugin can be used to build [cmake](https://cmake.org/) based projects
within Jenkins with free-style or pipeline jobs.  
It provides

-   a build-step that generates the build scripts from a *CMakeLists.txt* file
 and allows to run these with the appropriate build tool,
-   a build-step to invoke some tools of the CMake suite
 (CMake/CPack/CTest) with arbitrary arguments plus
-   automatic installation of the CMake tool suite,
-   compatibility with Jenkins pipeline,

------------------------------------------------------------------------

### Screenshots (for the impatient)

Screenshots showing the configuration can be viewed here [CMake Build
Configuration](https://wiki.jenkins.io/display/JENKINS/CMake+Build+Configuration),
here [Tool
Configuration](https://wiki.jenkins.io/display/JENKINS/Tool+Configuration)
and here [Global
Configuration](https://wiki.jenkins.io/display/JENKINS/Global+Configuration).

### Global configuration

To ease the pain of provisioning Jenkins worker nodes, the plugin can
download a recent version of cmake on demand from
[cmake.org](https://cmake.org/files/) and use that
exclusively for a build. (Thanks to cmake.org for constantly providing
portable/relocatable binaries for public download.)  
Jenkins admins may configure the available CMake versions on the [global
configuration page](https://wiki.jenkins.io/display/JENKINS/Global+Configuration).

### Build-script generator build step

This build step generates the build scripts from a CMakeLists.txt file
and is able to run the scripts. It accepts the [following
parameters](https://wiki.jenkins.io/display/JENKINS/CMake+Build+Configuration):

1.  CMake version to use — downloaded on demand, selectable versions get
    configured on the [global configuration
    page](https://wiki.jenkins.io/display/JENKINS/Global+Configuration)
2.  CMake Buildscript Generator
3.  Source directory
4.  Build directory
5.  CMake Build Type — Debug/Release/...
6.  Cache file — to pre-populate cmake cache variables
7.  Clean/Incremental build — to clean the build directory prior to
    buildscript generation
8.  Pass arbitrary command-line arguments to cmake.

To perform the actual build, this build step tries to detect the actual
build tool corresponding to the chosen CMake Buildscript Generator and

-   Can run the actual build tool as a sub-build step
    -   with arbitrary (but tool dependent) arguments
    -   with extra system environment variables.
-   Supports to run the build tool by an extra `execute-shell` or
    `execute-batch` build step (in case auto detection fails).

### CMake/CPack/CTest execution step

This build step allows to invoke the corresponding tool of the CMake
suite with arbitrary command-line arguments.  
It accepts the following [configuration parameters](https://wiki.jenkins.io/display/JENKINS/Tool+Configuration):

1.  CMake version to use
2.  Working directory
3.  Command-line arguments.

### Known Issues ([go to Tracker](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20%3D%20Open%20AND%20component%20%3D%20cmakebuilder-plugin))

Issues are tracked at the [Jenkins issue Tracker](https://issues.jenkins-ci.org/issues/?jql=component%20%3D%20cmakebuilder-plugin).

### Localizations
1. Chinese localization is maintained in /jenkinsci/localization-zh-cn-plugin.
