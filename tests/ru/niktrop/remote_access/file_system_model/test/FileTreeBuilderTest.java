package ru.niktrop.remote_access.file_system_model.test;

import org.junit.Test;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 15:25
 */
public class FileTreeBuilderTest {
  private final int MAX_TRIES = 20;

  @Test
  public void varMaxDepth() throws Exception {
    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");

    WatchService watcher = FileSystems.getDefault().newWatchService();

    FSImage fsi_0 = FSImages.getFromDirectory(a, 0, watcher);
    String xml_0 = "<directory name=\"a\" />";

    FSImage fsi_1 = FSImages.getFromDirectory(a, 1, watcher);
    String xml_1 =
            "<directory name=\"a\">" +
              "<directory name=\"b\" depth=\"1\" />" +
              "<file name=\"f\" depth=\"1\" />" +
            "</directory>";

    FSImage fsi_2 = FSImages.getFromDirectory(a, 2, watcher);
    String xml_2 =
            "<directory name=\"a\">" +
              "<directory name=\"b\" depth=\"1\">" +
                "<directory name=\"c\" depth=\"2\" />" +
              "</directory>" +
              "<file name=\"f\" depth=\"1\" />" +
            "</directory>";

    FSImage fsi_3 = FSImages.getFromDirectory(a, 3, watcher);
    String xml_3 =
            "<directory name=\"a\">" +
              "<directory name=\"b\" depth=\"1\">" +
                "<directory name=\"c\" depth=\"2\">" +
                  "<directory name=\"d\" depth=\"3\" />" +
                "</directory>" +
              "</directory>" +
              "<file name=\"f\" depth=\"1\" />" +
            "</directory>";

    assertThat(fsi_0.toXml()).isEqualTo(xml_0);
    assertThat(fsi_1.toXml()).isEqualTo(xml_1);
    assertThat(fsi_2.toXml()).isEqualTo(xml_2);
    assertThat(fsi_3.toXml()).isEqualTo(xml_3);

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testWatchingDepthZero() throws Exception {
    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");

    WatchService watcher = FileSystems.getDefault().newWatchService();
    FSImages.getFromDirectory(a, 0, watcher);

    Path a_g = a.resolve("g");
    Files.createFile(a_g);
    WatchKey key = watcher.take();
    assertThat(key.watchable()).isEqualTo(a);

    Path a_b_h = a.resolve("b").resolve("h");
    Files.createDirectory(a_b_h);
    key = watcher.poll(10, TimeUnit.MILLISECONDS);
    assertThat(key).isNull();
    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testWatchingDepthOne() throws Exception {
    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");

    WatchService watcher = FileSystems.getDefault().newWatchService();
    FSImages.getFromDirectory(a, 1, watcher);

    Path a_g = a.resolve("g");
    Files.createFile(a_g);
    WatchKey key = watcher.take();
    assertThat(key.watchable()).isEqualTo(a);

    Path a_b_h = a.resolve("b").resolve("h");
    Files.createDirectory(a_b_h);
    key = watcher.take();
    assertThat(key.watchable()).isEqualTo(a.resolve("b"));

    deleteTempDir(tempDir, MAX_TRIES);
  }

  //Sometimes it is not deleted at the first time.
  static void deleteTempDir(Path tempDir, int times) {
    for (int i = 0; i<times; i++) {
      try {
        deleteDirectory(tempDir.toFile());
        return;
      } catch (IOException e) {
        //System.out.println("Let's try once more...");
      }
    }
  }

  /*Creates temp directory with following structure:
  temp/a/b/c/d/
  temp/a/f
  */
  static Path createTempDir() throws IOException {
    Path tempDir = Files.createTempDirectory("temp");
    Path a = tempDir.resolve("a");
    Path a_b_c_d = a.resolve("b").resolve("c").resolve("d");
    Path a_f = tempDir.resolve("a").resolve("f");

    Files.createDirectories(a_b_c_d);
    Files.createFile(a_f);
    return tempDir;
  }

}
