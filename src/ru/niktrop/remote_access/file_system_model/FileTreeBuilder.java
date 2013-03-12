package ru.niktrop.remote_access.file_system_model;

import nu.xom.Attribute;
import nu.xom.Element;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 13:27
 */
public class FileTreeBuilder extends SimpleFileVisitor<Path> {
  private HashMap<Path, Element> map = new HashMap<>();
  private final Element rootDirElement;
  private final WatchService watcher;
  private int currentDepth = 0;
  private final int maxDepth;

  public FileTreeBuilder(Element rootDirElement, WatchService watcher, int maxDepth) {
    this.rootDirElement = rootDirElement;
    this.watcher = watcher;
    this.maxDepth = maxDepth;
  }


  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
    if (currentDepth <= maxDepth) {
      if (!Files.isReadable(dir) || Files.isSymbolicLink(dir))
        return FileVisitResult.SKIP_SUBTREE;
      try {
        addPathToTree(dir, currentDepth);
        dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
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
    if (currentDepth <= maxDepth) {
      if (!Files.isReadable(file) || Files.isSymbolicLink(file))
        return FileVisitResult.CONTINUE;
      try {
        addPathToTree(file, currentDepth);
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

  private void addPathToTree(Path path, Integer currentDepth) throws IOException {
    Element element = getElement(path);
    Attribute depth = new Attribute("depth", currentDepth.toString());
    element.addAttribute(depth);
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
