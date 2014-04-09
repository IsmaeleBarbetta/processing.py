package jycessing.mode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.regex.Pattern;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.EditorToolbar;
import processing.app.Formatter;
import processing.app.Mode;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Toolkit;

@SuppressWarnings("serial")
public class PyEditor extends Editor {

  final PythonMode pyMode;
  final PyKeyListener listener;
  Thread runner;

  protected PyEditor(final Base base, final String path, final EditorState state, final Mode mode) {
    super(base, path, state, mode);

    listener = new PyKeyListener(this, textarea);
    pyMode = (PythonMode)mode;
  }

  @Override
  public String getCommentPrefix() {
    return "# ";
  }

  @Override
  public void internalCloseRunner() {
    try {
      pyMode.getSketchServiceManager().stopSketch();
    } catch (SketchException e) {
      statusError(e);
    }
  }

  /**
   * Build menus.
   */
  @Override
  public JMenu buildFileMenu() {
    final String appTitle = PyToolbar.getTitle(PyToolbar.EXPORT, false);
    final JMenuItem exportApplication = Toolkit.newJMenuItem(appTitle, 'E');
    exportApplication.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportApplication });
  }

  @Override
  public JMenu buildHelpMenu() {
    final JMenu menu = new JMenu("Help");
    final JMenuItem item = new JMenuItem("Patches welcome");
    item.setEnabled(false);
    menu.add(item);
    return menu;
  }

  @Override
  public JMenu buildSketchMenu() {
    final JMenuItem runItem = Toolkit.newJMenuItem(PyToolbar.getTitle(PyToolbar.RUN, false), 'R');
    runItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        handleRun();
      }
    });

    final JMenuItem presentItem =
        Toolkit.newJMenuItemShift(PyToolbar.getTitle(PyToolbar.RUN, true), 'R');
    presentItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        handlePresent();
      }
    });

    final JMenuItem stopItem = new JMenuItem(PyToolbar.getTitle(PyToolbar.STOP, false));
    stopItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        handleStop();
      }
    });

    return buildSketchMenu(new JMenuItem[] { runItem, presentItem, stopItem });
  }

  @Override
  public Formatter createFormatter() {
    return pyMode.getFormatter();
  }

  @Override
  public EditorToolbar createToolbar() {
    return new PyToolbar(this, base);
  }

  /**
   * TODO(Adam Parrish): Create this!
   */
  public void handleExportApplication() {
    Base.showMessage("Sorry", "You can't do that yet.");
  }

  private void runSketch(final RunMode mode) {
    prepareRun();
    final SketchCode code = getSketch().getCurrentCode();
    final String sketchPath = code.getFile().getAbsolutePath();
    try {
      final String[] codePaths = new String[sketch.getCodeCount()];
      for (int i = 0; i < codePaths.length; i++) {
        codePaths[i] = sketch.getCode(i).getFile().getAbsolutePath();
      }
      pyMode.getSketchServiceManager().runSketch(this, mode, Base.getSketchbookLibrariesFolder(),
          new File(sketchPath).getAbsoluteFile(), code.getProgram(), codePaths, getX(), getY());
    } catch (SketchException e) {
      statusError(e);
    }
  }

  @Override
  public void deactivateRun() {
    restoreToolbar();
  }

  public void handleRun() {
    toolbar.activate(PyToolbar.RUN);
    runSketch(RunMode.WINDOWED);
  }

  public void handlePresent() {
    toolbar.activate(PyToolbar.RUN);
    runSketch(RunMode.PRESENTATION);
  }

  public void handleStop() {
    toolbar.activate(PyToolbar.STOP);
    internalCloseRunner();
    restoreToolbar();
  }

  private void restoreToolbar() {
    toolbar.deactivate(PyToolbar.SAVE);
    toolbar.deactivate(PyToolbar.STOP);
    toolbar.deactivate(PyToolbar.RUN);
    toFront();
  }

  public void handleSave() {
    toolbar.activate(PyToolbar.SAVE);
    super.handleSave(true);
    restoreToolbar();
  }

  @Override
  public boolean handleSaveAs() {
    toolbar.activate(PyToolbar.SAVE);
    final boolean result = super.handleSaveAs();
    restoreToolbar();
    return result;
  }

  @Override
  public void statusError(final String what) { // sketch died for some reason
    super.statusError(what);
    restoreToolbar();
  }

  @Override
  public void handleImportLibrary(final String jarPath) {
    sketch.ensureExistence();
    final String name = new File(jarPath).getParentFile().getParentFile().getName();
    if (Pattern.compile("^add_library\\(\\s*'" + name + "'\\s*\\)\\s*$", Pattern.MULTILINE)
        .matcher(getText()).find()) {
      return;
    }
    setSelection(0, 0); // scroll to start
    setSelectedText(String.format("add_library('%s')\n", name));
    sketch.setModified(true);
  }
}