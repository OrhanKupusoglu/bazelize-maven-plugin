package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;
import kupusoglu.orhan.bazelize_maven_plugin.model.MavenMeta;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Goal <strong>module</strong>
 * <br>
 * Order: 1
 * <br>
 * Saves to an intermediate JSON file the current module's meta data:
 * <br>
 * <ul>
 * <li>rootDir</li>
 * <li>relDir</li>
 * <li>groupId</li>
 * <li>artifactId</li>
 * <li>version</li>
 * <li>packaging</li>
 * <li>source</li>
 * </ul>
 * <br>
 * <strong>-DwhiteListPattern="src/"</strong> determines the root directory.
 * <br>
 * <strong>-DblackListPattern="/test|/integration-test|/target"</strong> excludes directories.
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:module -DblackListPattern="api/src|/test|/integration-test|/target"
 * mvn bazelize:module
 * </pre>
 */
@Mojo(
    name = "module",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalModule extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * root dir of the current Maven project - to be used by LifeCycle
     * @parameter
     */
    @Parameter(property = "rootDir", defaultValue = ".")
    private String rootDir;

    /**
     * if true back up the 'BUILD' and 'tmp-bzl-meta.json' files - to be used by LifeCycle
     * @parameter
     */
    @Parameter(property = "backup", defaultValue = "false")
    private Boolean backup;

    /**
     * if empty set current timestamp as suffix - to be used by LifeCycle
     * @parameter
     */
    @Parameter(property = "suffix", defaultValue = "")
    private String suffix;

    /**
     * pattern for directories to include
     * @parameter
     */
    @Parameter(property = "whiteListPattern", defaultValue = "src/")
    private String whiteListPattern;

    /**
     * pattern for directories to exclude
     * @parameter
     */
    @Parameter(property = "blackListPattern", defaultValue = "/test|/integration-test|/target")
    private String blackListPattern;


    public void execute() throws MojoExecutionException {
        // for LifeCycle.afterSessionEnd()
        project.setContextValue("rootDir", rootDir);
        project.setContextValue("backup", backup);
        project.setContextValue("suffix", suffix);
        project.setContextValue("log", getLog());

        Path root = Paths.get(rootDir).normalize().toAbsolutePath();

        Path baseDir = project.getBasedir().toPath();
        String pathBase = baseDir + File.separator;
        Path relative = root.relativize(baseDir);

        String libName = Common.sanitize(project.getGroupId()
                                         + Common.getSepSanitize()
                                         + project.getArtifactId());

        getLog().info("library name = " + libName);

        String buildDependency = Common.readTextFile(Paths.get(pathBase + Common.INPUT_FILES.BUILD_DEPENDENCY));
        String finalWhiteListPattern = null;
        String finalBlackListPattern = null;

        if (buildDependency.isEmpty()) {
            finalWhiteListPattern = whiteListPattern;
            finalBlackListPattern = blackListPattern;
        } else {
            JSONObject data = new JSONObject(buildDependency);

            if (data.has("srcWhiteList")) {
                StringBuilder sbWhitePattern = new StringBuilder();

                JSONArray whiteList = data.getJSONArray("srcWhiteList");
                for (int i = 0; i < whiteList.length(); i++) {
                    sbWhitePattern.append(whiteList.getString(i));
                    sbWhitePattern.append(Common.getSepWhiteList());
                }

                if (sbWhitePattern.length() > 0) {
                    sbWhitePattern.setLength(sbWhitePattern.length() - 1);

                    if (whiteListPattern.isEmpty()) {
                        finalWhiteListPattern = sbWhitePattern.toString();
                    } else {
                        finalWhiteListPattern = sbWhitePattern.insert(0, whiteListPattern + Common.getSepWhiteList()).toString();
                    }
                }
            }

            if (data.has("srcBlackList")) {
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
        }

        try {
            MavenMeta meta = new MavenMeta(getLog(),
                                           root,
                                           relative,
                                           project.getGroupId(),
                                           project.getArtifactId(),
                                           project.getVersion(),
                                           project.getPackaging(),
                                           finalWhiteListPattern,
                                           finalBlackListPattern);

            getLog().info(meta.recordBazelSources());

            File fileMeta = new File(meta.retrieveAbsDir().toString()
                                     + File.separator
                                     + Common.OUTPUT_FILES.JSON_MODULE);

            FileWriter metaWriter = new FileWriter(fileMeta);
            metaWriter.write(MavenMeta.getMetaData(meta));
            metaWriter.flush();
            metaWriter.close();

            getLog().info("output:\n" + Common.getIndentOne() + fileMeta);
        } catch (IOException e) {
            getLog().error(e.getMessage());
        }
    }
}
