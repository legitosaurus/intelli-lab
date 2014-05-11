package eu.broth.intellilab.ui;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.net.URL;

/**
 * @author Bastian Roth
 * @version 11.05.2014
 */
public class Icons {

	public static final Icon TASK_OPEN;
	public static final Icon TASK_CLOSED;
	public static final Icon BUG_OPEN;
	public static final Icon BUG_CLOSED;

	static {
		String theme = UIUtil.isUnderDarcula() ? "dark" : "light";
		TASK_OPEN = createIcon(theme, "task-open.png");
		TASK_CLOSED = createIcon(theme, "task-closed.png");
		BUG_OPEN = createIcon(theme, "bug-open.png");
		BUG_CLOSED = createIcon(theme, "bug-closed.png");
	}

	private static Icon createIcon(String theme, String filename) {
		URL resource = Icons.class.getClassLoader().getResource(theme + "/" + filename);
		return new ImageIcon(resource);
	}

	private Icons() {}
}
