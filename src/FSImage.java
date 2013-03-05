import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParentNode;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 10:28
 */
public class FSImage {
  private final Element fileTree;

  /*Should not be saved to xml serialization,
  for local use only.*/
  private final Path pathToRoot;


  private final String uuid = UUID.randomUUID().toString();
  public String getUuid() {
    return uuid;
  }

  FSImage(Element fileTree, Path pathToRoot) {
    this.fileTree = fileTree;
    this.pathToRoot = pathToRoot;
  }

  public String toXml() {
    return fileTree.toXML();
  }

  public Path getPathToRoot() {
    return pathToRoot;
  }

  public String getType(PseudoPath path) {
    Element element = getElement(path);
    if (element != null) {
      return element.getLocalName();
    } else {
      return null;
    }
  }

  Element getChildByName(Element element, String name) {
    Elements children = element.getChildElements();
    Element child;
    for(int i=0; i < children.size(); i++) {
      child = children.get(i);
      String childName = child.getAttributeValue("name");
      if (childName.equals(name)) {
        return child;
      }
    }
    return null;
  }

  Element getElement(PseudoPath path) {
    Element currentElement = fileTree;
    for(int i = 0; i < path.getNameCount(); i++) {
      currentElement = getChildByName(currentElement, path.getName(i));
    }
    return currentElement;
  }

  public String getRootAlias() {
    return fileTree.getAttributeValue("alias");
  }

  public void setRootAlias(String rootAlias) {
    fileTree.addAttribute(new Attribute("alias", rootAlias));
  }

  public synchronized void addToDirectory(PseudoPath dir, FSImage child) {
    if (!getType(dir).equals(FileType.DIR.getName())) {
      throw new IllegalArgumentException("Can add files only to directories.");
    }
    Element parent = getElement(dir);
    Element newChild = new Element(child.fileTree);

    String nameOfChildRoot = child.fileTree.getAttributeValue("name");
    PseudoPath pathToChild = dir.resolve(nameOfChildRoot);
    Element oldChild = getElement(pathToChild);
    if (oldChild != null) {
      parent.replaceChild(oldChild, newChild);
    } else {
      parent.appendChild(newChild);
    }
  }

  public synchronized void deletePath(PseudoPath path) {
    if (path.getNameCount() == 0) {
      throw new IllegalArgumentException("Empty path can not be deleted.");
    }

    Element toBeDeleted = getElement(path);
    if (toBeDeleted != null) {
      ParentNode parent = toBeDeleted.getParent();
      parent.removeChild(toBeDeleted);
    }
  }

  public boolean contains(PseudoPath path) {
    Element element = getElement(path);
    return element != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FSImage fsImage = (FSImage) o;

    String thisXml = this.toXml();
    String thatXml = fsImage.toXml();

    if (!thisXml.equals(thatXml)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return fileTree.toXML().hashCode();
  }
}
