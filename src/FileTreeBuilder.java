import nu.xom.Attribute;
import nu.xom.Element;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 13:27
 */
public class FileTreeBuilder extends SimpleFileVisitor<Path> {
  private HashMap<Path, Element> map = new HashMap<>();
  private final Element root;
  private final DirectoryWatcher watcher;
  private int currentDepth = 0;
  private final int maxDepth;

  public FileTreeBuilder(Element root, DirectoryWatcher watcher, int maxDepth) {
    this.root = root;
    this.watcher = watcher;
    this.maxDepth = maxDepth;
  }


  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
    if (currentDepth < maxDepth) {
      if (!Files.isReadable(dir) || Files.isSymbolicLink(dir))
        return FileVisitResult.SKIP_SUBTREE;
      try {
        addPathToTree(dir);
        watcher.register(dir);
        currentDepth++;
        return FileVisitResult.CONTINUE;
      } catch (IOException e) {
        e.printStackTrace();
        return FileVisitResult.SKIP_SUBTREE;
      }
    } else return FileVisitResult.SKIP_SIBLINGS;

  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
    if (currentDepth < maxDepth) {
      if (!Files.isReadable(file) || Files.isSymbolicLink(file))
        return FileVisitResult.CONTINUE;
      try {
        addPathToTree(file);
        return FileVisitResult.CONTINUE;
      } catch (IOException e) {
        e.printStackTrace();
        return FileVisitResult.CONTINUE;
      }
    } else return FileVisitResult.SKIP_SIBLINGS;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    currentDepth--;
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    //TODO добавить логирование
    //System.out.println(file.toString() + " visiting fails");
    return FileVisitResult.CONTINUE;
  }

  private void addPathToTree(Path path) throws IOException {
    Element element = getElement(path);
    map.put(path, element);
    Element parent = map.get(path.getParent());
    if (parent != null) {
      parent.appendChild(element);
    }
    else {
      root.appendChild(element);
    }
  }

  private Element getElement(Path path) throws IOException {
    Element element = new Element("default");
    setType(element, path);
    setName(element, path);
    return element;
  }

  //Set type of file as name of the element
  private void setType(Element element, Path path) {
    if (Files.isDirectory(path)) {
      element.setLocalName(FileType.DIR.getName());
      return;
    }
    else if (Files.isRegularFile(path)) {
      element.setLocalName(FileType.FILE.getName());
      return;
    }
    else if (Files.isSymbolicLink(path)) {
      element.setLocalName(FileType.SYMLINK.getName());
      return;
    }
    else
      element.setLocalName(FileType.OTHER.getName());
  }

  //Set file name to one of the attributes of the elements
  private void setName(Element element, Path path) {
    String fileName = (path.getNameCount() > 0) ?
            path.getFileName().toString(): null;
    Attribute name =  new Attribute("name", fileName);
    element.addAttribute(name);

  }


}
