import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 15:25
 */
public class FileTreeBuilderTest {
  @Test
  public void varMaxDepth() throws Exception {
    Path tempDir = Files.createTempDirectory("temp");
    Path a = tempDir.resolve("a");
    Path a_b_c_d = a.resolve("b").resolve("c").resolve("d");
    Path a_f = tempDir.resolve("a").resolve("f");

    Files.createDirectories(a_b_c_d);
    Files.createFile(a_f);

    WatchService watchService = FileSystems.getDefault().newWatchService();
    DirectoryWatcher watcher = new DirectoryWatcher(watchService);

    FSImage fsi_0 = FSImages.getFromDirectory(a, 0, watcher);
    String xml_0 = "<directory name=\"a\" />";

    FSImage fsi_1 = FSImages.getFromDirectory(a, 1, watcher);
    String xml_1 =
            "<directory name=\"a\">" +
              "<directory name=\"b\" />" +
              "<file name=\"f\" />" +
            "</directory>";

    FSImage fsi_2 = FSImages.getFromDirectory(a, 2, watcher);
    String xml_2 =
            "<directory name=\"a\">" +
              "<directory name=\"b\">" +
                "<directory name=\"c\" />" +
              "</directory>" +
              "<file name=\"f\" />" +
            "</directory>";

    FSImage fsi_3 = FSImages.getFromDirectory(a, 3, watcher);
    String xml_3 =
            "<directory name=\"a\">" +
              "<directory name=\"b\">" +
                "<directory name=\"c\">" +
                  "<directory name=\"d\" />" +
                "</directory>" +
              "</directory>" +
              "<file name=\"f\" />" +
            "</directory>";

    assertThat(fsi_0.toXml()).isEqualTo(xml_0);
    assertThat(fsi_1.toXml()).isEqualTo(xml_1);
    assertThat(fsi_2.toXml()).isEqualTo(xml_2);
    assertThat(fsi_3.toXml()).isEqualTo(xml_3);

    deleteTempDir(tempDir, 20);
  }

  //Sometimes it is not deleted at the first time.
  private void deleteTempDir(Path tempDir, int times) {
    for (int i = 0; i<times; i++) {
      try {
        deleteDirectory(tempDir.toFile());
        return;
      } catch (IOException e) {
        //System.out.println("Let's try once more...");
      }
    }
  }


}
