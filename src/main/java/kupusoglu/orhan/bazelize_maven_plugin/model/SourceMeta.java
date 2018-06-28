package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;


/**
 * A {@code FileVisitor} that finds Java source files, <strong>*.java</strong>.
 * <br>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/walk.html">Walking the File Tree</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java">Find.java</a>
 */
public class SourceMeta extends SimpleFileVisitor<Path> {
    private Log log;
    private Path root;
    private PathMatcher matcherSource;
    private PathMatcher matcherMaven;
    private Pattern whiteListPattern;
    private Pattern patternBlackList;

    private int numSourceMatch = 0;
    private int numMavenMatch = 0;
    private SortedSet<String> setSourceDir = new TreeSet<>();


    public SourceMeta() {
        super();
    }

    public SourceMeta(Log log, Path root, String patternSource, String patternMaven, String whiteListPattern, String blackListPattern) {
        this();

        this.log = log;
        this.root = root;
        this.matcherSource = setPathPattern(patternSource);
        this.matcherMaven = setPathPattern(patternMaven);

        setWhiteListPattern(whiteListPattern);
        setBlackListPattern(blackListPattern);
    }

    /**
     * If file is a source file, process it.
     * If there are more than one pom.xml, then terminate the file walk.
     * Because submodules with their own pom.xml will take care of their own sources.
     *
     * @param   file    A regular file
     * @return  result  FileVisitResult
     */
    public FileVisitResult collect(Path file) {
        Path name = file.getFileName();

        if (name != null) {
            Path absolutePathOfParent = file.toAbsolutePath().normalize().getParent();

            if (matcherMaven.matches(name)) {
                if (isBlackListed(absolutePathOfParent.toString())) {
                    return FileVisitResult.CONTINUE;
                } else {
                    numMavenMatch++;
                    if (numMavenMatch > 1) {
                        numSourceMatch = 0;
                        setSourceDir.clear();
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }
            } else if (isWhiteListed(file.toString())) {
                if (matcherSource.matches(name)) {
                    if (isBlackListed(absolutePathOfParent.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        numSourceMatch++;
                        processSource(absolutePathOfParent);
                        return FileVisitResult.CONTINUE;
                    }
                } else {
                    return FileVisitResult.CONTINUE;
                }
            } else {
                return FileVisitResult.CONTINUE;
            }
        }

        return FileVisitResult.CONTINUE;
    }

    public void processSource(Path absolutePath) {
        Path relative = root.relativize(absolutePath);
        String relPath = relative.toString();

        if (!setSourceDir.contains(relPath)) {
            setSourceDir.add(relPath);
        }
    }

    private PathMatcher setPathPattern(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    private void setWhiteListPattern(String whiteListPattern) {
        if (whiteListPattern == null || whiteListPattern.isEmpty()) {
            this.whiteListPattern = null;
        } else {
            this.whiteListPattern = Pattern.compile(whiteListPattern);
        }
    }

    private boolean isWhiteListed(String path) {
        if (this.whiteListPattern == null) {
            return true;
        } else {
            return this.whiteListPattern.matcher(path).find();
        }
    }

    private void setBlackListPattern(String blackListPattern) {
        if (blackListPattern == null || blackListPattern.isEmpty()) {
            this.patternBlackList = null;
        } else {
            this.patternBlackList = Pattern.compile(blackListPattern);
        }
    }

    private boolean isBlackListed(String path) {
        if (this.patternBlackList == null) {
            return false;
        } else {
            return this.patternBlackList.matcher(path).find();
        }
    }

    public String[] getMetaData() {
        List<String> metaList = new ArrayList<>();

        if (this.setSourceDir.size() == 0) {
            metaList.clear();
        } else {
            for (String source : setSourceDir) {
                metaList.add(source + File.separator + "*.java");
            }
        }

        String[] metaArray = new String[metaList.size()];
        metaArray = metaList.toArray(metaArray);

        return metaArray;
    }

    public String done() {
        return "completed\n\tmatched: " + numSourceMatch
                                        + " source files\n\tin "
                                        + setSourceDir.size()
                                        + " dir(s)";
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()) {
            return collect(file);
        } else {
            log.warn("ignored - not a regular file: " + file);
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        log.error(e.getMessage());
        return FileVisitResult.CONTINUE;
    }
}
