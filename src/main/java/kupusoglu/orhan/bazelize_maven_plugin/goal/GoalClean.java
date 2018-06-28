package kupusoglu.orhan.bazelize_maven_plugin.goal;

import kupusoglu.orhan.bazelize_maven_plugin.model.Common;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;


/**
 * Goal <strong>clean</strong>
 * <br>
 * Deletes non-essential output files: <strong>BUILD</strong> and <strong>WORKSPACE</strong> scripts are essential
 * <br>
 * <strong>ATTENTION:</strong> with option <strong>-Dexpunge</strong> essental scripts are also deleted
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:clean
 * mvn bazelize:clean
 * </pre>
 */
@Mojo(
    name = "clean",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalClean extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "expunge", defaultValue = "false")
    private Boolean expunge;


    public void execute() throws MojoExecutionException {
        boolean all = (expunge != null && expunge == true);
        Path baseDir = project.getBasedir().toPath().toAbsolutePath();
        Common.OUTPUT_FILES[] files = Common.OUTPUT_FILES.values();

        for (Common.OUTPUT_FILES file : files) {
            if (all) {
                Common.deleteFilesByPrefixAndSuffix(getLog(), baseDir, file.toString(), "*");
            } else if (file.isBazelFile()) {
                Common.deleteFilesByPrefixAndSuffix(getLog(), baseDir, file.toString(), "_*");
            } else {
                Common.deleteFilesByPrefixAndSuffix(getLog(), baseDir, file.toString(), "*");
            }
        }
    }
}
