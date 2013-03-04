import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 10:33
 */
public class FSImages {

  public static FSImage getFromDirectory(Path dir, int maxDepth, WatchService watcher)
          throws IOException {
    if (!Files.isDirectory(dir)) {
      throw new IllegalArgumentException("Path should be directory.");
    }
    else {
      Element fileTree = new Element("directory");

      FileTreeBuilder fileTreeBuilder = new FileTreeBuilder(fileTree, watcher, maxDepth);
      Files.walkFileTree(dir, fileTreeBuilder);

      return new FSImage(fileTree, dir);
    }
  }

  public static FSImage getFromXml(String xmlFileTree) throws ParsingException, IOException {
    Builder builder = new Builder();
    Document fileTreeDoc = builder.build(xmlFileTree, null);
    return new FSImage(fileTreeDoc.getRootElement(), null);
  }
}
