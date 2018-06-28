package kupusoglu.orhan.bazelize_maven_plugin.model;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;


public class CommonTest {
    @Test
    public void testRemoveLastChars() throws Exception {
        final String subject = "F-104G Starfighter 4733";
        final String expected = "F-104G Starfighter";
        final String actual = Common.removeLastChars(subject, 5);

        Assert.assertEquals("Remove last n chars - failure", expected, actual);
    }

    @Test
    public void testSanitize() throws Exception {
        final String subject = "magnificent.project-nice.test-1.2.3";
        final String expected = "magnificent_project_nice_test_1_2_3";
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
        final File file = new File("src/test/resources/test-file.txt");
        final File backupFile = new File("src/test/resources/test-file.txt_backup");
        final String path = file.getAbsolutePath();

        Common.renameFileIfExists(path, "backup");

        Assert.assertTrue("Rename file if exists - rename failure", backupFile.exists());

        if (backupFile.exists()) {
            Files.move(backupFile, file);
        }
    }

    @Test
    public void testCopyFileIfExists() throws Exception {
        final File file = new File("src/test/resources/test-file.txt");
        final File backupFile = new File("src/test/resources/test-file.txt_backup");
        final String path = file.getAbsolutePath();

        Common.copyFileIfExists(path, "backup");

        Assert.assertTrue("Copy file if exists - copy failure", backupFile.exists());

        if (backupFile.exists()) {
            backupFile.delete();
        }
    }

    @Test
    public void testDeleteFilesByPrefixAndSuffix() throws Exception {
        final File[] files = {new File("src/test/resources/delete-1.txt"),
                              new File("src/test/resources/delete-2.txt"),
                              new File("src/test/resources/delete-3.txt")};
        int counter = 0;

        for (File file : files) {
            file.createNewFile();
        }

        Path dir = Paths.get(files[0].getAbsolutePath() + "/..").normalize();

        Common.deleteFilesByPrefixAndSuffix(null, dir, "delete", "*");

        for (File file : files) {
            if (!file.exists()) {
                counter++;
            };
        }

        Assert.assertEquals("Delete files by prefix & suffix - delete failure", files.length, counter);
    }

    @Test
    public void testReadTextFile() throws Exception {
        final File file = new File("src/test/resources/test-file.txt");
        final String path = file.getAbsolutePath();
        final String expected = "After more than 80 years in service, the Douglas DC-3 is still going strong.\n"
                              + "https://www.flyingmag.com/dc-3-an-airplane-for-ages\n";
        final String actual = Common.readTextFile(Paths.get(path));

        Assert.assertEquals("Read file - content failure", expected, actual);
    }

    @Test
    public void testGetLongestCommonPathPrefix() throws Exception {
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