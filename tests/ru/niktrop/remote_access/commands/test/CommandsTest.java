package ru.niktrop.remote_access.commands.test;

import org.junit.Test;
import ru.niktrop.remote_access.commands.ChangeType;
import ru.niktrop.remote_access.commands.Commands;
import ru.niktrop.remote_access.commands.FSChange;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 08.03.13
 * Time: 14:08
 */
public class CommandsTest {
  @Test
  public void serializeAndDeserializeFSChange() throws Exception {
    PseudoPath path = new PseudoPath("a", "b");
    FSChange change_1 = new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"));
    FSChange change_2 =
            new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"), "<test xml />");

    String serialized_1 = Commands.serializeToString(change_1);
    String serialized_2 = Commands.serializeToString(change_2);

    FSChange deserialized_1 = (FSChange) Commands.getFromString(serialized_1);
    FSChange deserialized_2 = (FSChange) Commands.getFromString(serialized_2);

    assertThat(deserialized_1.getChangeType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(deserialized_1.getFsiUuid()).isEqualTo("test");
    assertThat(deserialized_1.getPath()).isEqualTo(path);

    assertThat(deserialized_2.getChangeType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(deserialized_2.getFsiUuid()).isEqualTo("test");
    assertThat(deserialized_2.getPath()).isEqualTo(path);
    assertThat(deserialized_2.getXmlFSImage()).isEqualTo("<test xml />");


  }
}
