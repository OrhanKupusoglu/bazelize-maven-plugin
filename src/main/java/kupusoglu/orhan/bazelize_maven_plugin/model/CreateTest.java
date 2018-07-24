package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;


/**
 * A {@code FileVisitor} to generate a Bazel <strong>java_test()</strong> rule for each test class.
 * <br>
 * @see <a href="https://docs.bazel.build/versions/master/be/java.html#java_test">Bazel java_test()</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/walk.html">Walking the File Tree</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java">Find.java</a>
 */
public class CreateTest extends SimpleFileVisitor<Path> {
    private Log log;
    private Path root;
    private String srcTest;
    private String resTest;
    private Path pathTest;
    private PathMatcher matcherTestJava;
    private Path fileBuildPath;

    private String libName;
    private int numJavaTestMatches = 0;


    public CreateTest(Log log, Path root, String srcTest, String resTest) {
        this.log = log;
        this.root = root;
        this.srcTest = srcTest;
        this.resTest = resTest;
        this.pathTest = Paths.get(this.root + File.separator + srcTest).toAbsolutePath();
        this.matcherTestJava = FileSystems.getDefault().getPathMatcher("glob:*.java");
        this.fileBuildPath = Paths.get(this.root + File.separator + Common.OUTPUT_FILES.BUILD).toAbsolutePath();

        findBuild(this.fileBuildPath);
    }

    public void findBuild(Path absolutePath) {
        String data = Common.readTextFile(absolutePath);
        Matcher regexMatcher = Common.getPatternBazelLib().matcher(data);

        if (regexMatcher.find()) {
            this.libName = regexMatcher.group(1);
            log.info("found Java library: " + libName + " -- " + absolutePath);
        } else {
            log.warn("Java library not found -- " + absolutePath);
        }
    }

    public boolean isLibraryFound() {
        return (this.libName != null && !this.libName.isEmpty());
    }

    public void collect(Path file) {
        Path name = file.getFileName();

        if (name != null) {
            if (matcherTestJava.matches(name)) {
                numJavaTestMatches++;
                Path absolutePath = file.toAbsolutePath().normalize();

                log.info("found Java test file: " + absolutePath);

                processTest(absolutePath);
            }
        }
    }

    // assumption: package name corresponds to folders in path
    public void processTest(Path absolutePath) {
        String srcRelTest = absolutePath.toString().replaceFirst(this.pathTest.toString(), "");
        String nameCanonical = srcRelTest.replaceFirst("/", "")
                                         .replace('/', '.')
                                         .replaceFirst(".java", "");
        String srcTestFile = this.srcTest + srcRelTest;

        log.info("Java test class: " + nameCanonical + " -- " + srcTestFile);

        addTest(nameCanonical, srcTestFile);
    }

    public void addTest(String nameCanonical, String srcTestFile) {
        String contentTest = Common.getTemplateTest();
        String resFiles= Common.getResources(resTest);
        StringBuilder sb = new StringBuilder();

        sb.append(Common.getIndentTwo());
        sb.append("\":");
        sb.append(this.libName);
        sb.append("\",");

        String depLibName = sb.toString();

        // append the rule
        try (
            FileWriter buildWriter = new FileWriter(fileBuildPath.toString(), true);
        ) {
            try {
                buildWriter.append("\n");
                buildWriter.append(contentTest.replaceFirst("#TEST_NAME#",
                                                            Common.sanitize(nameCanonical))
                                              .replaceFirst("#SRCS_GLOB#",
                                                            srcTestFile)
                                              .replaceFirst("#TEST_CLASS#",
                                                            nameCanonical)
                                              .replaceFirst("#RES_FILES#",
                                                            resFiles)
                                              .replaceFirst("#JAVA_DEPS#",
                                                            depLibName));
                ;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void done() {
        log.info("completed\n\tmatched: " + numJavaTestMatches
                                          + " Java Test files");
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
