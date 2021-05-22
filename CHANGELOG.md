## CMake plug-in Change Log

### Newer versions

See [GitHub releases](https://github.com/jenkinsci/cmakebuilder-plugin/releases)

### 2.6.3

Release date: 09 Nov 2019

- Moved docs to GitHub.

### 2.6.2

Release date: 07 Aug 2019

- FIX: The build message *Cleaning build dir* now prints on a line of its own.
- CHANGE: Online help was re-written and clarified.

### 2.6.1

Release date: 31 Mar 2019

- Internationalized to support Chinese localization. Chinese localization is maintained in
 [https://github.com/jenkinsci/localization-zh-cn-plugin](https://github.com/jenkinsci/localization-zh-cn-plugin/tree/master/plugins/cmakebuilder-plugin).
- FIX: User-defined Environment variables are not passed to steps within withCmake 
 (Thanks to Diorcet Yann).
- CHANGE: The plugin now requires Java 8 to run.
- CHANGE: Various non-user-facing changes in the testing code.

### 2.6.0

Release date: 21 Jun 2018

- FIXED [JENKINS-51904](https://issues.jenkins-ci.org/browse/JENKINS-51904):
    cmake install from archive paths are wrong.  
    Compatibility note: To be compatible with the *install from
    archive*-installer, it is no longer possible to specify a
    non-default tool name (e.g. *mingw32-cmake* instead of *cmake)* as
    the *Path to cmake* on the global tool configuration page. Consider
    using a cmake toolchain file instead.

This change **affects only** cmake installations that do **not install
automatically**. Any default *InSearchPath* installation with a tool
name of just *cmake* is auto-migrated. Anyway, You will find messages in
the Jenkins log if the cmake installation cannot be auto-migrated.

- FIXED [JENKINS-52104](https://issues.jenkins-ci.org/browse/JENKINS-52104):
    Plugin always fills arguments field with "all" for build tool.

### 2.5.2

Release date: 05 May 2018

- FIXED [JENKINS-51060](https://issues.jenkins-ci.org/browse/JENKINS-51060):
  Environment variables are not passed to cmake steps when running in pipeline withEnv.

### 2.5.1

Release date: 31 Mar 2018

- FIXED The Pipeline snippet generator duplicates argument 'cmakeArgs' as 'arguments
 and 'buildDir as 'workingDir'.

### 2.5.0

Release date: 27 Mar 2018

- FIXED [JENKINS-34998](https://issues.jenkins-ci.org/browse/JENKINS-34998): Make
 CMake plugin compatible with pipeline.

DO NOT USE WITH PIPELINE: The Pipeline snippet generator duplicates
argument 'cmakeArgs' as 'arguments and 'buildDir as 'workingDir'.

### 2.4.6

Release date: 11 Feb 2018

- FIXED [JENKINS-48102](https://issues.jenkins-ci.org/browse/JENKINS-48102):
    CMake plugin leaks file handle on CMakeCache.txt.

### 2.4.5

Release date: 04 Apr 2017

- FIXED [JENKINS-43175](https://issues.jenkins-ci.org/browse/JENKINS-43175):
    NPE at hudson.plugins.cmake.CmakeBuilder.perform()

### 2.4.4

Release date: 20 Sep 2016

- FIXED [JENKINS-38227](https://issues.jenkins-ci.org/browse/JENKINS-38227):
    Allow letting CMake choose the (default) generator.

### 2.4.3

Release date: 26 Jun 2016

- FIXED [JENKINS-35911](https://issues.jenkins-ci.org/browse/JENKINS-35911):
    Allow to ignore failure exit codes from CMake/CPack/CTest build step.

### 2.4.2

Release date: 09 May 2016

- Upgraded to new parent pom.
- Require Java 7 to run the plugin now.
- Integrated Findbugs and fixed potential errors discovered by it.
- CLOSED JENKINS--29142 (cannot be fixed, but mentioning workaround in online help)
- FIXED [JENKINS-34613:](https://issues.jenkins-ci.org/browse/JENKINS-34613)
 Set default value of Generator in Java instead of jelly.

### 2.4.1

Release date: 24 Feb 2016

- FIXED JENKINS-32657, reopened: Loss of cmake-argument and working
 directory settings in existing CMake build steps.

### 2.4.0

Release date: 6 Feb 2016 DO NOT INSTALL!

DO NOT INSTALL: Existing build steps will loose their cmake-argument and
working directory settings.  
For those who have it installed: Do not run *Manage Old Data* from the
Jenkins Management page.

- Added build step which allows to invoke some tools of the CMake
    suite (CMake/CPack/CTest) with arbitrary arguments.
- FIXED JENKINS-32657, FIXED JENKINS-30695

### 2.3.3

Release date: 20 Nov 2015

- Build tool step no longer discards the environment set up.with
    EnvInject. This allows to apply a workaround for  
    [JENKINS-30114](https://issues.jenkins-ci.org/browse/JENKINS-30114).
    (Thanks to Armin Novak for reporting and testing on Windows.)

### 2.3.2

Release date: 9 Oct 2015

- FIXED: JENKINS-30070: Fix path to cmake binary for Mac OS X (Thanks
    to Guillaume Egles for testing under OS X).

### 2.3.1

Release date: 4 Oct 2015

- FIXED: JENKINS-30070: Fix 64 bit arch for Mac OS X.

### 2.3

Release date: 11 Sep 2015

- FIXED: JENKINS-30070: Automatic download and installation of cmake
    from cmake.org. Can download and install cmake 2.6 and above for
    Linux, Windows, OS X, SunOS, AIX, HPUX, Irix and FreeBSD (Tested on
    linux and windows).
- Improved help texts.
- Minor tweaks in error reporting.

### 2.2

Release date: 28 Aug 2015

- FIXED: JENKINS-29329: Visual Studio/MSBuild projects can not be
    built implicitly anymore JENKINS-29267 (Thanks to Dominik Acri for
    testing under windows).
- Improved online help.
- Changed build message wording from ERROR to WARNING if
    CMAKE\_BUILD\_TOOL cannot be determined to avoid end-user confusion.

### 2.1

Release date: 11 Jul 2015

- FIXED: JENKINS-29267 CMAKE\_BUILD\_TOOL is deprecated in cmake version 3.0

### 2.0

Release date: 5 Jul 2015

This version is almost a complete re-write. Unfortunately, due to its
new set of features,  
it will be incompatible with older versions.  
Changes include

- Require Jenkins 1.580.3
- Use the standard ToolInstallation mechanism to select the cmake
    version per-build-step (similar to Ant and Maven).
- Eliminated mis-use of CMAKE\_INSTALL\_PREFIX.
- Gets the actual build tool name and path from CMakeCache.txt and
    exposes it as environment variable \`CMAKE\_BUILD\_TOOL\` for subsequent build
    steps. Users no longer have to care for the name of the build tool.
- Allow to run the build tool multiple times with different targets.
- Allow to pass extra environment variables to the build tool (e.g. DESTDIR=some/dir).

### 1.10

Release date: 14 Jun 2015

- New maintainer 15knots.
- FIXED: Expand environment variables in definition of CMake generator
    ([JENKINS-13049](https://issues.jenkins-ci.org/browse/JENKINS-13049))
- FIXED: Matrix build labels not expanded by CMake plugin
    ([JENKINS-8538](https://issues.jenkins-ci.org/browse/JENKINS-8538))
- ENHANCEMENT: Removed restriction to be applicable to only free-style or matrix project.

### 1.9

Release date: 17 Apr 2011
