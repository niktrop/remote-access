package ru.niktrop.remote_access;

import ru.niktrop.remote_access.commands.FSChange;

import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 15:32
 */
public class FSChangeHandler {
  private static final Logger LOG = Logger.getLogger(FSChangeHandler.class.getName());

  private final FileSystemWatcher fsWatcher;
  private final Controller controller;

  public FSChangeHandler(FileSystemWatcher fsWatcher, Controller controller) {
    this.fsWatcher = fsWatcher;
    this.controller = controller;
  }

  public void runHandler() {
    Thread fsChangeHandler = new Thread("FSChange handler") {
      @Override
      public void run() {
        CommandManager cm = controller.getCommandManager();
        while(true) {
          FSChange fsChange = fsWatcher.takeFSChange();
          if (fsChange == null)
            continue;

          cm.executeCommand(fsChange);

      if ( !controller.isClient()) {
        cm.sendCommand(fsChange, cm.getChannel());
      }
        }
      }
    };
    fsChangeHandler.start();

  }
}
