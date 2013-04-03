package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.NotificationManager;

import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 12:29
 */

/**
 * Command for showing different kinds of notification dialogs in the GUI.
 * Execution of
 * Execution of Notification.operationStarted() should be followed by Notification.operationFinished()
 * or Notification.operationFailed().
 * */
public class Notification implements SerializableCommand{
  private final NotificationType type;
  private final String message;
  private final String operationUuid;

  //Only for deserialization.
  Notification() {
    this(null, null, null);
  }

  private Notification(NotificationType type, String message, String operationUuid) {
    this.type = type;
    this.message = message;
    this.operationUuid = operationUuid;
  }

  public NotificationType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getOperationUuid() {
    return operationUuid;
  }

  public static Notification warning(String message) {
    return new Notification(NotificationType.WARNING, message, null);
  }

  public static Notification plain(String message) {
    return new Notification(NotificationType.PLAIN, message, null);
  }

  public static Notification operationStarted(String message, String uuid) {
    return new Notification(NotificationType.OPERATION_STARTED, message, uuid);
  }

  public static Notification operationContinued(String message, String uuid) {
    return new Notification(NotificationType.OPERATION_CONTINUED, message, uuid);
  }

  public static Notification operationFinished(String message, String uuid) {
    return new Notification(NotificationType.OPERATION_FINISHED, message, uuid);
  }

  public static Notification operationFailed(String message, String uuid) {
    return new Notification(NotificationType.OPERATION_FAILED, message, uuid);
  }

  @Override
  public void execute(Controller controller) {
    if (controller.isClient()) {
      NotificationManager nm = controller.getNotificationManager();
      nm.show(this);
    } else {
      CommandManager cm = controller.getCommandManager();
      cm.sendCommand(this);
    }
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(type.name());
    builder.append(groupSeparator);

    builder.append(message);

    if (operationUuid != null && operationUuid != "") {
      builder.append(groupSeparator);
      builder.append(operationUuid);
    }
    return builder.toString();
  }

  @Override
  public Notification fromString(String string) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(string, groupSeparator, false);
    NotificationType type = NotificationType.valueOf(st.nextToken());
    String message = st.nextToken();
    String operationUuid = null;
    if (st.hasMoreTokens()) {
      operationUuid = st.nextToken();
    }
    return new Notification(type, message, operationUuid);
  }

  public static enum NotificationType {
    WARNING,
    PLAIN,
    OPERATION_STARTED,
    OPERATION_CONTINUED,
    OPERATION_FINISHED,
    OPERATION_FAILED;
  }
}
