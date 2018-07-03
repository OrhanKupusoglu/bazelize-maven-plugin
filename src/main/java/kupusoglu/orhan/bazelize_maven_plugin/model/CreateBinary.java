package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;


/**
 * A {@code FileVisitor} to generate a <strong>Bazel java_binary()</strong> rule for the root BUILD file.
 * <br>
 * @see <a href="https://docs.bazel.build/versions/master/be/java.html#java_binary">Bazel java_binary()</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/walk.html">Walking the File Tree</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java">Find.java</a>
 */
public class CreateBinary extends SimpleFileVisitor<Path> {
    private Log log;
    private Path root;
    private String mainClass;
    private String binName;
    private PathMatcher matcherBuild;
    private File fileBuild;

    private List<String> listLibName = new ArrayList<>();
    private StringBuilder sbRules = new StringBuilder();
    private int numBuildMatches = 0;


    public CreateBinary(Log log, Path root, String mainClass, String binName) {
        this.log = log;
        this.root = root;
        this.mainClass = mainClass;
        this.binName = binName;
        this.matcherBuild = FileSystems.getDefault().getPathMatcher("glob:" + Common.OUTPUT_FILES.BUILD);
        this.fileBuild = new File(this.root + File.separator + Common.OUTPUT_FILES.BUILD);
    }

    public void collect(Path file) {
        Path name = file.getFileName();

        if (name != null) {
            if (matcherBuild.matches(name)) {
                numBuildMatches++;
                Path absolutePath = file.toAbsolutePath().normalize();

                log.info("found BUILD: " + absolutePath);

                processBuild(absolutePath);
            }
        }
    }

    public void processBuild(Path absolutePath) {
        String data = Common.readTextFile(absolutePath);
        Matcher regexMatcher = Common.getPatternBazelLib().matcher(data);
        String name;

        if (regexMatcher.find()) {
            name = regexMatcher.group(1);
            // get BUILD file's parent dir
            String relPath = this.root.relativize(absolutePath.getParent()).toString();
            StringBuilder sb = new StringBuilder();

            if (!relPath.isEmpty()) {
                sb.append("//");
                sb.append(relPath);
            }
            sb.append(":");
            sb.append(name);

            listLibName.add(sb.toString());
            log.info("found Java library: " + name + " -- " + absolutePath);
        } else {
            log.warn("Java library not found -- " + absolutePath);
        }
    }

    public void addBinary() {
        if (numBuildMatches == 0) {
            log.warn("no BUILD files found\n");
        } else {
            String contentBinary = Common.getTemplateBinary();
            StringBuilder sb = new StringBuilder();

            for (String libName : listLibName) {
                sb.append(Common.getIndentTwo());
                sb.append("\"");
                sb.append(libName);
                sb.append("\",\n");
            }

            // append the rule
            try (
                FileWriter buildWriter = new FileWriter(fileBuild, true);
            ) {
                try {
                    buildWriter.append("\n");
                    buildWriter.append(contentBinary.replaceFirst("#BIN_NAME#",
                                                                  Common.sanitize(this.binName))
                                                    .replaceFirst("#RUNTIME_DEPS#",
                                                                  Common.removeLastChars(sb.toString(), 1))
                                                    .replaceFirst("#MAIN_CLASS#",
                                                                  this.mainClass));
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void done() {
        log.info("completed\n\tmatched: " + numBuildMatches
                                             + " BUILD files");
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()) {
            collect(file);
        } else {
            log.warn("ignored - not a regular file: " + file);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        log.error(e.getMessage());
        return FileVisitResult.CONTINUE;
    }
}
