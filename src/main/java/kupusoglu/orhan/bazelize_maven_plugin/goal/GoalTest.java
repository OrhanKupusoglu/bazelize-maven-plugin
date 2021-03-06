package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.time.LocalDateTime;


/**
 * Goal <strong>test</strong>
 * <br>
 * Adds <strong>java_test()</strong> rules to BUILD scripts
 * <br>
 * <strong>-DsrcTest=src/test/java</strong> determines the root directory.
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:test
 * mvn bazelize:test
 * </pre>
 */
@Mojo(
    name = "test",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalTest extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * if true back up the 'BUILD' files
     * @parameter
     */
    @Parameter(property = "backup", defaultValue = "false")
    private Boolean backup;

    /**
     * if empty set current timestamp as suffix
     * @parameter
     */
    @Parameter(property = "suffix", defaultValue = "")
    private String suffix;

    /**
     * path of the test source files
     * @parameter
     */
    @Parameter(property = "srcTest", defaultValue = "src/test/java")
    private String srcTest;

    /**
     * path of the test resource files
     * @parameter
     */
    @Parameter(property = "resTest", defaultValue = "src/test/resources")
    private String resTest;


    public void execute() throws MojoExecutionException {
        String finalSuffix = null;
        String ctxSuffix = (String)project.getContextValue("suffix");

        if (backup) {
            finalSuffix = Common.getBackupSuffix(suffix, ctxSuffix);
        }

        if (srcTest == null || srcTest.isEmpty()) {
            getLog().error("Unknown test source: use -DsrcTest=src/test/java\n");
            System.exit(2);
        } else {
            try {
                Common.generateTest(getLog(),
                                    project.getBasedir().getAbsolutePath(),
                                    srcTest,
                                    resTest,
                                    finalSuffix);
            } catch (MojoExecutionException e) {
                getLog().error(e.getMessage());
            }
        }
    }
}
