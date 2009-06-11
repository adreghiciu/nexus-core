package org.sonatype.nexus.mock.pages;

import org.sonatype.nexus.mock.NexusTestCase;
import org.sonatype.nexus.mock.components.Button;
import org.sonatype.nexus.mock.components.Component;

import com.thoughtworks.selenium.Selenium;

public class RepositoriesTab
    extends Component
{

    public static final String REPOSITORIES_ST = "window.Ext.getCmp('view-repositories')";

    private MainPage mainPage;

    private RepositoriesGrid repositoriesGrid;

    private Button refreshButton;

    private Button addButton;

    private Button deleteButton;

    private Button typeButton;

    private Button addHostedButton;

    public RepositoriesTab( Selenium selenium, MainPage mainPage )
    {
        super( selenium, REPOSITORIES_ST );
        this.mainPage = mainPage;

        this.repositoriesGrid = new RepositoriesGrid( selenium );

        this.refreshButton = new Button( selenium, expression + ".refreshButton" );
        this.deleteButton = new Button( selenium, expression + ".toolbarDeleteButton" );
        this.typeButton = new Button( selenium, expression + ".browseTypeButton" );

        this.addButton = new Button( selenium, expression + ".toolbarAddButton" );
        this.addHostedButton = new Button( selenium, addButton.getExpression() + ".menu.items.items[0].el" );
        this.addHostedButton.idFunction = ".id";
    }

    public RepositoriesConfigurationForm addHostedRepo()
    {
        addButton.click();

        addHostedButton.clickNoWait();

        return new RepositoriesConfigurationForm( selenium, expression + ".cardPanel.getLayout().activeItem.getLayout().activeItem" );
    }

    public RepositoriesTab refresh()
    {
        refreshButton.click();

        return this;
    }

    public RepositoriesEditTabs select( String repoId )
    {
        this.repositoriesGrid.select( NexusTestCase.nexusBaseURL + "/service/local/repositories/" + repoId );

        return new RepositoriesEditTabs( selenium );
    }

    public MainPage getMainPage()
    {
        return mainPage;
    }

    public RepositoriesGrid getRepositoriesGrid()
    {
        return repositoriesGrid;
    }

    public Button getRefreshButton()
    {
        return refreshButton;
    }

    public Button getAddButton()
    {
        return addButton;
    }

    public Button getDeleteButton()
    {
        return deleteButton;
    }

    public Button getTypeButton()
    {
        return typeButton;
    }

    public Button getAddHostedButton()
    {
        return addHostedButton;
    }

    public MessageBox delete()
    {
        this.deleteButton.click();

        return new MessageBox(selenium);
    }

    public boolean contains( String repoId )
    {
        return this.repositoriesGrid.contains( NexusTestCase.nexusBaseURL + "/service/local/repositories/" + repoId );
    }

}
