package ru.niktrop.remote_access.commands.test;

import org.junit.Test;
import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:51
 */
public class FSChangeTest {
  @Test
  public void FSChangeToStringAndBack() throws Exception {
    PseudoPath path = new PseudoPath("a", "b");
    FSChange change = new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"));
    FSChange change2 =
            new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"), "<test xml />");

    String changeAsString = change.getStringRepresentation();
    String change2AsString = change2.getStringRepresentation();

    FSChange instance = FSChange.getEmptyInstance();
    FSChange changeFromString = instance.fromString(changeAsString);
    FSChange change2FromString = instance.fromString(change2AsString);

    assertThat(changeFromString.getChangeType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(changeFromString.getFsiUuid()).isEqualTo("test");
    assertThat(changeFromString.getPath()).isEqualTo(path);

    assertThat(change2FromString.getChangeType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(change2FromString.getFsiUuid()).isEqualTo("test");
    assertThat(change2FromString.getPath()).isEqualTo(path);
    assertThat(change2FromString.getXmlFSImage()).isEqualTo("<test xml />");
  }
}