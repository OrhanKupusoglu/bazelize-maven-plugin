package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;
import kupusoglu.orhan.bazelize_maven_plugin.model.MavenDependency;
import kupusoglu.orhan.bazelize_maven_plugin.model.MavenServer;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;


/**
 * Goal <strong>build</strong>
 * <br>
 * Order: 3
 * <br>
 * For each <strong>pom.xml</strong> generates a <strong>BUILD</strong> script
 * <br>
 * Dependencies can be removed with <strong>-DblackListPattern</strong>.
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:build -DblackListPattern="^jdk_tools|^com_sun_tools"
 * mvn bazelize:build
 * </pre>
 */
@Mojo(
    name = "build",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalBuild extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    private Settings settings;

    /**
     * path of the settings file relative to local repo
     * @parameter
     */
    @Parameter(property = "settingsFile", defaultValue = "../settings.xml")
    private String settingsFile;

    /**
     * if true back up the 'BUILD' files
     * @parameter
     */
    @Parameter(property = "backup", defaultValue = "true")
    private Boolean backup;

    /**
     * if empty set current timestamp as suffix
     * @parameter
     */
    @Parameter(property = "suffix", defaultValue = "")
    private String suffix;

    /**
     *add dependency to the black list to be ignored
     * @parameter
     */
    @Parameter(property = "blackListPattern", defaultValue = "^jdk_tools")
    private String blackListPattern;

    /**
     * default remote repository
     * @parameter
     */
    @Parameter(property = "defaultServer", defaultValue = "central")
    private String defaultServer;

    /**
     * set scope of the Maven dependency
     * @parameter
     */
    @Parameter(property = "addScope", defaultValue = "true")
    private Boolean addScope;

    /**
     * add hash of the Maven dependency
     * @parameter
     */
    @Parameter(property = "addHash", defaultValue = "false")
    private Boolean addHash;

    /**
     * add remote server of the Maven dependency
     * @parameter
     */
    @Parameter(property = "addServer", defaultValue = "false")
    private Boolean addServer;

    /**
     * path of the resource files
     * @parameter
     */
    @Parameter(property = "resMain", defaultValue = "src/main/resources")
    private String resMain;

    private SortedSet<MavenDependency> allDependencies = new TreeSet<>();
    private SortedSet<MavenServer> allServers = new TreeSet<>();
    private LocalDateTime dateTime = LocalDateTime.now();


    // sha1() in com.google.common.hash.Hashing has been deprecated
    @SuppressWarnings("deprecation")
    public void execute() throws MojoExecutionException {
        // for LifeCycle.afterSessionEnd()
        if (Common.getProjectData("suffix") == null) { //project.isExecutionRoot()) {
            project.setContextValue("rootDir", project.getBasedir().getAbsolutePath());
            project.setContextValue("backup", backup);
            project.setContextValue("log", getLog());

            String finalSuffix = null;

            if (backup) {
                finalSuffix = Common.getBackupSuffix(suffix);
            }

            // for BUILD files
            Common.setProjectData("suffix", finalSuffix);
            // for WORKSPACE file
            project.setContextValue("suffix", finalSuffix);

            getLog().info("suffix: " + finalSuffix);
        }

        SortedSet<String> removeDep = new TreeSet<>();
        StringBuilder build = new StringBuilder();
        String finalBlackListPattern = null;

        String pathBase = project.getBasedir().getPath() + File.separator;

        String buildPrepend = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.BUILD_PREPEND));
        String buildAppend = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.BUILD_APPEND));
        String buildDependency = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.BUILD_DEPENDENCY));

        if (buildDependency.isEmpty()) {
            finalBlackListPattern = blackListPattern;
        } else {
            JSONObject data = new JSONObject(buildDependency);

            if (data.has("depBlackList")) {
                StringBuilder sbBlackPattern = new StringBuilder();

                JSONArray blackList = data.getJSONArray("srcBlackList");
                for (int i = 0; i < blackList.length(); i++) {
                    sbBlackPattern.append(blackList.getString(i));
                    sbBlackPattern.append(Common.getSepBlackList());
                }

                if (sbBlackPattern.length() > 0) {
                    sbBlackPattern.setLength(sbBlackPattern.length() - 1);

                    if (blackListPattern.isEmpty()) {
                        finalBlackListPattern = sbBlackPattern.toString();
                    } else {
                        finalBlackListPattern = sbBlackPattern.insert(0, blackListPattern + Common.getSepBlackList()).toString();
                    }
                }
            }

            if (data.has("addDep")) {
                JSONArray add = data.getJSONArray("addDep");
                for (int i = 0; i < add.length(); i++) {
                    build.append(Common.getIndentTwo());
                    build.append("\"");
                    build.append(add.getString(i));
                    build.append("\",\n");
                }
            }

            if (data.has("removeDep")) {
                JSONArray remove = data.getJSONArray("removeDep");
                for (int i = 0; i < remove.length(); i++) {
                    removeDep.add(remove.getString(i));
                }
            }
        }

        Set<Artifact> artifacts = project.getArtifacts();

        String libName = Common.sanitize(project.getGroupId()
                                         + Common.getSepSanitize()
                                         + project.getArtifactId()
                                         + Common.getSepSanitize()
                                         + project.getVersion());


        String baseDir = project.getBasedir().getAbsolutePath();
        Common.setProjectData("baseDir", baseDir);

        getLog().info("reading from root directory: "
                      + Paths.get(baseDir, Common.OUTPUT_FILES.JSON_META.toString()).normalize().toAbsolutePath());

        Common.setBlackListPattern(finalBlackListPattern);

        String localRepo = settings.getLocalRepository();
        String pathSettings = localRepo + File.separator + settingsFile;
        String pathNormSettings = Paths.get(File.separator, pathSettings).normalize().toString();

        // read repository server entries
        allServers.clear();
        for (Profile profile : settings.getProfiles()) {
            for (Repository repository : profile.getRepositories()) {
                String repoId = repository.getId();
                String repoUrl = repository.getUrl();
                allServers.add(new MavenServer(repoId, repoUrl, pathNormSettings));
            }
        }

        // read JAR entries
        allDependencies.clear();
        for (Artifact arti : artifacts) {
            MavenDependency mavenDependency;
            File file = arti.getFile();
            String hash = "";

            mavenDependency = new MavenDependency(arti.getGroupId(), arti.getArtifactId(), arti.getVersion());

            if (this.addScope) {
                mavenDependency.setScope(arti.getScope());
            }

            if (this.addHash) {
                try {
                    byte[] contents = Files.toByteArray(file);
                    hash = Hashing.sha1().hashBytes(contents).toString();
                } catch (IOException e) {
                    throw new MojoExecutionException("Dependency could not be hashed!", e);
                }

                mavenDependency.setHash(hash);
            }

            if (this.addServer) {
                File remotes = new File(file.getParent() + File.separator + "_remote.repositories");

                String remoteDescriptorContent;

                try {
                    remoteDescriptorContent = Files.toString(remotes, StandardCharsets.UTF_8);

                    getLog().debug(remoteDescriptorContent);

                    Matcher jarServerMatcher = Common.getPatternSettingsJar().matcher(remoteDescriptorContent);

                    if (jarServerMatcher.find()) {
                        String server = jarServerMatcher.group(1);

                        if (server != null) {
                            mavenDependency.setServer(server);
                        } else {
                            mavenDependency.setServer(defaultServer);
                        }
                    } else {
                        mavenDependency.setServer(defaultServer);
                    }
                } catch (IOException e) {
                    getLog().warn("could not locate repository file for " + arti.getArtifactId()
                                                                          + ", setting to default server: " + defaultServer);
                    mavenDependency.setServer(defaultServer);
                }
            }

            allDependencies.add(mavenDependency);
        }

        // GENERATE OUTPUT
        String pathBuild = pathBase + Common.OUTPUT_FILES.BUILD;
        String pathDependency = pathBase + Common.OUTPUT_FILES.JSON_DEPENDENCY;
        String pathServer = pathBase + Common.OUTPUT_FILES.JSON_SERVER;

        if (backup) {
            Common.renameFileIfExists(pathBuild, Common.getProjectData("suffix"));
        }

        File fileBuild = new File(pathBuild);
        File fileDependency = new File(pathDependency);
        File fileServer = new File(pathServer);

        Common.Dependency metaDep = Common.queryLibrary(libName);

        String contentLibrary = Common.getTemplateLibrary();
        String resFiles= Common.getResources(resMain);

        StringBuilder jsonDependency = new StringBuilder();
        StringBuilder jsonServer = new StringBuilder();

        jsonDependency.append("[");
        jsonServer.append("[");

        try (
            FileWriter buildWriter = new FileWriter(fileBuild);
        ) {
            if (metaDep == null || metaDep.getLabel().isEmpty()) {
                if (!buildPrepend.isEmpty()) {
                    buildWriter.append(buildPrepend);
                    buildWriter.append("\n");
                }

                getLog().info("output:\n" + Common.getIndentOne() + pathBuild);
            } else {
                FileWriter dependencyWriter = new FileWriter(fileDependency);
                FileWriter serverWriter = new FileWriter(fileServer);

                for (MavenDependency dep : allDependencies) {
                    String depName = Common.sanitize(dep.getGroupId()
                                                     + Common.getSepSanitize()
                                                     + dep.getArtifactId()
                                                     + Common.getSepSanitize()
                                                     + dep.getVersion());

                    if (!Common.isBlackListed(depName)) {
                        JSONObject jsonObject = new JSONObject(dep);

                        jsonDependency.append(jsonObject.toString());
                        jsonDependency.append(",");

                        Common.Dependency currDep = Common.queryLibrary(dep.getName());

                        if (currDep == null || currDep.getLabel().isEmpty()) {
                            if (!removeDep.contains("@" + dep.getName() + "//jar")) {
                                build.append(Common.getIndentTwo());
                                build.append("\"@");
                                build.append(dep.getName());
                                build.append("//jar\",");
                                build.append("\n");
                            }
                        } else {
                            if (!removeDep.contains(currDep.getLabel())) {
                                build.append(Common.getIndentTwo());
                                build.append("\"");
                                build.append(currDep.getLabel());
                                build.append("\",");
                                build.append("\n");
                            }
                        }
                    }
                }

                for (MavenServer srv : allServers) {
                    JSONObject jsonObject = new JSONObject(srv);

                    jsonServer.append(jsonObject.toString());
                    jsonServer.append(",");
                }

                // 1. file: Bazel BUILD
                if (!buildPrepend.isEmpty()) {
                    buildWriter.append(buildPrepend);
                    buildWriter.append("\n");
                }

                buildWriter.append(contentLibrary.replaceFirst("#LIB_NAME#",
                                                               libName)
                                                 .replaceFirst("#SRCS_GLOB#",
                                                               Common.getGlobSources(metaDep.getSources()))
                                                 .replaceFirst("#RES_FILES#",
                                                               resFiles)
                                                 .replaceFirst("#JAVA_DEPS#",
                                                               Common.removeLastChars(build.toString(), 1)));

                if (!buildAppend.isEmpty()) {
                    buildWriter.append(buildAppend);
                }

                // 2. file: JSON serialize JARs - to be used in Bazel WORKSPACE file
                if (jsonDependency.length() > 1) {
                    jsonDependency.setLength(jsonDependency.length() - 1);
                }
                jsonDependency.append("]");

                dependencyWriter.append(jsonDependency);
                dependencyWriter.close();

                // 3. file: JSON serialize servers - to be used in Bazel WORKSPACE file
                if (jsonServer.length() > 1) {
                    jsonServer.setLength(jsonServer.length() - 1);
                }
                jsonServer.append("]");

                serverWriter.append(jsonServer);
                serverWriter.close();

                getLog().info("output:\n" + Common.getIndentOne() + pathServer + "\n"
                                          + Common.getIndentOne() + pathDependency + "\n"
                                          + Common.getIndentOne() + pathBuild);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage());
        }
    }
}
