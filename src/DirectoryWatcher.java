import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 16:03
 */
public class DirectoryWatcher {
  private final WatchService watcher;
  private final Map<WatchKey,Path> paths;

  public DirectoryWatcher(WatchService watcher) {
    this.watcher = watcher;
    this.paths = new HashMap<>();
  }

  public void register(Path dir) throws IOException {
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
    paths.put(key, dir);
  }

  public Path getPath(WatchKey key) {
    return paths.get(key);
  }
}
