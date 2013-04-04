package ru.niktrop.remote_access.controller;

import ru.niktrop.remote_access.commands.Notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 19.03.13
 * Time: 14:47
 */

/**
 * This class is responsible for showing notifications to the user.
 * */
public class NotificationManager {
  private static final Logger LOG = Logger.getLogger(NotificationManager.class.getName());

  private JFrame parentFrame;
  private Map<String, JDialog> dialogs = new ConcurrentHashMap<>();

  private final ExecutorService notificationDemonstrator = Executors.newSingleThreadExecutor();

  //Time (in milliseconds) to close dialog after operation is finished successfully
  private final long SHOW_FINISHED_TIME = 0L;


  public void show(Notification notification) {
    notificationDemonstrator.submit(new ShowNotificationTask(notification));
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
    dialog.toFront();
  }

  private void showOperationContinued(String message, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.setMessage(message);
    dialog.setTitle("Operation in progress");
    dialog.toFront();
    dialog.pack();
  }

  private void showOperationFinished(String message, String uuid) {
    final PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialogs.remove(uuid);
    dialog.operationFinished(message);
    dialog.toFront();
    dialog.pack();

    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(SHOW_FINISHED_TIME);
        } catch (InterruptedException e) {
        }
        dialog.dispose();
      }
    }.start();
  }

  private void showOperationFailed(String message, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialogs.remove(uuid);
    dialog.operationFailed(message);
    dialog.toFront();
    dialog.pack();
  }

  private void setVisibility(boolean on, String uuid) {
    PendingOperationDialog dialog = (PendingOperationDialog) dialogs.get(uuid);
    dialog.setVisible(on);
  }

  static class PendingOperationDialog extends JDialog {
    private JButton okButton;
    private JOptionPane optionPane;

    PendingOperationDialog(Frame owner, String message) {
      super(owner, false);
      setTitle("Operation started");
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

    void setMessage(String message) {
      optionPane.setMessage(message);
    }
  }

  private class ShowNotificationTask implements Runnable {

    private final Notification notification;

    ShowNotificationTask(Notification notification) {
      this.notification = notification;
    }

    @Override
    public void run() {
      String message = notification.getMessage();
      try {
        switch (notification.getType()) {
          case WARNING:
            showWarning(message);
            break;
          case PLAIN:
            showPlain(message);
            break;
          case OPERATION_STARTED:
            showOperationStarted(message, notification.getOperationUuid());
            break;
          case OPERATION_CONTINUED:
            showOperationContinued(message, notification.getOperationUuid());
            break;
          case OPERATION_FINISHED:
            showOperationFinished(message, notification.getOperationUuid());
            break;
          case OPERATION_FAILED:
            showOperationFailed(message, notification.getOperationUuid());
            break;
          case SET_VISIBILITY:
            setVisibility(Boolean.parseBoolean(message), notification.getOperationUuid());
            break;
        }
      } catch (Exception e) {
        String msg = String.format("Exception while showing notification %s", message);
        LOG.log(Level.WARNING, msg, e);
      }
    }
  }
}

