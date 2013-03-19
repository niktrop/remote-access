package ru.niktrop.remote_access.file_system_model.test;

import org.junit.AfterClass;
import org.junit.Test;
import ru.niktrop.remote_access.file_system_model.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.Queue;
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
  private static Queue<Path> tempDirs = new LinkedList<>();
  private static final int MAX_TRIES = 20;

  @AfterClass
  public static void clean() {
    while ( !tempDirs.isEmpty()) {
      deleteTempDir(tempDirs.poll(), MAX_TRIES);
    }
  }

  @Test
  public void varMaxDepth() throws Exception {
    Path tempDir = createTempDir();
    Path a = tempDir.resolve("a");

    WatchService watcher = FileSystems.getDefault().newWatchService();

    FSImage fsi_0 = FSImages.getFromDirectory(a, 0, watcher);
    PseudoFile root = new PseudoFile(fsi_0, new PseudoPath());

    assertThat(fsi_0.contains(new PseudoPath())).isTrue();
    assertThat(root.getContent().size() == 0).isTrue();

    FSImage fsi_1 = FSImages.getFromDirectory(a, 1, watcher);
    root = new PseudoFile(fsi_1, new PseudoPath());
    PseudoFile b = new PseudoFile(fsi_1, new PseudoPath("b"));
    PseudoFile f = new PseudoFile(fsi_1, new PseudoPath("f"));

    assertThat(root.getContent().size() == 2).isTrue();
    assertThat(root.getContent()).containsExactly(b, f);
    assertThat(b.getContent().size() == 0);
    assertThat(b.isDirectory()).isTrue();
    assertThat(f.getType()).isEqualTo(FileType.FILE.getName());
    assertThat(b.getDepth()).isEqualTo(f.getDepth()).isEqualTo(0);


    FSImage fsi_3 = FSImages.getFromDirectory(a, 3, watcher);
    PseudoFile b_c_d = new PseudoFile(fsi_3, new PseudoPath("b","c","d"));
    b = new PseudoFile(fsi_3, new PseudoPath("b"));

    assertThat(b.getDepth() == 2).isTrue();
    assertThat(b_c_d.getDepth() == 0).isTrue();

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

    tempDirs.offer(tempDir);
    return tempDir;
  }

}
