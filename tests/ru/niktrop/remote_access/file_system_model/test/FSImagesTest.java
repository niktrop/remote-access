package ru.niktrop.remote_access.file_system_model.test;

import nu.xom.ParsingException;
import org.junit.Test;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 13:32
 */
public class FSImagesTest {
  private final static String testXml =
          "<directory alias=\"test\">\n" +
          "\t<directory name=\"a\">\n" +
          "\t\t<directory name=\"b\">\n" +
          "\t\t\t<file name=\"f.txt\" />\n" +
          "\t\t</directory>\n" +
          "\t</directory>\n" +
          "\t<directory name=\"c\">\n" +
          "\t\t<directory name=\"d\" />\n" +
          "\t</directory>\n" +
          "\t<file name=\"f.txt\" />" +
          "</directory>";

  @Test
  public void fromXmlAndBack() throws ParsingException, IOException {
    FSImage fsi = FSImages.getFromXml(testXml);
    String xmlBack = fsi.toXml();
    assertThat(testXml).isEqualTo(xmlBack);
  }

  @Test
  public void testGetRootAlias() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXml);

    assertThat(fsi.getRootAlias()).isEqualTo("test");
  }

  @Test
  public void testPathToRoot() throws Exception {
    Path tempDir = FileTreeBuilderTest.createTempDir();
    Path a = tempDir.resolve("a");
    WatchService watcher = FileSystems.getDefault().newWatchService();
    FSImage fsi = FSImages.getFromDirectory(a, 1, watcher);

    assertThat(fsi.getPathToRoot().isAbsolute()).isTrue();
    assertThat(fsi.getPathToRoot().getFileName().toString()).isEqualTo("a");

    deleteTempDir(tempDir, 20);
  }

  //Sometimes it is not deleted at the first time.
  private static void deleteTempDir(Path tempDir, int times)  {
    for (int i = 0; i<times; i++) {
      try {
        Thread.sleep(10L);
        deleteDirectory(tempDir.toFile());
        return;
      } catch (IOException e) {
        //System.out.println("Let's try once more...");
      } catch (InterruptedException e) {
      }
    }
    System.out.println("Temporary directory " + tempDir.toString() + " was not deleted.");
  }
}
