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

  public static FSImage getFromDirectory(Path path, int maxDepth, DirectoryWatcher watcher)
          throws IOException {
    Element root = new Element("root");

    FileTreeBuilder fileTreeBuilder = new FileTreeBuilder(root, watcher, maxDepth);
    Files.walkFileTree(path, fileTreeBuilder);

    Element fileTree = root;
    return new FSImage(fileTree);
  }

  public static FSImage getFromXml(String xmlFileTree) throws ParsingException, IOException {
    Builder builder = new Builder();
    Document fileTreeDoc = builder.build(xmlFileTree, null);
    return new FSImage(fileTreeDoc.getRootElement());
  }
}
