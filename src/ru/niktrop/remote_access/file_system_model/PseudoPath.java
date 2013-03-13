package ru.niktrop.remote_access.file_system_model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 11:27
 */
public class PseudoPath {
  private List<String> names;

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
    this.names = new ArrayList<>();
    //empty Path represented by path with one empty filename
    int count = path.getNameCount();
    String name = path.getFileName().toString();
    if (path.getNameCount() == 1 && path.getFileName().toString().equals("")) {
      return;
    }
    for(int i = 0; i < path.getNameCount(); i++) {
      this.names.add(path.getName(i).toString());
    }
  }

  public Path toPath() {
    int size = names.size();
    if (size == 0) {
      return Paths.get(null);
    }
    String firstName = names.get(0);
    String[] otherNames = names.subList(1, size).toArray(new String[]{});

    return Paths.get(firstName, otherNames);
  }

  public String getName(int i) {
    return this.names.get(i);
  }

  public int getNameCount() {
    return this.names.size();
  }

  public PseudoPath resolve(String name) {
    List<String> newNames = new ArrayList<>(names);
    newNames.add(name);
    return new PseudoPath(newNames);
  }

  public PseudoPath getParent() {
    if (this.names.size() == 0) {
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
}