package ru.niktrop.remote_access.file_system_model;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 16:28
 */
public enum FileType {
  //only file and directory are really used
  DIR("directory"),
  FILE("file"),
  SYMLINK("symlink"),
  OTHER("other");

  private FileType(String name) {
    this.name = name;
  }

  private String name;

  public String getName() {
    return this.name;
  }
}
