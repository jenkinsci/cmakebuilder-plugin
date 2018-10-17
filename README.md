[![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/cmakebuilder-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/cmakebuilder-plugin/)

This plugin oversees the launch of CMake based builds.

It provides a user interface for configuring the following parameters of a build step

1. CMake Buildscript Generator
2. Source directory
3. Build directory
4. CMake Build Type - Debug/Release/...
5. Cache file - to prepopulate cmake variables
6. Clean/Incremental build - to clean the build directory prior to buildscript generation
7. Pass arbitrary command-line arguments to cmake.

To perform the actual build, it can detect the actual build tool corresponding to the CMake Generator and

1. Can run the actual build tool as a sub-build step
2. Supports to run the build tool from an extra ```execute-shell``` \ ```ececute-batch``` build step (in case auto detection fails).

To ease the pain of provisioning Jenkins worker nodes, it can

- download a recent version of cmake on demand from [cmake.org](https://cmake.org/files/) and use that exclusively for the build.
Jenkins admins may configure the per-build-step-selectable CMake versions on the Jenkins` global configuration page. (Thanks to cmake.org for constantly providing portable/relocatable binaries for public download.)

Issues are tracked at the [Jenkins issue Tracker](https://issues.jenkins-ci.org/issues/?jql=component%20%3D%20cmakebuilder-plugin).

### Localizations
1. Chinese localization is maintaned in https://github.com/jenkinsci/localization-zh-cn-plugin.
