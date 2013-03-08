import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.commons.io.FileUtils.*;
import static org.fest.assertions.api.Assertions.assertThat;


/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 05.03.13
 * Time: 16:43
 */
public class FSChangeExecuteTest {
  private final int MAX_TRIES = 20;

  @Test
  public void testDelete() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path b_c = a.resolve("b").resolve("c");
    deleteTempDir(b_c, MAX_TRIES);

    listenAndReflectChanges(controller);

    String xmlResult =
            "<directory name=\"a\">" +
              "<directory name=\"b\" />" +
              "<file name=\"f\" />" +
            "</directory>";
    assertThat(fsi.toXml().equals(xmlResult));

    Path f = a.resolve("f");
    Files.delete(f);

    listenAndReflectChanges(controller);

    xmlResult =
            "<directory name=\"a\">" +
              "<directory name=\"b\" />" +
            "</directory>";
    assertThat(fsi.toXml().equals(xmlResult));

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testDeleteSeesWhatNeeded() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);
    String xmlBefore = fsi.toXml();

    Path tempDir_2 = createTempDir();
    deleteTempDir(tempDir_2, MAX_TRIES);

    listenAndReflectChanges(controller);

    assertThat(fsi.toXml().equals(xmlBefore));

    Path a_b_c_d = a.resolve("b").resolve("c").resolve("d");
    Files.delete(a_b_c_d);

    listenAndReflectChanges(controller);

    assertThat(fsi.toXml().equals(xmlBefore));

    deleteTempDir(tempDir, MAX_TRIES);

  }

  @Test
  public void testCreateFile() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path a_g = a.resolve("g");
    Path a_b_c_d_e = a.resolve("b").resolve("c").resolve("d").resolve("e");

    Files.createFile(a_g);
    Files.createFile(a_b_c_d_e);

    listenAndReflectChanges(controller);

    PseudoPath g = new PseudoPath("g");
    PseudoPath b_c_d_e = new PseudoPath("b", "c", "d", "e");

    assertThat(fsi.contains(g));
    assertThat(fsi.getType(g)).isEqualTo(FileType.FILE.getName());
    assertThat(fsi.contains(b_c_d_e)).isFalse();

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testCreateDir() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path a_g = a.resolve("g");
    Path a_g_h = a_g.resolve("h");
    Path a_b_c_d_e = a.resolve("b").resolve("c").resolve("d").resolve("e");

    Files.createDirectory(a_g);
    Files.createFile(a_g_h);
    Files.createDirectory(a_b_c_d_e);

    listenAndReflectChanges(controller);

    PseudoPath g = new PseudoPath("g");
    PseudoPath g_h = new PseudoPath("g", "h");
    PseudoPath b_c_d_e = new PseudoPath("b", "c", "d", "e");

    assertThat(fsi.contains(g));
    assertThat(fsi.getType(g)).isEqualTo(FileType.DIR.getName());
    assertThat(fsi.contains(g_h));
    assertThat(fsi.getType(g_h)).isEqualTo(FileType.FILE.getName());
    assertThat(fsi.contains(b_c_d_e)).isFalse();

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testMoveDirectoryInside() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path a_b_c = a.resolve("b").resolve("c");
    boolean createDestinationDir = true;
    moveDirectoryToDirectory(a_b_c.toFile(), a.toFile(), createDestinationDir);

    listenAndReflectChanges(controller);

    PseudoPath c = new PseudoPath("c");
    PseudoPath c_d = new PseudoPath("c", "d");
    PseudoPath b_c = new PseudoPath("b", "c");

    assertThat(fsi.contains(c));
    assertThat(fsi.contains(c_d));
    assertThat(fsi.contains(b_c)).isFalse();
    assertThat(fsi.getType(c)).isEqualTo(FileType.DIR.getName());
    assertThat(fsi.getType(c_d)).isEqualTo(FileType.DIR.getName());

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testCopyDirectoryInside() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path a_b_c = a.resolve("b").resolve("c");
    copyDirectoryToDirectory(a_b_c.toFile(), a.toFile());

    listenAndReflectChanges(controller);

    PseudoPath c = new PseudoPath("c");
    PseudoPath c_d = new PseudoPath("c", "d");
    PseudoPath b_c = new PseudoPath("b", "c");

    assertThat(fsi.contains(c));
    assertThat(fsi.contains(c_d));
    assertThat(fsi.contains(b_c));
    assertThat(fsi.getType(c)).isEqualTo(FileType.DIR.getName());
    assertThat(fsi.getType(c_d)).isEqualTo(FileType.DIR.getName());
    assertThat(fsi.getType(b_c)).isEqualTo(FileType.DIR.getName());

    deleteTempDir(tempDir, MAX_TRIES);
  }

  @Test
  public void testMoveDirectoryFromOutside() throws Exception {
    Controller controller = new Controller();
    WatchService watcher = controller.getWatcher();

    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");
    FSImage fsi = FSImages.getFromDirectory(a, 2, watcher);
    controller.addFSImage(fsi);

    Path tempSource = createTempDir();
    Path source_a = tempSource.resolve("a");
    Path a_b = a.resolve("b");

    boolean createDestinationDir = true;
    moveDirectoryToDirectory(source_a.toFile(), a_b.toFile(), createDestinationDir);

    listenAndReflectChanges(controller);

    PseudoPath b_a = new PseudoPath("b","a");
    PseudoPath b_a_b = b_a.resolve("b");
    PseudoPath b_a_f = b_a.resolve("f");
    PseudoPath b_a_b_c = b_a_b.resolve("c");
    PseudoPath b_a_b_c_d = b_a_b_c.resolve("d");

    assertThat(fsi.contains(b_a));
    assertThat(fsi.contains(b_a_b));
    assertThat(fsi.contains(b_a_b_c));
    assertThat(fsi.contains(b_a_b_c_d)).isFalse();
    assertThat(fsi.getType(b_a_b_c)).isEqualTo(FileType.DIR.getName());
    assertThat(fsi.getType(b_a_f)).isEqualTo(FileType.FILE.getName());

    deleteTempDir(tempDir, MAX_TRIES);
    deleteTempDir(tempSource, MAX_TRIES);
  }

  /*Creates temp directory with following structure:
          temp/a/b/c/d/
          temp/a/f
          */
  private static Path createTempDir() throws IOException {
    Path tempDir = Files.createTempDirectory("temp");
    Path a = tempDir.resolve("a");
    Path a_b_c_d = a.resolve("b").resolve("c").resolve("d");
    Path a_f = tempDir.resolve("a").resolve("f");

    Files.createDirectories(a_b_c_d);
    Files.createFile(a_f);
    return tempDir;
  }

  //Sometimes it is not deleted at the first time.
  private static void deleteTempDir(Path tempDir, int times) {
    for (int i = 0; i<times; i++) {
      try {
        deleteDirectory(tempDir.toFile());
        return;
      } catch (IOException e) {
        //System.out.println("Let's try once more...");
      }
    }
  }

  private static void listenAndReflectChanges(Controller controller) throws InterruptedException {
    Thread.sleep(10L);

    BlockingQueue<FSChange> fsChanges = new LinkedBlockingQueue<>();
    controller.enqueueChanges(fsChanges);
    CommandContext context = controller.getContext();
    while (!fsChanges.isEmpty()) {
      fsChanges.poll().execute(context);
    }
  }

}
