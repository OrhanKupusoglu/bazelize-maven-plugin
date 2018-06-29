# Bazelize Maven Plugin

This [Apache Maven](https://maven.apache.org/) plugin prepares scripts required for the [Google Bazel](https://bazel.build/) build tool.

Maven interprets the [Project Object Model](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html) configurations, contained in **pom.xml** files, which are essential for a Maven build.

> It is an XML file that contains information about the project and configuration details used by Maven to build the project.

Bazel uses [Skylark](https://docs.bazel.build/versions/master/skylark/language.html), syntactically a subset of both Python 2 and Python 3, which is optimized for configuration management.

> Skylark is designed to be small, simple, and thread-safe. Although it is inspired from Python, it is not a general-purpose language and most Python features are not included.

Migration from Maven to Bazel means basically generation of Skylark **WORKSPACE** and **BUILD** scripts from a set of **pom.xml** configuration files.

The main culprit for the migration from Maven to Bazel is the slowness of the Maven builds. With the current emphasis on DevOps and CI, this slowness can be considered as *highly unsatisfactory*.

Although equipped with an impressive feature set, **Google Bazel** faces the usual *unfamiliarity issues*. For Java developers, after so many years of Maven experience, Bazel looks like a time-consuming challenge from **Maven's declarative (what to do)** to **Bazel's imperative (how to do)** mindset.

&nbsp;

## Alternatives

Bazel itself provides a [systematic approach](https://docs.bazel.build/versions/master/migrate-maven.html) to migrate from Maven to Bazel. The required **WORKSPACE** script can be generated with [Migration tooling](https://github.com/bazelbuild/migration-tooling). The **BUILD** scripts require manual intervention.

The [migrator-maven-plugin](https://github.com/zmeggyesi/migrator-maven-plugin) by *Zalán Meggyesi* may be a good starting point.

&nbsp;

## Apache Maven

The plugin is built with the following Maven version:

```
$ mvn --version
Apache Maven 3.5.3 (3383c37e1f9e9b3bc3df5050c29c8aff9f295297; 2018-02-24T22:49:05+03:00)
Maven home: /home/me/dev/tools/apache-maven-3.5.3
Java version: 1.8.0_171, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-8-oracle/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "4.10.0-38-generic", arch: "amd64", family: "unix"
```

Due to its popularity, Maven has good [documentation](https://maven.apache.org/plugin-developers/index.html) and [Internet presence](https://books.sonatype.com/mvnref-book/reference/index.html). To see the command line help:

```
$ mvn --help
```

### Effective POM

A **POM** combines with other settings and profiles, and creates an **effective pom.xml**. The so-called [Effective POM](http://maven.apache.org/plugins/maven-help-plugin/effective-pom-mojo.html) gives details about the current configuration.

```
$ mvn help:effective-pom
$ mvn help:effective-pom -Doutput=pom-effective.xml
```

### Inspection

Maven has many built-in goals for inspection of the current project. Listed dependencies and plugins can be checked easily for updates, too.

```
$ mvn dependency:list
$ mvn dependency:resolve
$ mvn dependency:tree
$ mvn versions:display-dependency-updates
$ mvn versions:display-plugin-updates
```

### Maven Coordinates

[Semantic Versioning](https://semver.org/) is an important concept to follow. [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) uniquely identify a project, dependency, or plugin defined in a **POM** with `groupId:artifactId:version` properties. As a project grows, its version increases, too. Instead of changing versions with ad hoc *find-replace*, Maven provides goals with the [Versions Maven Plugin](http://www.mojohaus.org/versions-maven-plugin/).

```
# Enter the new version
$ mvn versions:set

# Set immediately
$ mvn versions:set -DnewVersion=0.3.1

# Revert back
$ mvn versions:revert

# Remove the 'pom.xml.versionsBackup' files.
$ mvn versions:commit
```

### Build

Typically a Maven project is built with **clean** and **install**.

```
$ mvn clean install
```
Tests are called automatically, but this step can be skipped with `maven.test.skip` property.

[Apache Maven Surefire](http://maven.apache.org/surefire/index.html) test framework contains [Maven Failsafe Plugin](http://maven.apache.org/surefire/maven-failsafe-plugin/index.html)  for unit tests  and  [Maven Surefire Plugin](http://maven.apache.org/surefire/maven-surefire-plugin/index.html) for integration tests. **Failsafe** can be configured with the `skipTests`  property. **Surefire** plugin introduces the `skipITs` property. In contrast to Maven's own `maven.test.skip` property, both plugins compile the tests, but do not run them.

```
$ mvn clean install -Dmaven.test.skip

# skip unit tests with Failsafe
$ mvn clean install -DskipTests

# skip integration tests with Surefire
$ mvn clean install -DskipITs
```

### Reports

The [Apache Maven Site Plugin](https://maven.apache.org/plugins/maven-site-plugin/) generates a Web site for the plugin, which includes the standard [Javadoc](https://en.wikipedia.org/wiki/Javadoc) pages by default.

If the [Apache Maven Plugin](https://maven.apache.org/plugin-tools/maven-plugin-plugin/) is repeated in the **<reporting>** element of the **pom.xml**, then extra **plugin documentation** pages with details about each goal are added. The documentation is generated by default in `${project.build.directory}/site` which is the **target/site** directory.

```
$ mvn site
```
The [Apache Maven Javadoc Plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/) can be explicitly configured to run during a build phase, of course. The plugin can be called by itself, too.


```
$ mvn javadoc:javadoc
$ mvn javadoc:test-javadoc
```

The [Apache Maven JXR Plugin](https://maven.apache.org/jxr/maven-jxr-plugin/) generates line-numbered HTML pages for each source file.

The [Apache Maven Documentation Checker Plugin](http://maven.apache.org/plugins/maven-docck-plugin/usage.html) rates the configuration for a better output. However the **check** fails quite easily.

```
$ mvn docck:check
```

### Sources

The [Apache Maven Source Plugin](https://maven.apache.org/plugins/maven-source-plugin/index.html) creates JAR archives of the source files of the current project in the **target** directory. If configured, **source:jar** goal is called during the **package/install** phases automatically.

```
$ mvn source:jar
```
In general, bundling Test sources and Javadoc pages is best left to their plugin's respective goals.

```
$ mvn source:test-jar
$ mvn site:jar
$ mvn javadoc:jar
$ mvn javadoc:test-jar
```
&nbsp;

## Google Bazel

For years Google was using an internal automation tool called **Blaze**. In March 2015 a subset of this tool is released as the open-source project **Bazel**. **Google Bazel** aims to build and test software of any size, quickly and reliably. The project's motto is:

> {Fast, Correct} - Choose two

Java is only one of the many languages Bazel supports.

[Building a Java project with Bazel](https://docs.bazel.build/versions/master/tutorial/java.html) requires one **WORKSPACE** file to identify project's root directory and one or many **BUILD** files containing build instructions. The **WORKSPACE** and **BUILD** files can even be empty, but their mere existence may fulfill the Bazel requirements.

```
$ bazel version
Build label: 0.14.1
Build target: bazel-out/k8-opt/bin/src/main/java/com/google/devtools/build/lib/bazel/BazelServer_deploy.jar
Build time: Fri Jun 8 12:17:35 2018 (1528460255)
Build timestamp: 1528460255
Build timestamp as int: 1528460255
```

As expected, Bazel has extensive [documentation](https://docs.bazel.build/versions/master/bazel-overview.html). Bazel's [target pattern syntax](https://docs.bazel.build/versions/master/command-line-reference.html#target-pattern-syntax) determines the way rules are referred to.

> All target patterns starting with '//' are resolved relative to the current workspace.

To see the command line help:
```
$ bazel help
```

#### Caveat

An important caveat is Bazel's lack of support for [OSGi bundles](https://www.osgi.org/). Built-in support for [Google Protocol Buffers](https://developers.google.com/protocol-buffers/) is now OK [for Java](https://blog.bazel.build/2017/02/27/protocol-buffers.html).

Unfortunately OSGi bundle JARs are required for the [Apache Karaf](https://karaf.apache.org/) runtime environment, and can be easily generated on Maven with [Apache Felix](http://felix.apache.org/).

Missing features can be added by writing [extensions](https://docs.bazel.build/versions/master/skylark/concepts.html).
> Bazel extensions are files ending in .bzl. Use the load statement to import a symbol from an extension.

&nbsp;

## Goals of the Bazelize Maven Plugin

The **Bazelize Maven Plugin** aims to generate all of the required **WORKSPACE** and **BUILD** scripts by processing each **pom.xml** configuration file with four successive [goals](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html). A further four goals are added for convenience.

| GOAL          | ORDER | DESCRIPTION                              |
| :------------ | ----- | ---------------------------------------- |
| **module**    | 1     | Traverses a project's source files, typically **src/main/java** directories, and saves one **tmp-bzl-module.json** file for each corresponding **pom.xml**. |
| **meta**      | 2     | Parses **tmp-bzl-module.json** files, each containing a module's meta data, and consolidates this data into a single **tmp-bzl-meta.json** file. |
| **build**     | 3     | Generates a **BUILD** file corresonding to a **pom.xml** consisting of *Bazel java_library* rules. Serializes dependency and server data to **tmp-bzl-dependency.json** and **tmp-bzl-server.json** files respectively. |
| **workspace** | 4     | Generates a **WORKSPACE** file to download all dependencies referred in **BUILD** files with *Bazel maven_server* and *maven_jar* rules. |
| **test**      | -     | Appends to **BUILD** files a *Bazel test rule* for each Java Test class. |
| **binary**    | -     | Appends to the root BUILD file a *Bazel binary rule*, which refers to all other Java libraries. Requires the main class for the **MANIFEST.MF** file, of course: **-DmainClass=com.mycompany.app.App** |
| **clean**     | -     | Cleans all temporary files. With **-Dexpunge** cleans **WORKSPACE** and **BUILD** files, too. |
| **help**      | -     | Displays help.                           |

The first four goals are expected to be called in succession. A single goal is a too big code with no granularity for each step, and calling other goals with **@Execute** [annotation](https://maven.apache.org/plugin-tools/maven-plugin-plugin/examples/using-annotations.html) works only just once.

The [Maven lifecycle extension](https://maven.apache.org/examples/maven-3-lifecycle-extensions.html) uses *sessions*, but found to be of limited use. Still it can be seen in action by declaring the plugin in the target project's **pom.xml**:

```
<project>
...
    <build>
        <extensions>
            <extension>
                <groupId>kupusoglu.orhan</groupId>
                <artifactId>bazelize-maven-plugin</artifactId>
                <version>0.2.1</version>
            </extension>
        </extensions>
    </build>
...
</project>
```

With this addition, calling the first goal and the third goal is enough:

```
$ mvn bazelize:module
$ mvn bazelize:build
```

&nbsp;

## Sample Migration

After installing the **Maven-Bazelize** plugin, a sample project is needed.

```
$ mvn clean install
```

With the [Maven Archetype Plugin](https://maven.apache.org/archetype/maven-archetype-plugin/index.html) Maven can generate a [sample project](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html), which just prints, not surprisingly, *Hello World*.

```
$ cd ..
$ mvn archetype:generate -DgroupId=com.mycompany.app \
                         -DartifactId=my-app \
                         -DarchetypeArtifactId=maven-archetype-quickstart \
                         -DinteractiveMode=false
$ cd my-app
$ mvn package
$ java -cp target/my-app-1.0-SNAPSHOT.jar com.mycompany.app.App
Hello World!
```

Now the four goals must be called like this:

```
$ mvn kupusoglu.orhan:bazelize-maven-plugin:module
$ mvn kupusoglu.orhan:bazelize-maven-plugin:meta
$ mvn kupusoglu.orhan:bazelize-maven-plugin:build
$ mvn kupusoglu.orhan:bazelize-maven-plugin:workspace
```

Clearly this a verbose process, which can be shortened with [Maven plugin prefix resolution](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html). Since the plugin follows the Maven convention of `${prefix}-maven-plugin` names, its **groupID** can be declared in the [Maven settings file](https://maven.apache.org/settings.html). The Maven settings file is located at `${user.home}/.m2/settings.xml`.

```
<settings>
...
    <pluginGroups>
        <pluginGroup>kupusoglu.orhan</pluginGroup>
    </pluginGroups>
...
</settings>
```

With this simplification the goals can be called succinctly:

```
$ mvn bazelize:module
$ mvn bazelize:meta
$ mvn bazelize:build
$ mvn bazelize:workspace
```

Now a **WORKSPACE** and **BUILD** script is generated, so building with Bazel is now possible.

With the other goals test and binary rules can be added, and clean removes the temporary JSON files:

```
$ mvn bazelize:help
$ mvn bazelize:test
$ mvn bazelize:binary -DmainClass=com.mycompany.app.App
$ mvn bazelize:clean
```

Let's test with Bazel:

```
$ bazel build ...
Starting local Bazel server and connecting to it...
.........
INFO: Analysed 3 targets (18 packages loaded).
INFO: Found 3 targets...
INFO: Elapsed time: 7.288s, Critical Path: 2.36s
INFO: 7 processes: 2 linux-sandbox, 2 local, 3 worker.
INFO: Build completed successfully, 16 total actions

# call all tests: bazel test ...
$ bazel test com.mycompany.app.AppTest
INFO: Analysed target //:com.mycompany.app.AppTest (0 packages loaded).
INFO: Found 1 test target...
Target //:com.mycompany.app.AppTest up-to-date:
  bazel-bin/com.mycompany.app.AppTest.jar
  bazel-bin/com.mycompany.app.AppTest
INFO: Elapsed time: 0.584s, Critical Path: 0.31s
INFO: 1 process, linux-sandbox.
INFO: Build completed successfully, 2 total actions
//:com.mycompany.app.AppTest                                             PASSED in 0.2s

INFO: Build completed successfully, 2 total actions

$ bazel run com.mycompany.app.App
INFO: Analysed target //:com.mycompany.app.App (0 packages loaded).
INFO: Found 1 target...
Target //:com.mycompany.app.App up-to-date:
  bazel-bin/com.mycompany.app.App.jar
  bazel-bin/com.mycompany.app.App
INFO: Elapsed time: 0.237s, Critical Path: 0.00s
INFO: 0 processes.
INFO: Build completed successfully, 1 total action
INFO: Build completed successfully, 1 total action
Hello World!

$ bazel build libcom_mycompany_app_my_app_1_0_SNAPSHOT-src.jar
INFO: Analysed target //:libcom_mycompany_app_my_app_1_0_SNAPSHOT-src.jar (0 packages loaded).
INFO: Found 1 target...
Target //:libcom_mycompany_app_my_app_1_0_SNAPSHOT-src.jar up-to-date:
  bazel-bin/libcom_mycompany_app_my_app_1_0_SNAPSHOT-src.jar
INFO: Elapsed time: 0.275s, Critical Path: 0.06s
INFO: 1 process, linux-sandbox.
INFO: Build completed successfully, 3 total actions

$ bazel build com.mycompany.app.App_deploy.jar
INFO: Analysed target //:com.mycompany.app.App_deploy.jar (0 packages loaded).
INFO: Found 1 target...
Target //:com.mycompany.app.App_deploy.jar up-to-date:
  bazel-bin/com.mycompany.app.App_deploy.jar
INFO: Elapsed time: 0.334s, Critical Path: 0.09s
INFO: 1 process, linux-sandbox.
INFO: Build completed successfully, 4 total actions

$ bazel test com.mycompany.app.AppTest-src.jar
INFO: Analysed target //:com.mycompany.app.AppTest-src.jar (0 packages loaded).
INFO: Found 1 target and 0 test targets...
Target //:com.mycompany.app.AppTest-src.jar up-to-date:
  bazel-bin/com.mycompany.app.AppTest-src.jar
INFO: Elapsed time: 0.282s, Critical Path: 0.07s
INFO: 1 process, linux-sandbox.
INFO: Build completed successfully, 3 total actions
INFO: Build completed successfully, 3 total actions

$ bazel test com.mycompany.app.AppTest_deploy.jar
INFO: Analysed target //:com.mycompany.app.AppTest_deploy.jar (0 packages loaded).
INFO: Found 1 target and 0 test targets...
Target //:com.mycompany.app.AppTest_deploy.jar up-to-date:
  bazel-bin/com.mycompany.app.AppTest_deploy.jar
INFO: Elapsed time: 0.253s, Critical Path: 0.07s
INFO: 1 process, linux-sandbox.
INFO: Build completed successfully, 3 total actions
INFO: Build completed successfully, 3 total actions

# after review
$ bazel clean --expunge
INFO: Starting clean (this may take a while). Consider using --async if the clean takes more than several minutes.
```
Bazel can create a [JAR file](https://docs.bazel.build/versions/master/be/java.html#java_binary) containing the sources collected from the transitive closure of the target:

```
$ bazel build com.mycompany.app.App_deploy-src.jar
$ bazel test com.mycompany.app.AppTest_deploy-src.jar
```

Bazel can [profile](https://docs.bazel.build/versions/master/skylark/performance.html) a build for [issues](https://www.kchodorow.com/blog/2015/09/18/build-y-u-go-slow/). An HTML page, here *myprofile.out.html*, can be generated to review the build in minute detail:

```
$ bazel build --profile=myprofile.out ...
$ bazel analyze-profile --html myprofile.out
```

### Script

Using [heredoc](https://en.wikipedia.org/wiki/Here_document) a simple shell script to call the goals can easily be prepared at target project's root:

```
$ echo << 'EOF' > bazelize.sh
> #!/bin/bash
>
> MAIN_CLASS=com.mycompany.app.App
> MAVEN_PLUGIN=kupusoglu.orhan:bazelize-maven-plugin
>
> mvn ${MAVEN_PLUGIN}:module
> mvn ${MAVEN_PLUGIN}:meta
> mvn ${MAVEN_PLUGIN}:build
> mvn ${MAVEN_PLUGIN}:workspace
> mvn ${MAVEN_PLUGIN}:clean
> mvn ${MAVEN_PLUGIN}:test
>
> if [[ ! -z $MAIN_CLASS ]]
> then
>     mvn ${MAVEN_PLUGIN}:binary -DmainClass=${MAIN_CLASS}
> fi
> EOF

$ chmox +x bazelize.sh
$ ./bazelize.sh
```

&nbsp;

## Next Steps

Unlike a [Hello World](https://en.wikipedia.org/wiki/%22Hello,_World!%22_program) application, a large Java Maven project like [ONOS SDN Controller](https://onosproject.org/) will require some tweaks.

### Options

The **module** goal can add or remove source file locations with the **whiteListPattern** and **blackListPattern** options respectively:

```
$ mvn bazelize:module -DwhiteListPattern=src/ DblackListPattern=/test|/integration-test|/target
```

The **build** goal can eliminate unwanted dependencies with the **blackListPattern** option:

```
$ mvn bazelize:build -DblackListPattern="^jdk_tools|^com_sun_tools"
```

The **workspace** goal may add name to the WORKSPACE:
```
$ mvn bazelize:workspace -DworkspaceName=my-app
$ head -n 1 WORKSPACE
workspace(name = "my-app")
```

### Files

Pre-arranged text files containing Bazel rules can simply be *prepended* or *appended* to **WORKSPACE** and specific **BUILD** scripts.

Another option is the **bzl-build-dependency.json** files. The **module** goal reads these files to add or remove source files, which are located in unusual directories. The **build** goal reads these files to add or remove dependencies to configure Bazel for correct builds .

```
$ cat bzl-build-dependency.json
{
    "srcWhiteList": ["src/main/java/org/project/white/net"],
    "srcBlackList": ["src/main/java/org/project/black/net"],
    "depBlackList": ["^jdk_tools", "^com_sun_tools"],
    "addDep": [":exp_rule"],
    "removeDep": ["@io_netty_netty_3_10_5_Final//jar",
                  "@io_netty_netty_all_4_0_31_Final//jar"]
}
```

| FILE                                    | DESCRIPTION                              |
| :-------------------------------------- | ---------------------------------------- |
| **bzl&dash;build-prepend&dash;txt**     | Prepend Bazel rules to a BUILD file      |
| **bzl&dash;build&dash;append.txt**      | Append Bazel rules to a BUILD file       |
| **bzl&dash;build&dash;dependency.json** | Inject or remove dependencies/sources from a BUILD file |
| **bzl-workspace-prepend.txt**           | Prepend Bazel rules to a WORKSPACE file  |
| **bzl-workspace-append.txt**            | Append Bazel rules to a WORKSPACE file   |
