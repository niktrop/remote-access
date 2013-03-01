import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 10:33
 */
public class FSImages {

  public static FSImage getFromDirectory(Path dir, int maxDepth, DirectoryWatcher watcher)
          throws IOException {
    if (!Files.isDirectory(dir)) {
      throw new IllegalArgumentException("Path should be directory.");
    }
    else {
      Element rootDir = new Element("directory");

      FileTreeBuilder fileTreeBuilder = new FileTreeBuilder(rootDir, watcher, maxDepth);
      Files.walkFileTree(dir, fileTreeBuilder);

      Element fileTree = rootDir;
      return new FSImage(fileTree);
    }
  }

  public static FSImage getFromXml(String xmlFileTree) throws ParsingException, IOException {
    Builder builder = new Builder();
    Document fileTreeDoc = builder.build(xmlFileTree, null);
    return new FSImage(fileTreeDoc.getRootElement());
  }
}
