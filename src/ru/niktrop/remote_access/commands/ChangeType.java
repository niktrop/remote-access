package ru.niktrop.remote_access.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:38
 */
public enum ChangeType {
  CREATE_DIR,
  CREATE_FILE,
  DELETE,
  NEW_IMAGE;

  public static ChangeType getChangeType(WatchEvent<Path> event, Path fullPath) {
    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
      return ChangeType.DELETE;
    }
    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
      if (Files.isDirectory(fullPath))
        return ChangeType.CREATE_DIR;
      else
        return ChangeType.CREATE_FILE;
    } else throw new IllegalStateException("Non standard WatchEvent or wrong Path.");
  }
}
