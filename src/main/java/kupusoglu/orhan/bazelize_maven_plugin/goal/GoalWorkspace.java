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
 * Goal <strong>build</strong>
 * <br>
 * Order: 4
 * <br>
 * Generates the <strong>WORKSPACE</strong> script
 * <br><br>
 * <pre>
 * mvn kupusoglu.orhan:bazelize-maven-plugin:workspace
 * mvn bazelize:workspace
 * </pre>
 */
@Mojo(
    name = "workspace",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class GoalWorkspace extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "backup", defaultValue = "false")
    private Boolean backup;

    @Parameter(property = "suffix", defaultValue = "")
    private String suffix;

    @Parameter(property = "workspaceName", defaultValue = "")
    private String workspaceName;


    public void execute() throws MojoExecutionException {
        if (project.isExecutionRoot()) {
            String finalSuffix = null;

            if (backup) {
                LocalDateTime localDateTime = LocalDateTime.now();

                if (suffix == null || suffix.isEmpty()) {
                    finalSuffix = Common.getFormattedTimestamp(localDateTime);
                } else {
                    finalSuffix = suffix;
                }
            }

            try {
                Common.generateWorkspace(getLog(), project.getBasedir().getAbsolutePath(), workspaceName, finalSuffix);
            } catch (MojoExecutionException e) {
                getLog().error(e.getMessage());
            }
        } else {
            getLog().info("skipping");
        }
    }
}
