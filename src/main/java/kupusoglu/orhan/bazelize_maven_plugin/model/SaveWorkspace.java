package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Saves <strong>WORKSPACE</strong> script to file
 */
public class SaveWorkspace {
    private Log log;
    private String baseDir;
    private String workspaceName;


    public SaveWorkspace() {
        super();
    }

    public SaveWorkspace(Log log, String baseDir, String workspaceName) {
        this();

        this.log = log;
        this.baseDir = baseDir;
        this.workspaceName = workspaceName;
    }

    public void execute() throws MojoExecutionException {
        Path root = Paths.get(baseDir).normalize().toAbsolutePath();
        File fileWorkspace = new File(root + File.separator + Common.OUTPUT_FILES.WORKSPACE);

        if (fileWorkspace.exists()) {
            log.warn("Bazel WORKSPACE file already exists");
        } else {
            try {
                CreateWorkspace createWorkspace = new CreateWorkspace(log,
                                                                      root,
                                                                      workspaceName,
                                                                      Common.OUTPUT_FILES.JSON_DEPENDENCY.toString(),
                                                                      Common.OUTPUT_FILES.JSON_SERVER.toString());

                java.nio.file.Files.walkFileTree(root, createWorkspace);
                createWorkspace.workspace();
                createWorkspace.done();

                log.info("output:\n" + Common.getIndentOne() + fileWorkspace);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}
