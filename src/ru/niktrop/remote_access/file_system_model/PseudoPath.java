package ru.niktrop.remote_access.file_system_model;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 11:27
 */

/**
 * List of string names, represents abstract path in FSImage.
 * Can be serialized to string.
 * */
public class PseudoPath {
  private List<String> names;
  private static String unitSeparator = "\u001F";
  private static String nul = "\u0000";

  public PseudoPath(List<String> names) {
    if (names == null)
      this.names = new ArrayList<>();
    else
      this.names = names;
  }

  public PseudoPath(String... names) {
    this.names = new ArrayList<>();
    for(String name:names) {
      this.names.add(name);
    }
  }

  public PseudoPath(Path path) {
    names = new ArrayList<>();
    //empty Path represented by path with one empty filename
    int count = path.getNameCount();
    String name = path.getFileName().toString();
    if (count == 1 && name.equals("")) {
      return;
    }
    for(int i = 0; i < count; i++) {
      names.add(path.getName(i).toString());
    }
  }

  public Path toPath() {
    String firstName = "";
    String[] otherNames  = names.toArray(new String[]{});

    return Paths.get(firstName, otherNames);
  }

  public String getName(int i) {
    return names.get(i);
  }

  public String getFileName() {
    return names.get(names.size() - 1);
  }

  public int getNameCount() {
    return names.size();
  }

  public PseudoPath resolve(String name) {
    List<String> newNames = new ArrayList<>(names);
    newNames.add(name);
    return new PseudoPath(newNames);
  }

  public PseudoPath getParent() {
    if (names.size() == 0) {
      return null;
    }
    List<String> newNames = new ArrayList<>(names);
    newNames.remove(newNames.size() - 1);
    return new PseudoPath(newNames);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PseudoPath that = (PseudoPath) o;

    if (!names.equals(that.names)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return names.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    String separator = FileSystems.getDefault().getSeparator();
    for(int i = 0; i < getNameCount(); i++) {
      builder.append(separator);
      builder.append(getName(i));
    }
    return builder.toString();
  }

  public String serializeToString() {
    StringBuilder builder = new StringBuilder();

    if (getNameCount() == 0) {
      return nul;
    }

    for (int i = 0; i < getNameCount(); i++) {
      builder.append(getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }

  public static PseudoPath deserialize(String representation) {

    if (nul.equals(representation)) {
      return new PseudoPath();
    }
    StringTokenizer st = new StringTokenizer(representation, unitSeparator, false);
    PseudoPath path = new PseudoPath();
    while (st.hasMoreTokens()) {
      path = path.resolve(st.nextToken());
    }
    return path;
  }
}
