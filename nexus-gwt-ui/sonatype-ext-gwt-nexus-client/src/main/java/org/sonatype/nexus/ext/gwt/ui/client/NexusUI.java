package org.sonatype.nexus.ext.gwt.ui.client;

import org.sonatype.gwt.client.resource.Resource;
import org.sonatype.gwt.client.resource.Variant;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.Container;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;

/**
 * Entry point of the NexusUI.
 */
public class NexusUI implements EntryPoint {
    
    private ApplicationContext ctx;
    
    private boolean userLoggedIn = false;
    
    private Text username;
    private Link loginLink;
    private Text version;
    
    private TabPanel servers;
    private TabPanel tabPanel;
    
    public void onModuleLoad() {
        ctx = ApplicationContext.instance();
        
        Viewport viewport = new Viewport() {
            {
                setLayout(new BorderLayout());
            }
        };
        
        addHeader(viewport);
        addMenu(viewport);
        addTabPanel(viewport);
        
        getServerVersion();
        getLoginStatus();
        
        RootPanel.get().add(viewport);
    }
    
    private void addHeader(LayoutContainer container) {
        LayoutContainer header = new LayoutContainer() {
            {
                setId("st-header");
                addText("Sonatype Nexus").setId("st-logo");
            }
        };
        
        LayoutContainer rightSide = new LayoutContainer() {
            {
                setId("st-right-side");
            }
        };
        username = new Text() {
            {
                setTagName("span");
                setId("st-username");
            }
        };
        loginLink = new Link() {
            {
                setId("st-login-link");
            }
            public void onClick(ComponentEvent event) {
                if (!userLoggedIn) {
                    showLoginWindow();
                } else {
                    logout();
                }
            }
        };
        version = new Text() {
            {
                setId("st-version");
            }
        };
        rightSide.add(username);
        rightSide.add(loginLink);
        rightSide.add(version);
        header.add(rightSide);
        
        BorderLayoutData headerLayoutData = new BorderLayoutData(Style.LayoutRegion.NORTH) {
            {
                setMargins(new Margins(5, 5, 5, 5));
                setSize(30);
            }
        };
        
        container.add(header, headerLayoutData);
    }
    
    private void addMenu(LayoutContainer container) {
        ContentPanel menu = new ContentPanel() {
            {
                setHeading("Sonatype Servers");
                setLayout(new FitLayout());
            }
        };
        
        /* Tabs are added later by AddServers() */
        servers = new TabPanel() {
            {
                setId("st-server-tab-panel");
                setBodyBorder(false);
            }
        };
        menu.add(servers);
        
        BorderLayoutData menuLayoutData = new BorderLayoutData(Style.LayoutRegion.WEST) {
            {
                setMargins(new Margins(0, 5, 5, 5));
                setSize(185);
                setCollapsible(true);
                setSplit(false);
            }
        };
        
        container.add(menu, menuLayoutData);
    }
    
    private void addTabPanel(LayoutContainer container) {
        tabPanel = new TabPanel() {
            {
                setId("st-main-tab-panel");
                setResizeTabs(true);
                setTabScroll(true);
                setMinTabWidth(110);
            }
        };
        
        BorderLayoutData tabPanelLayoutData = new BorderLayoutData(Style.LayoutRegion.CENTER) {
            {
                setMargins(new Margins(0, 5, 5, 0));
            }
        };
        
        container.add(tabPanel, tabPanelLayoutData);
    }
    
    private void getServerVersion() {
        ctx.getLocalRepoServer().getResource(Constants.STATUS).get(new RequestCallback() {
            public void onError(Request request, Throwable exception) {
                version.setText("Version unavailable");
            }
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() == Response.SC_OK) {
                    Document doc = XMLParser.parse(response.getText());
                    String ver = doc.getElementsByTagName("version").item(0).getFirstChild().getNodeValue();
                    version.setText(ver);
                } else {
                    version.setText("Version unavailable");
                }
            }
        }, Variant.APPLICATION_XML);
    }
    
    private void getLoginStatus() {
        String authorizationToken = ctx.getCookie("authToken");
        if (authorizationToken == null) {
            updateLoginStatus(null, null);
            return;
        }

        Resource resource = ctx.getLocalRepoServer().getResource(Constants.AUTHENTICATION_LOGIN);
        resource.addHeader("Authorization",
                "NexusAuthToken " + authorizationToken);
        resource.get(new RequestCallback() {
            public void onError(Request request, Throwable exception) {
                updateLoginStatus(null, null);
            }
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() == Response.SC_OK) {
                    Document doc = XMLParser.parse(response.getText());
                    String authorizationToken = doc.getElementsByTagName("authToken").item(0).getFirstChild().getNodeValue();
                    updateLoginStatus(ctx.getCookie("username"), authorizationToken);
                } else {
                    updateLoginStatus(null, null);
                }
            }
        }, Variant.APPLICATION_XML);
    }
    
    private void showLoginWindow() {
        final Window loginWindow = new Window() {
            {
                setHeading("Nexus Log In");
                setAutoWidth(false);
                setWidth(350);
                setAutoHeight(true);
                setModal(true);
                setResizable(false);
                // TODO: Animate the login window when GXT adds support for that
                // animateTarget: 'login-link'
            }
        };
        
        final FormPanel loginForm = new FormPanel() {
            {
                setLabelAlign(LabelAlign.RIGHT);
                setLabelWidth(60);
                setFrame(true);
                setHeaderVisible(false);
                setButtonAlign(Style.HorizontalAlignment.CENTER);
            }
        };
        
        final TextField username = new TextField() {
            {
                setFieldLabel("Username");
                setName("username");
                setTabIndex(1);
                setWidth(150);
                setAllowBlank(false);
            }
        };
        loginForm.add(username);
        
        TextField password = new TextField() {
            {
                setFieldLabel("Password");
                setName("password");
                setPassword(true);
                setTabIndex(2);
                setWidth(150);
                setAllowBlank(false);
            }
        };
        loginForm.add(password);
        
        // TODO: Enter should start the login
        Button ok = new Button("Log In") {
            {
                setTabIndex(3);
                addSelectionListener(new SelectionListener<ComponentEvent>() {
                    public void componentSelected(ComponentEvent event) {
                        login(loginWindow, loginForm);
                    }
                });
            }
        };
        loginForm.addButton(ok);
        
        loginWindow.add(loginForm);

        loginWindow.addListener(Events.Show, new Listener<ComponentEvent>() {
            public void handleEvent(ComponentEvent event) {
                username.focus();
            }
        });
        loginWindow.addListener(Events.Close, new Listener<ComponentEvent>() {
            public void handleEvent(ComponentEvent event) {
                for (Field field : loginForm.getFields()) {
                    field.reset();
                }
            }
        });
        
        for (Field field : loginForm.getFields()) {
            field.clearInvalid();
        }
        
        loginWindow.show();
    }

    private void login(final Window loginWindow, FormPanel loginForm) {
        loginWindow.el().mask("Logging you in...");

        final Field username = loginForm.getFields().get(0);
        final Field password = loginForm.getFields().get(1);
        
        Resource resource = ctx.getLocalRepoServer().getResource(Constants.AUTHENTICATION_LOGIN);
        // TODO: Use real HTTP basic authentication
        resource.addHeader("Authorization", "Basic " +
                Util.base64Encode((String) username.getValue() + ":" + (String) password.getValue()));
        resource.get(new RequestCallback() {
            public void onError(Request request, Throwable exception) {
                loginWindow.el().unmask();
                password.focus();
            }
            public void onResponseReceived(Request request, Response response) {
                loginWindow.el().unmask();
                if (response.getStatusCode() == Response.SC_OK) {
                    loginWindow.hide();
                    
                    Document doc = XMLParser.parse(response.getText());
                    String authorizationToken = doc.getElementsByTagName("authToken").item(0).getFirstChild().getNodeValue();
                    // TODO: Extract permissions from reponse.clientPermissions
                    
                    updateLoginStatus((String) username.getValue(), authorizationToken);
                    
                    username.reset();
                    password.reset();
                }
            }
        }, Variant.APPLICATION_XML);
    }
    
    private void logout() {
        ctx.getLocalRepoServer().getResource(Constants.AUTHENTICATION_LOGOUT).get(new RequestCallback() {
            public void onError(Request request, Throwable exception) {
                updateLoginStatus(null, null);
            }
            public void onResponseReceived(Request request, Response response) {
                updateLoginStatus(null, null);
            }
        }, Variant.APPLICATION_XML);
    }
    
    // TODO: Require a user object instead of a name
    private void updateLoginStatus(String user, String authorizationToken) {
        if (user != null) {
            userLoggedIn = true;
            
            ctx.setCookie("username", user);
            ctx.setCookie("authToken", authorizationToken);
            ctx.getLocalRepoServer().addDefaultHeader("Authorization",
                    "NexusAuthToken " + authorizationToken);
            
            username.setText(user + " | ");
            username.show();
            loginLink.setHtml("Log Out");
        } else {
            userLoggedIn = false;
            
            ctx.removeCookie("username");
            ctx.removeCookie("authToken");
            ctx.getLocalRepoServer().removeDefaultHeader("Authorization");
            
            username.hide();
            loginLink.setHtml("Log In");
        }
        
        removeServers();
        removeTabs();
        addServers(user);
        addWelcomeTab();
    }
    
    private void removeServers() {
        servers.removeAll();
    }
    
    private void removeTabs() {
        tabPanel.removeAll();
    }
    
    private void addServers(String user) {
        for (ServerType serverType : ctx.getServerTypes()) {
            addServer(serverType, user);
        }
        
        servers.setSelection(servers.getItem(0));
    }
    
    private void addServer(ServerType serverType, String user) {
        final String serverID = Util.convertToStyleName(serverType.getName());
        
        TabItem instances = new TabItem(serverType.getName()) {
            {
                setId("st-server-" + serverID + "-tab");
            }
        };
        
        for (ServerInstance serverInstance: serverType.getInstances()) {
            addServerInstance(instances, serverType, serverInstance, user);
        }
        
        servers.add(instances);
    }
    
    private void addServerInstance(TabItem instances, ServerType serverType,
            ServerInstance serverInstance, String user) {
        ContentPanel groups = new ContentPanel() {
            {
                addStyleName("st-server-instance-panel");
                setHeaderVisible(false);
                setLayout(new FitLayout());
                setBodyBorder(false);
            }
        };

        for (ServerFunctionGroup serverFunctionGroup: serverType.getFunctionGroups()) {
            addServerFunctionGroup(groups, serverFunctionGroup, user);
        }

        instances.add(groups);
        
        if (instances.isRendered()) {
            instances.layout();
        }
    }

    private void addServerFunctionGroup(ContentPanel groups,
            final ServerFunctionGroup serverFunctionGroup, String user) {
        final ContentPanel functions = new ContentPanel() {
            {
                setHeading(serverFunctionGroup.getName());
                addStyleName("st-server-group-panel");
                setLayout(new FitLayout());
                setFrame(true);
            }
        };
        
        for (ServerFunction serverFunction: serverFunctionGroup.getFunctions()) {
            addServerFunction(functions, serverFunction, user);
        }
        
        groups.add(functions);
    }

    private void addServerFunction(ContentPanel functions, 
            final ServerFunction serverFunction, String user) {
        functions.add(new Link(serverFunction.getMenuName()) {
            public void onClick(ComponentEvent event) {
                addServerFunctionTab(serverFunction);
            }
        });
    }

    private void addServerFunctionTab(ServerFunction serverFunction) {
        String id = "st-" + Util.convertToStyleName(serverFunction.getTabName()) + "-tab";
        if (activateTab(id)) {
            return;
        }
        
        addTab(serverFunction.getPanel(ctx.getLocalRepoServer()),
                serverFunction.getTabName(), id);
    }
    
    private void addWelcomeTab() {
        if (activateTab("st-welcome-tab")) {
            return;
        }

        LayoutContainer welcome = new LayoutContainer() {
            {
                setStyleName("st-little-padding");
                addText("Welcome to the Sonatype Nexus Repository Manager.").setTagName("p");
                addText("You may browse and search the repositories using the options on the left. Administrators may login via the link on the top right.").setTagName("p");
            }
        };
        addTab(welcome, "Welcome", "st-welcome-tab");
    }
    
    private boolean activateTab(String id) {
        TabItem tabItem = tabPanel.findItem(id, false);
        if (tabItem == null) {
            return false;
        }

        tabPanel.setSelection(tabItem);

        return true;
    }
    
    private void addTab(final Container<Component> container, String title, final String id) {
        TabItem tabItem = new TabItem(title) {
            {
                setId(id);
                setClosable(true);
                add(container);
            }
        };

        tabPanel.add(tabItem);
        tabPanel.setSelection(tabItem);
    }
    
}
