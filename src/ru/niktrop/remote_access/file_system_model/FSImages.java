package ru.niktrop.remote_access.file_system_model;

import nu.xom.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.UUID;

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

      fileTree.addAttribute(new Attribute("alias", dir.toString()));

      String uuid = UUID.randomUUID().toString();
      fileTree.addAttribute(new Attribute("uuid", uuid));

      return new FSImage(fileTree, dir);
    }
  }

  public static FSImage getFromXml(String xmlFileTree) throws ParsingException, IOException {
    Builder builder = new Builder();
    Document fileTreeDoc = builder.build(xmlFileTree, null);
    return new FSImage(fileTreeDoc.getRootElement(), null);
  }

  public static Comparator<FSImage> byAlias = new Comparator<FSImage>() {
    @Override
    public int compare(FSImage o1, FSImage o2) {
      String alias1 = o1.getRootAlias();
      String alias2 = o2.getRootAlias();
      if (alias1 == null)
        return -1;
      else if (alias2 == null)
        return 1;
      else
        return alias1.compareTo(alias2);
    }
  };
}
