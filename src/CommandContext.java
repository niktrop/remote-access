import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 07.03.13
 * Time: 23:14
 */
public class CommandContext {
  private Map<String, FSImage> fsImageMap;
  private WatchService watcher;

  public CommandContext(Map<String, FSImage> fsImageMap, WatchService watcher) {
    this.fsImageMap = fsImageMap;
    this.watcher = watcher;
  }

  public CommandContext() throws IOException {
    this.fsImageMap = new HashMap<>();
    this.watcher = FileSystems.getDefault().newWatchService();
  }

  public Map<String, FSImage> getFsImageMap() {
    return fsImageMap;
  }

  public WatchService getWatcher() {
    return watcher;
  }

}
