package eu.broth.intellilab.ui.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.OptionalConfigurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Bastian Roth
 * @version 04.04.2014
 */
public class GitlabConfigurable implements Configurable, SearchableConfigurable, OptionalConfigurable, Configurable.NoScroll {
	public static final String DISPLAY_NAME = "GitLab";
	private static final String ID = "eu.broth.intellilab.settings";

	private GitlabConfigurationUI ui;
	private Project project;

	public GitlabConfigurable(@NotNull Project project) {
		this.project = project;
	}

	@Nls
	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return null;
	}

	@Override
	public boolean needDisplay() {
		return true;
	}

	@NotNull
	@Override
	public String getId() {
		return ID;
	}

	@Nullable
	@Override
	public Runnable enableSearch(String option) {
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		if (ui == null) {
			ui = new GitlabConfigurationUI(project);
		}
		return ui.getPanel();
	}

	@Override
	public boolean isModified() {
		return ui != null && ui.isModified();
	}

	@Override
	public void apply() throws ConfigurationException {
		ui.apply();
	}

	@Override
	public void reset() {
		ui.reset();
	}

	@Override
	public void disposeUIResources() {
		Disposer.dispose(ui);
		ui = null;
	}
}
