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
public class NotificationManager {
  private JFrame parentFrame;
  private Map<String, JDialog> dialogs = new HashMap<>();
  private final long TIME_TO_CLOSE = 100L;

  public void show(Notification n) {
    switch (n.getType()) {
      case WARNING:
        showWarning(n.getMessage());
        break;
      case PLAIN:
        showPlain(n.getMessage());
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

  private void showPlain(String message) {
    JOptionPane.showMessageDialog(parentFrame,
            message,
            "",
            JOptionPane.PLAIN_MESSAGE);
  }

  private void showOperationStarted(String message, String uuid) {
    PendingOperationDialog dialog = new PendingOperationDialog(parentFrame, message);
    dialogs.put(uuid, dialog);
    dialog.setVisible(true);
  }

  private void showOperationFinished(String message, String uuid) {
    final PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.operationFinished(message);
    dialogs.remove(uuid);

    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(TIME_TO_CLOSE);
        } catch (InterruptedException e) {
        }
        dialog.dispose();
      }
    }.start();

  }

  private void showOperationFailed(String message, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.operationFailed(message);
    dialogs.remove(uuid);
  }

  static class PendingOperationDialog extends JDialog {
    private JButton okButton;
    private JOptionPane optionPane;

    PendingOperationDialog(Frame owner, String message) {
      super(owner, false);
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
      setDefaultCloseOperation(HIDE_ON_CLOSE);
      setLocation(0, 0);
      //setLocationRelativeTo(null);
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

      setVisible(true);
      setTitle("Operation failed");
      optionPane.setMessage(message);
      optionPane.setMessageType(JOptionPane.WARNING_MESSAGE);
    }
  }
}

