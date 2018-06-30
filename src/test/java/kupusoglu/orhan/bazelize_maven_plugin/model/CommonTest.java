package kupusoglu.orhan.bazelize_maven_plugin.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;


public class CommonTest {
    final private String FILE_NAME = "test-file.txt";
    final private String FILE_SUFFIX = "_backup";
    final private String FILE_CONTENT = "After more than 80 years in service, the Douglas DC-3 is still going strong.\n"
                                      + "https://www.flyingmag.com/dc-3-an-airplane-for-ages\n";
    final private Path PATH_DIR_TEST = Paths.get("/tmp/bazelize-maven-plugin");


    private void rmDir() {
        try(
            Stream<Path> stream = Files.walk(PATH_DIR_TEST);
        ) {
            stream.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        Assert.assertFalse("Temp directory still exists: " + PATH_DIR_TEST.toString(),
                           Files.exists(PATH_DIR_TEST));
    }

    private void mkDir() {
        try {
            Files.createDirectory(PATH_DIR_TEST);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        Assert.assertTrue("Temp directory could not be created: " + PATH_DIR_TEST.toString(),
                          Files.exists(PATH_DIR_TEST) && Files.isDirectory(PATH_DIR_TEST));
    }

    private void mkFile(final String fileName) {
        Path path = Paths.get(PATH_DIR_TEST.toString(), fileName);

        try {
            final InputStream inputStream = Common.class.getClassLoader().getResourceAsStream(fileName);
            final String content = Common.readFromInputStream(inputStream);
            File tmpFile = new File(path.toString());
            FileWriter writer = new FileWriter(tmpFile);

            writer.write(content);
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        Assert.assertTrue("Temp file could not be created: " + path.toString(),
                          Files.exists(path) && Files.isRegularFile(path));
    }

    @Before
    public void setUp() {
        rmDir();
        mkDir();
        mkFile(FILE_NAME);
    }

    @After
    public void tearDown() {
        rmDir();
    }

    @Test
    public void testRemoveLastChars() throws Exception {
        final String subject = "F-104G 63-12733";
        final String expected = "F-104G";
        final String actual = Common.removeLastChars(subject, 9);

        Assert.assertEquals("Remove last n chars - failure", expected, actual);
    }

    @Test
    public void testSanitize() throws Exception {
        final String subject = "Vought XF8U-3!Crusader+146340@1.9,5:8".toLowerCase();
        final String expected = "vought_xf8u_3_crusader_146340_1_9_5_8";
        final String actual = Common.sanitize(subject);

        Assert.assertEquals("Java identifier - sanitation failure", expected, actual);
    }

    @Test
    public void testGetFormattedTimestamp() throws Exception {
        LocalDateTime dateTime = LocalDateTime.of(2018, 6, 22, 14, 55, 36, 0);
        final String expected = "2018-06-22_14-55-36";
        final String actual = Common.getFormattedTimestamp(dateTime);

        Assert.assertEquals("Timestamp - format failure", expected, actual);
    }

    @Test
    public void testRenameFileIfExists() throws Exception {
        final File file = new File(Paths.get(PATH_DIR_TEST.toString(), FILE_NAME).toString());
        final File backupFile = new File(Paths.get(PATH_DIR_TEST.toString(), FILE_NAME + FILE_SUFFIX).toString());
        final String path = file.getAbsolutePath();

        Common.renameFileIfExists(path, new StringBuilder(FILE_SUFFIX).deleteCharAt(0).toString());

        Assert.assertTrue("Rename file if exists - rename failure", backupFile.exists());

        if (backupFile.exists()) {
            Files.move(backupFile.toPath(), file.toPath());
        }
    }

    @Test
    public void testCopyFileIfExists() throws Exception {
        final File file = new File(Paths.get(PATH_DIR_TEST.toString(), FILE_NAME).toString());
        final File backupFile = new File(Paths.get(PATH_DIR_TEST.toString(), FILE_NAME + FILE_SUFFIX).toString());
        final String path = file.getAbsolutePath();

        Common.copyFileIfExists(path, new StringBuilder(FILE_SUFFIX).deleteCharAt(0).toString());

        Assert.assertTrue("Copy file if exists - copy failure", backupFile.exists());

        if (backupFile.exists()) {
            backupFile.delete();
        }
    }

    @Test
    public void testDeleteFilesByPrefixAndSuffix() throws Exception {
        final String[] fileNames = {"F-100D.txt",
                                    "F-102A.txt",
                                    "F-104S.txt"};
        int counter = 0;

        for (String fileName : fileNames) {
            File tmpFile = new File(Paths.get(PATH_DIR_TEST.toString(), fileName).toString());
            tmpFile.createNewFile();
        }

        Common.deleteFilesByPrefixAndSuffix(null, PATH_DIR_TEST, "F-", "*");

        for (String fileName : fileNames) {
            if (!Files.exists(Paths.get(PATH_DIR_TEST.toString(), fileName))) {
                counter++;
            };
        }

        Assert.assertEquals("Delete files by prefix & suffix - delete failure", fileNames.length, counter);
    }

    @Test
    public void testReadTextFile() {
        final String actual = Common.readTextFile(Paths.get(PATH_DIR_TEST.toString(), FILE_NAME));

        Assert.assertEquals("Read file - content failure", FILE_CONTENT, actual);
    }

    @Test
    public void testGetLongestCommonPathPrefix() {
        final SortedSet<String> set =  new TreeSet<>();

        set.add("a/b/c/d/e");
        set.add("a/b/c/d");
        set.add("a/b/c");
        set.add("a/b");

        final String expected = "a";
        final String actual = Common.getLongestCommonPathPrefix(set);

        Assert.assertEquals("Get longest common path prefix - failure", expected, actual);
    }
}