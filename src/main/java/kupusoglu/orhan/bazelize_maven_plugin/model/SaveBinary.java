package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Appends a <strong>Bazel java_binary()</strong> rule to the root BUILD script
 */
public class SaveBinary {
    private Log log;
    private String baseDir;
    private String mainClass;
    private String binName;


    public SaveBinary() {
        super();
    }

    public SaveBinary(Log log, String baseDir, String mainClass, String binName) {
        this();

        this.log = log;
        this.baseDir = baseDir;
        this.mainClass = mainClass;
        this.binName = binName;
    }

    public void execute() throws MojoExecutionException {
        Path root = Paths.get(baseDir).normalize().toAbsolutePath();
        File fileBuild = new File(root + File.separator + Common.OUTPUT_FILES.BUILD);

        if (fileBuild.exists()) {
            log.info("root BUILD:\n" + Common.getIndentOne() + fileBuild);

            try {
                CreateBinary createBinary = new CreateBinary(log,
                                                             root,
                                                             mainClass,
                                                             binName);

                java.nio.file.Files.walkFileTree(root, createBinary);
                createBinary.addBinary();
                createBinary.done();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } else {
            log.warn("Bazel root BUILD file does not exist\n");
        }
    }
}
