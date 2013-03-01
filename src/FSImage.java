import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 10:28
 */
public class FSImage {
  private Element fileTree;

  FSImage(Element fileTree) {
    this.fileTree = fileTree;
  }

  public String toXml() {
    return fileTree.toXML();
  }


  String getType(PseudoPath path) {
    Element element = getElement(path);
    return element.getLocalName();
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
