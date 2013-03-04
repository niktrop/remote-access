import nu.xom.ParsingException;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 13:32
 */
public class FSImagesTest {
  private final static String testXml =
          "<root alias=\"test\">\n" +
          "\t<directory name=\"a\">\n" +
          "\t\t<directory name=\"b\">\n" +
          "\t\t\t<file name=\"f.txt\"/>\n" +
          "\t\t</directory>\n" +
          "\t</directory>\n" +
          "\t<directory name=\"c\">\n" +
          "\t\t<directory name=\"d\"/>\n" +
          "\t</directory>\n" +
          "\t<file name=\"f.txt\"/>" +
          "</root>";

  @Test
  public void fromXmlAndBack() throws ParsingException, IOException {
    FSImage fsi = FSImages.getFromXml(testXml);
    String xmlBack = fsi.toXml();
    assertThat(testXml.equals(xmlBack));
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
    WatchService watchService = FileSystems.getDefault().newWatchService();
    DirectoryWatcher watcher = new DirectoryWatcher(watchService);
    FSImage fsi = FSImages.getFromDirectory(a, 1, watcher);

    assertThat(fsi.getPathToRoot().isAbsolute());
    assertThat(fsi.getPathToRoot().getFileName().toString()).isEqualTo("a");

    FileTreeBuilderTest.deleteTempDir(tempDir, 20);
  }
}
