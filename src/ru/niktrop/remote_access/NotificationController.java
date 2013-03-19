package ru.niktrop.remote_access;

import ru.niktrop.remote_access.commands.Notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 14:47
 */
public class NotificationController {
  private JFrame parentFrame;
  private Map<String, JDialog> dialogs = new HashMap<>();

  public void show(Notification n) {
    switch (n.getType()) {
      case WARNING:
        showWarning(n.getMessage());
        break;
      case OPERATION_STARTED:
        showOperationStarted(n.getMessage(), n.getOperationUuid());
        break;
      case OPERATION_FINISHED:
        showOperationFinished(n.getMessage(), n.getOperationUuid());
        break;
      case OPERATION_FAILED:
        showOperationFailed(n.getMessage(), n.getOperationUuid());
        break;
    }
  }

  public JFrame getParentFrame() {
    return parentFrame;
  }

  public void setParentFrame(JFrame parentFrame) {
    this.parentFrame = parentFrame;
  }

  private void showWarning(String message) {
    JOptionPane.showMessageDialog(parentFrame,
            message,
            "Warning",
            JOptionPane.WARNING_MESSAGE);
  }

  private void showOperationStarted(String message, String uuid) {
    PendingOperationDialog dialog = new PendingOperationDialog(parentFrame, message);
    dialogs.put(uuid, dialog);
    dialog.setVisible(true);
  }

  private void showOperationFinished(String message, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.operationFinished(message);
    dialogs.remove(uuid);
  }

  private void showOperationFailed(String message, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.operationFailed(message);
    dialogs.remove(uuid);
  }

  static class PendingOperationDialog extends JDialog {
    private String message;
    private JButton okButton;
    private JOptionPane optionPane;

    PendingOperationDialog(Frame owner, String message) {
      super(owner, false);
      this.message = message;
      setTitle("Operation in progress");
      okButton = new JButton( "OK" );

      okButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          PendingOperationDialog.this.dispose();
        }
      });

      okButton.setEnabled(false);
      optionPane = new JOptionPane( message,
              JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION,
              null,
              new JButton[] { okButton } );
      setContentPane(optionPane);
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      setLocationByPlatform(true);
      setMinimumSize(getSize());
      pack();
    }

    void operationFinished(String message) {
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      okButton.setEnabled(true);
      setTitle("Operation complete");
      optionPane.setMessage(message);
    }

    void operationFailed(String message) {
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      okButton.setEnabled(true);
      setTitle("Operation failed");
      optionPane.setMessage(message);
      optionPane.setMessageType(JOptionPane.WARNING_MESSAGE);
    }
  }
}

