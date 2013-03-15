package ru.niktrop.remote_access.file_system_model;

import nu.xom.Element;
import nu.xom.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 26.02.13
 * Time: 21:24
 */
public class PseudoFile {
  private final PseudoPath path;
  private final FSImage fsi;

  public PseudoFile(FSImage fsi, PseudoPath path) {

    this.fsi = fsi;

    if (path == null)
      this.path = new PseudoPath();
    else
      this.path = path;
  }

  public String getType() {
    return fsi.getType(path);
  }

  public PseudoPath getPseudoPath() {
    return path;
  }

  public boolean isDirectory() {
    return getType().equals(FileType.DIR.getName());
  }

  public List<PseudoFile> getContent() {
    Element parent = fsi.getElement(path);
    List<PseudoFile> content = new ArrayList<>();
    Elements children = parent.getChildElements();
    for(int i=0; i < children.size(); i++) {
      String childName = children.get(i).getAttributeValue("name");
      content.add(new PseudoFile(fsi, path.resolve(childName)));
    }
    return content;
  }

  public List<String> getContentNames() {
    List<String> names = new ArrayList<>();
    for (PseudoFile file : getContent()) {
      names.add(file.getName());
    }
    return names;
  }

  public PseudoFile getParent() {
    PseudoPath path = getPseudoPath();

    //empty pseudopath has no parent
    if (path.getNameCount() == 0) {
      return null;
    }

    PseudoPath parent = path.getParent();
    return new PseudoFile(fsi, parent);
  }

  public String getName() {
    int count = path.getNameCount();
    if (count > 0)
      return path.getName(count - 1);
    else
      return null;
  }

  public String getFsiUuid() {
    return fsi.getUuid();
  }

  public int getDepth() {
    Element element = fsi.getElement(path);
    String depth = element.getAttributeValue("depth");
    if (depth == null) {
      return -1;
    }
    else return Integer.parseInt(depth);
  }


  public String serializeToString() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';

    builder.append(fsi.getUuid());
    builder.append(groupSeparator);
    for (int i = 0; i < path.getNameCount(); i++) {
      builder.append(path.getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PseudoFile that = (PseudoFile) o;

    if (fsi != null ? !fsi.equals(that.fsi) : that.fsi != null) return false;
    if (path != null ? !path.equals(that.path) : that.path != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = path != null ? path.hashCode() : 0;
    result = 31 * result + (fsi != null ? fsi.hashCode() : 0);
    return result;
  }
}
