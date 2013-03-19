package ru.niktrop.remote_access.file_system_model;

import java.util.*;

import static ru.niktrop.remote_access.file_system_model.FSImages.byAlias;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 14:37
 */
public class FSImageCollection {

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
}
