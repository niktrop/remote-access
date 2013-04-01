package ru.niktrop.remote_access.file_system_model;

import nu.xom.Element;
import nu.xom.Elements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 26.02.13
 * Time: 21:24
 */

/**
 * Represents a real node in the specific FSImage.
 * */
public class PseudoFile {
  private final PseudoPath pseudoPath;
  private final FSImage fsImage;

  public PseudoFile(FSImage fsImage, PseudoPath pseudoPath) {

    this.fsImage = fsImage;

    if (pseudoPath == null)
      this.pseudoPath = new PseudoPath();
    else
      this.pseudoPath = pseudoPath;
  }

  public String getType() {
    return fsImage.getType(pseudoPath);
  }

  public FSImage getFsImage() {
    return fsImage;
  }

  public PseudoPath getPseudoPath() {
    return pseudoPath;
  }

  public boolean isDirectory() {
    return getType().equals(FileType.DIR.getName());
  }

  public boolean exists() {
    return fsImage.contains(pseudoPath);
  }

  public List<PseudoFile> getContent() {
    Element parent = fsImage.getElement(pseudoPath);
    List<PseudoFile> content = new ArrayList<>();
    Elements children = parent.getChildElements();
    for(int i=0; i < children.size(); i++) {
      String childName = children.get(i).getAttributeValue("name");
      content.add(new PseudoFile(fsImage, pseudoPath.resolve(childName)));
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
    return new PseudoFile(fsImage, parent);
  }

  public String getName() {
    int count = pseudoPath.getNameCount();
    if (count > 0)
      return pseudoPath.getName(count - 1);
    else
      return null;
  }

  public String getFsiUuid() {
    return fsImage.getUuid();
  }

  public int getDepth() {
    Element element = fsImage.getElement(pseudoPath);
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

    builder.append(fsImage.getUuid());
    builder.append(groupSeparator);
    for (int i = 0; i < pseudoPath.getNameCount(); i++) {
      builder.append(pseudoPath.getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }

  /**
   * Returns full pseudoPath to this Pseudofile if FSImage is local (knows pseudoPath to root),
   * and relative pseudoPath if it is not.
   * */
  public Path toPath() {
    if (fsImage.isLocal()) {
      return fsImage.getPathToRoot().resolve(pseudoPath.toPath());
    }
    else {
      return pseudoPath.toPath();
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PseudoFile that = (PseudoFile) o;

    if (fsImage != null ? !fsImage.equals(that.fsImage) : that.fsImage != null) return false;
    if (pseudoPath != null ? !pseudoPath.equals(that.pseudoPath) : that.pseudoPath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = pseudoPath != null ? pseudoPath.hashCode() : 0;
    result = 31 * result + (fsImage != null ? fsImage.hashCode() : 0);
    return result;
  }
}
