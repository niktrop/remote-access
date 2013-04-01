package ru.niktrop.remote_access.file_system_model;

import java.nio.file.Path;
import java.util.*;

import static ru.niktrop.remote_access.file_system_model.FSImages.byAlias;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 14:37
 */

/**
 * Collection of FSImages. These can be local or not.
 * Returned lists are sorted by the alias of the FSImage.
 * */
public class FSImageCollection{

  //Keys are fsiUuid.
  private final Map<String, FSImage> fsImageMap = new HashMap<>();

  public FSImage get(String fsiUuid) {
    return fsImageMap.get(fsiUuid);
  }

  private Iterable<FSImage> getFSImages() {
    return fsImageMap.values();
  }

  /**
   * Returns list of local FSImages, sorted by root alias.
   * */
  public List<FSImage> getLocal() {
    List<FSImage> result = new ArrayList<>();
    for (FSImage fsImage : getFSImages()) {
      if (fsImage.isLocal()) {
        result.add(fsImage);
      }
    }
    Collections.sort(result, byAlias);
    return result;
  }

  /**
   * Returns list of remote FSImages, sorted by root alias.
   * */
  public List<FSImage> getRemote() {
    List<FSImage> result = new ArrayList<>();
    for (FSImage fsImage : getFSImages()) {
      if (!fsImage.isLocal()) {
        result.add(fsImage);
      }
    }
    Collections.sort(result, byAlias);
    return result;
  }

  public void addFSImage(FSImage fsi) {
    fsImageMap.put(fsi.getUuid(), fsi);
  }

  //returns local FSImage, containing path
  public FSImage findContainingFSImage(Path path) {
    for (FSImage fsImage : fsImageMap.values()) {
      Path pathToRoot = fsImage.getPathToRoot();

      //FSImage is not local
      if (pathToRoot == null)
        continue;

      if (path.startsWith(pathToRoot)) {

        Path relative = pathToRoot.relativize(path);
        if (fsImage.contains(new PseudoPath(relative))) {
          return fsImage;
        }
      }
    }
    return null;
  }

  public int size() {
    return fsImageMap.size();
  }

  public PseudoFile defaultDirectory() {
    List<FSImage> fsImages = new ArrayList<>(getLocal());
    fsImages.addAll(getRemote());
    return new PseudoFile(fsImages.get(0), new PseudoPath());
  }
}
