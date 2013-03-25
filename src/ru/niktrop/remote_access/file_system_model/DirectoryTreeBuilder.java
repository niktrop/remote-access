package ru.niktrop.remote_access.file_system_model;

import nu.xom.Attribute;
import nu.xom.Element;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 23.03.13
 * Time: 13:51
 */

/**
 * Builds FSImage, containing only and all subdirectories of a given directory.
 * */
public class DirectoryTreeBuilder extends SimpleFileVisitor<Path> {
  private static final Logger LOG = Logger.getLogger(FileTreeBuilder.class.getName());

  private HashMap<Path, Element> map = new HashMap<>();
  private final Element rootDirElement;

  public DirectoryTreeBuilder (Element rootDirElement) {
    this.rootDirElement = rootDirElement;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
    try {
      addPathToTree(dir);
      return FileVisitResult.CONTINUE;
    } catch (IOException e) {
      e.printStackTrace();
      return FileVisitResult.SKIP_SUBTREE;
    }
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    LOG.info("Visit file failed: " + file.toString());
    return FileVisitResult.CONTINUE;
  }

  private void addPathToTree(Path path) throws IOException {
    Element element = getElement(path);
    Element parent = map.get(path.getParent());
    if (parent != null) {
      map.put(path, element);
      parent.appendChild(element);
    }
    else {
      map.put(path, rootDirElement);
      setName(rootDirElement, path);
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
    if (path.getNameCount() > 0) {
      String fileName = path.getFileName().toString();
      Attribute name =  new Attribute("name", fileName);
      element.addAttribute(name);
    }
  }
}
