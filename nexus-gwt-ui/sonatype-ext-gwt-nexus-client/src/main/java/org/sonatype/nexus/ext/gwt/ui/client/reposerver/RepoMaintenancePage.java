package org.sonatype.nexus.ext.gwt.ui.client.reposerver;

import org.sonatype.nexus.ext.gwt.ui.client.Action;
import org.sonatype.nexus.ext.gwt.ui.client.Constants;
import org.sonatype.nexus.ext.gwt.ui.client.ServerFunctionPanel;
import org.sonatype.nexus.ext.gwt.ui.client.ServerInstance;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.TreeModel;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.ToolButton;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.table.CellRenderer;
import com.extjs.gxt.ui.client.widget.table.Table;
import com.extjs.gxt.ui.client.widget.table.TableColumn;
import com.extjs.gxt.ui.client.widget.table.TableColumnModel;
import com.extjs.gxt.ui.client.widget.toolbar.TextToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.tree.Tree;
import com.google.gwt.user.client.Window;

public class RepoMaintenancePage extends LayoutContainer implements ServerFunctionPanel {

    ContentPanel repoPanel;

    ContentPanel repoTree;
    ContentPanel repoTreePanel;
    RepoTreeBinding repoTreeBinding;

    public void init(ServerInstance server) {
        setLayout(new BorderLayout());
        setWidth("100%");
        setHeight("100%");

        addRepoList(server);
        addRepoPanel();

        createRepoTree();

        showRepoHelp();
    }
    
    private void addRepoList(final ServerInstance server) {
        ContentPanel panel = new ContentPanel() {
            {
                setHeaderVisible(false);
                setLayout(new FitLayout());
            }
        };

        Table table = new Table() {
            {
                setColumnModel(new TableColumnModel(
                    new TableColumn("name", "Repository", 175f),
                    new TableColumn("repoType", "Type", 50f),
                    new TableColumn("status", "Status", 200f) {
                    	{
                    		setRenderer(new CellRenderer() {
                    			public String render(Component item, String property, Object value) {
                    				if (value == null) {
                    					return "<i>retrieving</i>";
                    				}
                    				return (String) value;
                    			}
                    		});
                    	}
                    },
                    new TableColumn("resourceURI", "Repository Path", 1f) {
                        {
                            setRenderer(new CellRenderer() {
                                public String render(Component item, String property, Object value) {
                                    String path = (String) value;
                                    path = Constants.HOST + path.replace(Constants.SERVICE_REPOSITORIES, Constants.CONTENT_REPOSITORIES);
                                    return "<a href=\"" + path + "\" target=\"_blank\">" + path + "</a>";
                                }
                            });
                        }
                    }
                ));
            }
        };
        
        final RepoTableBinding tableBinding = new RepoTableBinding(table, server);
        tableBinding.getBinder().addSelectionChangedListener(new SelectionChangedListener<ModelData>() {
            public void selectionChanged(SelectionChangedEvent event) {
                showRepoTree(event.getSelectedItem());
            }
        });

        ToolBar toolBar = new ToolBar();
        TextToolItem refreshButton = new TextToolItem("Refresh", "st-icon-refresh") {
            {
                addSelectionListener(new SelectionListener<ComponentEvent>() {
                    public void componentSelected(ComponentEvent ce) {
                        tableBinding.reload();
                        showRepoHelp();
                    }
                });
            }
        };
        toolBar.add(refreshButton);

        panel.setTopComponent(toolBar);
        panel.add(table);

        BorderLayoutData panelLayoutData = new BorderLayoutData(Style.LayoutRegion.NORTH) {
            {
                setSplit(true);
                setSize(200);
                setMinSize(150);
                setMaxSize(400);
            }
        };

        add(panel, panelLayoutData);
        
        ContextMenuProvider tableMenu =
            new ContextMenuProvider(table, tableBinding.getBinder());
        
        tableMenu.addAction(new Action<ModelData>("View") {
            public void execute(ModelData data) {
                Window.alert(getCaption());
            }
        });
        
        tableMenu.addAction(new Action<ModelData>("Clear Cache") {
            public boolean supports(ModelData data) {
                String repoType = (String) data.get("repoType");
                return repoType.equals("hosted") || repoType.equals("proxy");
            }
            public void execute(ModelData data) {
                Window.alert(getCaption());
            }
        });
        
        tableMenu.addAction(new Action<ModelData>("Re-Index") {
            public void execute(ModelData data) {
                Window.alert(getCaption());
            }
        });
        
        tableMenu.addAction(new Action<ModelData>("Block Proxy") {
            public boolean supports(ModelData data) {
                return data.get("repoType").equals("proxy");
            }
            public void execute(ModelData data) {
                Window.alert(getCaption());
            }
        });
        
        tableMenu.addAction(new Action<ModelData>("Put Out of Service") {
            public void execute(ModelData data) {
                /*
                String path = data.get("contentUri") + "/status";
                Document doc = XMLParser.createDocument();
                doc.createElement("");
                String request = "{\"data\":{\"id\":\"central-m1\",\"repoType\":\"virtual\",\"localStatus\":\"inService\"}}";
                server.getResource(path).put(new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                        Window.alert("ERROR: " + exception.getMessage());
                    }
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() != Response.SC_OK) {
                            Window.alert("ERROR - " + response.getStatusText());
                        }
                        Window.alert(response.getText());
                    }
                }, new Representation(Variant.APPLICATION_XML, request));
                */
            }
        });
    }

    private void addRepoPanel() {
        repoPanel = new ContentPanel() {
            {
                setHeading("Repository Information");
                setLayout(new FitLayout());
                setLayoutOnChange(true);
            }
        };

        add(repoPanel, new BorderLayoutData(Style.LayoutRegion.CENTER));
    }

    private void createRepoTree() {
        repoTree = new ContentPanel() {
            {
                setFrame(true);
                setHeaderVisible(false);
                setLayout(new FitLayout());
            }
        };

        Tree tree = new Tree() {
            {
                setItemIconStyle("tree-leaf");
            }
        };

        repoTreeBinding = new RepoTreeBinding(tree);

        repoTreePanel = new ContentPanel() {
            {
                setId("st-repo-browser");
                setBodyBorder(true);
                setBorders(true);
                setScrollMode(Style.Scroll.AUTO);
                getHeader().addTool(new ToolButton("x-tool-refresh", new SelectionListener<ComponentEvent>() {
                    public void componentSelected(ComponentEvent ce) {
                        repoTreeBinding.reload();
                    }
                }));
            }
        };

        ContextMenuProvider treeMenu =
            new ContextMenuProvider(tree, repoTreeBinding.getBinder());
        
        treeMenu.addAction(new Action<TreeModel>("Re-Index") {
            public void execute(TreeModel data) {
                Window.alert(getCaption());
            }
        });
        
        treeMenu.addAction(new Action<TreeModel>("Download") {
            public boolean supports(TreeModel data) {
                return data.isLeaf();
            }
            public void execute(TreeModel data) {
                Window.alert(getCaption());
            }
        });

        repoTreePanel.add(tree);
        repoTree.add(repoTreePanel);
    }

    private void showRepoTree(ModelData repo) {
        if (repo == null) {
            return;
        }

        repoTreePanel.setHeading((String) repo.get("name") + " Repository Content");
        repoTreeBinding.selectRepo(repo);

        repoPanel.removeAll();
        repoPanel.add(repoTree);
    }

    private void showRepoHelp() {
        repoPanel.removeAll();
        repoPanel.addText("Select a repository to view it")
                 .setStyleName("st-little-padding");
    }
}
