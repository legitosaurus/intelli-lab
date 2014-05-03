package eu.broth.intellilab.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

/**
 * @author Bastian Roth
 * @version 03.05.2014
 */
public class IssuesWindowFactory implements ToolWindowFactory {

	@Override
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		IssuesWindow window = new IssuesWindow();
		window.init(project);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(window, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
