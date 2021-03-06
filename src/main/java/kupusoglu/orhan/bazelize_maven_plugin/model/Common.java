package kupusoglu.orhan.bazelize_maven_plugin.model;

import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Common utilities of the plugin
 */
public class Common {
    private static HashMap<String, Dependency> MAP_META_DEPENDENCY;
    private static HashMap<String, String> MAP_PROJECT_DATA;
    private static Optional<String> TEMPLATE_LIBRARY = Optional.empty();
    private static Optional<String> TEMPLATE_BINARY = Optional.empty();
    private static Optional<String> TEMPLATE_TEST = Optional.empty();
    private static Optional<String> TEMPLATE_SERVER = Optional.empty();
    private static Optional<String> RES_CLASS = Optional.empty();
    private static Pattern PATTERN_BLACK_LIST = Pattern.compile("^jdk_tools");

    private static final char SEP_SANITIZE = '_';
    private static final String SEP_BLACK_LIST = "|";
    private static final String SEP_WHITE_LIST = "|";
    private static final String INDENT_1 = "    ";
    private static final String INDENT_2 = INDENT_1 + INDENT_1;
    private static final String DIR_CURRENT = ".";
    private static final Pattern PATTERN_SETTINGS_JAR = Pattern.compile("^.+?\\.[^javadoc]\\.jar\\>(.+?)\\=$", Pattern.MULTILINE);
    private static final Pattern PATTERN_BAZEL_LIB = Pattern.compile("\\s*java_library\\(\n?\\s*name\\s*=\\s*\"(.*)\",", Pattern.MULTILINE);
    private static final String FORMATTED_TIMESTAMP = "yyyy-MM-dd_HH-mm-ss";
    private static final DateTimeFormatter FORMATTED_PATTERN = DateTimeFormatter.ofPattern(FORMATTED_TIMESTAMP);


    /**
     * Store dependency data
     */
    public static class Dependency {
        private String name;
        private String label;
        private String[] sources;
        private String dir;
        private String jar;

        public Dependency(String name, String label, String[] sources) {
            this.name = name;
            this.label = label;
            this.sources = sources;
        }

        public Dependency(String name, String label, String[] sources, String dir) {
            this(name, label, sources);
            this.dir = dir;
        }

        public Dependency(String name, String label, String[] sources, String dir, String jar) {
            this(name, label, sources, dir);
            this.jar = jar;
        }

        public String getLabel() {
            return this.label;
        }

        public String getName() {
            return this.name;
        }

        public String[] getSources() {
            return this.sources;
        }

        public String getDir() {
            return this.dir;
        }

        public String getJar() {
            return this.jar;
        }
    }

    /**
     * INPUT file names
     */
    public enum INPUT_FILES {
        TEMPLATE_LIBRARY {
            public String toString() {
                return "library.template";
            }
        },
        TEMPLATE_BINARY {
            public String toString() {
                return "binary.template";
            }
        },
        TEMPLATE_TEST {
            public String toString() {
                return "test.template";
            }
        },
        TEMPLATE_SERVER {
            public String toString() {
                return "server.template";
            }
        },
        BUILD_PREPEND {
            public String toString() {
                return "bzl-build-prepend.txt";
            }
        },
        BUILD_APPEND {
            public String toString() {
                return "bzl-build-append.txt";
            }
        },
        BUILD_DEPENDENCY {
            public String toString() {
                return "bzl-build-dependency.json";
            }
        },
        WORKSPACE_PREPEND {
            public String toString() {
                return "bzl-workspace-prepend.txt";
            }
        },
        WORKSPACE_APPEND {
            public String toString() {
                return "bzl-workspace-append.txt";
            }
        }
    }

    /**
     * OUTPUT file names
     */
    public enum OUTPUT_FILES {
        BUILD(true) {
            public String toString() {
                return "BUILD";
            }
        },
        WORKSPACE(true) {
            public String toString() {
                return "WORKSPACE";
            }
        },
        JSON_MODULE(false) {
            public String toString() {
                return "tmp-bzl-module.json";
            }
        },
        JSON_META(false) {
            public String toString() {
                return "tmp-bzl-meta.json";
            }
        },
        JSON_DEPENDENCY(false) {
            public String toString() {
                return "tmp-bzl-dependency.json";
            }
        },
        JSON_SERVER(false) {
            public String toString() {
                return "tmp-bzl-server.json";
            }
        };

        private final boolean isBazelFile;

        OUTPUT_FILES(boolean isBazelFile) {
            this.isBazelFile = isBazelFile;
        }

        public boolean isBazelFile() {
            return this.isBazelFile;
        }
    }


    private Common() {
        // no instance required, use static factory methods
    }

    // GETTERS
    public static char getSepSanitize() {
        return SEP_SANITIZE;
    }

    public static String getSepBlackList() {
        return SEP_BLACK_LIST;
    }

    public static String getSepWhiteList() {
        return SEP_WHITE_LIST;
    }

    public static String getIndentOne() {
        return INDENT_1;
    }

    public static String getIndentTwo() {
        return INDENT_2;
    }

    public static String getDirCurrent() {
        return DIR_CURRENT;
    }

    public static Pattern getPatternSettingsJar() {
        return PATTERN_SETTINGS_JAR;
    }

    public static Pattern getPatternBazelLib() {
        return PATTERN_BAZEL_LIB;
    }

    // UNIT TEST START
    public static String removeLastChars(String s, int n) {
        if (s == null || s.length() == 0 || n >= s.length()) {
            return "";
        } else if (n == 0) {
            return s;
        }

        return s.substring(0, s.length() - n);
    }

    public static String sanitize(CharSequence input) {
        return CharMatcher.forPredicate(Character::isJavaIdentifierPart).negate().replaceFrom(input, getSepSanitize());
    }

    public static String getFormattedTimestamp(LocalDateTime dateTime) {
        return dateTime.format(FORMATTED_PATTERN);
    }

    public static void renameFileIfExists(String filePath, String suffix) {
        File file = new File(filePath);

        if (file.exists() && suffix != null) {
            File newFile = new File(filePath + getSepSanitize() + suffix);

            try {
                Files.move(file, newFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyFileIfExists(String filePath, String suffix) {
        File file = new File(filePath);

        if (file.exists() && suffix != null) {
            File newFile = new File(filePath + getSepSanitize() + suffix);

            try {
                Files.copy(file, newFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteFilesByPrefixAndSuffix(Log log, final Path path, final String prefix, final String suffix) {
        try (DirectoryStream<Path> directoryStream = java.nio.file.Files.newDirectoryStream(path, prefix + suffix)) {
            for (final Path item : directoryStream) {
                java.nio.file.Files.delete(item);
                if (log == null) {
                    System.out.println("deleting: " + item);
                } else {
                    log.info("deleting: " + item);
                }
            }
        } catch (Exception e) {
            if (log == null) {
                System.out.println(e.getMessage());
            } else {
                log.info(e.getMessage());
            }
        }
    }

    public static String readTextFile(Path absolutePath, String endLine) {
        File file = new File(absolutePath.toString());

        if (file.exists() && file.isFile() && file.canRead()) {
            try {
                InputStream inputStream = new FileInputStream(file);
                StringBuilder resultStringBuilder = new StringBuilder();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        resultStringBuilder.append(line).append(endLine);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return resultStringBuilder.toString();
            } catch(FileNotFoundException e2){
                e2.printStackTrace();
            }
        }

        return "";
    }

    public static String readTextFile(Path absolutePath) {
        return readTextFile(absolutePath, "\n");
    }

    public static String getLongestCommonPathPrefix(String first, String last) {
        int len = first.length();
        int i = 0;

        while (i < len && first.charAt(i) == last.charAt(i)) {
            i++;
        }

        String prefix = first.substring(0, i);
        prefix = prefix.substring(0, prefix.lastIndexOf(File.separator));

        return prefix;
    }

    public static String getLongestCommonPathPrefix(SortedSet<String> strings) {
        String first = strings.first();
        String last = strings.last();

        return getLongestCommonPathPrefix(first, last);
    }
    // UNIT TEST END


    public static String getBackupSuffix(String suffix, String ctxSuffix) {
        LocalDateTime localDateTime = LocalDateTime.now();
        String finalSuffix;

        // de Morgan's Law: 'NOT (a OR b)' = '(NOT a) AND (NOT b)'
        if (!(ctxSuffix == null || suffix.isEmpty())) {
            finalSuffix = ctxSuffix;
        } else if (suffix == null || suffix.isEmpty()) {
            finalSuffix = Common.getFormattedTimestamp(localDateTime);
        } else {
            finalSuffix = suffix;
        }

        return finalSuffix;
    }

    public static void setProjectData(String key, String value) {
        if (MAP_PROJECT_DATA == null) {
            MAP_PROJECT_DATA = new HashMap<>();
        }

        MAP_PROJECT_DATA.put(key, value);
    }

    public static String getProjectData(String key) {
        if (MAP_PROJECT_DATA == null) {
            return null;
        } else {
            if (MAP_PROJECT_DATA.containsKey(key)) {
                return MAP_PROJECT_DATA.get(key);
            } else {
                return null; // might have been empty string
            }
        }
    }

    public static Dependency queryLibrary(String key) {
        if (MAP_META_DEPENDENCY == null) {
            try {
                jsonToMap();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (MAP_META_DEPENDENCY.containsKey(key)) {
            return MAP_META_DEPENDENCY.get(key);
        } else {
            return null;
        }
    }

    public static String getTemplateLibrary() {
        if (!TEMPLATE_LIBRARY.isPresent()) {
            TEMPLATE_LIBRARY = readTemplate(INPUT_FILES.TEMPLATE_LIBRARY);
        }

        return TEMPLATE_LIBRARY.orElse("");
    }

    public static String getTemplateBinary() {
        if (!TEMPLATE_BINARY.isPresent()) {
            TEMPLATE_BINARY = readTemplate(INPUT_FILES.TEMPLATE_BINARY);
        }

        return TEMPLATE_BINARY.orElse("");
    }

    public static String getTemplateTest() {
        if (!TEMPLATE_TEST.isPresent()) {
            TEMPLATE_TEST = readTemplate(INPUT_FILES.TEMPLATE_TEST);
        }

        return TEMPLATE_TEST.orElse("");
    }

    public static String getTemplateServer() {
        if (!TEMPLATE_SERVER.isPresent()) {
            TEMPLATE_SERVER = readTemplate(INPUT_FILES.TEMPLATE_SERVER);
        }

        return TEMPLATE_SERVER.orElse("");
    }

    public static void setBlackListPattern(String blackListPattern) {
        if (blackListPattern == null || blackListPattern.isEmpty()) {
            PATTERN_BLACK_LIST = null;
        } else {
            PATTERN_BLACK_LIST = Pattern.compile(blackListPattern);
        }
    }

    public static String getResources(String dir) {
        Optional<String> res = Optional.empty();

        // De Morgan's laws
        // https://en.wikipedia.org/wiki/De_Morgan%27s_laws
        // if (dir != null && !dir.isEmpty())
        if (!(dir == null || dir.isEmpty())) {
            if (RES_CLASS.isPresent()) {
                res = RES_CLASS;
            } else {
                try (
                    Stream<Path> stream = java.nio.file.Files.walk(Paths.get(dir));
                ) {
                    StringBuilder sb = new StringBuilder();
                    List<File> files = stream.filter(java.nio.file.Files::isRegularFile)
                            .map(Path::toFile)
                            .collect(Collectors.toList());

                    if (files.size() > 0) {
                        sb.append("\n");
                        for (File file : files) {
                            sb.append(Common.getIndentTwo());
                            sb.append("\"");
                            sb.append(file.toString());
                            sb.append("\",");
                            sb.append("\n");
                        }
                    }

                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                        res = Optional.of(sb.toString());
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                RES_CLASS = res;
            }
        }

        return res.orElse("");
    }

    private static void jsonToMap() throws IOException {
        String filenameMeta = OUTPUT_FILES.JSON_META.toString();
        String stringMeta = "";
        InputStream inputMeta;

        try {
            if (getProjectData("baseDir") == null) {
                inputMeta = Common.class.getClassLoader().getResourceAsStream(filenameMeta);
            } else {
                inputMeta = new FileInputStream(getProjectData("baseDir") + File.separator + filenameMeta);
            }

            try {
                stringMeta = readFromInputStream(inputMeta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e1) {
            String errMsg = "[ERROR] file not found exception: " + filenameMeta;
            System.err.println(errMsg + "\n");
            throw new IOException(errMsg);
        } catch (Exception e2) {
            e2.printStackTrace();
        }

        JSONArray jsonArray = new JSONArray(stringMeta);
        MAP_META_DEPENDENCY = new HashMap<>();

        String keyName = "name";
        String keyLabel = "label";
        String keySources = "sources";

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);

            List<String> sourcesList = new ArrayList<>();
            JSONArray sourcesArray = item.getJSONArray(keySources);
            for (int j = 0; j < sourcesArray.length(); j++) {
                sourcesList.add(sourcesArray.getString(j));
            }

            String[] sources = new String[sourcesList.size()];
            sources = sourcesList.toArray(sources);

            String name = item.getString(keyName);
            Dependency dep = new Dependency(name, item.getString(keyLabel), sources);

            MAP_META_DEPENDENCY.put(name, dep);
        }
    }

    private static Optional<String> readTemplate(INPUT_FILES input) {
        String filenameBuild = input.toString();

        Optional<String> template = Optional.empty();

        try {
            InputStream inputStream = Common.class.getClassLoader().getResourceAsStream(filenameBuild);

            try {
                template = Optional.of(readFromInputStream(inputStream));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return template;
    }

    // called by CommonTest
    protected static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        } catch (NullPointerException e) {
            String errMsg = "[ERROR] file not found exception: " + inputStream.toString();
            System.err.println(errMsg + "\n");
            throw new IOException(errMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultStringBuilder.toString();
    }

    public static boolean isBlackListed(String dep) {
        if (PATTERN_BLACK_LIST == null) {
            return false;
        } else {
            return PATTERN_BLACK_LIST.matcher(dep).find();
        }
    }

    public static void generateMetaFile(Log log, String baseDir, String suffix)
        throws MojoExecutionException {
        if (suffix != null) {
            renameFileIfExists(baseDir + File.separator + OUTPUT_FILES.JSON_META, suffix);
        }

        SaveMeta saveMeta = new SaveMeta(log, baseDir);
        saveMeta.execute();
    }

    public static void generateWorkspace(Log log, String baseDir, String workspaceName, String suffix)
        throws MojoExecutionException {
        if (suffix != null) {
            renameFileIfExists(baseDir + File.separator + OUTPUT_FILES.WORKSPACE, suffix);
        }

        SaveWorkspace saveWorkspace = new SaveWorkspace(log, baseDir, workspaceName);
        saveWorkspace.execute();
    }

    public static void generateBinary(Log log, String baseDir, String mainClass, String binName, String suffix)
        throws MojoExecutionException {
        if (suffix != null) {
            copyFileIfExists(baseDir + File.separator + OUTPUT_FILES.BUILD, suffix);
        }

        SaveBinary saveBinary = new SaveBinary(log, baseDir, mainClass, binName);
        saveBinary.execute();
    }

    public static void generateTest(Log log, String baseDir, String srcTest, String resTest, String suffix)
        throws MojoExecutionException {
        if (suffix != null) {
            copyFileIfExists(baseDir + File.separator + OUTPUT_FILES.BUILD, suffix);
        }

        SaveTest saveTest = new SaveTest(log, baseDir, srcTest, resTest);
        saveTest.execute();
    }

    public static String getGlobSources(String[] sources) {
        StringBuilder sb = new StringBuilder();
        List<String> src = Arrays.asList(sources);

        sb.append("glob([");
        sb.append(src.stream()
                     .collect(Collectors.joining("\",\n" + INDENT_2 + INDENT_2 + " \"",
                                                 "\"",
                                                 "\"")));
        sb.append("])");

        return sb.toString();
    }
}
