/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class NewProject extends Wizard implements INewWizard {

	public NewProject() {
		setNeedsProgressMonitor(true);
	}

	private WizardNewProjectCreationPage main = new WizardNewProjectCreationPage("main") {
		
		@Override
		protected boolean validatePage() {
			if( !super.validatePage())
				return false;
			
			schema.setNewProject(main.getProjectHandle());
			copyrightTemplates.setNewProject(main.getProjectHandle());
			return true;
		}
	};
	
	private SchemaWizardPage schema = new SchemaWizardPage(true); 

	private ImportCopyrightTemplatesPage copyrightTemplates = new ImportCopyrightTemplatesPage("copyright-templates", true); 
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		main.setTitle("New CIMTool Project");
		main.setDescription("Create and configure a new CIMTool project and import a copy of the CIM.");
		//
		copyrightTemplates.setTitle("Project Copyright Templates Configuration");
		copyrightTemplates.setDescription("Import and/or configure copyright templates to be used in the artifacts generated by this project.");
		copyrightTemplates.setMultiLineCopyrightSources(new String[]{"*.txt", "*.copyright-multi-line"});
		copyrightTemplates.setSingleLineCopyrightSources(new String[]{"*.txt", "*.copyright-single-line"});
		//
		schema.setTitle("Import Initial Schema");
		schema.setDescription("Import an XMI, OWL or EA project file as your base schema.");
	}

	@Override
	public void addPages() {
		addPage(main);
		addPage(copyrightTemplates);
		addPage(schema);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRunnable job =  Task.createProject(main.getProjectHandle(), main.useDefaults()? null: main.getLocationURI());
		
		String multilineCopyright = copyrightTemplates.getMultiLineCopyrightTemplateTextForSelectedOption();
		InputStream multilineInputStream = new ByteArrayInputStream(multilineCopyright.getBytes());
		IFile multilineCopyrightTemplateFile = copyrightTemplates.getMultiLineCopyrightFile();
		job = Task.chain(job, Task.importInputStreamToFile(multilineCopyrightTemplateFile, multilineInputStream));
		
		String singleLineCopyright = copyrightTemplates.getSingleLineCopyrightTemplateTextForSelectedOption();
		InputStream singleLineInputStream = new ByteArrayInputStream(singleLineCopyright.getBytes());
		IFile singleLineCopyrightTemplateFile = copyrightTemplates.getSingleLineCopyrightFile();
		job = Task.chain(job, Task.importInputStreamToFile(singleLineCopyrightTemplateFile, singleLineInputStream));
		
		String pathname = schema.getPathname();
		if(pathname != null && pathname.length() != 0) {
			IFile schemaFile = schema.getFile();
			String namespace = schema.getNamespace();
			job = Task.chain( job, Task.importSchema(schemaFile, pathname, namespace));
		}

		return Jobs.runInteractive(job, ResourcesPlugin.getWorkspace().getRoot(), getContainer(), getShell());
	}
}
