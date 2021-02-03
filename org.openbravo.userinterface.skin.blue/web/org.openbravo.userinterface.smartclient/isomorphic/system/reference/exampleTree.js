isc.ExampleTree.create({
    ID:"exampleTree",
    openProperty:"isOpen",
    nodeVisibility:"sdk",
    root:{
        name:"root/",
        children:[
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/house.png",
                id:"Welcome",
                isOpen:true,
                screenshot:"screenshots/tabs_ds_code.png",
                screenshotHeight:"176",
                screenshotWidth:"291",
                showSkinSwitcher:"true",
                title:"Featured Samples",
                description:"\n    \n    <div class=\"explorerWelcomePageTitle\">\n    Welcome to the SmartClient Feature Tour!\n    </div>\n    \n\t<div>&nbsp;</div>\n    \n    <div style=\"float: left;\">\n    \t<div style=\"line-height: 20pt; padding-bottom:5px\">\n\t        <div class=\"explorerWelcomePageBullet\">\n    \t  \t    <span class=\"explorerWelcomePageNumber\"> 1 </span>\n        \t</div>\n        \t<span>To load a featured sample, click the name in the tree on the left</span>\n        </div>\n    \n    \t<div style=\"line-height: 20pt; padding-bottom:5px\">\n\t        <div class=\"explorerWelcomePageBullet\">\n    \t  \t    <span class=\"explorerWelcomePageNumber\"> 2 </span>\n        \t</div>\n        \tWith an example loaded, view the source code by clicking on the tabs shown above the running example\n        </div>\n    \n    \t<div style=\"line-height: 20pt;\">\n        \t<div style=\"float: left; margin-bottom: 3pt; margin-right: 20pt;\">\n\t\t        <div class=\"explorerWelcomePageBullet\">\n    \t\t  \t    <span class=\"explorerWelcomePageNumber\"> 3 </span>\n        \t\t</div>\n        \t\tSee <a href=\"http://www.smartclient.com/docs/9.0/a/b/c/go.html#featureExplorerOverview\">reference docs</a> for how to use this feature explorer including how to use code in standalone apps\n        \t</div>\n\t\t</div>\n\t\t\n\t\t<div>&nbsp;</div>\n    </div>\n\n\n    \n",
                children:[
                    {
                        jsURL:"welcome/helloButton.js",
                        title:"Hello World",
                        xmlURL:"welcome/helloButton.xml",
                        description:"\n        A SmartClient <code>IButton</code> component responds to mouse clicks by showing a\n        modal <code>Dialog</code> component with the \"Hello world!\" message.  Source code is\n        provided in both XML and JS formats.\n        "
                    },
                    {
                        jsURL:"welcome/helloStyled.js",
                        title:"Hello World (styling)",
                        visibility:"sdk",
                        xmlURL:"welcome/helloStyled.xml",
                        tabs:[
                            {
                                title:"CSS",
                                url:"welcome/helloStyled.css"
                            }
                        ],
                        description:"\n        This <code>Label</code> component is heavily styled with a combination of CSS class,\n        CSS attribute shortcuts, and SmartClient attributes.  Source code is\n        provided in both XML and JS formats.\n        "
                    },
                    {
                        jsURL:"welcome/helloForm.js",
                        title:"Hello You (form)",
                        visibility:"sdk",
                        xmlURL:"welcome/helloForm.xml",
                        description:"\n        This SmartClient <code>FormLayout</code> provides a text field and a button control.\n        Type a name in the field, then click the button for a personalized message.\n        Source code is provided in both XML and JS formats.\n        "
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_put.png",
                        id:"fetchOperationFS",
                        ref:"fetchOperation",
                        title:"Live Grid",
                        description:"\n        <p>Rows are fetched automatically as the user drags the scrollbar. Drag the \n        scrollbar quickly to the bottom to fetch a range near the end (a prompt will appear \n        during server fetch).</p>\n        <p>Scroll slowly back up to fill in the middle.</p>\n        <p>Another key unique feature of SmartClient is lazy rendering of columns. Most \n        browsers cannot handle displaying a large number of columns and have serious \n        performance issues. SmartClient, however, does not render all columns outside the \n        visible area by default and only renders them as the grid is horizontally scrolled. This \n        feature can be disabled if desired.</p>\n        "
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_detail.png",
                        id:"adaptiveFilterFS",
                        ref:"adaptiveFilter",
                        title:"Adaptive Filter"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/sc_insertformula.png",
                        id:"filterBuilderBracketFS",
                        ref:"filterBuilderBracket",
                        title:"Advanced Filter"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_detail.png",
                        id:"dynamicFreezeFS",
                        ref:"dynamicFreeze",
                        title:"Dynamic Frozen Columns"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_side_tree.png",
                        id:"userDefinedHilitesFS",
                        ref:"userDefinedHilites",
                        title:"User-Defined Hilites"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_side_tree.png",
                        id:"dynamicGroupingFS",
                        ref:"dynamicGrouping",
                        title:"Dynamic Grouping"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_side_tree.png",
                        id:"summaryGridFS",
                        ref:"summaryGrid",
                        title:"Grid Summaries"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                        id:"massUpdateFS",
                        ref:"massUpdate",
                        title:"Mass Update"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_split.png",
                        id:"expansionRelatedRecordsFS",
                        ref:"expansionRelatedRecords",
                        title:"Expanding Rows"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                        id:"stockPriceChartingFS",
                        ref:"stockPriceCharting",
                        title:"Zoomable Charts"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                        id:"multiSeriesChartMAFS",
                        ref:"multiSeriesChartMA",
                        title:"Multi-Axis Charts"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_columns.png",
                        id:"databoundDependentSelectsFS",
                        ref:"databoundDependentSelects",
                        title:"Dependent Selects (Grid)"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_columns.png",
                        id:"formDependentSelectsFS",
                        ref:"formDependentSelectsLocal",
                        title:"Dependent Selects (Form)"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_columns.png",
                        id:"filterRelatedFS",
                        ref:"filterRelated",
                        title:"Filter Related Records"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/database_link.png",
                        id:"databoundDragCopyFS",
                        ref:"databoundDragCopy",
                        title:"Databound Dragging"
                    },
                    {
                        dataSource:"supplyCategory",
                        fullScreen:"true",
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_osx.png",
                        id:"showcaseApp",
                        jsURL:"demoApp/demoAppJS.js",
                        needServer:"true",
                        screenshot:"demoApp/demoApp.png",
                        screenshotHeight:"337",
                        screenshotWidth:"480",
                        title:"Complete Application",
                        xmlURL:"demoApp/demoAppXML.xml",
                        tabs:[
                            {
                                title:"supplyItem",
                                url:"supplyItem.ds.xml"
                            }
                        ],
                        description:"Demonstrates a range of SmartClient GUI components, data binding operations,\n        and layout managers in a single-page application.\n        "
                    },
                    {
                        id:"FSserverExamples",
                        isOpen:true,
                        title:"Server Examples",
                        description:"\n    The SmartClient Server framework is a collection of .jar files and optional servlets that work with\n    any J2EE or J2SE container and are easily integrated into existing applications.  Its major\n    features include:<ul>\n    <li><b>Simplified server integration:</b> A pre-built network protocol for browser-server\n        communication, which handles data paging, transactions/batch operations, server-side\n        sort, automatic cache updates, validation and other error handling, optimistic\n        concurrency (aka long transactions) and binary file uploads.<P></li>\n    <li><b>SQL, JPA & Hibernate Connectors:</b> Secure, flexible, transactional support for all\n        CRUD operations, either directly via JDBC or via Hibernate or JPA beans.<P></li>\n    <li><b>Rapid integration with Java Beans:</b> Robust, complete, bi-directional translation\n        between Java and Javascript objects for rapid integration with any Java beans-based\n        persistence system, such as Spring services or custom ORM implementations.  Send and\n        receive complex structures including Java Enums and Java Generics without the need to\n        write mapping or validation code.  Declaratively trim and rearrange data so that only\n        selected data is sent to the client <b>without</b> the need to create and populate\n        redundant DTOs (data transfer objects).<P></li>\n    <li><b>Server enforcement of Validators:</b> A single file specifies validation rules\n        which are enforced on both the client and server side<P></li>\n    <li><b>Declarative Security:</b> Easily attach role or capability-based security rules to\n        data operations with server-side enforcement, plus automatic client-side effects such as\n        hiding fields or showing fields as read-only based on the user role.<P></li>\n    <li><b>Export:</b> Export any dataset to CSV or true Excel spreadsheets, including data\n        highlights and formatting rules<br><br></li>\n    <li><b>High speed data delivery / data compression:</b> automatically use the fastest \n        possible mechanism for delivering data to the browser<br></li>\n    </ul>\n    The SmartClient Server framework is an optional, commercially-licensed package.  See the \n    <a href=http://www.smartclient.com/product/index.jsp>products page</a> for details.\n    \n",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                id:"FSServerValidation",
                                isOpen:false,
                                title:"Validation",
                                description:"\n        The SmartClient Server provides powerful support for server-based validation.\n    ",
                                children:[
                                    {
                                        id:"FSsingleSourceValidation",
                                        ref:"singleSourceValidation",
                                        title:"Single Source"
                                    },
                                    {
                                        id:"FSdmiValidation",
                                        ref:"dmiValidation",
                                        title:"DMI Validation"
                                    },
                                    {
                                        id:"FSvelocityValidation",
                                        ref:"velocityValidation",
                                        title:"Velocity Expression"
                                    },
                                    {
                                        id:"FSinlineScriptValidation",
                                        ref:"inlineScriptValidation",
                                        title:"Inline Script"
                                    },
                                    {
                                        id:"FSuniqueCheckValidation",
                                        ref:"uniqueCheckValidation",
                                        title:"Unique Check"
                                    },
                                    {
                                        id:"FShasRelatedValidation",
                                        ref:"hasRelatedValidation",
                                        title:"Related Records"
                                    },
                                    {
                                        id:"FSblockingErrors",
                                        ref:"blockingErrors",
                                        title:"Blocking Errors"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_lightning.png",
                                isOpen:false,
                                title:"SQL",
                                description:"\n        The SmartClient Server provides powerful built-in support for codeless connection to\n        mainstream SQL databases.\n    ",
                                children:[
                                    {
                                        id:"SQLsqlWizard",
                                        ref:"sqlWizard",
                                        title:"Database Wizard"
                                    },
                                    {
                                        id:"SQLsqlConnector",
                                        ref:"sqlConnector",
                                        title:"Basic Connector"
                                    },
                                    {
                                        id:"SQLlargeValueMapSQL",
                                        ref:"largeValueMapSQL",
                                        title:"Large Value Map"
                                    },
                                    {
                                        id:"SQLuserSpecificData",
                                        ref:"userSpecificData",
                                        title:"User-Specific Data"
                                    },
                                    {
                                        id:"SQLdynamicReporting",
                                        ref:"dynamicReporting",
                                        title:"Dynamic Reporting"
                                    },
                                    {
                                        id:"SQLautoTransactions",
                                        ref:"autoTransactions",
                                        title:"Transactions"
                                    },
                                    {
                                        id:"SQLsqlIncludeFrom",
                                        ref:"sqlIncludeFrom",
                                        title:"Field Include"
                                    },
                                    {
                                        id:"SQLsqlIncludeVia",
                                        ref:"sqlIncludeVia",
                                        title:"Multiple Field Include"
                                    },
                                    {
                                        id:"SQLsqlIncludeFromDynamic",
                                        ref:"sqlIncludeFromDynamic",
                                        title:"Dynamic Field Include"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                isOpen:false,
                                title:"Hibernate / Beans",
                                description:"\n        The SmartClient Server provides powerful built-in support for Hibernate\n    ",
                                children:[
                                    {
                                        id:"FShibernateAutoDerivation",
                                        ref:"hibernateAutoDerivation",
                                        title:"Auto Derivation"
                                    },
                                    {
                                        id:"FShibernateConnector",
                                        ref:"hibernateConnector",
                                        title:"Beanless Mode"
                                    },
                                    {
                                        id:"FSadvancedFilterHibernate",
                                        ref:"advancedFilterHibernate",
                                        title:"Advanced Filtering"
                                    },
                                    {
                                        id:"FShbRelationManyToOneSimple",
                                        ref:"hbRelationManyToOneSimple",
                                        title:"Many-to-One Relation"
                                    },
                                    {
                                        id:"FShbRelationOneToMany",
                                        ref:"hbRelationOneToMany",
                                        title:"One-to-Many Relation"
                                    },
                                    {
                                        id:"FShbIncludeFrom",
                                        ref:"hbIncludeFrom",
                                        title:"Field Include"
                                    },
                                    {
                                        id:"FShbIncludeFromDynamic",
                                        ref:"hbIncludeFromDynamic",
                                        title:"Dynamic Field Include"
                                    },
                                    {
                                        id:"FSmasterDetail",
                                        ref:"masterDetail",
                                        title:"Master-Detail (Batch Load and Save)"
                                    },
                                    {
                                        id:"FSflattenedBeans",
                                        ref:"flattenedBeans",
                                        title:"Data Selection"
                                    },
                                    {
                                        id:"FShibernateProduction",
                                        ref:"hibernateProduction",
                                        title:"Spring with Beans"
                                    },
                                    {
                                        id:"FSjavaBeans",
                                        ref:"javaBeans",
                                        title:"Java Beans"
                                    },
                                    {
                                        id:"FSDMI",
                                        ref:"DMI",
                                        title:"DMI"
                                    },
                                    {
                                        id:"FSautoTransactionsHB",
                                        ref:"autoTransactionsHB",
                                        title:"Auto Transactions"
                                    },
                                    {
                                        id:"FSuploadHB",
                                        ref:"uploadHB",
                                        title:"Upload"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                isOpen:false,
                                title:"JPA",
                                description:"\n        The SmartClient Server's built-in support for JPA/JPA2 allows you to easily use your JPA annotated entities\n        in SmartClient's client-side widgets.<p/>\n    ",
                                children:[
                                    {
                                        id:"FSjpaConnector",
                                        ref:"jpaConnector",
                                        title:"Auto Derivation"
                                    },
                                    {
                                        id:"FSjpa2Connector",
                                        ref:"jpa2Connector",
                                        title:"Advanced Filtering"
                                    },
                                    {
                                        id:"FSjpaRelationManyToOneSimple",
                                        ref:"jpaRelationManyToOneSimple",
                                        title:"Many-to-One Relation"
                                    },
                                    {
                                        id:"FSjpaRelationOneToMany",
                                        ref:"jpaRelationOneToMany",
                                        title:"One-to-Many Relation"
                                    },
                                    {
                                        id:"FSjpaIncludeFrom",
                                        ref:"jpaIncludeFrom",
                                        title:"Field Include"
                                    },
                                    {
                                        id:"FSjpaIncludeFromDynamic",
                                        ref:"jpaIncludeFromDynamic",
                                        title:"Dynamic Field Include"
                                    },
                                    {
                                        id:"FSuploadJPA",
                                        ref:"uploadJPA",
                                        title:"Upload"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                id:"transactionsFolderJ",
                                isOpen:false,
                                title:"Transactions",
                                description:"\n            SmartClient provides robust support for transactional applications.\n            <P>\n            Queueing makes combining operations together into a single\n            transaction extremely easy, for more efficient data loading and transactional saves.\n            <P>\n            Automatic Transaction Management support in the SmartClient Server, with \n            specific implementations for the built-in SQL and Hibernate DataSources, allows \n            for queued requests to be committed or rolled back as a single database transaction.\n            This feature is only available in Power and Enterprise editions.\n            <P>\n            Transaction Chaining allows for declarative handling of data dependencies\n            between operations submitted together in a queue.  This feature is only available\n            in Power and Enterprise editions.\n     ",
                                children:[
                                    {
                                        id:"FSqueuing",
                                        ref:"queuing",
                                        title:"Simple Queueing"
                                    },
                                    {
                                        id:"FSautoTransactions",
                                        ref:"autoTransactions",
                                        title:"Automatic Transaction Management"
                                    },
                                    {
                                        id:"FSqueuedAdd",
                                        ref:"queuedAdd",
                                        title:"Master/Detail Add"
                                    },
                                    {
                                        id:"FSmassUpdate",
                                        ref:"massUpdate",
                                        title:"Mass Update"
                                    },
                                    {
                                        id:"FSdataboundDragCopy",
                                        ref:"databoundDragCopy",
                                        title:"Multi-Row Drag & Save"
                                    },
                                    {
                                        id:"FSrow-drag-save-pivot",
                                        ref:"row-drag-save-pivot",
                                        title:"Many-to-Many Drag & Save"
                                    },
                                    {
                                        id:"FSrollback",
                                        ref:"rollback",
                                        title:"Rollback"
                                    },
                                    {
                                        id:"FSjdbcOperations",
                                        ref:"jdbcOperations",
                                        title:"Transactional User Operations"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                isOpen:false,
                                title:"Custom DataSources",
                                description:"\n        Examples showing how to leverage the SmartClient Server to create partially or completely\n        customized DataSource implementations.\n    ",
                                children:[
                                    {
                                        id:"FSjavabeanWizard",
                                        ref:"javabeanWizard",
                                        title:"Javabean Wizard"
                                    },
                                    {
                                        id:"FScustomDataSource",
                                        ref:"customDataSource",
                                        title:"Simple (Hardcoded)"
                                    },
                                    {
                                        id:"FSormDataSource",
                                        ref:"ormDataSource",
                                        title:"ORM DataSource"
                                    },
                                    {
                                        id:"FSreusableORMDataSource",
                                        ref:"reusableORMDataSource",
                                        title:"Reusable ORM DataSource"
                                    },
                                    {
                                        id:"FSeditableServerSideDataSource",
                                        ref:"editableServerSideDataSource",
                                        title:"Editable Server-Side DataSource"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                isOpen:false,
                                showSkinSwitcher:"true",
                                title:"Export",
                                description:"\n    Exporting Data from DataSources and DataBoundComponents.\n    ",
                                children:[
                                    {
                                        id:"FSexport",
                                        ref:"export",
                                        title:"Excel Export"
                                    },
                                    {
                                        id:"FSformattedExportBuiltin",
                                        ref:"formattedExportBuiltin",
                                        title:"Formatted Export (Declared Formats)"
                                    },
                                    {
                                        id:"FSformattedExport",
                                        ref:"formattedExport",
                                        title:"Formatted Export (Custom Formatting)"
                                    },
                                    {
                                        id:"FSformattedServerExport",
                                        ref:"formattedServerExport",
                                        title:"Server-side Formatted Export"
                                    },
                                    {
                                        id:"FSpdfExport",
                                        ref:"pdfExport",
                                        title:"PDF Export"
                                    },
                                    {
                                        id:"FSchartImageExport",
                                        ref:"chartImageExport",
                                        title:"Chart Image Export"
                                    },
                                    {
                                        id:"FSchartPDFExport",
                                        ref:"chartPDFExport",
                                        title:"Chart PDF Export"
                                    },
                                    {
                                        id:"FSdrawingExport",
                                        ref:"drawingExport",
                                        title:"Drawing Export"
                                    },
                                    {
                                        id:"FScustomExport",
                                        ref:"customExport",
                                        title:"Custom Export"
                                    },
                                    {
                                        id:"FScustomExportCustomResponse",
                                        ref:"customExportCustomResponse",
                                        title:"Custom Export (Custom Response)"
                                    }
                                ]
                            },
                            {
                                id:"FSauditing",
                                ref:"auditing",
                                title:"Auditing"
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                isOpen:false,
                                showSkinSwitcher:"true",
                                title:"Upload / Download",
                                description:"\n                Samples with Upload and Download files\n            ",
                                children:[
                                    {
                                        id:"FSupload",
                                        ref:"upload",
                                        title:"Upload"
                                    },
                                    {
                                        id:"FSbatchUpload",
                                        ref:"batchUpload",
                                        title:"Batch Data Upload"
                                    },
                                    {
                                        id:"FSmultiFileItem",
                                        ref:"multiFileItem",
                                        title:"MultiFileItem"
                                    },
                                    {
                                        id:"FScustomDownload",
                                        ref:"customDownload",
                                        title:"Custom Download"
                                    },
                                    {
                                        id:"FScustomBinaryField",
                                        ref:"customBinaryField",
                                        title:"Custom Binary Field"
                                    }
                                ]
                            },
                            {
                                isOpen:false,
                                showSkinSwitcher:"true",
                                title:"Component XML",
                                description:"\n            Component XML is a format for specifying UI components declaratively in XML.\n            <P>\n            Using Component XML, you can separate the layout of your application from its\n            business logic, so that less technical users can edit the layout while JavaScript\n            developers implement business logic.\n            <P>\n            Component XML also allows visual tools such as Visual Builder to be used to modify\n            the layout of your applications.\n            \n        ",
                                children:[
                                    {
                                        id:"addingHandlers",
                                        jsURL:"componentXML/addingHandlers.js",
                                        title:"Adding Handlers",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                doEval:"false",
                                                title:"component XML",
                                                url:"ui/addingHandlers.ui.xml"
                                            },
                                            {
                                                dataSource:"supplyItem",
                                                name:"supplyItem"
                                            }
                                        ],
                                        descriptionHeight:"200",
                                        description:"\n                    The form and button below have been loaded from Component XML, then, a\n                    click handler has been added via JavaScript code which performs some\n                    specialized validation before saving.\n                    <P>\n                    Click the \"Save\" button to see a warning message generated by the click\n                    handler, then check the \"In Stock\" button and hit \"Save\" again to allow\n                    saving to proceed.\n                    <P>\n                    With this development approach, the form and button can be reorganized\n                    arbitrarily by editing of XML, possibly in a visual tool such as Visual\n                    Builder, or in any tool of your own design that can produce Component XML.\n                    So long as the relevant controls and fields are not renamed, the JavaScript\n                    code will work unchanged with the new layout.\n                \n                "
                                    },
                                    {
                                        id:"replacePlaceholder",
                                        jsURL:"componentXML/replacePlaceholder.js",
                                        title:"Replace Placeholder",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                doEval:"false",
                                                title:"component XML",
                                                url:"ui/replacePlaceholder.ui.xml"
                                            },
                                            {
                                                dataSource:"supplyItem",
                                                name:"supplyItem"
                                            }
                                        ],
                                        descriptionHeight:"175",
                                        description:"\n                    Programmatically created components can be inserted into the middle of a\n                    layout that is otherwise controlled by Component XML. Just leave a\n                    placeholder component in the Component XML layout and replace it\n                    programatically.\n                    <P>\n                    In the example below, the form and placeholder have been loaded from a\n                    Component XML file.  Use the \"Replace PlaceHolder\" button to replace the\n                    placeholder with a programatically generated grid component.\n                    <P>\n                    Or, check the \"Auto-replace Placeholder\" checkbox and press the \"Reload\n                    Component XML\" button to see the replacement take place automatically as\n                    soon as the Component XML is loaded.\n                \n                "
                                    },
                                    {
                                        id:"customComponents",
                                        jsURL:"componentXML/customComponents.js",
                                        title:"Custom Components",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                doEval:"false",
                                                title:"component XML",
                                                url:"ui/customComponents.ui.xml"
                                            },
                                            {
                                                dataSource:"supplyItem",
                                                name:"supplyItem"
                                            }
                                        ],
                                        descriptionHeight:"200",
                                        description:"\n                    You can use custom components in screens created via Component XML. Just\n                    use the \"constructor\" attribute to indicate that your custom class should\n                    be used.\n                    <P>\n                    You can even provide custom properties to your custom class.  In the sample\n                    below, the custom ListGrid subclass \"MyListGrid\" has a boolean setting\n                    \"hilitePricesOverTen\" that causes prices over $10 to appear in red color.\n                    This boolean setting is false by default, but the Component XML file sets\n                    the property to true on the grid.\n                    <P>\n                    It's also possible to declare a Component Schema so that &lt;MyListGrid&gt;\n                    can be used directly as the XML tag, with no need to set the constructor or\n                    declare the types of custom properties like \"hilitePricesOverTen\".\n                \n                "
                                    },
                                    {
                                        id:"screenReuse",
                                        jsURL:"componentXML/screenReuse.js",
                                        title:"Screen Reuse",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                doEval:"false",
                                                title:"screenReuse XML",
                                                url:"ui/screenReuse.ui.xml"
                                            },
                                            {
                                                dataSource:"countryDS",
                                                name:"countryDS"
                                            },
                                            {
                                                dataSource:"supplyItem",
                                                name:"supplyItem"
                                            }
                                        ],
                                        descriptionHeight:"200",
                                        description:"\n                    Using <code>createScreen()</code>, you can load multiple copies of the same Component XML\n                    screen, and the copies will not interfere with each other.  This allows Component XML\n                    screens to be treated as simple reusable components.\n                    <P>\n                    Use the \"DataSources\" drop-down below to select a DataSource.  Each time you select a new\n                    DataSource, a new copy of the same Component XML screen is created, and its components are\n                    bound to the selected DataSource.\n                \n                "
                                    }
                                ]
                            },
                            {
                                isOpen:false,
                                showSkinSwitcher:"true",
                                title:"Real-Time Messaging",
                                description:"\n\t     RTM module provides low-latency, high data volume streaming\n         capabilities for latency-sensitive applications such as trading desks and operations\n         centers.\n\t    ",
                                children:[
                                    {
                                        id:"FSportfolioGrid",
                                        ref:"portfolioGrid",
                                        title:"Portfolio Grid"
                                    },
                                    {
                                        id:"FSstockQuotesChart",
                                        ref:"stockQuotesChart",
                                        title:"Stock Chart"
                                    }
                                ]
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_lightning.png",
                                isOpen:false,
                                title:"Server Scripting",
                                description:"\n        Simple business logic and validation rules can be embedded directly in *.ds.xml files.  Use Java, or scripting\n\t\tlanguages such as Groovy or JavaScript.\n\t\t",
                                children:[
                                    {
                                        id:"FSscriptingUserSpecificData",
                                        ref:"scriptingUserSpecificData",
                                        title:"User-Specific Data"
                                    },
                                    {
                                        id:"FSIinlineScriptValidation",
                                        ref:"inlineScriptValidation",
                                        title:"Validation"
                                    }
                                ]
                            },
                            {
                                id:"FSrssFeed",
                                ref:"rssFeed",
                                requiresModules:"SCServer",
                                title:"HTTP Proxy",
                                descriptionHeight:"140",
                                description:"\n            The SmartClient Server includes an HTTP Proxy servlet which supports contacting REST and\n            WSDL web services as though they were hosted by a local web server, avoiding the \"same origin\n            policy\" restriction which normally prevents web applications from accessing remote\n            services.\n            <P>\n            The proxy is used automatically whenever an attempt to contact a URL on another host is performed. No\n            special code is needed.  In this example, a DataSource is configured to download the\n            Slashdot RSS feed, with no server-side code or proxy configuration required.\n            <P>\n            Configuration files allow for restricting proxying to specific\n            services that should be accessible to users through your application.\n        "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_tile.png",
                        id:"tilingFilterFS",
                        ref:"tilingFilter",
                        title:"Tile Sort & Filter"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/cube_simple.png",
                        id:"basicCubeFS",
                        ref:"basicCube",
                        title:"Simple Cube"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/cube_simple.png",
                        id:"analyticsFS",
                        ref:"analytics",
                        title:"Advanced Cube"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/cal.png",
                        id:"dataBoundCalendarFS",
                        ref:"databoundCalendar",
                        title:"Databound Calendar"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/world.png",
                        id:"portalFS",
                        ref:"portal",
                        title:"Dashboards & Tools"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/printer.png",
                        id:"printingFS",
                        ref:"printing",
                        title:"Printing"
                    },
                    {
                        descriptionHeight:"180",
                        id:"offlineSupport",
                        jsURL:"offline/offlineSupport.js",
                        title:"Offline support",
                        tabs:[
                            {
                                dataSource:"supplyItem",
                                name:"supplyItem"
                            },
                            {
                                dataSource:"supplyCategory",
                                name:"supplyCategory"
                            }
                        ],
                        description:"\n        SmartClient has support for caching server responses in browser storage, allowing\n        these cached responses to be returned to an application at some future point when \n        the application is offline.  Offline support is automatic once enabled - if the user\n        switches the application or browser into offline mode, or the browser detects that it\n        is offline, the framework automatically and transparently starts returning cached \n        responses whenever it can (application code can determine that responses have come \n        from offline cache if necessary)<p>\n        Use the tree to navigate categories. Click a category to load the grid with\n        matching items.  Now reload the page and click \"Go offline\" (or switch the\n        browser into offline mode).  If a category is clicked that had previously been selected before\n        the reload, the grid will still be populated from the Offline cache. If \n        a category is clicked that had not been previously selected, or a node is opened\n        in the tree that had not been previously opened, the \"Data not available\n        while offline\" message will be displayed.\n        "
                    },
                    {
                        descriptionHeight:"160",
                        id:"offlinePrefs",
                        jsURL:"offline/offlinePrefs.js",
                        title:"Offline preferences",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"countryDS",
                                url:"grids/ds/countrySQLDS.ds.xml"
                            }
                        ],
                        description:"\n        SmartClient provides a unified Offline browser storage API that can be used \n        for any client-side persistence task.  In this example, the ListGrid's \n        viewState to browser-local storage is stored. Try resizing or reordering some columns in the\n        grid, click \"Persist State\", then press F5 to reload, or close and re-open the browser.\n        The changes have been remembered.  Try adding a formula field to the grid and reload \n        again. Persisting a user's preferences like this is a compelling addition to any application, \n        and in this case doesn't even require a server round trip.<p>\n        Offline support is provided in modern HTML5 browsers, and also in older versions of \n        Internet Explorer (6 and greater). The underlying technologies used are very different,\n        but the SmartClient API is the same, regardless of which browser is used.\n        "
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/database_table.png",
                        id:"patternReuseFS",
                        ref:"patternReuse",
                        title:"Pattern Reuse"
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/bug.png",
                        jsURL:"devConsole/devConsole.js",
                        showSkinSwitcher:false,
                        showSource:false,
                        title:"Developer Console",
                        descriptionHeight:"105",
                        description:"\nThe Developer Console is a suite of development tools implemented in SmartClient itself.  The\nConsole runs in its own browser window, parallel to the running application, so it is always\navailable, in every browser, and in every deployment environment.<BR><BR> \nClick on the name of a screenshot below to see more information about developer\nconsole features.\n        "
                    },
                    {
                        jsURL:"docs/docs.js",
                        showSkinSwitcher:false,
                        showSource:false,
                        title:"SmartClient Docs",
                        description:"\n        SmartClient contains over 100 documented components with more than 2000 documented,\n        supported APIs.  All of SmartClient's documentation is integrated into a\n        SmartClient-based, searchable documentation browser, including API reference, concepts,\n        tutorials, live examples, architectural blueprints and deployment instructions.\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/application_view_columns.png",
                isOpen:false,
                showSkinSwitcher:"true",
                title:"Grids",
                description:"\n    High-performance interactive data grids.\n",
                children:[
                    {
                        isOpen:false,
                        title:"Appearance",
                        description:"\n    Styling, sizing and formatting options for grids, as well as built-in end user controls.\n",
                        children:[
                            {
                                id:"columnOrder",
                                jsURL:"grids/layout/columnOrder.js",
                                title:"Column order",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Drag and drop the column headers to rearrange columns in the grid.\n        Right-click the column headers to hide or show columns.\n        Click the buttons to hide or show the \"Capital\" column.\n        "
                            },
                            {
                                id:"columnSize",
                                jsURL:"grids/layout/columnSize.js",
                                title:"Column size",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click and drag between the column headers to resize columns in the grid.\n        "
                            },
                            {
                                jsURL:"grids/layout/columnAlign.js",
                                title:"Column align",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the radio buttons to change the alignment of the \"Flag\" column.\n        "
                            },
                            {
                                jsURL:"grids/layout/columnHeaders.js",
                                title:"Column headers",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to show or hide the column headers.\n        "
                            },
                            {
                                jsURL:"grids/layout/columnTitles.js",
                                title:"Column titles",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to change the title of the \"Country\" column.\n        "
                            },
                            {
                                id:"multilineValues",
                                jsURL:"grids/layout/multiLineValues.js",
                                title:"Multiline values",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryDataDetail.js"
                                    }
                                ],
                                description:"\n        Click and drag between the \"Background\" and \"Flag\" column headers, or resize the browser\n        window to change the size of the entire grid. The \"Background\" values are\n        confined to a fixed row height.\n        "
                            },
                            {
                                id:"gridHeaderSpans",
                                ref:"headerSpans",
                                title:"Header Spans"
                            },
                            {
                                jsURL:"grids/formatting/cellStyles.js",
                                title:"Cell styles",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/formatting/cellStyles.css"
                                    },
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Mouse over the rows and click-drag to select rows, to see the effects of different\n        base styles on these two grids.\n        "
                            },
                            {
                                id:"addStyle",
                                jsURL:"grids/formatting/addStyle.js",
                                title:"Style cells (add style)",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This grid hilites \"Population\" values greater than 1 billion or less than 50 million\n        using additive style attributes (text color and weight).\n        "
                            },
                            {
                                id:"replaceStyle",
                                jsURL:"grids/formatting/replaceStyle.js",
                                title:"Style cells (replace style)",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/formatting/replaceStyle.css"
                                    },
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This grid hilites \"Population\" values greater than 1 billion or less than 50 million\n        using a full set of compound styles (with customized background colors). Mouse over or\n        click-drag rows to see how these styles apply to different row states.\n    "
                            },
                            {
                                id:"fieldPicker",
                                jsURL:"grids/layout/fieldPicker.js",
                                title:"Field Picker",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/layout/cellStyles.css"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n        Some grids show a small subset of 100s of available fields.  With this many fields,  \n        it becomes awkward to choose and arrange fields by drag reordering of\n        headers and picking fields from a menu.\n        <P>\n        Setting <code>useAdvancedFieldPicker</code> causes an alternative field picking and\n        ordering interface to be used, shown below.  Drag fields from <i>Available Fields</i>\n        to <i>Visible Fields</i> to display them.  Use drag and drop to reorder fields as well.\n        <P>\n        This interface also allows you to search for fields by name, and optionally to choose\n        which are frozen.\n        <P>\n        The end user can open this dialog via the \"Columns..\" menu item in the header menu.\n    "
                            },
                            {
                                jsURL:"grids/formatting/animatedSelection_RollOver.js",
                                title:"Animated Selection",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/formatting/simpleCellStyles.css"
                                    },
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n\t\t\n\t\tRollOver, and select the rows in the grid to see rollover and selection indicators fade\n        into view. This is achieved via the <code>rollOverCanvas</code> and <code>selectionCanvas</code> subsystem. \n        Note that the opacity setting on the <code>rollUnderCanvas</code> allows true color layering.\n          \n        "
                            },
                            {
                                jsURL:"grids/formatting/rollOverControls.js",
                                title:"RollOver Controls",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n\t\t\n\t\t RollOver the rows in the grid to see row-level control buttons appear embedded in the row.\n        This example utilizes the <code>rollOverCanvas</code> subsystem to achieve this effect. \n          \n        "
                            },
                            {
                                descriptionHeight:"120",
                                id:"formatValuesBuiltin",
                                jsURL:"grids/formatting/formatValuesBuiltin.js",
                                title:"Format values",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This grid applies formatters to the \"Nationhood\" and \"Area\" columns using SmartClient's\n        built-in declarative formatting feature, which can format dates and numbers using \n        format strings like \"MMM dd yyyy\".  If you have Pro+, this formatting will also be \n        exported to Excel.\n        <p>\n        Click on the \"Nationhood\" or \"Area\" column headers to sort the underlying data values.\n        "
                            },
                            {
                                descriptionHeight:"120",
                                id:"formatValues",
                                jsURL:"grids/formatting/formatValues.js",
                                title:"Format values (custom)",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This grid applies custom formatters to the \"Nationhood\" and \"Area\" columns.  Custom \n        formatters are written in Javascript; they are useful when you have unusual formatting\n        requirements that cannot be achieved with the built-in declarative formatting features\n        (as is the case with the \"Nationhood\" formatting in this sample).\n        <p>\n        Click on the \"Nationhood\" or \"Area\" column headers to sort the underlying data values.\n        "
                            },
                            {
                                id:"emptyValues",
                                jsURL:"grids/formatting/emptyValues.js",
                                title:"Empty values",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Double-click any cell, delete its value, and press Enter or click outside the cell to\n        save and display the empty value. This grid shows \"--\" for empty date values, and\n        \"unknown\" for other empty values.\n        "
                            },
                            {
                                id:"emptyGrid",
                                jsURL:"grids/layout/emptyGrid.js",
                                title:"Empty grid",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to add or remove all data in the grid.\n        "
                            },
                            {
                                id:"gridComponents",
                                jsURL:"grids/layout/gridComponents.js",
                                title:"Custom Layout",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        ListGrids can be customized by changing the order of standard components, and adding\n        entirely custom components. In this example\n        the filter row is showing below the ListGrid header bar, and a custom set of controls has\n        been added underneath the body.\n        "
                            },
                            {
                                jsURL:"grids/formatting/roundedSelection.js",
                                title:"Rounded Selection",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/formatting/simpleCellStyles.css"
                                    },
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n\t\t\n\t\tSelect a row to see a custom selection effect with rounded edges achieved via the\n        <code>selectionCanvas</code> subsystem.\n        \n        ",
                                badSkins:"BlackOps",
                                bestSkin:"TreeFrog"
                            },
                            {
                                jsURL:"grids/formatting/rollOverRecticle.js",
                                title:"RollOver Reticle Effect",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n\t\t\n\t\t RollOver the rows in the grid to see a custom roll over recticle effect, \n        achieved via the <code>rollOverCanvas</code> subsystem.\n          \n        ",
                                badSkins:"BlackOps",
                                bestSkin:"TreeFrog"
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Interaction",
                        description:"\n    Selection and drag and drop of data, hovers, and grid events.\n",
                        children:[
                            {
                                jsURL:"grids/interaction/rollover.js",
                                title:"Rollover",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Move the mouse over rows in the grid to see rollover highlights.\n        Click the buttons to enable or disable this behavior.\n        "
                            },
                            {
                                jsURL:"grids/selection/singleSelect.js",
                                title:"Single select",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click to select any single row in the grid.\n        "
                            },
                            {
                                id:"multipleSelect",
                                jsURL:"grids/selection/multipleSelect.js",
                                title:"Multiple select",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click to select a single row in the grid. Shift-click to select a continuous range of rows.\n        Ctrl-click to add or remove individual rows from the selection.\n        "
                            },
                            {
                                jsURL:"grids/selection/simpleSelect.js",
                                title:"Simple select",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click to select or deselect any row in the grid.\n        "
                            },
                            {
                                id:"checkboxSelect",
                                jsURL:"grids/selection/checkboxSelect.js",
                                title:"Checkbox Select",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n\t\t\n\t\t By setting <code>selectionAppearance</code> to \"checkbox\", the ListGrid can use checkboxes \n        to indicate the selected state of records. Only by clicking on a checkbox will the \n        corresponding record be selected or unselected.\n          \n        "
                            },
                            {
                                jsURL:"grids/selection/dragSelect.js",
                                title:"Drag select",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click and drag to select a continuous range of rows in the grid.\n        "
                            },
                            {
                                id:"cellSelection",
                                jsURL:"grids/selection/cellSelection.js",
                                title:"Cell Selection",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Grids support cell-level selection.  Click and drag to select a contiguous\n        block of cells.  Use Ctrl-Click (Option-Click on Mac) to select or deselect individual\n        cells.  Shift-clicking will extend the current selection to include the target cell.\n        "
                            },
                            {
                                id:"valueHoverTips",
                                jsURL:"grids/interaction/valueHover.js",
                                title:"Value hover tips",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Move the mouse over a value in the \"Government\" column and pause (hover) for a\n        longer description of that value.\n        "
                            },
                            {
                                jsURL:"grids/interaction/headerHover.js",
                                title:"Header hover tips",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Move the mouse over a column header and pause (hover) for a longer description\n        of that column.\n        "
                            },
                            {
                                id:"gridsDragReorder",
                                jsURL:"grids/interaction/dragOrder.js",
                                title:"Drag reorder",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Drag and drop to change the order of countries in this list.\n        "
                            },
                            {
                                id:"gridsDragMove",
                                jsURL:"grids/interaction/dragMove.js",
                                title:"Drag move",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Drag and drop to move rows between the two lists. \n        "
                            },
                            {
                                id:"gridsDragCopy",
                                jsURL:"grids/interaction/dragCopy.js",
                                title:"Drag copy",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Drag and drop to copy rows from the first list to the second list.\n        "
                            },
                            {
                                id:"disabledRows",
                                jsURL:"grids/interaction/disabled.js",
                                title:"Disabled rows",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Mouse over, drag, or click on any values in this grid.\n        All \"Europe\" country records in this grid are disabled.\n        "
                            },
                            {
                                id:"recordClicks",
                                jsURL:"grids/interaction/recordClicks.js",
                                title:"Record clicks",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click, double-click, or right-click any row in the grid.\n        "
                            },
                            {
                                id:"cellClicks",
                                jsURL:"grids/interaction/cellClicks.js",
                                title:"Cell clicks",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click, double-click, or right-click any value in the grid.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Autofit",
                        description:"\n    Various auto-fit behaviors built-in to every Grid\n",
                        children:[
                            {
                                id:"autoFitFreeSpace",
                                jsURL:"grids/autofit/autoFitFreeSpace.js",
                                title:"Free Space",
                                tabs:[
                                    {
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                description:"\n        By default, grids used in a Layout will fill available space, allowing users to show \n        and hide other components on the screen in order to view and interact with more data \n        at once in the grid.  Imagine that the blue outline represents all the space that is\n        available for this interface.  Click on the \"Details\" header to hide the tabs and\n        reveal more rows.  Click on the resizebar next to the Navigation tree to hide it,\n        allowing more space for columns.\n        "
                            },
                            {
                                id:"autofitValues",
                                jsURL:"grids/autofit/autoFitValues.js",
                                title:"Cell Values",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryDataDetail.js"
                                    }
                                ],
                                description:"\n        Click and drag between the \"Background\" and \"Flag\" column headers, or resize the browser\n        window to change the size of the entire grid. The rows resize to fit\n        the \"Background\" values.\n        "
                            },
                            {
                                id:"autofitRows",
                                jsURL:"grids/autofit/autoFitRows.js",
                                title:"Rows",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to show different numbers of records. The grid resizes to fit\n        all rows without scrolling.\n        "
                            },
                            {
                                id:"autofitColumns",
                                jsURL:"grids/autofit/autoFitColumns.js",
                                title:"Columns",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click and drag between the column headers to resize the columns. The grid resizes to\n        fit the new column widths.  The width setting on the grid as a whole acts as a minimum.\n        "
                            },
                            {
                                id:"autofitColumnWidths",
                                jsURL:"grids/autofit/autoFitColumnWidths.js",
                                title:"Column Widths",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        ListGrid fields can be set to auto-fit to their titles and/or field values. In this\n        example the first two columns are set to auto-fit when the grid is drawn. The first\n        field's title exceeds the space used by its values. With the second field, the reverse\n        is true. In both cases the column is correctly sized to fit its content.\n        Note that the user can also perform one time auto-fit of columns at runtime by\n        double-clicking on any header or using\n        the context-menu option.\n        "
                            },
                            {
                                id:"autofitNewRecords",
                                jsURL:"grids/autofit/autoFitNewRecords.js",
                                title:"New Records",
                                description:"\n        Autofit to rows can be made subject to a maximum. Add new rows to the grid, and note that the\n        grid expands to show the new rows. This grid is configured to stop expanding once there are more\n        than 5 rows, and begin scrolling instead.\n        "
                            },
                            {
                                id:"autofitFilter",
                                jsURL:"grids/autofit/autoFitFilter.js",
                                title:"Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Change the filter to show the grid resizing within the constraint of its maximum \n        autofit rows. Enter a \"Country\" filter of \"cook island\" to see the grid shrink down to minimum\n        size. Change the \"Country\" filter to \"island\" to show the grid at almost maximum size, but\n        not scrolling.  Change the \"Country\" filter to \"land\" to show the grid scrolling because\n        its maximum autofit size (10) isn't large enough to display all rows.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Filtering",
                        description:"\n    SmartClient grids provide interactive filtering of standard and custom data types,\n    with automatic client/server coordination.\n",
                        children:[
                            {
                                id:"filter",
                                jsURL:"grids/filtering/filter.js",
                                title:"Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Type \"island\" above the \"Country\" column, then press Enter or click the filter button\n        (top-right corner of the grid) to show only countries with \"island\" in their name.\n        Select \"North America\" above the \"Continent\" column to filter countries by that continent.\n        "
                            },
                            {
                                id:"liveFilter",
                                jsURL:"grids/filtering/liveFilter.js",
                                title:"Live Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Begin typing a country name into in the filter box for the \"Country\" column.  Grids can \n        be configured to filter as you type.        \n        "
                            },
                            {
                                descriptionHeight:"190",
                                id:"adaptiveFilter",
                                jsURL:"grids/filtering/adaptiveFilter.js",
                                title:"Adaptive Filter",
                                tabs:[
                                    {
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                description:"\n        SmartClient combines large dataset handling with adaptive use of\n        client-side filtering.  Begin typing an Item name in the filter box above the \"Item\"\n        column (for example, enter \"add\").  When the dataset becomes small enough, SmartClient\n        switches to client-side filtering automatically. Enter more letters, or criteria on\n        other columns, to see this.  The label underneath the grid flashes briefly\n        every time SmartClient needs to visit the server.\n        <P>\n        Delete part of the item name to see SmartClient automatically switch back to\n        server-side filtering when necessary.  \n        <P>\n        Adaptive filtering eliminates up to 90% of the most costly types of server contact\n        (searching through large datasets), <b>dramatically improving responsiveness and\n        scalability</b>.\n        "
                            },
                            {
                                id:"advancedFilter",
                                jsURL:"grids/filtering/advancedFilter.js",
                                title:"Advanced Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        SmartClient's Advanced Filter feature allows you to create complex, multi-condition \n        filters.  Ordinary <code>DynamicForms</code> can be used to generate AdvancedCriteria objects, as is\n        shown here, simply by specifying a valid \"operator\" on one or more of the form fields.  \n        "
                            },
                            {
                                id:"filterBuilder",
                                jsURL:"grids/filtering/filterBuilder.js",
                                title:"Custom Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Use the FilterBuilder to construct custom queries that combine multiple criteria across\n        any field in your DataSource.  Note that the operator\n        select list only shows operators suitable for the field selected, and the comparison\n        field changes to suit the type of the selected field (for example, select field \n        \"independence\" and note that the comparison field changes to a date). Add clauses to\n        your query with the \"+\" icon. Click \"Filter\" to see the result in the ListGrid.\n        "
                            },
                            {
                                id:"filterBuilderBracket",
                                jsURL:"grids/filtering/filterBuilderBracket.js",
                                title:"Nested Filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"160",
                                description:"\n        Use the FilterBuilder to construct queries of arbitrary complexity.  The FilterBuilder,\n        and the underlying <code>AdvancedCriteria</code> system, support building queries with subclauses\n        nested to any depth.  The initial criteria in this example is set up to display only\n        European countries where either the name ends with \"land\", or the population is less \n        than 3 million - an unlikely query, perhaps, but one that shows the simplest example of\n        the FilterBuilder's flexibility.\n        <p>\n        Add clauses to the query with the \"+\" icon. Add nested subclauses with the \"+()\" button.\n        Click \"Filter\" to see the result in the ListGrid.\n        "
                            },
                            {
                                id:"bigFilter",
                                jsURL:"grids/filtering/bigFilter.js",
                                title:"Big Filter",
                                tabs:[
                                ],
                                descriptionHeight:"160",
                                description:"\n        When a FilterBuilder must work with a very large number of fields, it is possible to set \n        <code>FilterBuilder.fieldDataSource</code> to a DataSource containing records that represent the\n        fields to display in the FieldPickers in each clause.  The FilterBuilder below is \n        created without a normal DataSource, but specifies a fieldDataSource and the \n        FieldPicker items in each clause are populated dynamically with it's records.  In this \n        mode, the FieldPickers are represented by ComboBoxItems, rather than SelectItems, and \n        have default settings that provide type-ahead auto-completion.\n        <P>Note also that, when fieldDataSource is specified and the operator for a clause\n        is of a type that uses a field-lookup, the valueField is also populated dynamically by \n        the fieldDataSource.\n        "
                            },
                            {
                                id:"headerSpans",
                                jsURL:"grids/sorting/headerSpans.js",
                                title:"Header Spans",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        HeaderSpans are a second level of headers that appears above the normal ListGrid \n        headers, providing a visual cue for grouping. Resize the columns and note that the \n        HeaderSpans change accordingly. Right-click in the header and note that the ability to hide\n        and display spanned columns as a group is available, as well as individually.\n        "
                            },
                            {
                                id:"disableFilter",
                                jsURL:"grids/filtering/disable.js",
                                title:"Disable filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Type \"island\" above the \"Country\" column, then press Enter or click the filter button\n        (top-right corner of the grid) to show only countries with \"island\" in their name.\n        Select \"North America\" above the \"Continent\" column to filter countries by that continent.\n        Filtering is disabled on the \"Flag\" and \"Capital\" columns.\n        "
                            },
                            {
                                id:"gridFilterAutoFit",
                                ref:"autofitFilter",
                                title:"Autofit filter"
                            },
                            {
                                id:"dateRangeFilter",
                                jsURL:"grids/filtering/dateRangeFilter.js",
                                title:"Date Range",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n        Smartclient provides special widgets for filtering date values, including recognised \n        RelativeDate strings that cause filtering relative to some other base date. \n        <P>The <code>RelativeDateRangeItem</code> allows selection of dates in three ways: you can select a\n        preset date string, like \"Today\" or \"Tomorrow\", or a \"ranged\" date string, such as \n        \"N days from now\" and enter a quantity to associate with it, or you can directly enter\n        a date string in a recognized format.  You can also select a date from the DateChooser\n        by clicking the icon to the right of the widget.\n        <P>The first example below demonstrates using a DateRangeItem in a seperate DynamicForm\n        to filter a ListGrid.  Select start and end values for the range using one of the \n        methods described above and click the \"Search\" button to see the data filtered \n        according to the values in the \"Nationhood\" field.\n        <P>The second example below demonstrates filtering grid data using a <code>MiniDateRangeItem</code>\n        to filter data when a ListGrid is showing it's FilterEditor.  In\n        this example, click the Date icon in the header for the \"Nationhood\" field to open a \n        popup DateRangeItemDialog.  In the dialog, select start and end values for the range, \n        as described above, and click Ok to close the Window.  Then click the Filter button in\n        the top right of the grid to see the data filtered.  You can hover the mouse over the \n        \"Nationhood\" field-header to see the full date-range string.\n        "
                            },
                            {
                                id:"dateRangeFilterPresets",
                                jsURL:"grids/filtering/dateRangeFilterPresets.js",
                                title:"Date Range (Presets)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"RecentDateRangeItem",
                                        url:"grids/filtering/recentDateRangeItem.js"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"presetDateRangeData.js",
                                        url:"grids/data/presetDateRangeData.js"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n        SmartClient provides a <code>PresetCriteriaItem</code>, which allows a user to select \n        criteria from a SelectItem using a recognizable display-name, from a list of options\n        provided to it.  The item can also include a \"Custom...\" entry which, when selected, \n        calls the <code>getCustomCriteria()</code> override point, allowing the developer to provide a \n        custom interface for selecting criteria. There's also a simple subclass of this item, \n        <code>PresetDateRangeItem</code>, which has custom code to show a DateRangeDialog for \n        collecting custom-criteria.\n        <P>This example demonstrates using a subclass of the second example in both a ListGrid's\n        FilterEditor, and in a stand-alone FilterBuilder.A simple subclass of \n        <code>PresetDateRangeItem</code> is created, called <i>RecentDateRangeItem</i>, that lists a number of \n        common options for filtering by recent date ranges.  This is then used as an editor in \n        both UIs.  In the first example, select an option in the \"Order Date\" field and click \n        the 'Filter' icon to see the filter applied.  In the second example, choose\n        a named-range in the \"value\" field to the right of the FilterBuilder and click the \n        'Filter' button below to have the selected criteria applied to the bottom grid.\n        "
                            },
                            {
                                id:"expressionFilter",
                                jsURL:"grids/filtering/expressionFilter.js",
                                title:"Expression filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n        DynamicForms and FormItems are capable of parsing simple expressions entered as part of\n        their values, when <code>allowExpressions</code> is true on either entity.  ListGrids use this\n        facility, when <code>showFilterEditor</code> and <code>allowFilterExpressions</code> are true, to allow \n        expressions to be entered directly into the FormItems displayed in the filterEditor.\n        <P>Below is a ListGrid with a FilterEditor and <code>allowFilterExpressions: true</code>.  Some \n        expression-based filter-criteria has been applied via <code>initialCriteria</code>. The list displays \n        countries with no 'i's in the country name, with a \"Capital\" that starts with a letter \"A\"\n        through \"F\" and with a \"Population\" less than 1 million or more than 100 million.\n        <P>See the table on the right for the supported expression-symbols.  Note that \n        logical \"and\" and \"or\" expressions are treated as text in text-based fields and ignored.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Sorting",
                        description:"\n    SmartClient grids provide interactive sorting of standard and custom data types,\n    with automatic client/server coordination.\n",
                        children:[
                            {
                                id:"sort",
                                jsURL:"grids/sorting/sort.js",
                                title:"Sort",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on any column header to sort by that column. To reverse the sort direction,\n        click on the same column header, or the top-right corner of the grid.\n        "
                            },
                            {
                                jsURL:"grids/sorting/disableSort.js",
                                title:"Disable sort",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Sorting is disabled on the \"Flag\" column. Click on any other column header to sort\n        on the corresponding column.\n        "
                            },
                            {
                                jsURL:"grids/sorting/sortArrow.js",
                                title:"Sort arrows",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on any column header to sort or reverse-sort by that column.\n        This grid shows the sort-direction arrow in the top-right corner only.\n        "
                            },
                            {
                                id:"dataTypes",
                                jsURL:"grids/sorting/dataTypes.js",
                                title:"Data-Aware Sort",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on any column header to sort by that column.\n        The \"Nationhood\", \"Area\", and \"GDP (per capita)\" columns are sorted as date, number, and\n        calculated number values, respectively.\n        "
                            },
                            {
                                id:"multiLevelSortLG",
                                jsURL:"grids/sorting/multiLevelSortLG.js",
                                title:"Multilevel Sort (UI)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n        This grid is displayed pre-sorted on two fields: first by \"Category\" ascending and then\n        by \"Item Name\" descending.  As well as the field's title, the header of each field included \n        in the sort configuration displays a sort-arrow indicating the direction of sort on that \n        field and, when multiple fields are sorted, a small numeral indicating this field's \n        position in the list of fields being sorted.  By using SHIFT-click, \n        an already sorted column-headers direction can be reversed, or an unsorted column\n        header can be added to the list of fields being sorted.  Clicking a column header\n        without holding down SHIFT clears the current sort configuration and initializes a new\n        sort on the selected field.\n        <P>SmartClient's SQL and Hibernate adapters support server-side multi-sorting and this\n        is in evidence in this example.\n        "
                            },
                            {
                                id:"multiLevelSortDialog",
                                jsURL:"grids/sorting/multiLevelSort.js",
                                title:"Multilevel Sort (Dialog)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                description:"\n\t\t\n\t\t Click the \"Multilevel Sort\" button to show a <code>MultiSortDialog</code>.  Select a set of sort\n        properties and directions and click \"Save\" to see the grid re-sorted by those properties.\n           \n        "
                            },
                            {
                                id:"adaptiveSort",
                                jsURL:"grids/sorting/adaptiveSort.js",
                                title:"Adaptive Sort",
                                tabs:[
                                    {
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                description:"\n        SmartClient combines large dataset handling with adaptive use of\n        client-side sort.  Click any header and server-side sort will be used for this\n        large dataset.  Check \"Limit to Electronics\" to limit the dataset and sort again.\n        When the dataset becomes small enough, SmartClient switches to client-side\n        sorting automatically.  The label underneath the grid flashes briefly \n        every time SmartClient needs to visit the server.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Editing",
                        description:"\n    SmartClient grids provide inline editing of all data types, with automatic validation and\n    client/server updates.<br><br>\n    These examples are all bound to the same remote DataSource, so the\n    changes are saved on SmartClient.com and will appear in all Grid Editing examples during this\n    session. To end the SmartClient.com session and reset the example data on the server, simply\n    close all instances of the web browser.\n",
                        children:[
                            {
                                id:"editByRow",
                                jsURL:"grids/editing/editRows.js",
                                title:"Edit by row",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing. Use Tab, Shift-Tab, Up Arrow and Down Arrow to move \n\t\tbetween cells. Changes are saved automatically when moving to another row. Press Enter to\n\t\tsave the current row and dismiss the editors, or Esc to discard changes for the current row\n\t\tand dismiss the editors.\n        "
                            },
                            {
                                id:"editByCell",
                                jsURL:"grids/editing/editCells.js",
                                title:"Edit by cell",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing. Use Tab, Shift-Tab, Up Arrow and Down Arrow to move between\n\t\tcells. Press Enter to save the current row and dismiss the editors, or Esc to discard changes for\n\t\tthe current cell and dismiss the editors.\n        "
                            },
                            {
                                id:"enterNewRows",
                                jsURL:"grids/editing/enterRows.js",
                                title:"Enter new rows",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing, then Tab or Down Arrow past the last row in the grid to create\n\t\ta new row. Alternatively, click the \"Edit New\" button to create a new data-entry row at the end of\n\t\tthe grid.\n        "
                            },
                            {
                                id:"editingAutoFitNewRows",
                                ref:"autofitNewRecords",
                                title:"Autofit new rows"
                            },
                            {
                                id:"massUpdate",
                                jsURL:"grids/editing/massUpdate.js",
                                title:"Mass Update",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"120",
                                description:"\n        Click on any cell to start editing, then Tab or Down Arrow past the last row in the grid to create \n\t\ta new row. Alternatively, click the \"Edit New\" button to create a new data-entry row at the end of \n\t\tthe grid. Unlike the other editing examples, none of the changes are being automatically saved to \n\t\tthe server.  Note how SmartClient highlights changed values, and new rows. Click the \"Save\" button \n\t\tto save all changes at once, or click the \"Discard\" button to discard all changes (including any new \n\t\trows) and revert to the data as it was before editing started.\n        "
                            },
                            {
                                id:"modalEditing",
                                jsURL:"grids/editing/modalEditing.js",
                                title:"Modal editing",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Double-click on any cell to start editing. Click anywhere outside of the cell editors to save changes,\n\t\tor press the Esc key to discard changes.\n        "
                            },
                            {
                                id:"disableEditing",
                                jsURL:"grids/editing/disableEditing.js",
                                title:"Disable editing",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing. Use Tab/Arrow keys to move between cells,\n        Enter/Esc keys to save or cancel. Editing is disabled for the \"Country\" and \"G8\" columns.\n        "
                            },
                            {
                                id:"customEditors",
                                jsURL:"grids/editing/customEditors.js",
                                title:"Custom editors",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing. The \"Government\", \"Population\", and \"Nationhood\"\n        columns specify custom editors. In this example, they are a multiple-line Text Area, a Numeric Spinner\n\t\tand a Compound Date Control.\n        "
                            },
                            {
                                id:"dataValidation",
                                jsURL:"grids/editing/validation.js",
                                title:"Data validation",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click on any cell to start editing. Delete the value in a \"Country\" cell, or type a\n        non-numeric value in a \"Population\" cell, to see validation errors.\n        "
                            },
                            {
                                id:"databoundDependentSelects",
                                jsURL:"grids/editing/dependentSelects.js",
                                title:"Dependent Selects",
                                tabs:[
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    },
                                    {
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n                \n                <p />In the first example, Double Click on any row to start editing. Select a value \n                in the \"Division\" column to change the set of options available in the \"Department\" \n                column.\n                <p />\n                <p />In the second example, click the \"Order New Item\" button to add an editable row \n                to the grid.  Select a \"Category\" in the second column to change the set of options \n                available in the \"Item\" column.\n            "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Frozen Columns",
                        description:"\n    SmartClient supports rendering out grids with frozen fields.<br><br>\n    Frozen fields are fields that do not scroll horizontally with the other fields, remaining\n    visible on the screen while other fields may be scrolled out of view.\n",
                        children:[
                            {
                                id:"simpleFreeze",
                                jsURL:"grids/freezeFields/simpleFreeze.js",
                                title:"Simple Freeze",
                                tabs:[
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:"\n        Setting <code>frozen:true</code> on a column definition establishes a\n        frozen column.  Column resize and reorder work normally.\n        "
                            },
                            {
                                descriptionHeight:"120",
                                id:"dynamicFreeze",
                                jsURL:"grids/freezeFields/dynamicFreeze.js",
                                title:"Dynamic Freeze",
                                tabs:[
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:"\n        Right click on any column header to show a menu that allows you to freeze\n        that column. Multiple columns may be frozen, and frozen columns may be\n        reordered.<br>\n        Right click on a frozen column to unfreeze it.\n        "
                            },
                            {
                                id:"canEditFreeze",
                                jsURL:"grids/freezeFields/freezeEditing.js",
                                title:"Editing",
                                tabs:[
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:"\n        SmartClient's inline editing support works normally with frozen columns\n        with no further configuration.\n        "
                            },
                            {
                                jsURL:"grids/freezeFields/freezeDragDrop.js",
                                title:"Drag and Drop",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/splitCountryData.js"
                                    }
                                ],
                                description:"\n        SmartClient's drag and drop support works normally with frozen columns\n        with no further configuration.  Drag countries within grids to reorder them, or between\n        grids to move countries back and forth.\n        "
                            }
                        ]
                    },
                    {
                        isopen:"false",
                        title:"Grouping & Summaries",
                        description:"\n    List entries can be grouped according to field value.\n    ",
                        children:[
                            {
                                id:"dynamicGrouping",
                                jsURL:"grids/grouping/dynamicGrouping.js",
                                title:"Dynamic Grouping",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Right click on any column header to show a menu that allows grouping by that \n        column. Right click and select \"Ungroup\" to return to a flat listing.\n        "
                            },
                            {
                                id:"groupedEditing",
                                jsURL:"grids/grouping/groupedEditing.js",
                                title:"Grouped Editing",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Inline editing works normally with grouped data. Edit the field that records \n        are grouped by and notice that the record will move to its new group automatically.\n        "
                            },
                            {
                                id:"customGrouping",
                                jsURL:"grids/grouping/customGrouping.js",
                                title:"Custom Grouping",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        You can specify custom grouping behaviors for a field. Group by the \"Nationhood\" and \n        \"Population\" fields to see examples of custom grouping.\n        "
                            },
                            {
                                id:"multiGrouping",
                                jsURL:"grids/grouping/multiGrouping.js",
                                title:"Multi Grouping",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"OrderItem",
                                        url:"grids/ds/orderItemLocalDS.ds.js"
                                    }
                                ],
                                descriptionHeight:"120",
                                description:"\n        Grids support multiple levels of grouping, including a built-in dialog that\n        allows users to configure grouping.  This is enabled by a single setting:\n        <code>canMultiGroup</code>.\n        <P>\n        The grid below is grouped by Category, then by Ship Date.  Push the \"Configure Grouping\"\n        button to launch a dialog for configuring multi-level grouping.  This can also be accessed\n        from the drop-down menu on any column header, via the \"Configure Grouping...\" menu item.\n        "
                            },
                            {
                                id:"groupingModes",
                                jsURL:"grids/grouping/groupingModes.js",
                                title:"Grouping Modes",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        This sample shows that a single ListGrid field may be grouped in more than\n        one way, by setting its groupingModes property, a valueMap.  By default the\n        grid is shown grouped by the \"Hemisphere\" mode of the Continent field--you\n        can also group by the Continent Name.  Similarly, there are two ways to\n        group by the Nationhood field.  You must define the getGroupValue() method\n        of a field to apply logic appropriate to the field's groupingMode.\n        "
                            },
                            {
                                id:"summaryGrid",
                                jsURL:"grids/summaries/gridSummary.js",
                                title:"Grid Summaries",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"OrderItem",
                                        url:"grids/ds/orderItemLocalDS.ds.js"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n        ListGrids support displaying summaries of the current data set in various ways.\n        Fields from individual records can be summarized into a single field value. In this\n        example the \"Total\" field displays a summary value calculated by\n        multiplying the \"Quantity\" and \"Price\" fields.\n        <P>\n        Summaries can also be displayed for multiple records. This example shows a summary row\n        at the end of each group in the grid, as well as an overall summary row with information\n        about every record in the grid. Note that in addition to standard summary functions\n        such as <code>\"sum\"</code> to generate a total, or <code>\"count\"</code> to generate\n        a count of records, completely custom functions may be used. This is demonstrated in\n        the \"Category\" field where a custom function determines how many categories exist in this dataset. \n        <P>\n        Click to edit and summaries are dynamically re-calculated to reflect the changes made. \n        "
                            },
                            {
                                id:"gridHeaderSummary",
                                jsURL:"grids/summaries/gridHeaderSummary.js",
                                title:"Grid Header Summary",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"OrderItem",
                                        url:"grids/ds/orderItemLocalDS.ds.js"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n\tListGrids support displaying summaries of the current data set in various ways.\tFields from individual records can be summarized into a single field value. In this example the \"Total\" field displays a\n\tsummary value calculated by multiplying the \"Quantity\" and \"Price\" fields.<P>\n\n\tSummaries can also be displayed for multiple records. This example shows a summary row at the end of each group in the grid as\n\twell as an overall summary row with information about every record in the grid. Note that in addition to standard summary\n\tfunctions such as <code>\"sum\"</code> to generate a total, or <code>\"count\"</code> to generate a count of records, completely custom functions may be\n\tused. This is demonstrated in the \"Category\" field where a custom function determines how many categories exist in this\n\tdataset.<P>\n\n\tAs illustrated in this sample, by calling <code>showGroupSummaryInHeader:true</code> field summary values for each group are\n\tdisplayed directly in the group header node, rather than showing up at the bottom of each expanded group.\n        "
                            },
                            {
                                id:"multiLineSummaries",
                                jsURL:"grids/summaries/multiLineSummaries.js",
                                title:"Multi Line Summaries",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"OrderItem",
                                        url:"grids/ds/orderItemLocalDS.ds.js"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n\tListGrids support displaying summaries of the current data set in various ways.\tFields from individual records can be summarized into a single field value. In this example the \"Total\" field displays a\n\tsummary value calculated by multiplying the \"Quantity\" and \"Price\" fields.<P>\n\n\tSummaries can also be displayed for multiple records. This example shows a summary row at the end of each group in the grid as\n\twell as an overall summary row with information about every record in the grid. Note that in addition to standard summary\n\tfunctions such as <code>\"sum\"</code> to generate a total, or <code>\"count\"</code> to generate a count of records, completely custom functions may be\n\tused. This is demonstrated in the \"Category\" field where a custom function determines how many categories exist in this\n\tdataset.<P>\n\n\tBy calling <code>summaryFunction</code> multiple times allows for applying more than one summary function to a field. This allows the\n\tdeveloper to set up multi-line summaries where each specified summary function result will show up in a separate summary row.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Expanding Rows",
                        description:"\n    SmartClient grids support a special <code>expansionField</code>.<br><br>\n    When <code>grid.canExpandRecords</code> is true, the expansionField is rendered out at the beginning of\n    the field list.  When this field is clicked for a record, the record is expanded and a\n    built-in component is embedded into the record's row, beneath it's field values.\n    <br><br>\n    A variety of components are supported by default, according to the value <code>grid.expansionMode</code>,\n    and <code>grid.getExpansionComponent()</code> can also be overriden  to add custom expansion behaviors.\n",
                        children:[
                            {
                                id:"expansionDetailField",
                                jsURL:"grids/expansion/expansionDetailField.js",
                                title:"Detail Field",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryDataDetail.js"
                                    }
                                ],
                                description:"\n        This grid displays fields from the Countries DataSource.  Expand a \n        row by clicking the special <code>expansionField</code> to see the details of\n        the selected country's background in the expanded section.\n        "
                            },
                            {
                                id:"expansionDetails",
                                jsURL:"grids/expansion/expansionDetails.js",
                                title:"Details",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:" \n        This grid displays a limited number of fields from the supplyItem DataSource.  Expand\n        a row by clicking the special <code>expansionField</code> to see a DetailViewer\n        embedded in the expanded portion of the record which displays the rest of the\n        data from the DataSource that isn't already visible in the grid.\n        "
                            },
                            {
                                id:"expansionRelatedRecords",
                                jsURL:"grids/expansion/expansionRelatedRecords.js",
                                title:"Related Records",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:" \n        In this grid of Supply Categories, expand a row by clicking the special \n        <code>expansionField</code> to see a sub-grid containing the list of Supply Items \n        applicable to the selected \"Category\".\n        "
                            },
                            {
                                id:"customExpansionComponent",
                                jsURL:"grids/expansion/customExpansionComponent.js",
                                title:"Custom Component",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                descriptionHeight:"140",
                                description:"\n        This sample uses a Custom Expansion Component to achieve what the\n        Related Records sample does with built-in SmartClient Framework functionality.\n        <P>\n        In this grid of Supply Categories, expand a row by clicking the special \n        <code>expansionField</code> to see a sub-grid containing the list of Supply Items \n        applicable to the selected \"Category\".\n        "
                            },
                            {
                                id:"expansionLimitedWithDetails",
                                jsURL:"grids/expansion/expansionLimitedWithDetails.js",
                                title:"Limited Data",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItemWithOps",
                                        name:"supplyItem"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:" \n        This grid displays a limited number of fields from the supplyItem DataSource.  Only the\n        visible data values have been returned from the server, via the \n        <code>operationBinding.outputs</code> feature.  Expand a row by clicking the special \n        <code>expansionField</code>. The system will access the server to retrieve the entire record, create a \n        DetailViewer to display that data and expand the row to show the DetailViewer.  See \n        the code in the overridden <code>getExpansionComponent()</code> method.\n        <P>\n        Note also the use of the <code>maxExpandedRecords</code> attribute to limit the total number \n        of simultaneously expanded records.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Hiliting",
                        description:"\n\t\tThe \"hiliting\" system allows end users to visually define data highlighting rules, such \n\t\tas using colors to pick out high values, or using multiple colors to indicate\n\t\tranges of values.\n\t\t<P>\n\t\tBecause \"hilites\" can be easily stored and re-applied, it's easy to build an interface \n        that allows users to store their own private data highlighting rules, or even build a\n\t\thighlighted report to share with other users.\t\t\n\t",
                        children:[
                            {
                                id:"userDefinedHilites",
                                jsURL:"grids/hiliting/userDefinedHilites.js",
                                title:"User Defined",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"240",
                                description:"\n\t\t<code>DataBoundComponents</code> allow end-users to create \"hilites\" with rules based on the values \n        of data.  There are two sorts of hilites. Simple hilites, which allow a single \n        criterion based on a single field to affect a single target field (the same one) and \n        Advanced hilites, which allow very complex criteria, based on multiple fields, to \n        affect multiple target fields.\n\t\t<P>\n\t\tClick the \"Edit Hilites\" button below to show the <code>HiliteEditor</code> interface.  To set up a \n        simple hilite, click on the \"Area (km<sup>2</sup>)\" record in the list to the left.  \n        When the simple hilite rule appears on the right, select the \"greater than\" operation \n        from the drop-down box, type \"5000000\" into the value textBox, select a color from the \n        Color picker widget and click \"Save\".  Notice that all \"Area (km<sup>2</sup>)\" \n        values in the grid that exceed 5000000 are now highlighted in the chosen color.\n        <P>\n\t\tNow, add an Advanced criteria.  Again, click the \"Edit Hilites\" button and then click \n        the \"Add Advanced Rule\" button in the top left of the <code>HiliteEditor</code> - The \n\t\t<code>AdvancedHiliteEditor</code> window is displayed.  Add a new criterion that specifies \"GDP ($M) greater \n        than 1000000\".  Click the green plus icon beneath the criterion and add a second criterion, \n        this time specifying \"Area (km<sup>2</sup>) less than 500000\".  In the list below, \n        select both \"GDP ($M)\" and \"Area (km<sup>2</sup>)\" and select a background color.  \n        Clicking \"Save\" now will update the grid, showing both \"GDP\" and \"Area\" data in your \n        selected background color, where \"GDP\" is higher than 1 million and \"Area\" is less than\n\t\t500,000.\n\t\t<P>\n        It is very easy to provide users with the ability to save and restore their hilite \n        information by simply having the ability to save the data as a string. Click the \n        button below to see the grid's hilite state retrieved and serialized, by calling \n\t\t<code>getHiliteState()</code>, the grid destroyed and it's hilite-state restored to another \n        grid via <code>setHiliteState()</code>. \n    "
                            },
                            {
                                id:"preDefinedHilites",
                                jsURL:"grids/hiliting/preDefinedHilites.js",
                                title:"Pre-Defined",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"140",
                                description:"\n\t\tThis example demonstrates producing hilites in code.  The hilites applied to the grid \n        below match those suggested in the <i>User Defined</i> hilites example.  Additionaly, \n        the Advanced Hilite in this example also demonstrates using <code>Canvas.imgHTML()</code> \n        and the <code>htmlAfter</code> attribute of hilite-objects to append a warning icon to the \n        end of each field value, as part of the hilite.\n\t\t<P>\n\t\tHilite-objects also support an <code>htmlBefore</code> attribute - These before \n        and after properties can be used to extend color-based hilites to format values, for instance, as \n        <b>bold</b> or <i>italic</i> text using HTML tags.\n    "
                            },
                            {
                                id:"dataDrivenHilites",
                                jsURL:"grids/hiliting/dataDrivenHilites.js",
                                title:"Data-Driven",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"countryDataHilites",
                                        url:"grids/data/countryDataHilites.js"
                                    },
                                    {
                                        canEdit:"true",
                                        title:"countryHilitesDS",
                                        url:"grids/ds/countryHilitesDS.ds.js"
                                    }
                                ],
                                description:"\n\t\tThis example demonstrates hiliting in a data-driven fashion, where hilites contain no\n        criteria, and instead the data itself is flagged by setting the \n        <code>DataBoundComponent.hiliteProperty</code> attribute on each record.  This method is \n        useful when complex server-based calculation is used to decide which records to \n        hilite, and the client only needs to handle displaying them.\n    "
                            },
                            {
                                id:"formulaHilites",
                                jsURL:"grids/hiliting/formulaHilites.js",
                                title:"Formula Hilites",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n        Hiliting can be applied to any field in a <code>DataBoundComponent</code>, including custom formula\n        and summary fields.\n        <P>\n        Launch the Formula Builder and enter the title of the new field, and the desired \n        formula. In this case, enter \"Population Density\" for the field title and in the \n        formula field enter \"A / B\". As indicated in the dialog, \"A\" represents the \n        \"Population\" field, and \"B\" represents the \"Area\" field. Notice that sorting is now available on this\n        newly added \"Population Density\" field, just like any other field. Click the help \n        icon to view the various supported built-in functions.\n        <P>\n\t\tNow, click the \"Edit Hilites\" button to show the HiliteEditor interface.  To set up a \n        simple hilite on the custom \"Population Density\" field, select it in the list to \n        the left.  When the simple hilite rule appears on the right, select the \"greater than\" \n        operation from the drop-down box, type \"300\" into the value textBox, select a color \n        from the Color picker widget and click \"Save\".  You'll see that all the grid-values \n        in the \"Population Density\" field that exceed 300 are now hilighted in the chosen color.\n    "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Cell Widgets",
                        description:"\n    Examples of SmartClient's ability to embed arbitrary widgets in ListGrid cells.\n    ",
                        children:[
                            {
                                id:"gridCellWidgets",
                                jsURL:"grids/cellWidgets/gridCellWidgets.js",
                                title:"Grid Cell Widgets",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryDataDetail.js"
                                    }
                                ],
                                description:"\n        This example illustrates embedding arbitrary widgets in ListGrid cells. Notice how \n        reordering the column with widgets works as any other column. SmartClient uses widget \n        pooling to maximize efficiency. However, for better performance consider using one or \n        more fields of type \"icon\".\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Hover Components",
                        description:"\n    Any SmartClient Canvas can display a hover label when a user pauses momentarily\n    above it.  This is a built-in behavior for showing arbitrary HTML text, configured by\n    returning a value from <codei>Canvas.hoverHTML</code>.  In addition to this, the built-in Hover\n    Label can be replaced with any other Canvas-based component by overriding and returning\n    a component from <code>getHoverComponent()</code>.\n    <P>\n    This section covers some examples of this feature.\n    <P>\n    When <code>showHoverComponents</code> is true and the mouse hovers over a field, a built-in \n    component is created and used in place of the standard hover Label.\n    <br><br>\n    A variety of components are supported by default, according to the value of <code>grid.hoverMode</code>,\n    and overriding <code>grid.getHoverComponent()</code> allows for adding custom hover behaviors.\n",
                        children:[
                            {
                                id:"hoverRelatedRecords",
                                jsURL:"grids/hover/hoverRelatedRecords.js",
                                title:"Related Records",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"customerOrders",
                                        url:"grids/data/customerOrders.js"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"customerOrderMessages",
                                        url:"grids/data/customerOrderMessages.js"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"orderDS",
                                        url:"grids/ds/orderDS.ds.js"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"true",
                                        title:"orderMessagesDS",
                                        url:"grids/ds/orderMessagesDS.ds.js"
                                    }
                                ],
                                descriptionHeight:"100",
                                description:" \n        In this grid of Customer Orders, hover over a row to see a list of messages\n        attached to the order.  This gives a quick preview of discussions about the order,\n        without the need to leave the list of orders.\n        <P>\n        In a complete application, clicking the order would lead to a detail screen showing the\n        full order details and the ability to add to the discussion.\n        "
                            },
                            {
                                id:"hoverDetails",
                                jsURL:"grids/hover/hoverDetails.js",
                                title:"Details",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItemWithOps",
                                        name:"supplyItem"
                                    }
                                ],
                                description:" \n        This grid displays a limited number of fields from the supplyItem DataSource.  Only the\n        visible data values been returned from the server, via the \n        <code>operationBinding.outputs</code> feature.  When hovering over a row, the system returns \n        to the server to retrieve the entire record, creates a DetailViewer to display that data \n        and shows it as the hoverComponent.  See the code in the overridden <code>getCellHoverComponent()</code> method.\n        "
                            }
                        ]
                    },
                    {
                        id:"customColumns",
                        jsURL:"grids/customColumns.js",
                        title:"Custom Columns",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"countryDS",
                                url:"grids/ds/countrySQLDS.ds.xml"
                            }
                        ],
                        descriptionHeight:"220",
                        description:"\n        Formula and Summary fields provide built-in wizards for end users to define \n        formula fields that can compute values using other fields, or summary fields that can \n        combine other fields with intervening / surrounding text. Available in all \n        <code>DataBoundComponents</code> and easy to persist as preferences.\n        <P>\n        The Formula and Summary Builders are accessible from the grid header context menu. They \n        can also be invoked programmatically as demonstrated by clicking the buttons in this \n        sample.\n        <P>\n        Launch the Formula Builder and enter the title of the new field, and the desired \n        formula. For example, enter \"Population Density\" for the field title and in the \n        formula field enter \"A / B\". As indicated in the dialog, <b>A</b> represents the \n        \"Population\" field, and <b>B</b> represents the \"Area\" field. Notice sorting is now available on this \n        newly added \"Population Density\" field just like any other field. Click the help \n        icon to view the various supported built-in functions.\n        <P>\n        Next, launch the Summary Builder and enter the title of the new field, and the Summary \n        formulation. For example, enter \"Country (Flag)\" for the field name and enter \n        \"#B (#A)\" in the summary field.\n        <P>\n        Once some additional user-fields have been added, all that is needed to persist the column-layout\n        for later restoration to another grid, is the ability to save a string.  Click the \n        button below to store the grid's state by calling <code>getFieldState()</code>, destroy the \n        grid and restore it's state to another grid using <code>setFieldState()</code>. \n        \n"
                    },
                    {
                        isOpen:false,
                        title:"Tiling",
                        description:"\n    Using the TileGrid to display data in a tiled format.\n",
                        children:[
                            {
                                id:"tilingBasic",
                                jsURL:"grids/tiling/basic.js",
                                title:"Basic",
                                tabs:[
                                    {
                                        title:"animalData",
                                        url:"grids/data/animalData.js"
                                    }
                                ],
                                description:"\n       SmartClient can display data in a \"tiled\" view.  Mouse over widgets to see rollovers, click to\n       select (shift- and ctrl-click for multi-select).\n        "
                            },
                            {
                                cssURL:"grids/tiling/tileStyle.css",
                                id:"tilingFilter",
                                jsURL:"grids/tiling/filter.js",
                                title:"Filter & Sort",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"animalsDS",
                                        url:"grids/ds/animalsSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Tiled views can be filtered and sorted just like ListGrids.  Use the \"Search\" form to eliminate\n        some tiles and watch remaining tiles animate to new positions.  Use the \"Sort\" form to change\n        the sort direction.\n        "
                            },
                            {
                                cssURL:"grids/tiling/tileStyle.css",
                                id:"tilingEditing",
                                jsURL:"grids/tiling/editing.js",
                                title:"Editing",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"animalsDS",
                                        url:"grids/ds/animalsSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n       Tiled views can be connected to editors.  The TiledView automatically reacts to changes to the\n       underlying dataset.  Change the life span of Gazelle to 2 to see it\n       animate to the beginning of the list.\n        "
                            },
                            {
                                cssURL:"grids/tiling/tileStyle.css",
                                id:"tilingCustomTiles",
                                jsURL:"grids/tiling/customTiles.js",
                                title:"Customized Tiles",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"animalsDS",
                                        url:"grids/ds/animalsSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        The tiles in a TileGrid can be customized.  This example demonstrates adding a \"Remove\"\n        button to each tile which, when clicked, executes a DataSource operation to remove the\n        selected tile.\n        "
                            },
                            {
                                cssURL:"grids/tiling/tileStyle.css",
                                id:"fullyCustomTiles",
                                jsURL:"grids/tiling/fullyCustomTiles.js",
                                title:"Fully Custom Tiles",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"animalsDS",
                                        url:"grids/ds/animalsSQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"120",
                                description:"\n        TileGrids allow a completely custom component to be used as the tile.  \n        <p>\n        In this sample, each tile is a DynamicForm (with no editable fields).  The tabular\n        layout of the DynamicForm has been used to show each animals' status (Endangered) and\n        lifespan adjacent to its image, instead of beneath it.\n        "
                            }
                        ]
                    },
                    {
                        id:"export",
                        jsURL:"grids/export.js",
                        requiresModules:"SCServer",
                        title:"Export",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"worldDSExport",
                                url:"grids/ds/worldSQLDSExport.ds.xml"
                            }
                        ],
                        descriptionHeight:"130",
                        description:"\n            It's now easy to export data from a DataSource or from <code>DataboundComponents</code>, \n            such as ListGrid, TreeGrid and TileGrid.  In the example \n            below, choose an export format from the \"Export Type\" list, decide whether to \n            download the results or view them in a window using the checkbox and \n            click the \"Export\" button.  Because exporting to JSON is allowed only via \n            server-side custom code or via an <code>OperationBinding</code> (for security reasons), choosing\n            <b>JSON</b> from the select-item issues the export using the operationId set up in\n            the DataSource but still respects the \"Show in Window\" checkbox.  See the \n            \"JS\" and \"worldDSExport\" tabs below.<p>\n\t\t\t\n\t\t\tTry changing the filters and sort-order on the grid to see that the exported data \n            is filtered and sorted according to criteria applied to the grid.\n        "
                    },
                    {
                        id:"gridsDataTypes",
                        isOpen:false,
                        title:"Data types",
                        description:"\n    Built-in display and editing behaviors for common data types, and how to customize them.\n",
                        children:[
                            {
                                jsURL:"grids/dataTypes/text.js",
                                title:"Text",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        All fields in this grid are text fields.\n        "
                            },
                            {
                                id:"imageType",
                                jsURL:"grids/dataTypes/image.js",
                                title:"Image",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        \"Flag\" is an image field.\n        "
                            },
                            {
                                id:"longText",
                                jsURL:"grids/dataTypes/longtext.js",
                                title:"Long Text",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the data values to edit.\n        \"Government\" is a long text field with a popup editor.\n        "
                            },
                            {
                                jsURL:"grids/dataTypes/date.js",
                                title:"Date",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"Nationhood\" is a date field.\n        "
                            },
                            {
                                jsURL:"grids/dataTypes/integer.js",
                                title:"Integer",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"Population\" is an integer field.\n        "
                            },
                            {
                                jsURL:"grids/dataTypes/decimal.js",
                                title:"Decimal",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"GDP\" is a decimal (aka float) field.\n        "
                            },
                            {
                                jsURL:"grids/dataTypes/boolean.js",
                                title:"Boolean",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"G8\" is a boolean (true/false) field.\n        "
                            },
                            {
                                jsURL:"grids/dataTypes/linkText.js",
                                title:"Link (text)",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the values in the \"Info\" column to open external links.\n        "
                            },
                            {
                                id:"linkImage",
                                jsURL:"grids/dataTypes/linkImage.js",
                                title:"Link (image)",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the book images in the \"Info\" column to open external links.\n        "
                            },
                            {
                                id:"listType",
                                jsURL:"grids/dataTypes/list.js",
                                title:"List",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"Continent\" is a list (aka valueMapped) field.\n        "
                            },
                            {
                                id:"calculatedCellValue",
                                jsURL:"grids/dataTypes/calculated.js",
                                title:"Calculated",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click on the column headers to sort, or on the data values to edit.\n        \"GDP (per capita)\" is calculated from the \"GDP\" and \"Population\" fields.\n        "
                            }
                        ]
                    },
                    {
                        id:"gridsDataBinding",
                        isOpen:false,
                        title:"Data binding",
                        description:"\n    How to bind grids to DataSources to share field (column) definitions with other components,\n    and how to load data from local and remote data sources and services.    \n",
                        children:[
                            {
                                id:"listGridFields",
                                jsURL:"grids/dataBinding/fieldsGrid.js",
                                title:"ListGrid fields",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This ListGrid takes its field (column) settings from the <code>fields</code>\n        property of the component definition only. This technique is appropriate for\n        presentation-only grids that do not require data binding.\n        "
                            },
                            {
                                id:"dataSourceFields",
                                jsURL:"grids/dataBinding/fieldsDS.js",
                                title:"DataSource fields",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    },
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryMergeDS.ds.js"
                                    }
                                ],
                                description:"\n        This ListGrid takes its field (column) settings from the\n        \"countryDS\" DataSource specified in the <code>dataSource</code> property of the\n        component definition. This technique is appropriate for easy display of a shared\n        data model with the default UI appearance and behaviors.\n        "
                            },
                            {
                                id:"mergedFields",
                                jsURL:"grids/dataBinding/fieldsMerged.js",
                                title:"Merged fields",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    },
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryMergeDS.ds.js"
                                    }
                                ],
                                description:"\n        This ListGrid merges field settings from both the component <code>fields</code>\n        (for presentation attributes) and the \"countryDS\" DataSource (for\n        data model attributes). This is the usual approach to customize the look and feel of a\n        data-bound component.\n        "
                            },
                            {
                                id:"inlineData",
                                jsURL:"grids/dataProviders/inlineData.js",
                                title:"Inline data",
                                description:"\n        This ListGrid uses an inline data array in the component definition. This\n        technique is appropriate for very small read-only data sets, typically with static data\n        values.\n        "
                            },
                            {
                                id:"localData",
                                jsURL:"grids/dataProviders/localData.js",
                                title:"Local data",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        This ListGrid loads data from a local data array (included in a separate\n        JavaScript data file). This technique is appropriate for read-only data sets, typically\n        with less than 500 records.\n        "
                            },
                            {
                                id:"localDataSource",
                                jsURL:"grids/dataProviders/databound.js",
                                title:"Local DataSource",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    },
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryLocalDS.ds.js"
                                    }
                                ],
                                description:"\n        This ListGrid binds to a client-only DataSource that loads data\n        from a local data array. This technique is appropriate for client-only rapid prototyping\n        when the production application will support add or update (write operations), switchable\n        data providers (JSON, XML, WSDL, Java), arbitrarily large data sets (1000+ records), or\n        a data model that is shared by multiple components.\n        "
                            },
                            {
                                id:"jsonDataSource",
                                jsURL:"grids/dataProviders/databound.js",
                                title:"JSON DataSource",
                                tabs:[
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryJSONDS.ds.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"countryData.json",
                                        url:"grids/data/countryData.json"
                                    }
                                ],
                                description:"\n        This ListGrid binds to a DataSource that loads data from a\n        remote JSON data provider.  This approach of loading simple JSON data over HTTP can be\n        used with PHP and other server technologies.\n        "
                            },
                            {
                                id:"xmlDataSource",
                                jsURL:"grids/dataProviders/databound.js",
                                needXML:"true",
                                title:"XML DataSource",
                                tabs:[
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryXMLDS.ds.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"countryData.xml",
                                        url:"grids/data/countryData.xml"
                                    }
                                ],
                                description:"\n        This ListGrid binds to a DataSource that loads data from a\n        remote XML data provider.  This approach of loading simple XML data over HTTP can be\n        used with PHP and other server technologies.\n        "
                            },
                            {
                                id:"WSDLDataSource",
                                jsURL:"grids/dataProviders/WSDLBound.js",
                                needXML:"true",
                                title:"WSDL DataSource",
                                tabs:[
                                    {
                                        title:"countryDS",
                                        url:"grids/ds/countryWSDLDS.ds.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"soapRequest.xml",
                                        url:"grids/data/countrySoapRequest.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"soapResponse.xml",
                                        url:"grids/data/countrySoapResponse.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"WSDL",
                                        url:"grids/ds/SmartClientOperations.wsdl"
                                    }
                                ],
                                description:"\n        This ListGrid binds to a DataSource that loads data via a\n        WSDL service.  This example WSDL service supports all 4 basic operation types (fetch,\n        add, update, remove) and can be implemented with any server technology.  Sample\n        request/response SOAP messages for a \"fetch\" operation are shown.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Data operations",
                        description:"\n    Basic operations on datasets, both local and remote.\n",
                        children:[
                            {
                                jsURL:"grids/dataOperations/localSet.js",
                                title:"Local set",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to populate the grid with records from a local data set.\n        "
                            },
                            {
                                jsURL:"grids/dataOperations/localAdd.js",
                                title:"Local add",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click the buttons to add records to the top and bottom of the list.\n        "
                            },
                            {
                                jsURL:"grids/dataOperations/localRemove.js",
                                title:"Local remove",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click \"Remove first\" to remove the first record in the list. Click the other buttons to\n        remove records based on your selection (click, Ctrl-click, or\n        Shift-click in the list to select records).\n        "
                            },
                            {
                                jsURL:"grids/dataOperations/localUpdate.js",
                                title:"Local update",
                                visibility:"sdk",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Click to select any record in the list, then click one of the buttons to change\n        the \"Continent\" value for that record. Also see the \"Grids > Editing\" examples\n        for automatic update behavior.\n        "
                            },
                            {
                                id:"databoundFetch",
                                jsURL:"grids/dataOperations/databoundFetch.js",
                                title:"Databound fetch",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click the buttons to fetch (exact match) country records from the server.\n        Click the \"Fetch All\" button to fetch the first \"page\" of 50 records, then scroll\n        the grid to fetch new pages of data on demand.\n        "
                            },
                            {
                                id:"databoundFilter",
                                jsURL:"grids/dataOperations/databoundFilter.js",
                                title:"Databound filter",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click the buttons to filter (partial match) records from the server. Also see the\n        <b>Grids &gt; Sort &amp; filter &gt; Filter</b> example for automatic databound Filter\n        operations triggered by user input.\n        "
                            },
                            {
                                id:"databoundAdd",
                                jsURL:"grids/dataOperations/databoundAdd.js",
                                title:"Databound add",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click the \"Add new country\" button to create a new country record on the server.\n        Also see the <b>Grids &gt; Editing &gt; Enter New Rows</b> example for automatic databound\n        add operations triggered by user input.\n        "
                            },
                            {
                                id:"databoundRemove",
                                jsURL:"grids/dataOperations/databoundRemove.js",
                                title:"Databound remove",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click \"Remove first\" to remove (delete) the first record in the list from the server.\n        Click the other buttons to remove records based on your selection (click, Ctrl-click, or\n        Shift-click in the list to select records).\n        "
                            },
                            {
                                id:"databoundUpdate",
                                jsURL:"grids/dataOperations/databoundUpdate.js",
                                title:"Databound update",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click to select any record in the list, then click one of the buttons to change\n        the \"Continent\" value for that record on the server. Also see the <b>Grids &gt; Editing</b>\n        examples for automatic databound update operations triggered by user input.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Copy & Paste",
                        description:"\n    Copy and Paste Text between Excel and ListGrids.\n",
                        children:[
                            {
                                id:"gridToExcel",
                                jsURL:"grids/excel/gridToExcel.js",
                                title:"Grid to Excel",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Click and drag to select cells, then either right-click or press the \"Copy\"\n        button to bring up a dialog that can be used to copy and paste values to an Excel\n        spreadsheet\n        "
                            },
                            {
                                id:"gridToGrid",
                                jsURL:"grids/excel/gridToGrid.js",
                                title:"Grid to Grid",
                                tabs:[
                                ],
                                description:"\n        Each tab below holds one data grid component.  Drag to select cells within the\n        grid, then use the \"Copy\" button to copy that data to the clipboard.  Click the \"Paste\"\n        button below any grid to paste the copied data into that grid.\n        "
                            },
                            {
                                id:"excelToGrid",
                                jsURL:"grids/excel/excelToGrid.js",
                                title:"Excel to Grid",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"countryDS",
                                        url:"grids/ds/countrySQLDS.ds.xml"
                                    }
                                ],
                                description:"\n        Open an Excel spreadsheet, select some cells, and use \"Ctrl-C\" to copy them.\n        Click the \"Paste Cells\" button to open a dialog where you can paste the data.\n        "
                            }
                        ]
                    },
                    {
                        id:"serviceDataIntegration",
                        ref:"dataIntegration",
                        title:"Service Integration",
                        visibility:"none"
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_organisation.png",
                isOpen:false,
                showSkinSwitcher:"true",
                title:"Trees",
                description:"\n    High-performance interactive tree views\n    <BR>\n    <BR>\n    Trees are based on grid views, and so share all of the appearance, interactivity and\n    databinding features of grids, in addition to tree-specific features.\n",
                children:[
                    {
                        isOpen:false,
                        title:"Appearance",
                        description:"\n        Trees can have dynamic titles, display multiple columns and show connector\n        lines.\n    ",
                        children:[
                            {
                                dataSource:"employees",
                                id:"nodeTitles",
                                jsURL:"trees/appearance/nodeTitles.js",
                                title:"Node Titles",
                                description:"\n            Formatter interfaces allow you to add custom tree titles.\n            "
                            },
                            {
                                dataSource:"employees",
                                jsURL:"trees/appearance/multipleColumns.js",
                                title:"Multiple Columns",
                                description:"\n            Trees can show multiple columns of data for each node.  Each column has the\n            styling, formatting, and data type awareness features of columns in a normal\n            grid.\n\n            Try drag reordering columns, or sorting by the \"Salary\" field.\n            "
                            },
                            {
                                cssURL:"trees/appearance/connectors.css",
                                dataSource:"employees",
                                id:"connectors",
                                jsURL:"trees/appearance/connectors.js",
                                title:"Connectors",
                                description:"\n            Trees can show skinnable connector lines. Toggle the checkbox to show or hide \"full\"\n            connector lines.\n        ",
                                badSkins:[
                                    "BlackOps",
                                    "SilverWave"
                                ],
                                bestSkin:"TreeFrog"
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Dragging",
                        description:"\n        Trees have built-in drag and drop behaviors and tree-specific events.\n    ",
                        children:[
                            {
                                id:"treeDragReparent",
                                jsURL:"trees/interaction/dragReparent.js",
                                title:"Drag reparent",
                                tabs:[
                                    {
                                        name:"employeeData",
                                        url:"trees/employeeData.js"
                                    }
                                ],
                                description:"\n            Try dragging employees under new managers.  Note that a position indicator line\n            appears during drag, allowing employees to be placed in a particular order.\n            "
                            },
                            {
                                id:"treesDragTree",
                                ref:"dragTree",
                                title:"Drag nodes"
                            },
                            {
                                id:"treesTreeDragReparent",
                                ref:"treeDragReparent",
                                title:"Springloaded Folders",
                                description:"\n            Try dragging employees under new managers.  Note that closed folders automatically\n            open if you hover over them momentarily.\n            "
                            },
                            {
                                id:"treeDropEvents",
                                jsURL:"trees/interaction/dropEvents.js",
                                title:"Drop Events",
                                tabs:[
                                    {
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    },
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    }
                                ],
                                description:"\n            Click on any category on the left to show items from that category on the right.  \n            Drag and drop items from the list on the right into new categories in the tree on\n            the left.\n            "
                            }
                        ]
                    },
                    {
                        id:"cascadingSelection",
                        jsURL:"trees/cascadingSelection.js",
                        title:"Cascading Selection",
                        description:"\n        Tree selection can be automatically propagated up and down the tree. Select a\n        parent or child node to see how other nodes are affected.\n        "
                    },
                    {
                        id:"treesDataBinding",
                        isOpen:false,
                        title:"Data binding",
                        description:"\n        Trees can bind to DataSources and handle all the data formats that grids can, using\n        additional properties to control tree structure, open state, and folders.\n    ",
                        children:[
                            {
                                id:"parentLinking",
                                jsURL:"trees/dataBinding/parentLinking.js",
                                title:"Parent Linking",
                                description:"\n            Tree data can be specified as a flat list of nodes that refer to each other by\n            ID.  This format is also used for load on demand.\n            "
                            },
                            {
                                id:"childrenArrays",
                                jsURL:"trees/dataBinding/childrenArrays.js",
                                title:"Children Arrays",
                                description:"\n            Tree data can be specified as a tree of nodes where each node lists its children.\n            "
                            },
                            {
                                id:"loadXMLParent",
                                jsURL:"trees/dataBinding/loadXMLParentLinked.js",
                                title:"Load XML (Parent Linked)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"employeesXMLData",
                                        url:"trees/dataBinding/employeesDataParentLinked.xml"
                                    }
                                ],
                                description:"\n            Tree data can be loaded in XML or JSON format.  For a \"parent-linked\" Tree, the\n            <code>primaryKey</code> and <code>foreignKey</code> declarations in the DataSource\n            control how nodes are linked together to form the tree structure.\n            \n            "
                            },
                            {
                                id:"treeLoadXML",
                                jsURL:"trees/dataBinding/loadXMLChildrenArrays.js",
                                needXML:"true",
                                title:"Load XML (Child Arrays)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"employeesXMLData",
                                        url:"trees/dataBinding/employeesDataChildrenArrays.xml"
                                    }
                                ],
                                description:"\n            Tree data can be loaded in XML or JSON format.  For a \"children arrays\" Tree, one\n            DataSource field is marked <code>childrenProperty:true</code>, and the children of\n            a node are expected to appear under the same-named XML element or JSON property.\n            \n            "
                            },
                            {
                                dataSource:"employees",
                                id:"treeLoadOnDemand",
                                jsURL:"trees/dataBinding/loadOnDemand.js",
                                title:"Load on Demand",
                                description:"\n            Begin opening folders and note the prompt which briefly appears during server\n            fetches.  Trees can load data one folder at a time.  When a folder is opened for the first\n            time, the tree asks the server for the children of the node just opened by passing\n            the unique id of the parent as search criteria.\n            "
                            },
                            {
                                dataSource:"employees",
                                id:"initialData",
                                jsURL:"trees/dataBinding/initialDataLOD.js",
                                title:"Initial Data & Load on Demand",
                                description:"\n            Begin opening folders and note the load on demand behavior.\n            \n            Trees that use load on demand can optional specify an initial dataset set up front.  \n            "
                            },
                            {
                                dataSource:"employeesOpenNodes",
                                id:"multiLevelLOD",
                                jsURL:"trees/dataBinding/multiLevelLOD.js",
                                requiresModules:"SCServer",
                                title:"Multi-Level LOD",
                                description:"\n                Server logic can return multiple levels of the tree in response to a single\n                request when using load on demand.\n                <p>\n                In the tree below, the nodes \"Charles Madigen\" and \"Tammy Plant\" have been\n                returned by the server already open.  The server included the children of\n                these nodes to avoid the need for the tree to immediately contact the server\n                again to load children.\n            "
                            },
                            {
                                dataSource:"hugeTree",
                                id:"pagingForChildren",
                                jsURL:"trees/dataBinding/pagingForChildren.js",
                                requiresModules:"SCServer",
                                title:"Paging for Children",
                                description:"\n                SmartClient supports loading children as they are scrolled into view, which is\n                needed for very large trees where the number of children under a single node\n                can be very large.\n                <p>\n                In the tree below, there are thousands of root-level nodes in the dataset\n                stored on the server.  Scroll down to cause more nodes to be loaded from the\n                server.\n                <p>\n                Open the folder \"Root #4\", to reveal another large set of children which can be\n                incrementally loaded.  Within these children, open \"First #5\" to reveal another\n                large set of children.\n            "
                            },
                            {
                                dataSource:"hugeTreeOpenNodes",
                                id:"multiLevelChildPaging",
                                jsURL:"trees/dataBinding/multiLevelChildPaging.js",
                                requiresModules:"SCServer",
                                title:"Multi-Level Child Paging",
                                description:"\n                Server logic can return multiple levels of the tree in response to a single\n                request when using child paging.\n                <p>\n                In the tree below, the folders \"Root #4\" and \"First #5\" have been returned by\n                the server already open.  The server included the children of these nodes to\n                avoid the need for the tree to immediately contact the server again to load\n                children.\n                <p>\n                However, both of these nodes have a very large number of children, so they only\n                returned a portion of their children, using the <code>childCountProperty</code>\n                to tell the tree the total number of children.\n            "
                            }
                        ]
                    },
                    {
                        dataSource:"employees",
                        jsURL:"trees/filtering.js",
                        title:"Filtering",
                        description:"\n        Trees can be filtered without server-side support. Parent nodes can be dropped if they\n        don't match the criteria or they can be retained if matching children are present.\n\n        Click on the \"filter\" buttons to change the filter criteria.\n\n        <br>\n\n        Note that if \"keep parents\" is not checked, keepParentsOnFilter will be false for\n        the TreeGrid in the sample, excluding all nodes not matching the criteria, and all\n        nodes below the excluded nodes in the TreeGrid.  (So if the filter excludes the root\n        node, no nodes will be visible.)\n    \n    "
                    },
                    {
                        dataSource:"employees",
                        jsURL:"trees/sorting.js",
                        title:"Sorting",
                        description:"\n        Trees sort per folder.  Click on the \"Name\" column header to sort alphabetically by\n        folder name, or on the \"Salary\" column header to sort by Salary.\n    "
                    },
                    {
                        dataSource:"employees",
                        id:"treesEditing",
                        jsURL:"trees/editing.js",
                        title:"Editing",
                        description:"\n        Click on employees in the tree to edit them, and drag and drop employees to rearrange them.\n        Choose an employee via the dropdown picklist menu to see that employee's direct reports in the ListGrid.  Changes\n        made in the tree or ListGrid are automatically saved to the server and reflected in the other\n        components.\n    "
                    },
                    {
                        dataSource:"employees",
                        id:"freezeTree",
                        jsURL:"trees/freezeTree.js",
                        title:"Frozen Columns",
                        description:"\n     Setting <code>frozen:true</code> enables frozen columns for Trees.  Columns\n     can be frozen and unfrozen by right-clicking on column headers.<br>\n     Column resize, column reorder, drag and drop and load on demand all function normally.\n     "
                    },
                    {
                        dataSource:"employees",
                        id:"millerColumns",
                        jsURL:"trees/millerColumns.js",
                        title:"Miller Columns",
                        description:"\n        The <code>ColumnTree</code> provides an alternate navigation paradigm for Tree data,\n        sometimes called \"Miller Columns\" and seen in Apple&trade; iTunes&trade;.\n        The <code>ColumnTree</code> provides identical data binding and load on demand facilities to\n        normal TreeGrids.\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/cube_blue.png",
                id:"cubeGrid",
                isOpen:false,
                title:"Cubes",
                description:"\n    Multidimensional \"cube\" data sets as used in BI, Analytics and OLAP applications.\n    Load-on-demand, drill-down, roll-up, in-browser dataset pivoting, multiple frozen panes, \n    resizing and reorder of fields, tree dimensions, chart generation, editing and other\n    features.\n",
                children:[
                    {
                        id:"basicCube",
                        jsURL:"cubes/basicCube.js",
                        requiresModules:"Analytics",
                        title:"Basic Cube",
                        tabs:[
                            {
                                title:"productData",
                                url:"cubes/productData.js"
                            }
                        ],
                        description:"\n        In this multi-dimensional dataset, each cell value has a series of attributes,\n        called \"facets\", that appear as stacked headers labelling the cell value. \n        "
                    },
                    {
                        id:"cubeAnalytics",
                        ref:"analytics",
                        requiresModules:"Analytics",
                        title:"Analytics"
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/cube_blue.png",
                id:"comboBoxAndFamily",
                isOpen:false,
                title:"ComboBox & Family",
                description:"\n",
                children:[
                    {
                        id:"comboListComboBox",
                        ref:"variousControls",
                        title:"Data Binding"
                    },
                    {
                        id:"comboRelatedRecords",
                        ref:"relatedRecords",
                        title:"Related Records"
                    },
                    {
                        dataSource:"employees",
                        id:"formatRelatedValue",
                        jsURL:"combobox/formatRelatedValue.js",
                        title:"Format Related Value",
                        description:"\n        When using an <code>optionDataSource</code> to allow a user to select a record from\n        a related DataSource, it is possible to show a formatted value based on multiple \n        fields in the related record.  When you install a formatValue() method on a \n        <code>ComboBoxItem</code>, you can also set <code>formatOnBlur</code> which, as shown\n        in the second item in the example, causes the formatting to be applied only when the \n        item does not have focus - formatting is removed when the item receives focus.\n        "
                    },
                    {
                        id:"comboFormDependentSelects",
                        ref:"formDependentSelectsLocal",
                        title:"Dependent Selects"
                    },
                    {
                        id:"dropdownGrid",
                        jsURL:"combobox/dropDownGrid.js",
                        title:"Dropdown Grid",
                        tabs:[
                            {
                                title:"supplyItem",
                                url:"supplyItem.ds.xml"
                            }
                        ],
                        description:"\n        The SelectItem displays multiple fields in a ListGrid.\n        Scroll to dynamically load more records.\n        This pattern works with any DataSource.\n        "
                    },
                    {
                        dataSource:"employees",
                        id:"formatDropdown",
                        jsURL:"combobox/formatDropDown.js",
                        title:"Format Dropdown",
                        description:"\n        The dropdown list supports formatting APIs that can use multiple fields\n        from related records.\n        "
                    },
                    {
                        id:"comboFilterRelated",
                        ref:"filterRelated",
                        title:"Multi-Field Search"
                    },
                    {
                        id:"multiSelect",
                        jsURL:"combobox/multiSelect.js",
                        title:"Multi-Select",
                        description:"Demonstration of SelectItems with multiple selections."
                    },
                    {
                        id:"comboComboBoxStyled",
                        ref:"comboBoxStyled",
                        title:"Styled ComboBox"
                    },
                    {
                        dataSource:"supplyItem",
                        id:"multiComboBoxItem",
                        jsURL:"combobox/multiComboBoxItem.js",
                        title:"Multi ComboBox",
                        description:"\n        A <code>MultiComboBoxItem</code> displays a selection of multiple values as\n        buttons along with a combo box that is used to select additional values.\n        Clicking on a button removes the value from the selection.\n        <p>\n        Pressing the Enter/Return key within the combo box selects the first matching value\n        without leaving the field, allowing several values to be selected using only the\n        keyboard. Pressing the Tab key within the combo box selects the first matching value\n        and then leaves the field normally, taking focus to the next focusable item.\n        "
                    },
                    {
                        id:"specialValues",
                        jsURL:"combobox/specialValues.js",
                        title:"Special Values",
                        tabs:[
                            {
                                title:"supplyItem",
                                url:"supplyItem.ds.xml"
                            }
                        ],
                        description:"\n        Sometimes a list of options needs to contain special values like \"All\", \"None\" or \"Not\n        Applicable\" that aren't present in the data stored in a DataSource.  \n        <p>\n        Open the ComboBoxItem and SelectItem below to see special values shown in a separate\n        area above the options list.  The normal <code>supplyItem</code> sample DataSource is\n        being used here as the source or options, and the special values did not need to be\n        added to the DataSource data.  Also, loading data on demand works as normal - just\n        scroll to load more rows.\n        <p>\n        Type a search string into the ComboBox and note that the special values remain visible\n        at the top of the list.\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/vcard_edit.png",
                isOpen:false,
                title:"Forms",
                description:"\n    Form managers and input controls.\n",
                children:[
                    {
                        id:"layout",
                        isOpen:false,
                        title:"Layout",
                        description:"\n        A specialized form layout manager allows forms to grow into available space,\n        hide sections, and span across tabs.\n    ",
                        children:[
                            {
                                id:"formLayoutTitles",
                                jsURL:"forms/layout/titles.js",
                                title:"Titles",
                                description:"\n            Click on \"Swap Titles\" to change title orientation.\n            \n            Form layout automatically places titles next to fields.  Left-oriented titles take\n            up a column so that labels line up.  Top oriented titles don't.\n            "
                            },
                            {
                                id:"columnSpanning",
                                jsURL:"forms/layout/spanning.js",
                                title:"Spanning",
                                description:"\n            Drag resize the form from the right edge to see the effect of spanning.\n            \n            Specifying column widths and column spanning items allows for larger and smaller\n            input areas.\n            "
                            },
                            {
                                id:"formLayoutFilling",
                                jsURL:"forms/layout/filling.js",
                                title:"Filling",
                                description:"\n            Click on the \"Short Message\" and \"Long Message\" buttons to change the amount of\n            space available to the form.\n            \n            SmartClient form layouts allow for filling available space, even when\n            available space cannot be known in advance because it is data-dependant.\n            "
                            },
                            {
                                id:"formSplitting",
                                jsURL:"forms/layout/valuesManager.js",
                                showSkinSwitcher:true,
                                title:"Splitting",
                                xmlURL:"ValuesManager.xml",
                                description:"\n            Click \"Submit\" to jump to a validation error in the \"Stock\" tab.\n            \n            Forms which are split for layout purposes can behave like a single logical form for\n            validation and saves.\n            <BR><BR>JS and XML tabs show two alternative versions of source, only one is\n                required.\n            "
                            },
                            {
                                id:"formSections",
                                jsURL:"forms/layout/sectionItem.js",
                                showSkinSwitcher:true,
                                title:"Sections",
                                xmlURL:"SectionItem.xml",
                                description:"\n            Click on \"Stock\" to reveal fields relating to stock on hand.\n            <BR><BR>JS and XML tabs show two alternative versions of source, only one is\n                required.\n            "
                            },
                            {
                                id:"formsValidationFieldBindingV",
                                ref:"validationFieldBinding",
                                title:"Data Binding"
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Field Dependencies",
                        description:"\n        Common field dependencies within a form, such as fields that are only applicable to\n        some users, can be handled by specifying simple expressions.\n    ",
                        children:[
                            {
                                id:"formShowAndHide",
                                jsURL:"forms/fieldDependencies/showAndHide.js",
                                title:"Show & Hide",
                                description:"\n            Select \"On order\" to reveal the \"Shipping Date\" field.\n            "
                            },
                            {
                                id:"fieldEnableDisable",
                                jsURL:"forms/fieldDependencies/enableAndDisable.js",
                                title:"Enable & Disable",
                                description:"\n            Check \"I accept the agreement\" to enable the \"Proceed\" button.\n            "
                            },
                            {
                                id:"conditionallyRequired",
                                jsURL:"forms/fieldDependencies/conditionallyRequired.js",
                                title:"Conditionally Required",
                                description:"\n            Select \"No\" and click the \"Validate\" button - the reason field becomes required.\n            "
                            },
                            {
                                id:"matchValue",
                                jsURL:"forms/fieldDependencies/matchValue.js",
                                title:"Match Value",
                                description:"\n            Try entering mismatched values for \"Password\" and \"Password Again\", then click\n            \"Create Account\" to see a validation error.\n            "
                            },
                            {
                                id:"formDependentSelectsLocal",
                                jsURL:"forms/fieldDependencies/dependentSelectsLocal.js",
                                title:"Dependent Selects (Local)",
                                descriptionHeight:"110",
                                description:"\n            Select a \"Division\" to cause the \"Department\" select to be \n            populated with departments from that division.\n            "
                            },
                            {
                                id:"formDependentSelectsDatabound",
                                jsURL:"forms/fieldDependencies/dependentSelectsDatabound.js",
                                title:"Dependent Selects (Databound)",
                                tabs:[
                                    {
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    },
                                    {
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n            This example demonstrates two select items, both of which load data \"on-the-fly\" from\n            a DataSource, where the \"Category\" drop-down controls the list of available items\n            in the \"Item\" drop-down.  Try selecting a value in the \"Category\" drop-down list to change the set of options \n            available in the \"Item\" drop-down.\n            "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Form Controls",
                        description:"\n        The form has built-in editors and pickers for common types such as numbers and dates,\n        as well as the ability to use the databinding framework to pick from lists and trees of\n        related records.\n    ",
                        children:[
                            {
                                aliases:"textItem,textAreaItem,spinnerItem,sliderItem,checkboxItem,listComboBox,colorItem",
                                dataSource:"supplyItem",
                                id:"variousControls",
                                jsURL:"forms/dataTypes/variousControls.js",
                                title:"Various Controls",
                                descriptionHeight:"130",
                                description:"\n              Demonstration of several form controls. <P>\n              For the ComboBoxes at the bottom, start typing in either field to see a list of matching options.\n              The field labelled \"Item Name\" retrieves options dynamically from the SupplyItem DataSource.\n            "
                            },
                            {
                                descriptionHeight:"350",
                                id:"maskedTextItem",
                                jsURL:"forms/dataTypes/maskedTextItem.js",
                                title:"Text - Masked",
                                description:"\n            <p>TextItems support a masked entry to restrict and format what can be entered in any text field.</p>\n            <p>Overview of available mask characters</p>\n            <p><table class=\"normal\">\n            <tr>\n                <th>Character</th>\n                <th>Description</th>\n            </tr>\n            <tr>\n                <td>0</td>\n                <td>Digit (0 through 9) or plus [+] or minus [-] signs</td>\n            </tr>\n            <tr>\n                <td>9</td>\n                <td>Digit or space</td>\n            </tr>\n            <tr>\n                <td>#</td>\n                <td>Digit</td>\n            </tr>\n            <tr>\n                <td>L</td>\n                <td>Letter (A through Z)</td>\n            </tr>\n            <tr>\n                <td>?</td>\n                <td>Letter (A through Z) or space</td>\n            </tr>\n            <tr>\n                <td>A</td>\n                <td>Letter or digit</td>\n            </tr>\n            <tr>\n                <td>a</td>\n                <td>Letter or digit</td>\n            </tr>\n            <tr>\n                <td>C</td>\n                <td>Any character or space</td>\n            </tr>\n            <tr>\n                <td>&nbsp;</td>\n            </tr>\n            <tr>\n                <td>&lt;</td>\n                <td>Causes all characters that follow to be converted to lowercase</td>\n            </tr>\n            <tr>\n                <td>&gt;</td>\n                <td>Causes all characters that follow to be converted to uppercase</td>\n            </tr>\n            </table></p>\n            <p>Any character not matching one of the above mask characters or that is\n            escaped with a backslash (\\) is considered to be a literal.</p>\n            <p>Custom mask characters can be defined by standard regular expression\n            character set or range. For example, a hexadecimal color code mask could be:\n            <UL>\n                <LI>Color: \\#>[0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F]</LI>\n            </UL></p>\n            "
                            },
                            {
                                id:"dateItem",
                                jsURL:"forms/dataTypes/dateItem.js",
                                title:"Date",
                                xmlURL:"DateItem.xml",
                                description:"\n            DateItems support direct or pickList-based input of dates, and have a built-in\n            pop-up day picker.\n            <BR><BR>JS and XML tabs show two alternative versions of source, only one is\n             required.\n            "
                            },
                            {
                                id:"timeItem",
                                jsURL:"forms/dataTypes/timeItem.js",
                                title:"Time",
                                xmlURL:"TimeItem.xml",
                                descriptionHeight:"180",
                                description:"\n            TimeItem allows the user to edit a logical time value (stored internally using a JavaScript date\n            object). This item understands most standard formats (try entering \"14:00\" or \"2pm\", for example).\n            <P>\n            TimeItems have a second mode where the user picks hour, minute and second from drop-down controls.\n            The control can be configured to limit choices of minutes or hours, which can be useful for\n            scheduling applications.  Note in the control below, choices have been limited to every 15 minutes.\n            <BR><BR>JS and XML tabs show two alternative versions of source, only one is\n             required.\n            "
                            },
                            {
                                id:"selectItem",
                                jsURL:"forms/dataTypes/listSelect.js",
                                title:"List - Select",
                                description:"\n            Note the icons and customized text styling.  Click to reveal the options and note\n            the drop shadow.  \n            \n            The SmartClient SelectItem offers more powerful and consistent control over\n            appearance and behavior than the HTML &lt;SELECT&gt; element.\n            "
                            },
                            {
                                id:"formControlMultiSelect",
                                ref:"multiSelect",
                                title:"List - Select Multiple"
                            },
                            {
                                dataSource:"supplyItem",
                                id:"comboBoxStyled",
                                jsURL:"forms/dataTypes/comboBoxStyled.js",
                                title:"Combo Box - Styled",
                                description:"\n           Combo box rows can be styled via HTML to display data in almost any \n           way imaginable. Row hovers are also customized in this example.\n        "
                            },
                            {
                                dataSource:"supplyItem",
                                id:"relatedRecords",
                                jsURL:"forms/dataTypes/relatedRecords.js",
                                showSkinSwitcher:true,
                                title:"List - Related Records",
                                description:"\n            Open the picker in either form to select an item to order from the\n            \"supplyItem\" DataSource.  The picker on the left stores the \"itemId\" from the\n            related \"supplyItem\" records.  The picker on the right stores the \"SKU\" while\n            displaying multiple fields.  Scroll to dynamically load more records.  \n            This pattern works with any DataSource.  \n            "
                            },
                            {
                                dataSource:"supplyItem",
                                descriptionHeight:"100",
                                id:"filterRelated",
                                jsURL:"forms/dataTypes/filterPickList.js",
                                showSkinSwitcher:true,
                                title:"List - Multi-Field Search",
                                description:"\n            Click on the SelectItem on the left to see the full set of data. Enter filter\n            criteria directly on the drop-down list in either field to filter the set of\n            options down to a managable size.<P>\n            Now move focus to the ComboBoxItem and start typing. The set of options displayed are\n            automatically filtered against both fields as typing occurs. Tab or Enter will complete selection.\n            "
                            },
                            {
                                dataSource:"supplyCategory",
                                id:"pickTree",
                                jsURL:"forms/dataTypes/pickTree.js",
                                showSkinSwitcher:true,
                                title:"Tree",
                                xmlURL:"PickTree.xml",
                                description:"\n            Click on \"Department\" or \"Category\" below to show hierarchical menus.  The\n            \"Category\" menu loads options dynamically from the \"SupplyCategory\" DataSource.\n            <BR><BR>JS and XML tabs show two alternative versions of source, only one is\n             required.\n            "
                            },
                            {
                                title:"List - Select Other",
                                visibility:"sdk",
                                xmlURL:"SelectOtherItem.xml",
                                description:"\n            Select \"Other..\" from the drop down to enter a custom value.\n            <BR><BR>This example source is written in XML. \n            SmartClient supports code written directly in JavaScript, or in this declaritive XML\n            format.\n            "
                            },
                            {
                                id:"fControlsRichTextEditor",
                                ref:"RichTextEditor",
                                title:"HTML"
                            },
                            {
                                dataSource:"countryDS",
                                id:"canvasItem",
                                jsURL:"forms/dataTypes/canvasItem.js",
                                title:"CanvasItem",
                                descriptionHeight:"140",
                                description:"\n\t\t\t\n        A special type of form control called a <code>CanvasItem</code> allows any kind of SmartClient widget to\n                participate in form layout and values management.\n\t\t<p>          \n                Drag resize the form (blue bordered area) - notice how the embedded ListGrid fills the available space.\n        <p>\n                The embedded ListGrid starts out showing the initial value provided to the form (\"Germany\").\n                Click the button titled \"Set Value: France\" to provide a new value to the form, causing the\n                CanvasItem to display this value.\n        <p>\n                Click on any country in the list - the form picks up the value and fires standard change\n                events, causing new values to be displayed in a Label.\n        <p>\n                This CanvasItem provides functionality similar to an HTML &lt;select multiple&gt;, however, because it's\n                based on a ListGrid, any ListGrid behavior could be added (data paging, drag and drop, hovers,\n                inline search, inline editing, grouping, etc).\n                  \n\t        "
                            },
                            {
                                id:"nestedEditor",
                                jsURL:"forms/dataTypes/nestedEditing.js",
                                title:"CanvasItem - Nested Editor",
                                descriptionHeight:"140",
                                description:"\t\n\t\t\t\n        This example shows a reusable <code>CanvasItem</code> that edits nested data structures.\n        <p>\n                Here, a Hibernate entity representing an Order contains OrderItems - in the\n                Record for an Order, value of the field \"items\" in an Array of Records representing OrderItems.\n        <p>\n                The <code>CanvasItem</code> embeds an editable ListGrid to provide an editing interaction for the\n                OrderItems right in the midst of the form.  It can be used with any DataSource that\n                has nested records.\n             \n\t        ",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderHB",
                                        title:"masterDetail_orderHB"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderItemHB",
                                        name:"masterDetail_orderItem"
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        id:"formsvalidation",
                        isOpen:false,
                        title:"Validation",
                        description:"\n        Typical validation needs are covered by validators built-in to the SmartClient\n        framework.  Validators can be combined into custom type definitions which are reusable\n        across all components.\n    ",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                id:"formsvalidationCopy",
                                isOpen:false,
                                title:"Server-based",
                                description:"\n            The SmartClient Server provides powerful support for server-based validation.\n        ",
                                children:[
                                    {
                                        id:"formsSingleSourceValidation",
                                        ref:"DBsingleSourceValidation",
                                        title:"Single Source"
                                    },
                                    {
                                        id:"formsUniqueCheckValidation",
                                        ref:"DBuniqueCheckValidation",
                                        title:"Unique Check"
                                    },
                                    {
                                        id:"formsVelocityValidation",
                                        ref:"DBvelocityValidation",
                                        title:"Velocity Expression"
                                    },
                                    {
                                        id:"formsInlineScriptValidation",
                                        ref:"DBinlineScriptValidation",
                                        title:"Inline Script"
                                    },
                                    {
                                        id:"formsDmiValidation",
                                        ref:"DBdmiValidation",
                                        title:"DMI Validation"
                                    },
                                    {
                                        id:"formsHasRelatedValidation",
                                        ref:"DBhasRelatedValidation",
                                        title:"Related Records"
                                    },
                                    {
                                        id:"formsBlockingErrors",
                                        ref:"DBblockingErrors",
                                        title:"Blocking Errors"
                                    }
                                ]
                            },
                            {
                                id:"formsValidationType",
                                ref:"DBvalidationType",
                                title:"Type"
                            },
                            {
                                id:"formsValidationBuiltins",
                                ref:"validationBuiltins",
                                title:"Built-ins"
                            },
                            {
                                id:"formsRegularExpression",
                                ref:"DBregularExpression",
                                title:"Regular Expression"
                            },
                            {
                                id:"formsValueTransform",
                                ref:"DBvalueTransform",
                                title:"Value Transform"
                            },
                            {
                                id:"formsCustomSimpleType",
                                ref:"DBcustomSimpleType",
                                title:"Custom Types"
                            },
                            {
                                id:"formsValidationFieldBinding",
                                ref:"validationFieldBinding",
                                title:"Customized Binding"
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Details",
                        description:"\n        Hovers and hints explain the form to the user.  Icons provide an easy extension point\n        for help, custom pickers and other extensions.  KeyPress filtering allows character\n        casing to be forced on entry or invalid keystrokes to be ignored.\n    ",
                        children:[
                            {
                                id:"formIcons",
                                jsURL:"forms/layout/icons.js",
                                title:"Icons",
                                description:"\n            Click on the help icon below to see a description for severity levels.  Form items\n            can show an arbitrary number of icons to do whatever you need.\n            "
                            },
                            {
                                id:"itemHoverHTML",
                                jsURL:"forms/details/hovers.js",
                                title:"Hovers",
                                description:"\n            Hover anywhere over the field to see what the current value means.  Change the\n            value or disable the field to see different hovers.  Note that the hovers contain\n            HTML formatting.  \n            "
                            },
                            {
                                id:"formHints",
                                jsURL:"forms/layout/hints.js",
                                title:"Hints",
                                description:"\n            Hints provide guidance to the user filling out the form.  In this case, the \"MM/YYYY\"\n            hint tells the user the expected format for the free-form date field. Note both\n            trailing and in-field styles are shown.\n            "
                            },
                            {
                                id:"formFilters",
                                jsURL:"forms/details/filters.js",
                                title:"KeyPress Filters",
                                description:"\n            KeyPress filters help prevent the user from entering invalid characters.\n            Additionally, character casing can be forced to either upper or lowercase.\n            "
                            }
                        ]
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/cal.png",
                isOpen:false,
                showSkinSwitcher:"true",
                title:"Calendars",
                description:"Customizable calendars that display events in day, week, and month views.",
                children:[
                    {
                        cssURL:"calendar/calendar.css",
                        id:"simpleCalendar",
                        jsURL:"calendar/simpleCalendar.js",
                        requiresModules:"Calendar",
                        title:"Simple Calendar",
                        tabs:[
                            {
                                title:"eventData",
                                url:"calendar/calendarData.js"
                            }
                        ],
                        description:"\n            This calendar is bound to an array of event data. Drag events to alter their start times, and\n            resize events to alter their durations. Click and drag in an empty cell to create new events,\n            or click on an existing event to edit it. Notice how the red event can't be edited. This has \n            been specified within the event data itself (see the \"eventData\" tab below).\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"databoundCalendar",
                        jsURL:"calendar/databoundCalendar.js",
                        requiresModules:"Calendar",
                        title:"Databound Calendar",
                        tabs:[
                            {
                                title:"eventData",
                                url:"calendar/calendarData.js"
                            }
                        ],
                        description:"\n            This calendar is bound to a dataSource. Drag events to alter their start times, and\n            resize events to alter their durations. Click and drag in an empty cell to create new events,\n            or click on an existing event to edit it. Notice how the red event can't be edited. This has \n            been specified within the event data itself (see the \"eventData\" tab below).\n        "
                    },
                    {
                        id:"compactCalendar",
                        jsURL:"calendar/compactCalendar.js",
                        requiresModules:"Calendar",
                        title:"Compact Calendar",
                        tabs:[
                            {
                                title:"eventData",
                                url:"calendar/calendarData.js"
                            }
                        ],
                        description:"\n            Hover over the days with the check icon in them to see the events for those days.\n            Use the \"Next\" and \"Previous\" arrows to change months.\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"workdayCalendar",
                        jsURL:"calendar/workdayCalendar.js",
                        requiresModules:"Calendar",
                        title:"Workday Calendar",
                        tabs:[
                            {
                                title:"eventData",
                                url:"calendar/calendarData.js"
                            }
                        ],
                        description:"\n            The calendar can focus in on workday hours, giving a clearer view of events that occur\n            during the working day. The boundaries of the workday itself can also be customized.\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"customCalendar",
                        jsURL:"calendar/customEventEditing.js",
                        requiresModules:"Calendar",
                        title:"Custom Event Editing",
                        tabs:[
                            {
                                title:"eventData",
                                url:"calendar/calendarData.js"
                            }
                        ],
                        description:"\n            Click in an empty cell or in an event to see custom fields in the quick event editor and in \n            the full event editor. Notice how the red event can't be edited. This has been \n            specified within the event data itself (see the \"eventData\" tab below).\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"eventAutoArrange",
                        jsURL:"calendar/eventAutoArrange.js",
                        requiresModules:"Calendar",
                        title:"Event Auto-Arranging",
                        tabs:[
                            {
                                title:"eventOverlapData",
                                url:"calendar/calendarOverlapData.js"
                            }
                        ],
                        description:"\n            The calendar can automatically arrange events that share time so that each is always\n            fully visible at its proper location.  Drag one event onto or away from another to \n            see the effect.\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"eventOverlapping",
                        jsURL:"calendar/eventOverlapping.js",
                        requiresModules:"Calendar",
                        title:"Event Overlapping",
                        tabs:[
                            {
                                title:"eventOverlapData",
                                url:"calendar/calendarOverlapData.js"
                            }
                        ],
                        description:"\n\t\t\n        When <code>eventAutoArrange</code> is true, the Calendar can overlap concurrent \n        events slightly.  The z-order is from left to right and the overlap-size is a \n        percentage of event-width (see the \"JS\" tab).  If two events start at exactly the \n        same time, the default behavior is to reject the overlap to avoid the first event's \n        close button from being hidden by the second event (see the \"JS\" tab).  This can be seen\n        by dropping one event onto the start-time of another below.\n                     \n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"simpleDayLanes",
                        jsURL:"calendar/dayLanes.js",
                        requiresModules:"Calendar",
                        title:"Day Lanes",
                        tabs:[
                            {
                                title:"taskData",
                                url:"calendar/dayLaneData.js"
                            }
                        ],
                        description:"\n            This example uses individual Day Lanes to show the daily schedules of a number of\n            staff simultaneously. \n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"simpleTimeline",
                        jsURL:"calendar/simpleTimeline.js",
                        requiresModules:"Calendar",
                        title:"Simple Timeline",
                        tabs:[
                            {
                                title:"taskData",
                                url:"calendar/taskData.js"
                            }
                        ],
                        description:"\n            This example uses a Timeline, a simple subclass of Calendar, to show a variety of\n            development tasks assigned to developers over a 3-week period.  This timeline is \n            bound to an array of event data. Drag events to alter their start dates, and resize \n            events to alter their durations. Hover over an event to see it's details or click \n            to edit them.\n        "
                    },
                    {
                        cssURL:"calendar/calendar.css",
                        id:"databoundTimeline",
                        jsURL:"calendar/databoundTimeline.js",
                        requiresModules:"Calendar",
                        title:"Databound Timeline",
                        tabs:[
                            {
                                dataSource:"tasks",
                                name:"tasks"
                            }
                        ],
                        description:"\n            This example uses a Timeline, a simple subclass of Calendar, to show a variety of\n            development tasks assigned to developers over a 3-week period.  This timeline is \n            bound to an SQL DataSource and demonstrates drag-and-drop editing and saving to a \n            DataSource.  Drag events to alter their start dates, and resize events to alter \n            their durations. Hover over an event to see it's details or click to edit them.\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/application_side_list.png",
                isOpen:false,
                title:"Layout",
                description:"\n    Liquid layout managers and user interface containers.\n",
                children:[
                    {
                        jsURL:"forms/layout/filling.js",
                        title:"Filling",
                        description:"\n        Click on the \"Short Message\" and \"Long Message\" buttons to change the amount of\n        space available to the form.\n        \n        Layouts automatically react to resizes and re-apply the layout policy.\n        "
                    },
                    {
                        id:"layoutNesting",
                        jsURL:"layout/nesting.js",
                        showSkinSwitcher:true,
                        title:"Nesting",
                        description:"\n        Use the resize bars to reallocate space between the 3 panes.\n        \n        Layouts can be nested to create standard application views.  Resize bars are built-in.\n        "
                    },
                    {
                        id:"userSizing",
                        jsURL:"layout/userSizing.js",
                        title:"User Sizing",
                        description:"\n        Resize the outer frame to watch \"Member 1\" and \"Member 2\" split the space.  Now resize\n        either member and resize the outer frame again.\n        \n        Layouts track sizes which have been set by user action and respect the user's settings.\n        "
                    },
                    {
                        id:"layoutCenterAlign",
                        jsURL:"layout/centerAlign.js",
                        title:"Center Align",
                        descriptionHeight:"160",
                        description:"\n          \n            <p>\n              To center components within layouts, set <code>layout.align</code> to center along the\n              length axis (vertical axis for a <code>VLayout</code>, horizontal axis for an \n              <code>HLayout</code>).\n            </p><p>\n              To center along the breadth axis (horizontal axis for a <code>VLayout</code>, \n              vertical axis for an <code>HLayout</code>), set <code>member.layoutAlign</code> on each \n              member that should be centered, or set <code>layout.defaultLayoutAlign</code> to center \n              all members.\n            </p><p>\n              Combine both settings to center along both axes.\n            </p><p>\n              You can also use LayoutSpacers to center components.  This is particularly useful where \n\t\t\t  there is a layout that has something that needs to be centered in the remaining space after\n\t\t\t  other components have taken the space they require.\n            </p>\n          \n        "
                    },
                    {
                        id:"snapTo",
                        jsURL:"layout/snapto.js",
                        title:"Snap To",
                        descriptionHeight:"120",
                        description:"\n\t\t\n        <p>\n        Snap-to positioning can be used to place components along a specific edge or corners of a \n\t\tcontainer, or centered in the container.\n\t\tThe <code>snapTo</code> allows for components to be attached to the edge of the container and\n\t\t<code>snapOffsetLeft</code> and <code>snapOffsetTop</code> allows for components to be placed\n\t\tat a specific pixel or percentage offset relative to a snap position. \n\t\t</p><p>\n\t\tDrag resize the containers below to see a variety of snap-to positioning behaviors. \n\t\t</p>\n\t\t\n        "
                    },
                    {
                        id:"dragSnapTo",
                        jsURL:"dragdrop/dragSnapTo.js",
                        showSkinSwitcher:true,
                        title:"Snap-to-grid Dragging",
                        description:"\n        Drag the box around the grid. It will snap into alignment according to the values \n        set in the radio buttons below. Snap-to-grid dragging can be enabled separately for \n        moving and resizing. Toggle the checkboxes to see this working.\n        "
                    },
                    {
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Portal Layout",
                        description:"\n           A Portal Layout allows the user to arrange Portlets via drag-and-drop to create\n           customized environments.\n        ",
                        children:[
                            {
                                id:"portalDragRearrangeSamples",
                                isOpen:false,
                                title:"Drag Rearrange",
                                children:[
                                    {
                                        id:"repositionPortlets",
                                        jsURL:"layout/portal/rearranging.js",
                                        title:"Repositioning",
                                        description:"\n                            Rearrange Portlets via drag-and-drop. Try dragging a Portlet by its\n                            title bar to a new position above or below another Portlet. Also, \n                            drop a Portlet beside another Portlet to form a row.\n                        "
                                    },
                                    {
                                        id:"portalCrossWindowDrag",
                                        jsURL:"layout/portal/crossWindowDrag.js",
                                        title:"Cross-Window Drag",
                                        tabs:[
                                            {
                                                title:"exampleData",
                                                url:"dragdrop/dragList_data.js"
                                            }
                                        ],
                                        description:"\n                            This sample demonstrates dragging a portlet from one PortalLayout to another PortalLayout\n                            in a different browser window.\n                            <p>\n                            Open a second browser window (or browser tab) with this same sample running.  Drag any\n                            portlet to the portal layout shown in the other browser window, then drop.\n                            <p>\n                            Depending on your browser and operating system, it may be necessary to hover over the\n                            second browser tab or over an application icon to cause the tab or browser to come to\n                            the front so you can drop on it.\n                            <p>\n                            Data is transferred directly from one browser instance to another using HTML5\n                            techniques.  This allows you to build applications that span multiple browser windows or\n                            tabs, which makes it easier to take advantage of multiple physical screens.\n                        "
                                    }
                                ]
                            },
                            {
                                id:"addRemovePortalColumn",
                                jsURL:"layout/portal/addingRemovingColumns.js",
                                title:"Add/Remove Columns",
                                description:"\n                    By default, a PortalLayout displays menus allowing the user to\n                    add or remove columns. Try adding a column and moving some Portlets to the new column.\n                "
                            },
                            {
                                title:"Portlet Contents",
                                description:"\n                    Create the contents of Portlets like Windows, or by dragging components\n                    in. \n                ",
                                children:[
                                    {
                                        jsURL:"layout/portal/windowContents.js",
                                        title:"Window contents",
                                        description:"\n                            A Porlet is a subclass of Window, so its contents can be defined in any of the\n                            ways that work for Windows.\n                        "
                                    },
                                    {
                                        id:"portletContentsDragging",
                                        jsURL:"layout/portal/draggingComponents.js",
                                        title:"Dragging components",
                                        tabs:[
                                            {
                                                title:"exampleData",
                                                url:"dragdrop/dragList_data.js"
                                            }
                                        ],
                                        description:"\n                            Components can be dragged into PortalLayouts, creating Portlets on the fly. Try\n                            selecting some records in the ListGrid and drag them into the Portal Layout.\n                            A new Portlet will be created on the fly containing the records that were dragged.\n                            Try dragging the chess pieces to the layout -- they will be moved into\n                            a Portlet created on the fly.\n                        "
                                    },
                                    {
                                        jsURL:"layout/portal/palettes.js",
                                        requiresModules:"Tools",
                                        title:"Palettes",
                                        description:"\n                            With the Tools framework, you can create palettes from which to drag Portlets.\n                            Try dragging from the Tree Palette to the Portal Layout ... Portlets will be\n                            created on the fly.\n                        "
                                    }
                                ]
                            },
                            {
                                title:"Sizing",
                                description:"\n                    Portal Layouts arrange the size of Portlets.\n                ",
                                children:[
                                    {
                                        id:"portalLayoutColumnHeight",
                                        jsURL:"layout/portal/columnHeight.js",
                                        title:"Column height",
                                        descriptionHeight:"100",
                                        description:"\n                            The Portlets on the left have a height which requires the PortalLayout\n                            to scroll. By default, each column scrolls individually. If the large Portlets are dragged\n                             to the right column, then it will scroll. PortalLayouts respect the height that has been set for Portlets\n\t\t\t\t\t\t\t and scroll columns if necessary.\n                            <p>\n                            If it is preferred to have the whole PortalLayout scroll together, try setting the\n                            columnOverflow to \"visible\". Other combinations of <code>overflow</code> and <code>columnOverflow</code>\n                            are also possible.\n                        "
                                    },
                                    {
                                        id:"portalColumnWidth",
                                        jsURL:"layout/portal/columnWidth.js",
                                        title:"Column width",
                                        descriptionHeight:"160",
                                        description:"\n                            By default, PortalLayouts make their columns equal width. Resize bars can be displayed\n                            to allow the user to change column widths.\n                            <p>\n                            Try dragging the resize bar, and see how the columns change width.\n                            <p>\n                            Try making both columns smaller. By default, the PortalLayout will extend\n                            the last column to fill the available width (preventing underflow). This behavior can\n                            be changed with the <code>preventUnderflow</code> attribute.\n                            <p>\n                            Try making both columns bigger. By default, the PortalLayout will scroll\n                            if the columns overflow the available width.\n                        "
                                    },
                                    {
                                        id:"portletHeight",
                                        jsURL:"layout/portal/portletHeight.js",
                                        title:"Portlet height",
                                        description:"\n                            PortalLayouts distribute available column height amongst Portlets equally,\n                            or by the sizes that are specified (like an ordinary Layout).\n                            <p>\n                            By default, the PortalLayout will add space to the last Portlet in a column,\n                            if needed to fill that column. This can be changed by turning \n                            <code>preventColumnUnderflow</code> off.\n                        "
                                    },
                                    {
                                        id:"portletWidth",
                                        jsURL:"layout/portal/portletWidth.js",
                                        title:"Portlet width",
                                        description:"\n                            PortalLayouts distribute available column width amongst Portlets equally,\n                            or by the sizes that are specified (like an ordinary Layout).\n                            <p>\n                            By default, the PortalLayout will add width to the last (or only) Portlet \n                            in a row in order to always fill the row. This can be changed by turning\n                            <code>preventRowUnderflow</code> off.\n                        "
                                    },
                                    {
                                        id:"resizingPortlets",
                                        jsURL:"layout/portal/portletResizing.js",
                                        title:"Portlet resizing",
                                        descriptionHeight:"160",
                                        description:"\n                            Portlets can be drag-resized by their edges (just like resizing Windows).\n                            <p>\n                            Try changing the height of Portlet 4.  Notice how\n                            all the Portlets in that row change height\n                            together. See how each column will scroll if the\n                            height of Portlets is resized to exceed the\n                            available space.\n                            <p>\n                            Try changing the width of Portlet 1. Notice how the\n                            width of the entire column changes. Now try\n                            changing the width of Portlet 2. See how it takes\n                            width from Portlet 3 rather than changing the\n                            column's width.\n                        "
                                    }
                                ]
                            },
                            {
                                id:"portalLayout",
                                jsURL:"layout/portal/portal.js",
                                title:"Portlet Animation",
                                description:"\n                    Click on the portlet list to the left to create portlets and see them animate into place. Drag portlets around to new locations and they animate into place. \n                "
                            },
                            {
                                id:"portletEvents",
                                jsURL:"layout/portal/portletEvents.js",
                                title:"Portlet Events",
                                description:"\n                     Explore events fired by the portal and information passed to the event handler.\n                "
                            }
                        ]
                    },
                    {
                        id:"formslayout",
                        isOpen:false,
                        title:"Form Layout",
                        description:"\n        A specialized form layout manager allows forms to grow into available space,\n        hide sections, and span across tabs.\n    ",
                        children:[
                            {
                                id:"layoutFormTitles",
                                ref:"formLayoutTitles",
                                title:"Titles"
                            },
                            {
                                id:"layoutColumnSpanning",
                                ref:"columnSpanning",
                                title:"Spanning"
                            },
                            {
                                id:"layoutFormFilling",
                                ref:"formLayoutFilling",
                                title:"Filling"
                            },
                            {
                                id:"layoutFormSplitting",
                                ref:"formSplitting",
                                title:"Splitting"
                            },
                            {
                                id:"layoutFormSections",
                                ref:"formSections",
                                title:"Sections"
                            },
                            {
                                id:"layoutValidationFieldBinding",
                                ref:"validationFieldBinding",
                                title:"Data Binding"
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_cascade.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Windows",
                        description:"\n        Windows for dialogs, wizards, tools and free-form application layouts.\n    ",
                        children:[
                            {
                                id:"windowAutosize",
                                jsURL:"layout/window/autoSize.js",
                                title:"Auto Size",
                                description:"\n\t\t\t\t \n                            Windows can <code>autoSize</code> to content or can dictate the content's size.\n                        \n            \n            "
                            },
                            {
                                id:"windowsModality",
                                ref:"modality",
                                title:"Modality"
                            },
                            {
                                id:"windowsDragging",
                                jsURL:"layout/window/dragging.js",
                                title:"Dragging",
                                description:"\n            Grab the window by its title bar to move it around.  Resize it by the right or\n            bottom edge.\n            "
                            },
                            {
                                id:"winWindowMinimize",
                                ref:"windowMinimize",
                                title:"Minimize"
                            },
                            {
                                id:"windowHeaderControls",
                                jsURL:"layout/window/controls.js",
                                title:"Header Controls",
                                description:"\n            Header controls can be reordered and custom controls added.\n            "
                            },
                            {
                                id:"windowFooter",
                                jsURL:"layout/window/footer.js",
                                title:"Footer",
                                description:"\n            Windows support a footer with a visible resizer and updateable status bar.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/tab.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Tabs",
                        description:"\n        Tabs for sectioning applications and forms.\n    ",
                        children:[
                            {
                                id:"tabsOrientation",
                                jsURL:"layout/tabs/orientation.js",
                                title:"Orientation",
                                description:"\n            Tabs can be horizontally or vertically oriented.  To select tabs, click on them, or\n            click on the \"Select Blue\" and \"Select Green\" buttons.\n            "
                            },
                            {
                                id:"tabsAlign",
                                jsURL:"layout/tabs/align.js",
                                title:"Align",
                                description:"\n            Tabs can be left or right aligned (for horizontal tabs) or top or bottom aligned\n            (for vertical tabs)\n            "
                            },
                            {
                                id:"tabsAddAndRemove",
                                jsURL:"layout/tabs/addAndRemove.js",
                                title:"Add and Remove",
                                description:"\n            Click on \"Add Tab\" and \"Remove Tab\" to add and remove tabs.   When too many\n            tabs have been added to display at once, a tab scrolling interface will appear.\n            "
                            },
                            {
                                id:"closeableTabs",
                                jsURL:"layout/tabs/closeableTabs.js",
                                title:"Closeable Tabs",
                                description:"\n            Click on the close icons to close tabs.  Tabbed views can have any mixture of\n            closeable and permanent tabs.\n            "
                            },
                            {
                                id:"titleChange",
                                jsURL:"layout/tabs/titleChange.js",
                                title:"Title Change",
                                description:"\n            Titles can be changed on the fly.  Type in a name to see the \"Preferences\" tab\n            change its title to include that name.  Note that the tab automatically sizes to\n            accommodate the longer title. Automatic sizing also happens at initialization.\n            "
                            },
                            {
                                id:"userEditableTitles",
                                jsURL:"layout/tabs/userEditableTitles.js",
                                title:"User-Editable Titles",
                                description:"\n            Optionally, titles can be directly edited in place by the application's end users.\n            This TabSet specifies <code>canEditTabTitles</code>. Double-click a tab title to \n            edit it.  Individual tabs can override the TabSet standard behavior. In this example, the \n            \"Can't change me\" tab has <code>canEditTitle</code> set to false.  Cancelling the user changes can also be implemented.\n\t\t\tTry editing the \"123-Yellow\" tab to a title that doesn't begin with \"123-\" to see this.\n            "
                            },
                            {
                                id:"selectionEvents",
                                jsURL:"layout/tabs/selectionEvents.js",
                                title:"Selection and Deselection Handling",
                                description:"\n\t\t\t\t \n            Developers can apply custom event handler logic to fire when the user selects tabs.\n            The preferences pane in this example has a <code>tabSelected</code> handler which will create\n            its pane lazily the first time the tab is selected, and a <code>tabDeselected</code> handler\n            which returns false to stop the user changing tabs if the form item is unchecked.\n            \n            "
                            },
                            {
                                id:"viewLoading",
                                jsURL:"advanced/viewLoading.js",
                                needXHR:"true",
                                title:"View Loading",
                                description:"\n            Click on \"Tab2\" to load a grid view on the fly.\n            \n            Declarative view loading allows extremely large applications to be split into\n            separately loadable chunks, and creates an easy integration path for applications\n            with server-driven application flow.\n            ",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"loadedView",
                                        url:"advanced/loadedView.js"
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_tile_vertical.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Sections",
                        description:"\n        Sections (also called Accordions) label sections of the application\n        and allow users to hide or resize sections.\n    ",
                        children:[
                            {
                                id:"sectionsExpandCollapse",
                                jsURL:"layout/sections/expandCollapse.js",
                                title:"Expand / Collapse",
                                description:"\n            Click on any section header showing an arrow to expand and collapse it (the \"Green \n            Cube\" section is marked not collapsible).  Click on the \"Expand Blue\" and \n            \"Collapse Blue\" buttons to expand and collapse sections externally.\n            "
                            },
                            {
                                id:"resizeSections",
                                jsURL:"layout/sections/resizeSections.js",
                                title:"Resize Sections",
                                description:"\n            Drag the \"Help 2\" header to resize sections, or press \"Resize Help 1\" to resize to\n            fixed height.  The \"Blue Pawn\" section is marked not resizeable.\n            "
                            },
                            {
                                id:"sectionControls",
                                jsURL:"layout/sections/sectionControls.js",
                                title:"Custom Controls",
                                description:"\n            Custom controls may appear on section headers.\n            "
                            },
                            {
                                id:"sectionsAddAndRemove",
                                jsURL:"layout/sections/addAndRemove.js",
                                title:"Add and Remove",
                                description:"\n            Press the \"Add Section\" and \"Remove Section\" buttons to add or remove sections.\n            "
                            },
                            {
                                id:"sectionsShowAndHide",
                                jsURL:"layout/sections/showAndHide.js",
                                title:"Show and Hide",
                                description:"\n            Press the \"Show Section\" and \"Hide Section\" buttons to reveal or hide the Yellow\n            Section.  Showing and hiding sections makes a SectionStack reusable for slightly\n            different purposes, hiding or revealing relevant sections.\n            "
                            }
                        ]
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/disconnect.png",
                isOpen:false,
                title:"Data Binding",
                description:"\n    Data binding allows multiple components to share a central definition of an object (called\n    a DataSource), so that all components can consistently retrieve, display, edit, validate\n    and save objects of that type.\n",
                children:[
                    {
                        id:"lists",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Lists",
                        description:"\n    How to bind grids to DataSources to share field (column) definitions with other components,\n    and how to load data from local and remote data sources and services.    \n",
                        children:[
                            {
                                id:"listsListGridFields",
                                ref:"listGridFields",
                                title:"ListGrid fields"
                            },
                            {
                                id:"listsDataSourceFields",
                                ref:"dataSourceFields",
                                title:"DataSource fields"
                            },
                            {
                                id:"listsMergedFields",
                                ref:"mergedFields",
                                title:"Merged fields"
                            },
                            {
                                id:"listsInlineData",
                                ref:"inlineData",
                                title:"Inline data"
                            },
                            {
                                id:"listsLocalData",
                                ref:"localData",
                                title:"Local data"
                            },
                            {
                                id:"listsLocalDataSource",
                                ref:"localDataSource",
                                title:"Local DataSource"
                            },
                            {
                                id:"listsJsonDataSource",
                                ref:"jsonDataSource",
                                title:"JSON DataSource"
                            },
                            {
                                id:"listsXmlDataSource",
                                ref:"xmlDataSource",
                                title:"XML DataSource"
                            },
                            {
                                id:"listsWSDLDataSource",
                                ref:"WSDLDataSource",
                                title:"WSDL DataSource"
                            }
                        ]
                    },
                    {
                        id:"trees",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Trees",
                        description:"\n        Trees can bind to DataSources and handle all the data formats that grids can, using\n        additional properties to control tree structure, open state, and folders.\n    ",
                        children:[
                            {
                                id:"treeParentLinking",
                                ref:"parentLinking",
                                title:"Parent Linking"
                            },
                            {
                                id:"treechildrenArrays",
                                ref:"childrenArrays",
                                title:"Children Arrays"
                            },
                            {
                                id:"treeloadXMLParent",
                                ref:"loadXMLParent",
                                title:"Load XML (Parent Linked)"
                            },
                            {
                                id:"treeTreeLoadXML",
                                ref:"treeLoadXML",
                                title:"Load XML (Child Arrays)"
                            },
                            {
                                id:"treeTreeLoadOnDemand",
                                ref:"treeLoadOnDemand",
                                title:"Load on Demand"
                            },
                            {
                                id:"treeinitialData",
                                ref:"initialData",
                                title:"Initial Data & Load on Demand"
                            },
                            {
                                id:"treeMultiLevelLOD",
                                ref:"multiLevelLOD",
                                title:"Multi-Level LOD"
                            },
                            {
                                id:"treePagingForChildren",
                                ref:"pagingForChildren",
                                title:"Paging for Children"
                            },
                            {
                                id:"treeMultiLevelChildPaging",
                                ref:"multiLevelChildPaging",
                                title:"Multi-Level Child Paging"
                            }
                        ]
                    },
                    {
                        id:"operations",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Operations",
                        description:"\n    DataBound Components understand a core set of operations called \"Fetch\", \"Add\", \"Update\"\n    and \"Remove\" (also known as CRUD operations).  These operations can be programmatically\n    initiated or automatically initiated in response to user action.\n    In either case the integration model and APIs are the same.\n    ",
                        children:[
                            {
                                dataSource:"supplyItem",
                                id:"fetchOperation",
                                jsURL:"databind/operations/fetch.js",
                                title:"Fetch",
                                xmlURL:"databind/operations/fetch.xml",
                                descriptionHeight:"210",
                                description:"\n            Rows are fetched automatically as the user drags the scrollbar.  Drag the scrollbar\n            quickly to the bottom to fetch a range near the end (a prompt will appear during\n            server fetch).  Scroll slowly back up to fill in the middle.\n            "
                            },
                            {
                                dataSource:"supplyItem",
                                id:"addOperation",
                                title:"Add",
                                xmlURL:"databind/operations/add.xml",
                                description:"\n            Use the form to create a new stock item.  Create an item in the currently shown\n            \"Category\" to see it appear in the filtered listing automatically.  Create an item in\n            any other category and note that it is filtered out.\n            "
                            },
                            {
                                dataSource:"supplyItem",
                                id:"updateOperation",
                                title:"Update",
                                xmlURL:"databind/operations/update.xml",
                                description:"\n            Select an item and use the form to change its price.  The list updates\n            automatically.  Now change the item's category and note that it is removed\n            automatically from the list.\n            "
                            },
                            {
                                dataSource:"supplyItem",
                                id:"removeOperation",
                                title:"Remove",
                                xmlURL:"databind/operations/remove.xml",
                                description:"\n            Click the \"Remove\" button to remove the selected item.\n            "
                            }
                        ]
                    },
                    {
                        id:"DBvalidation",
                        isOpen:false,
                        title:"Validation",
                        description:"\n        Typical validation needs are covered by validators built-in to the SmartClient\n        framework.  Validators can be combined into custom type definitions which are reusable\n        across all components.\n    ",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                                id:"DBvalidationCopy",
                                isOpen:false,
                                title:"Server-based",
                                description:"\n            The SmartClient Server provides powerful support for server-based validation.\n        ",
                                children:[
                                    {
                                        dataSource:"supplyItem",
                                        id:"DBsingleSourceValidation",
                                        jsURL:"dataIntegration/java/serverValidation.js",
                                        requiresModules:"SCServer",
                                        title:"Single Source",
                                        description:"\n            Validation rules are automatically enforced on both the client and server-side based on\n            a single, shared declaration.  Press \"Save\" to see errors from the client-side\n            validation.  Press \"Clear Errors\", then \"Disable Validation\", then \"Save\" again to see the\n            same errors caught by the SmartClient server.\n            "
                                    },
                                    {
                                        dataSource:"queuing_userHB",
                                        id:"DBuniqueCheckValidation",
                                        jsURL:"dataIntegration/java/uniqueCheckValidation.js",
                                        requiresModules:"SCServer",
                                        title:"Unique Check",
                                        description:"\n            Enter the email address \"kamirov@server.com\" in the email field and press Tab. Do so with\n            any other email address as well.\n            <P/>\n            The resulting validation error is based upon the server-side <code>isUnique</code> validator that\n            checks to see if there is already a record in the DataSource and if so returns a validation failure. \n            "
                                    },
                                    {
                                        dataSource:"velocity_orderForm",
                                        id:"DBvelocityValidation",
                                        jsURL:"dataIntegration/java/velocityValidation.js",
                                        requiresModules:"SCServer",
                                        title:"Velocity Expression",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                dataSource:"StockItem",
                                                title:"StockItem"
                                            }
                                        ],
                                        description:"\n            Use the \"Item\" ComboBox to select an item,  enter a very large quantity (999999)\n            and press the \"Submit Order\" button.\n            <P/>\n            The resulting validation error is based upon a server-side condition specified in\n            the validator using a Velocity expression. It checks a related DataSource (StockItem)\n            to see if there is sufficient quantity in stock to fulfill the order.\n            "
                                    },
                                    {
                                        dataSource:"inlineScript_orderForm",
                                        id:"DBinlineScriptValidation",
                                        jsURL:"dataIntegration/java/inlineScriptValidation.js",
                                        requiresModules:"SCServer",
                                        title:"Inline Script",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                dataSource:"StockItem",
                                                title:"StockItem"
                                            }
                                        ],
                                        description:"\n            Use the \"Item\" ComboBox to select an item,  enter a very large quantity (999999)\n            and press the \"Submit Order\" button.\n            <P/>\n            The resulting validation error is based upon a server-side condition specified in\n            the validator using inline scripting. It checks a related DataSource (StockItem)\n            to see if there is sufficient quantity in stock to fulfill the order.\n            "
                                    },
                                    {
                                        dataSource:"validationDMI_orderForm",
                                        id:"DBdmiValidation",
                                        jsURL:"dataIntegration/java/validationDMI.js",
                                        requiresModules:"SCServer",
                                        title:"DMI Validation",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                dataSource:"StockItem",
                                                title:"StockItem"
                                            },
                                            {
                                                canEdit:"false",
                                                doEval:"false",
                                                title:"ValidatorDMI.java",
                                                url:"serverExamples/validation/ValidatorDMI.java"
                                            }
                                        ],
                                        descriptionHeight:"150",
                                        description:"\n            Use the \"Item\" ComboBox to select an item,  enter a very large quantity (999999)\n            and press the \"Submit Order\" button.\n            <P/>\n            The resulting validation error is based upon server-side logic in <code>ValidatorDMI.java</code>\n            that checks a related DataSource (StockItem) to see if there is sufficient quantity in\n            stock to fulfill the order.  Hover over the error icon to see the error message and\n            note that it includes an indication of the stock level. Error messages are Velocity \n            templates, and DMI validators can easily populate variable values, as \n            <code>ValidatorDMI.java</code> shows\n            <P/>\n            Validators can use SmartClient DMI to call any server-side method to check the validity\n            of data, including methods on Java beans looked up via Spring.\n            "
                                    },
                                    {
                                        dataSource:"complaint",
                                        id:"DBhasRelatedValidation",
                                        jsURL:"dataIntegration/java/hasRelatedValidation.js",
                                        requiresModules:"SCServer",
                                        title:"Related Records",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                dataSource:"masterDetail_orderHB",
                                                title:"masterDetail_orderHB"
                                            }
                                        ],
                                        descriptionHeight:"200",
                                        description:"\n            Enter a complaint for a received shipment using its tracking number. The tracking\n            number must reference an existing tracking number so try with an existing number\n            (4110884 or 9631143) and with a random number (like 1234).\n            <P/>\n            The <code>relatedRecord</code> validator is used to validate that an ID entered by\n            a user actually exists.  This is useful in situations where using a ComboBox for record\n            lookup is inappropriate (the user should not be able to select against all valid tracking\n            numbers, or among other types of IDs, such as license keys or driver's license numbers)\n            or in situations such as batch upload of many records.\n            <P/>\n            The <code>relatedRecord</code> validator can also be used with a ComboBox as the UI in order to\n            enforce that related records are checked <b>before</b> a request reaches business logic\n            where it would be convenient to assume the ID is already validated, or as a means of\n            enforcing referential integrity in systems that don't have built-in enforcement.\n            "
                                    },
                                    {
                                        dataSource:"complaint",
                                        id:"DBblockingErrors",
                                        jsURL:"dataIntegration/java/blockingErrors.js",
                                        requiresModules:"SCServer",
                                        title:"Blocking Errors",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                dataSource:"masterDetail_orderHB",
                                                title:"masterDetail_orderHB"
                                            }
                                        ],
                                        descriptionHeight:"200",
                                        description:"\n            Enter a complaint for a received shipment using its tracking number. The tracking\n            number must reference an existing tracking number so try with an existing number\n            (4110884 or 9631143) and with a random number (like 1234). Note that when a\n            non-existant value is entered, focus is not allowed to move forwards.\n            <P/>\n            The <code>relatedRecord</code> validator can be used to validate that an ID entered by\n            a user actually exists.  This is useful in situations where using a comboBox for record\n            lookup is inappropriate (the user should not be able to select against all valid tracking\n            numbers, or among other types of IDs, such as license keys or driver's license numbers)\n            or in situations such as batch upload of many records.\n            <P/>\n            The <code>relatedRecord</code> validator can also be used with a ComboBox as the UI in order to\n            enforce that related records are checked <b>before</b> a request reaches business logic\n            where it would be convenient to assume the ID is already validated, or as a means of\n            enforcing referential integrity in systems that don't have built-in enforcement.\n            "
                                    }
                                ]
                            },
                            {
                                dataSource:"databind/validation/type.ds.xml",
                                id:"DBvalidationType",
                                jsURL:"databind/validation/type.js",
                                title:"Type",
                                description:"\n            Type a non-numeric value into the field and press \"Validate\" to receive a\n            validation error.\n            \n            Declaring field type implies automatic validation anywhere a value is edited.\n            "
                            },
                            {
                                dataSource:"databind/validation/builtins.ds.xml",
                                id:"validationBuiltins",
                                jsURL:"databind/validation/builtins.js",
                                title:"Built-ins",
                                description:"\n            Type a number greater than 20 or less than 1 and press \"Validate\" to receive a\n            validation error.\n            \n            SmartClient implements the XML Schema set of validators on both client and server\n            "
                            },
                            {
                                dataSource:"databind/validation/regularExpression.ds.xml",
                                id:"DBregularExpression",
                                jsURL:"databind/validation/regularExpression.js",
                                title:"Regular Expression",
                                description:"\n            Enter a bad email address (eg just \"mike\") and press \"Validate\" to receive a\n            validation error.\n            \n            The regular expression validator allows simple custom field types, with automatic\n            enforcement on client and server.\n            "
                            },
                            {
                                dataSource:"databind/validation/valueTransform.ds.xml",
                                id:"DBvalueTransform",
                                jsURL:"databind/validation/valueTransform.js",
                                title:"Value Transform",
                                description:"\n            Enter a 10 digit US phone number with any typical punctuation. Press \"Validate\" to see it\n            transformed to a canonical format.\n            "
                            },
                            {
                                dataSource:"databind/validation/customTypes.ds.xml",
                                id:"DBcustomSimpleType",
                                jsURL:"databind/validation/customTypes.js",
                                title:"Custom Types",
                                description:"\n            Enter a bad zip code (eg just \"123\") and press \"Validate\" to receive a\n            validation error.\n            \n            Custom types can be declared based on built-in validators and re-used in multiple\n            DataSources\n            "
                            },
                            {
                                dataSource:"databind/forms/users.ds.xml",
                                id:"validationFieldBinding",
                                jsURL:"databind/forms/customBinding.js",
                                title:"Customized Binding",
                                description:"\n            Click \"Validate\" to see validation errors triggered by rules both in this form and\n            in the DataSource.\n            \n            Screen-specific fields and validation logic, such as the duplicate password entry\n            box, can be added to a particular form while still sharing schema information that\n            applies to all views.\n            "
                            }
                        ]
                    },
                    {
                        id:"dataDragging",
                        isOpen:false,
                        title:"Dragging",
                        description:"\n        Databound components have built-in dragging behaviors that operate on persistent\n        datasets.\n    ",
                        children:[
                            {
                                dataSource:"employees",
                                id:"dataDragTreeReparent",
                                jsURL:"databind/drag/treeReparent.js",
                                ref:"treeReparent",
                                title:"Tree Reparent",
                                description:"\n            Dragging employees between managers in this tree automatically saves the new\n            relationship to a DataSource, without writing any code.  Make changes, then \n            reload the page. The changes persist.\n            "
                            },
                            {
                                dataSource:"supplyCategory",
                                id:"dataDragTreeRecategorize",
                                jsURL:"databind/drag/treeRecategorize.js",
                                ref:"treeRecategorize",
                                title:"Recategorize (Tree)",
                                tabs:[
                                    {
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                description:"\n            Dragging items from the list and dropping them on categories in the tree automatically\n            re-categorizes the item, without any code needed.  Make changes, then \n            reload the page. The changes persist.  This behavior is (optionally) automatic where\n            SmartClient can establish a relationship via foreign key between the DataSources\n            two components are bound to.\n            "
                            },
                            {
                                dataSource:"supplyItem",
                                id:"dataDragListRecategorize",
                                jsURL:"databind/drag/listRecategorize.js",
                                ref:"listRecategorize",
                                title:"Recategorize (List)",
                                description:"\n            The two lists are showing items in different categories.  Drag items from one list to\n            another to automatically recategorize the items without writing any code.  Make\n            changes, then reload the page. The changes persist.\n            "
                            },
                            {
                                id:"dataDragRecategorizeTiles",
                                ref:"recategorizeTiles",
                                title:"Recategorize (Tile)"
                            },
                            {
                                dataSource:"employees",
                                id:"dataDragDataboundDragCopy",
                                jsURL:"databind/drag/listCopy.js",
                                ref:"databoundDragCopy",
                                showSkinSwitcher:true,
                                title:"Copy",
                                tabs:[
                                    {
                                        title:"teamMembers",
                                        url:"teamMembers.ds.xml"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n            Drag employee records into the Project Team Members list.  SmartClient recognizes that the \n            two dataSources are linked by a <code>foreignKey</code> relationship, and automatically uses that \n            relationship to populate values in the record that is added when the drop occurs. SmartClient\n            also populates fields based on current criteria and maps explicit <code>titleFields</code> as \n            necessary.<p>\n            In this example, note that SmartClient is automatically populating all three\n            of the fields in the \"teamMembers\" dataSource, even though none of those fields is present \n            in the \"employees\" dataSource that is being dragged from.  Change the \"Team for Project\" select \n            box, then try dragging employees across. Note that the \"Project Code\" column is being \n            correctly populated for the dropped records.\n            "
                            }
                        ]
                    },
                    {
                        id:"dataDragadaptiveFilter",
                        ref:"adaptiveFilter",
                        title:"Adaptive Filter"
                    },
                    {
                        id:"dataDragAdaptiveSort",
                        ref:"adaptiveSort",
                        title:"Adaptive Sort"
                    },
                    {
                        id:"dataDragRelatedRecords",
                        ref:"relatedRecords",
                        title:"Related Records",
                        description:"\n        Open the picker in either form to select the item you want to order from the\n        \"supplyItem\" DataSource.  The picker on the left stores the \"itemId\" from the\n        related \"supplyItem\" records.  The picker on the right stores the \"SKU\" while\n        displaying multiple fields.  Scroll to dynamically load more records.  \n        This pattern works with any DataSource.  \n    "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_table.png",
                id:"dataIntegration",
                isOpen:false,
                title:"Data Integration",
                description:"\n    SmartClient supports declarative, XPath-based binding of visual components to web services\n    that return XML or JSON responses.  SmartClient understands XML Schema and can bind\n    components directly to WSDL web services.  \n",
                children:[
                    {
                        id:"xmlDataIntegration",
                        isOpen:false,
                        title:"XML",
                        description:"\n        SmartClient can declaratively bind to standard formats like WSDL or RSS, homebrew\n        formats, or simple flat files.  \n    ",
                        children:[
                            {
                                id:"rssFeed",
                                jsURL:"dataIntegration/xml/rssFeed.js",
                                needXML:"true",
                                showSkinSwitcher:true,
                                title:"RSS Feed",
                                description:"\n            DataSources can bind directly to simple XML documents where field values appear as\n            attributes or sub-elements.\n            "
                            },
                            {
                                id:"xpathBinding",
                                jsURL:"dataIntegration/xml/xpathBinding.js",
                                needXML:"true",
                                showSkinSwitcher:true,
                                title:"XPath Binding",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"contactsData.xml",
                                        url:"dataIntegration/xml/contactsData.xml"
                                    }
                                ],
                                description:"\n            DataSources can extract field values from complex XML documents via XPath\n            expressions.  Note how the \"address\" fields, which are represented in the \"contacts\"\n            data as a sub-element, appear as columns in the grid. This approach of loading\n            simple XML data over HTTP can be used with PHP and other server technologies.\n            "
                            },
                            {
                                id:"wsdlOperation",
                                jsURL:"dataIntegration/xml/wsdlWebServiceOperations.js",
                                needXML:"true",
                                showSkinSwitcher:false,
                                title:"WSDL Web Services",
                                description:"\n            SmartClient can load WSDL service definitions and call web service operations\n            with automatic JSON<->XML translation.\n            \n            SOAP encoding rules, namespacing, and element ordering are handled automatically\n            for the inputs and outputs. \n            "
                            },
                            {
                                id:"wsdlBinding",
                                jsURL:"dataIntegration/xml/weatherForecastSearch.js",
                                needXML:"true",
                                showSkinSwitcher:true,
                                title:"Weather SOAP Search",
                                description:"\n            Enter a zip code  in the \"Zip\" field to retrieve a weather forecast. \n            \n            DataSources can bind directly to the structure of WSDL messages.\n            "
                            },
                            {
                                id:"xmlEditSave",
                                jsURL:"dataIntegration/xml/operationBinding_dataURL.js",
                                title:"Edit and Save",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_fetch.xml",
                                        url:"dataIntegration/xml/responses/country_fetch.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_add.xml",
                                        url:"dataIntegration/xml/responses/country_add.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_update.xml",
                                        url:"dataIntegration/xml/responses/country_update.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_remove.xml",
                                        url:"dataIntegration/xml/responses/country_remove.xml"
                                    }
                                ],
                                description:"\n        Demonstrates Add, Update and Remove operations with a server that\n        returns simple XML responses, an integration strategy popular with PHP, Ruby and Perl\n        backends.\n        <br>\n        Each operation is directed to a different XML file containing a sample response for\n        that <code>operationType</code>.  The server returns the data-as-saved to allow the grid to update\n        its cache.\n        "
                            },
                            {
                                id:"restEditSave",
                                jsURL:"dataIntegration/xml/restDS_operationBinding.js",
                                title:"RestDataSource - Edit and Save",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_fetch.xml",
                                        url:"dataIntegration/xml/responses/country_fetch_rest.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_add.xml",
                                        url:"dataIntegration/xml/responses/country_add_rest.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_update.xml",
                                        url:"dataIntegration/xml/responses/country_update_rest.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"country_remove.xml",
                                        url:"dataIntegration/xml/responses/country_remove_rest.xml"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n\t\t\n        The <code>RestDataSource</code> provides a simple protocol based on XML or JSON over HTTP.  This\n        protocol can be implemented with any server technology (PHP, Ruby, etc) and \n        includes all the features of SmartClient's databinding layer (data paging, server\n        validation errors, cache sync, etc).<br>\n        In this example, each DataSource operation is directed to a different XML file\n        containing a sample response for that <code>operationType</code>.  The server returns the\n        data-as-saved to allow the grid to update its cache.\n        "
                            },
                            {
                                id:"xmlServerValidationErrors",
                                jsURL:"dataIntegration/xml/serverValidationErrors/serverValidationErrors.js",
                                needXML:"true",
                                showSkinSwitcher:false,
                                title:"Server Validation Errors",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"serverResponse.xml",
                                        url:"dataIntegration/xml/serverValidationErrors/serverResponse.xml"
                                    }
                                ],
                                description:"\n            Click \"Save\" to see validation errors derived from an XML response.\n            \n            Validation errors expressed in application-specific XML formats can be \n            communicated to visual components by implementing\n            <code>DataSource.transformResponse()</code>.  The resulting validation\n            errors will be displayed and tracked by forms and editabled grids.\n            "
                            },
                            {
                                id:"xmlSchemaImport",
                                needXML:"true",
                                showSkinSwitcher:true,
                                title:"XML Schema Import",
                                url:"dataIntegration/xml/xmlSchemaImport.js",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"supplyItem.xsd",
                                        url:"dataIntegration/xml/supplyItem.xsd"
                                    }
                                ],
                                description:"\n\t\t\t\n        Click \"Load Schema\" to load a version of the \"supplyItem\"\n            DataSource expressed in XML Schema format, and bind the Grid and Form to it.  Note\n            that the form and grid choose appropriate editors according to declared XML Schema\n            types.  Click \"Validate\" to see validation errors from automatically imported\n            validators.\n        \n            "
                            },
                            {
                                id:"schemaChaining",
                                needXML:"true",
                                showSkinSwitcher:true,
                                title:"Schema Chaining",
                                url:"dataIntegration/xml/schemaChaining.js",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"supplyItem.xsd",
                                        url:"dataIntegration/xml/supplyItem.xsd"
                                    }
                                ],
                                description:"\n\t\t\t\n\t\t\tClick \"Load Schema\" to load a version of the \"supplyItem\" DataSource from\n            XML Schema format, then extend that schema with SmartClient-specific presentation\n            attributes, and bind the Grid and Form to it.  Note that the internal \"itemId\"\n            field has been hidden from the user, some fields have been retitled, and default\n            editors overridden.\n        \n            "
                            },
                            {
                                id:"WSDLDataSource2",
                                needXML:"true",
                                ref:"WSDLDataSource",
                                showSkinSwitcher:true,
                                title:"SmartClient WSDL"
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"JSON",
                        description:"\n        SmartClient brings declarative XPath binding and typed schema (even XML Schema) to the\n        simple and convenient JSON format.\n    ",
                        children:[
                            {
                                id:"simpleJSON",
                                jsURL:"dataIntegration/json/simpleJSON.js",
                                showSkinSwitcher:true,
                                title:"Simple JSON",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"countries_small.js",
                                        url:"dataIntegration/json/countries_small.js"
                                    }
                                ],
                                description:"\n            DataSources can bind directly to JSON data where records appear as an Array of\n            JavaScript Objects with field values as properties.  This approach of loading\n            simple JSON data over HTTP can be used with PHP and other server technologies.\n            "
                            },
                            {
                                id:"jsonXPath",
                                jsURL:"dataIntegration/json/xpathBinding.js",
                                showSkinSwitcher:true,
                                title:"JSON XPath Binding",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"contactsData.js",
                                        url:"dataIntegration/json/contactsData.js"
                                    }
                                ],
                                description:"\n            DataSources can extract field values from complex JSON structures via XPath\n            expressions.  Note how the address fields, which are represented in the contacts\n            data as a sub-object, appear as columns in the grid.\n            "
                            },
                            {
                                id:"jsonServerValidationErrors",
                                jsURL:"dataIntegration/json/serverValidationErrors/serverValidationErrors.js",
                                showSkinSwitcher:false,
                                title:"Server Validation Errors",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"serverResponse.js",
                                        url:"dataIntegration/json/serverValidationErrors/serverResponse.js"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n\t\t\t\n\t\t\tClick \"Save\" to see validation errors derived from a JSON response.<br><br>\n            \n            Validation errors expressed in application-specific JSON formats can be \n            communicated to the SmartClient system by implementing\n            <code>DataSource.transformResponse()</code>.  The resulting validation\n            errors will be displayed and tracked by forms and editabled grids.\n             \n            "
                            }
                        ]
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                id:"serverExamples",
                isOpen:false,
                title:"Server examples",
                description:"\n    The SmartClient Server framework is a collection of .jar files and optional servlets that work with\n    any J2EE or J2SE container and are easily integrated into existing applications.  Its major\n    features include:<ul>\n    <li><b>Simplified server integration:</b> A pre-built network protocol for browser-server\n        communication, which handles data paging, transactions/batch operations, server-side\n        sort, automatic cache updates, validation and other error handling, optimistic\n        concurrency (aka long transactions) and binary file uploads.<P></li>\n    <li><b>SQL, JPA & Hibernate Connectors:</b> Secure, flexible, transactional support for all\n        CRUD operations, either directly via JDBC or via Hibernate or JPA beans.<P></li>\n    <li><b>Rapid integration with Java Beans:</b> Robust, complete, bi-directional translation\n        between Java and Javascript objects for rapid integration with any Java beans-based\n        persistence system, such as Spring services or custom ORM implementations.  Send and\n        receive complex structures including Java Enums and Java Generics without the need to\n        write mapping or validation code.  Declaratively trim and rearrange data so that only\n        selected data is sent to the client <b>without</b> the need to create and populate\n        redundant DTOs (data transfer objects).<P></li>\n    <li><b>Server enforcement of Validators:</b> A single file specifies validation rules\n        which are enforced on both the client and server side<P></li>\n    <li><b>Declarative Security:</b> Easily attach role or capability-based security rules to\n        data operations with server-side enforcement, plus automatic client-side effects such as\n        hiding fields or showing fields as read-only based on the user role.<P></li>\n    <li><b>Export:</b> Export any dataset to CSV or true Excel spreadsheets, including data\n        highlights and formatting rules<br><br></li>\n    <li><b>High speed data delivery / data compression:</b> automatically use the fastest \n        possible mechanism for delivering data to the browser<br></li>\n    </ul>\n    The SmartClient Server framework is an optional, commercially-licensed package.  See the \n    <a href=http://www.smartclient.com/product/index.jsp>products page</a> for details.\n    \n",
                children:[
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        id:"serverValidation",
                        isOpen:false,
                        title:"Validation",
                        description:"\n        The SmartClient Server provides powerful support for server-based validation.\n    ",
                        children:[
                            {
                                dataSource:"supplyItem",
                                id:"singleSourceValidation",
                                jsURL:"dataIntegration/java/serverValidation.js",
                                requiresModules:"SCServer",
                                title:"Single Source",
                                description:"\n        Validation rules are automatically enforced on both the client and server-side based on\n        a single, shared declaration.  Press \"Save\" to see errors from the client-side\n        validation.  Press \"Clear Errors\", then \"Disable Validation\", then \"Save\" again to see the\n        same errors caught by the SmartClient server.\n        "
                            },
                            {
                                dataSource:"validationDMI_orderForm",
                                id:"dmiValidation",
                                jsURL:"dataIntegration/java/validationDMI.js",
                                requiresModules:"SCServer",
                                title:"DMI Validation",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"StockItem",
                                        title:"StockItem"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ValidatorDMI.java",
                                        url:"serverExamples/validation/ValidatorDMI.java"
                                    }
                                ],
                                descriptionHeight:"140",
                                description:"\n        Use the \"Item\" ComboBox to select an item,  enter a very large quantity (999999)\n        and press the \"Submit Order\" button.\n        <P/>\n        The resulting validation error is based on server-side logic in <code>ValidatorDMI.java</code>\n        that checks a related DataSource (StockItem) to see if there is sufficient quantity in\n        stock to fulfill the order.\n        <P/>\n        Validators can use SmartClient DMI to call any server-side method to check the validity\n        of data, including methods on Java beans looked up via Spring.\n        "
                            },
                            {
                                dataSource:"velocity_orderForm",
                                id:"velocityValidation",
                                jsURL:"dataIntegration/java/velocityValidation.js",
                                requiresModules:"SCServer",
                                title:"Velocity Expression",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"StockItem",
                                        title:"StockItem"
                                    }
                                ],
                                description:"\n        Use the \"Item\" ComboBox to select an item,  enter a very large quantity (999999)\n        and press the \"Submit Order\" button.\n        <P/>\n        The resulting validation error is based on a server-side condition specified in\n        the validator using a Velocity expression. It checks a related DataSource (StockItem)\n        to see if there is sufficient quantity in stock to fulfill the order.\n        "
                            },
                            {
                                dataSource:"inlineScript_orderForm",
                                id:"inlineScriptValidation",
                                jsURL:"dataIntegration/java/inlineScriptValidation.js",
                                requiresModules:"SCServer",
                                title:"Inline Script",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"StockItem",
                                        title:"StockItem"
                                    }
                                ],
                                description:"\n            Use the \"Item Id\" ComboBox to select an item,  enter a very large quantity (999999)\n            and press the \"Submit Order\" button.\n            <P/>\n            The resulting validation error is based on a server-side condition specified in\n            the validator using inline scripting. It checks a related DataSource (StockItem)\n            to see if there is sufficient quantity in stock to fulfill the order.\n            "
                            },
                            {
                                dataSource:"queuing_userHB",
                                id:"uniqueCheckValidation",
                                jsURL:"dataIntegration/java/uniqueCheckValidation.js",
                                requiresModules:"SCServer",
                                title:"Unique Check",
                                description:"\n        Enter the email address \"kamirov@server.com\" in the email field and press Tab. Do so with\n        any other email address as well.\n        <P/>\n        The resulting validation error is based on the server-side <code>isUnique</code> validator that\n        checks to see if there is already a record in the DataSource and if so fails validation. \n        "
                            },
                            {
                                dataSource:"complaint",
                                id:"hasRelatedValidation",
                                jsURL:"dataIntegration/java/hasRelatedValidation.js",
                                requiresModules:"SCServer",
                                title:"Related Records",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderHB",
                                        title:"masterDetail_orderHB"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n        Enter a complaint for a received shipment using its tracking number. The tracking\n        number must reference an existing tracking number (use one of the existing numbers\n        4110884 or 9631143 and again with a random number like 1234).\n        <P/>\n        The <code>relatedRecord</code> validator can be used to validate that an ID entered by\n        a user actually exists.  This is useful in situations where using a ComboBox for record\n        lookup is inappropriate (the user should not be able to select against all valid tracking\n        numbers, or among other types of IDs, such as license keys or driver's license numbers),\n        or in situations such as a batch upload of many records.\n        <P/>\n        The <code>relatedRecord</code> validator can also be used with a ComboBox as the UI in order to\n        ensure that related records are checked before a request reaches business logic,\n        where it would be convenient to assume the ID is already validated, or as a means of\n        enforcing referential integrity in systems that don't have built-in enforcement.\n        "
                            },
                            {
                                dataSource:"complaint",
                                id:"blockingErrors",
                                jsURL:"dataIntegration/java/blockingErrors.js",
                                requiresModules:"SCServer",
                                title:"Blocking Errors",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderHB",
                                        title:"masterDetail_orderHB"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n             Enter a complaint for a received shipment using its tracking number. The tracking\n        number must reference an existing tracking number (use one of the existing numbers\n        4110884 or 9631143 and again with a random number like 1234).\n        <P/>\n        The <code>relatedRecord</code> validator can be used to validate that an ID entered by\n        a user actually exists.  This is useful in situations where using a comboBox for record\n        lookup is inappropriate (the user should not be able to select against all valid tracking\n        numbers, or among other types of IDs, such as license keys or driver's license numbers),\n        or in situations such as a batch upload of many records.\n        <P/>\n        The <code>relatedRecord</code> validator can also be used with a ComboBox as the UI in order to\n        enforce that related records are checked before a request reaches business logic\n        where it would be convenient to assume the ID is already validated, or as a means of\n        enforcing referential integrity in systems that don't have built-in enforcement.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/database_lightning.png",
                        isOpen:false,
                        title:"SQL",
                        description:"\n        The SmartClient Server provides powerful built-in support for codeless connection to\n        mainstream SQL databases.\n    ",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_gear.png",
                                id:"sqlWizard",
                                jsURL:"serverExamples/sql/vb_Database.js",
                                requiresModules:"SCServer",
                                showSkinSwitcher:false,
                                showSource:false,
                                title:"Database Wizard",
                                description:"\n            SmartClient's Visual Builder tool provides an extremely easy and completely codeless \n            way to create DataSources for instantly connecting to existing database tables. \n            Click the \"New\" button, select \"Existing SQL Table\", and the Database Browser will\n            show the actual tables, column details and the even the data.  Select a table, and \n            Visual Builder will create a fully-functioning DataSource that can perform\n            all four CRUD operations on that table, including (if using the Power  or \n            Enterprise Edition), complex searches enabled by SmartClient's <code>AdvancedCriteria</code> system.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_gear.png",
                                id:"sqlConnector",
                                jsURL:"serverExamples/sql/basicConnector.js",
                                requiresModules:"SCServer",
                                title:"Basic Connector",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDS",
                                        url:"grids/ds/worldSQLDS.ds.xml"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n            The basic SQL Connector gives you the ability to immediately connect SmartClient components to\n            SQL databases without writing any code.  \n            <P>\n            Either use the SQL Wizard in Visual Builder to generate a DataSource descriptor\n            (*.ds.xml file) from an existing SQL table, or use the Admin Console to generate an SQL table\n            from a DataSource descriptor that is manually written.  Either way, gives the immediate ability to perform\n            all 4 basic SQL operations (SELECT, INSERT, UPDATE, DELETE) from any of SmartClient's\n            data-aware components.\n            <P>\n            The grid below is connected to an SQL DataSource and has settings enabled to allow this grid to\n            perform all 4 of these operations.  Type in the input boxes above each column to do query by example.\n            Note that data paging is automatically enabled. Scroll to load data on demand.  Click on\n            the red \"X\" to delete a record.  Click on a record to edit it and click \"Add New\" to add a new record.\n            <P>\n            It's easy to add business logic that takes place before and after SQL operations to enforce\n            security or add additional data validation rules.\n            <P>\n            Even if the primary data storage approach is non-SQL or if JPA or other ORM is the chosen\n            system for most objects, the SQL connector is still valuable for initial prototypes and for\n            lightweight storage when a full ORM approach would be overkill.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/funnel.png",
                                id:"filterBuilderBracketSQL",
                                ref:"filterBuilderBracket",
                                requiresModules:"SCServer,serverCriteria",
                                title:"Server Advanced Filtering (SQL)",
                                descriptionHeight:"150",
                                description:"\n            Use the FilterBuilder to construct queries of arbitrary complexity.  The FilterBuilder,\n            and the underlying AdvancedCriteria system, support building queries with subclauses\n            nested to any depth. Add clauses to the query with the \"+\" icon. Add nested subclauses \n            with the \"+()\" button. Click \"Filter\" to see the results in the ListGrid.\n            <P>\n            Note that this example is backed by an \"SQL\" dataSource. The SmartClient Server is \n            automatically generating the SQL queries required to implement the filters that the \n            FilterBuilder can assemble.  This works adaptively and seamlessly with client-side \n            Advanced Filtering. The generated SQL query will yield exactly the same result-set \n            as the client-side filtering.  This means SmartClient is able to switch to client-side\n            filtering when its cache is full, giving a more responsive, more scalable application.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_relationship.png",
                                id:"largeValueMapSQL",
                                jsURL:"serverExamples/sql/largeValueMap/largeValueMap.js",
                                requiresModules:"SCServer,customSQL",
                                title:"Large Value Map",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"largeValueMap_orderItem",
                                        name:"orderItem"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n            This example shows the simple use of custom SQL clauses to provide a DataSource that\n            joins multiple tables while retaining SmartClient's automatic paging and filtering\n            behavior.  When trying this example, remember that this is <b>automatic</b> \n            dataset-handling behavior that works without any coding, even though the data is being \n            provided by a custom SQL query.<p>\n            \n            The list contains order items. Each order item holds an \"itemId\", which is being used\n            to join to the \"SupplyItem\" table to obtain the \"Item Name\".  Note that you can filter on\n            the \"Item Name\". Either select a full \"Item Name\" or just enter a partial value in the \n            combo box.  Pagination is also active. Try quickly dragging the scrollbar down, and\n            SmartClient will be seen contacting the server for more records.<p>\n           \n             Editing is also enabled in this example.  Try filtering to a small sample of items,\n             then edit one of them by double-clicking it and choose a different item.  Note how \n             that order item is immediately filtered out of the list. SmartClient's intelligent \n             cache sync also automatically handles custom SQL statements.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/user_orange.png",
                                id:"userSpecificData",
                                jsURL:"serverExamples/sql/userSpecificData/userSpecificData.js",
                                requiresModules:"SCServer,customSQL",
                                title:"User-Specific Data",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"cartItem",
                                        name:"cartItem"
                                    },
                                    {
                                        canEdit:"false",
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CartDMI.java",
                                        url:"serverExamples/sql/userSpecificData/CartDMI.java"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n            This example shows the use of some simple user-written server code in conjunction with\n            SmartClient databound dragging features and the SmartClient SQL DataSource to implement\n            a simple, but secure, shopping cart example.\n            <p>\n            Via DMI (Direct Method Invocation), the \"cartItem\" DataSource declares\n            that all DataSource operations should go through a custom Java method\n            <code>CartDMI.enforceUserAccess()</code> <b>before</b> proceeding to read or write\n            to the database.  &nbsp;&nbsp;<code>CartDMI.enforceUserAccess()</code> adds the current sessionId to the\n            DSRequest, so that the user can only read and write their own shopping cart.\n            <P>\n            Drag items from the left-hand grid to the right-hand grid.  The right hand grid allows for \n            editing quantity and deleting records.  Verify that the example\n            is protecting each user's data from others by running the example in two different\n            browsers (eg one Firefox and one IE) - this creates distinct sessions with separate\n            carts.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_multiple.png",
                                id:"dynamicReporting",
                                jsURL:"serverExamples/sql/dynamicReporting/dynamicReporting.js",
                                requiresModules:"SCServer,customSQL",
                                title:"Dynamic Reporting",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"dynamicReporting_orderItem",
                                        name:"orderItem"
                                    }
                                ],
                                descriptionHeight:"215",
                                description:"\n            This example shows the use of custom SQL clauses to build a fairly complex query, including\n            both standard and bespoke WHERE conditions and the use of aggregate functions and a \n            GROUP BY.  It is important to note that this can be done, whilst still keeping the normal \n            benefits of SmartClient DataSources, such as automatic dataset paging and arbitrary\n            filtering and sorting.  Also note that this example, though it makes heavy use of custom\n            SQL clauses, doesn't make use of any database-specific syntax or functions, so it is \n            portable across different database products.<p>\n            \n            The list contains a summary of orders in a given date range, summarized by item - each\n            item appears just once in the list, alongside the total quantity of that item ordered \n            in the given date range.  Change the date range to be more restrictive (all the rows\n            in the sample database have dates in February 2009) and click \"Filter\", Note how\n            the quantities change, and items disappear from the list.  Also, use the \n            filter editor at the top of the grid to arbitrarily filter the records, or click\n            the column headings to sort.<p>\n            \n            Scroll the grid quickly to the bottom, and a brief notification will be seen, as \n            SmartClient contacts the server. Pagination still works normally, despite the unusual\n            and complex query.\n            "
                            },
                            {
                                id:"autoTransactionsFS",
                                ref:"autoTransactions",
                                title:"Transactions"
                            },
                            {
                                id:"sqlIncludeFrom",
                                jsURL:"serverExamples/sql/relations/sqlIncludeFrom.js",
                                requiresModules:"SCServer",
                                title:"Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromSQL",
                                        name:"cityIncludeFromSQL"
                                    },
                                    {
                                        dataSource:"countryIncludeFromSQL",
                                        name:"countryIncludeFromSQL"
                                    },
                                    {
                                        dataSource:"continentIncludeFromSQL",
                                        name:"continentIncludeFromSQL"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n                Fields from related data sources can be included by declaring a field with the <code>includeFrom</code>\n                property, which points to a field in a related DataSource. The Target field can be included from another\n                DataSource. For example \"CityDS\" includes the field \"continentName\" from the \"CountryDS\" DataSource, which is\n                included from the \"ContinentDS\" DataSource.\n                <p/>\n                Either specify a different field name or omit it altogether. In this case, the field name will default\n                to the name of the included field.\n            "
                            },
                            {
                                id:"sqlIncludeVia",
                                jsURL:"serverExamples/sql/relations/sqlIncludeVia.js",
                                requiresModules:"SCServer",
                                title:"Multiple Field Include",
                                tabs:[
                                    {
                                        dataSource:"moneyTransfer",
                                        name:"moneyTransfer"
                                    },
                                    {
                                        dataSource:"currency",
                                        name:"currency"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n\t\t\t\tA DataSource is sometimes related to another DataSource in <b>two</b> ways, for example, a \"moneyTransfer\" DataSource\n\t\t\t\tmay be related to a \"currency\" DataSource by two <code>foreignKey</code>s - one representing the source currency, and one \n\t\t\t\trepresenting the payment currency.\n\t\t\t\t<p>\n\t\t\t\tWhen this happens, you can still use the <code>includeFrom</code> feature to include fields from the related DataSource,\n\t\t\t\tbut you need to use the <code>includeVia</code> property to specify which <code>foreignKey</code> should be used.\n\t\t\t"
                            },
                            {
                                id:"sqlIncludeFromDynamic",
                                jsURL:"serverExamples/sql/relations/sqlIncludeFromDynamic.js",
                                requiresModules:"SCServer",
                                title:"Dynamic Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromSQL",
                                        name:"cityIncludeFromSQL"
                                    },
                                    {
                                        dataSource:"countryIncludeFromSQL",
                                        name:"countryIncludeFromSQL"
                                    },
                                    {
                                        dataSource:"continentIncludeFromSQL",
                                        name:"continentIncludeFromSQL"
                                    }
                                ],
                                descriptionHeight:"100",
                                description:"\n                Fields from related DataSources can be included without directly declaring them in the DataSource. \"CityDS\"\n\t\t\t\tdoes not have the declared field \"countryCode\". This field is specified in the ListGrid with the <code>includeFrom</code>\n\t\t\t\tproperty which points to the relevant field in the related \"CountryDS\" DataSource.\n                For this widget only, the system automatically adds the specified fields and retrieves their values.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        isOpen:false,
                        title:"Hibernate / Beans",
                        description:"\n        The SmartClient Server's built-in support for Hibernate\n    ",
                        children:[
                            {
                                dataSource:"supplyItemHBAutoDerive",
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/coffeebean.png",
                                id:"hibernateAutoDerivation",
                                jsURL:"serverExamples/hibernate/autoDerivation/hibernateAutoDerivation.js",
                                requiresModules:"SCServer",
                                showSkinSwitcher:false,
                                title:"Auto Derivation",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemHB.java",
                                        url:"serverExamples/hibernate/autoDerivation/SupplyItemHB.java"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n            If you have pre-existing Hibernate beans, SmartClient can automatically derive fully functional\n            DataSources given just the Java classname of the Hibernate Bean.  The grid below is connected\n            to a Hibernate-managed bean via the simple declarations in <code>supplyItemHBAutoDerive.ds.xml</code>\n            - no other configuration or Java code is required beyond the bean itself and possibly Hibernate \n            mappings (<code>.hbm.xml</code> files).  In this sample, the bean is annotated, so no separate \n            mappings are required.\n            <p/>\n            To search, use the controls above the grid's header. Note that data paging is automatically\n            enabled. Scroll down to load data on demand. Click on the red icon next to each record to\n            delete it. Click on a record to edit it and click \"Add New\" to add a new record.  Note that the\n            editing controls are type sensitive. A date picker appears for the \"Next Shipment\" field, and\n            the \"Units\" field shows a picklist as its Java type is an Enum.  Also note that the \"Item Name\" \n            and \"Description\" fields have automatically been flagged as <code>required:true</code>; this \n            was derived from annotations on the corresponding members of the bean - see the \n            <code>SupplyItemHB.java</code> tab.\n            <p/>\n            You can use DMI to add business logic that takes place before and after Hibernate operations to\n            enforce security or add additional data validation rules.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/coffeebean.png",
                                id:"hibernateConnector",
                                jsURL:"serverExamples/hibernate/hibernateConnector.js",
                                requiresModules:"SCServer",
                                title:"Beanless Mode",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"worldHB",
                                        title:"worldHB"
                                    }
                                ],
                                descriptionHeight:"210",
                                description:"\n            Beanless mode allows Hibernate to be used for persistence without writing any Java code at all.\n            Declare the properties of the object in the DataSource descriptor (*.ds.xml file), and\n            SmartClient will generate the Hibernate configuration automatically.  The Admin\n            Console can be used to generate the underlying SQL table as well, meaning, the only file that needs to be\n\t\t\tcreated is the *.ds.xml file.\n            <P>\n            As with the previous example, the grid below provides the ability to search, edit, and delete\n            records.\n            <P>\n            Beanless mode helps to avoid writing boilerplate Java code (several classes full of getter\n            and setter methods that do nothing) for simple entities.  Even in beanless mode, \n            DMI can still be used to add Java business logic that takes place before and after Hibernate operations. The\n            Hibernate data is represented as a Java Map. \n            <P>\n            A mixture of beanless mode and normal Hibernate beans can also be used, even in the same\n            transaction.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/funnel.png",
                                id:"advancedFilterHibernate",
                                jsURL:"serverExamples/hibernate/advancedFilter/advancedFilterHibernate.js",
                                requiresModules:"SCServer,serverCriteria",
                                title:"Advanced Filtering",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItemHB",
                                        name:"supplyItemHB"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n            Use the FilterBuilder to construct queries of arbitrary complexity.  The FilterBuilder,\n            and the underlying <code>AdvancedCriteria</code> system, support building queries with subclauses\n            nested to any depth. Add clauses to the query with the \"+\" icon and add nested subclauses \n            with the \"+()\" button. Click \"Filter\" to see the result in the ListGrid.\n            <p>\n            Note that this example is backed by a \"hibernate\" DataSource. The SmartClient Server is \n            automatically generating the Hibernate Criteria Queries (including database-specific SQL\n            where necessary) to implement the filters that the FilterBuilder \n            can assemble.    This works adaptively and seamlessly with client-side \n            Advanced Filtering. The generated Criteria query will yield exactly the same resultset \n            as the client-side filtering.  This means SmartClient is able to switch to client-side\n            filtering when its cache is full, giving a more responsive, more scalable application.\n            "
                            },
                            {
                                id:"hbRelationManyToOneSimple",
                                jsURL:"serverExamples/hibernate/relations/hbRelationManyToOneSimple.js",
                                requiresModules:"SCServer",
                                title:"Many-to-One Relation",
                                tabs:[
                                    {
                                        dataSource:"cityManyToOneSimpleHB",
                                        name:"cityManyToOneSimpleHB"
                                    },
                                    {
                                        dataSource:"countryManyToOneSimpleHB",
                                        name:"countryManyToOneSimpleHB"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CountryManyToOneSimple.java"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n                SmartClient handles Hibernate Many-to-One relationships transparently, such as cities within\n                countries.  Declare a <code>foreignKey</code> field on the \"City\" DataSource to indicate\n                the need to use the related Hibernate bean, \"Country\".\n                <P>\n                The grid below shows Cities, but the Country name is automatically shown even though the\n                \"countryName\" is stored in the related Hibernate bean, \"Country\".  Any fields from any number of\n                related beans can automatically be loaded this way.\n                <P>\n                Click to edit and change the Country of a City.  The list of Countries is automatically\n                loaded from the related Hibernate bean, along with their IDs.  \n                <P>\n                Changing the Country of a City sends the ID of the new Country back to the server, and\n                SmartClient automatically makes all the required Hibernate calls to persist the change. No\n                server-side code needs to be written beyond the Hibernate beans themselves and their\n                annotations.\n            "
                            },
                            {
                                id:"hbRelationOneToMany",
                                jsURL:"serverExamples/hibernate/relations/hbRelationOneToMany.js",
                                requiresModules:"SCServer",
                                title:"One-to-Many Relation",
                                tabs:[
                                    {
                                        dataSource:"cityOneToManyHB",
                                        name:"cityOneToManyHB"
                                    },
                                    {
                                        dataSource:"countryOneToManyHB",
                                        name:"countryOneToManyHB"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityOneToMany.java",
                                        url:"serverExamples/hibernate/relations/CityOneToMany.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryOneToMany.java",
                                        url:"serverExamples/hibernate/relations/CountryOneToMany.java"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n                SmartClient handles Hibernate One-to-Many relationships transparently (such as a Country which has\n                multiple Cities).  Just declare a collection field (<code>multiple:true</code>) on the Country\n                DataSource to indicate loading its list of Cities.\n                <P>\n                Click on a Country below - its list of Cities is revealed without an additional round trip to the\n                server.  Cities can be now edited in the lower grid.  \n                <P>\n                When data is saved, all changes to the Country and its Cities are sent in one save\n                request, and SmartClient automatically makes all the required Hibernate calls to persist the\n                changes. No server-side code needs to be written beyond the Hibernate beans themselves and\n                their annotations.\n            "
                            },
                            {
                                id:"hbIncludeFrom",
                                jsURL:"serverExamples/hibernate/relations/hbIncludeFrom.js",
                                requiresModules:"SCServer",
                                title:"Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromHB",
                                        name:"cityIncludeFromHB"
                                    },
                                    {
                                        dataSource:"countryIncludeFromHB",
                                        name:"countryIncludeFromHB"
                                    },
                                    {
                                        dataSource:"continentIncludeFromHB",
                                        name:"continentIncludeFromHB"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CountryManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ContinentManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/ContinentManyToOneSimple.java"
                                    }
                                ],
                                descriptionHeight:"135",
                                description:"\n                Fields can be included from other, related DataSources by just declaring the name of the\n                DataSource and field to include, using the <code>includeFrom</code> property.\n                <p>\n                In the example below, a DataSource that stores Cities is shown.  It includes the\n                field \"Country Name\" from a related DataSource that stores Countries.  It also\n                includes the field \"Continent\" from an <i>indirectly</i> related DataSource that stores\n                Continents.  At the database layer, this related data is all being fetched using an efficient\n                SQL join.\n                <p>\n                Click to edit the \"Country Name\" field for a row - this field has been configured as a\n                ComboBox that edits which Country a City belongs to.  Try shifting a City to a Country on a\n                different Continent, and note how the \"Continent\" field automatically updates.  This happens\n                because <code>includeFrom</code> declarations are automatically applied when the server returns\n                data to update the UI.\n            "
                            },
                            {
                                id:"hbIncludeFromDynamic",
                                jsURL:"serverExamples/hibernate/relations/hbIncludeFromDynamic.js",
                                requiresModules:"SCServer",
                                title:"Dynamic Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromHB",
                                        name:"cityIncludeFromHB"
                                    },
                                    {
                                        dataSource:"countryIncludeFromHB",
                                        name:"countryIncludeFromHB"
                                    },
                                    {
                                        dataSource:"continentIncludeFromHB",
                                        name:"continentIncludeFromHB"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/CountryManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ContinentManyToOneSimple.java",
                                        url:"serverExamples/hibernate/relations/ContinentManyToOneSimple.java"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n                Fields can be included from other, related DataSources on-demand, on a screen-specific basis,\n                by using the <code>includeFrom</code> attribute on a ListGridField.  \n                <p>\n                In the grid below, declarations in the DataSource cause the \"Country Name\" field to appear for\n                each City.  However the field \"Country Code\" is included on-demand, just for this grid, by\n                declaring the <code>includeFrom</code> attribute.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_multiple.png",
                                id:"masterDetail",
                                jsURL:"serverExamples/hibernate/masterDetail/masterDetail.js",
                                requiresModules:"SCServer",
                                title:"Master-Detail (Batch Load and Save)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderHB",
                                        name:"masterDetail_order"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"masterDetail_orderItemHB",
                                        name:"masterDetail_orderItem"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Order.java",
                                        url:"serverExamples/hibernate/masterDetail/Order.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Order.hbm.xml",
                                        url:"serverExamples/hibernate/masterDetail/Order.hbm.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"OrderItem.java",
                                        url:"serverExamples/hibernate/masterDetail/OrderItem.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"OrderItem.hbm.xml",
                                        url:"serverExamples/hibernate/masterDetail/OrderItem.hbm.xml"
                                    }
                                ],
                                descriptionHeight:"125",
                                description:"\n            This example shows a simple way to implement an update-able parent-child relationship\n            with SmartClient, the SmartClient Server and Hibernate.  Fom the \n            various source tabs, it can be seen that \"Order\" and \"OrderItem\" are related via \n            a unidirectional set collection in Hibernate.\n            The order DataSource also declares its \"items\" field as being\n            of type <code>masterDetail_orderItemHB</code>, which tells SmartClient to use that \n            DataSource as the schema when processing the detail lines.  With this configuration in\n            place, creating a UI capable of updating across this parent-child association becomes\n            extremely easy (only two lines of SmartClient code, beyond the creation and layout \n            of the visual components themselves, is required).\n            <p>\n            Click a record in the top grid to see the order's details and the associated detail \n            lines in the form and grid below.\n            Edit the order information using this screen (both header and detail - \n            double-click the grid to edit the details); when \"Save\" is clicked, SmartClient will \n            submit the master and detail information together, and Hibernate will save all \n            changes as a single operation.\n            "
                            },
                            {
                                dataSource:"flattenedBeans_flatUserHB",
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/branch.png",
                                id:"flattenedBeans",
                                jsURL:"serverExamples/hibernate/flattenedBeans/flattenedBeans.js",
                                requiresModules:"SCServer",
                                title:"Data Selection",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"FlatUser.java",
                                        url:"serverExamples/hibernate/flattenedBeans/FlatUser.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"FlatUser.hbm.xml",
                                        url:"serverExamples/hibernate/flattenedBeans/FlatUser.hbm.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Address.java",
                                        url:"serverExamples/hibernate/flattenedBeans/Address.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Address.hbm.xml",
                                        url:"serverExamples/hibernate/flattenedBeans/Address.hbm.xml"
                                    }
                                ],
                                descriptionHeight:"130",
                                description:"\n            This example shows the SmartClient Server's support for flattening and reconstructing\n            hierarchical data, by use of XPaths.  The ListGrid below shows each user's address,\n            city and state as if those fields were part of the user's data.  In fact, this address\n            information is held in a separate Address bean. This information is extracted from \n            the separate bean at fetch time by the SmartClient Server, based purely on the XPath\n            declarations of those fields in the DataSource.\n            <p>\n            More interestingly, the SmartClient Server is also able to reconstruct the hierarchical\n            data from the flattened version, again transparently by use of the XPath.  This means\n            that you can update the flattened fields in this example - (e.g. Change a\n            user's city and notice how the changes are correctly persisted).\n            <p>\n            Note also that the User bean has a <code>password</code> attribute which is being \n            completely excluded from this example.  When you specify <code>dropExtraFields</code>\n            on a DataSource, as is done here, SmartClient Server returns just those fields \n            defined in the DataSource.  So, in this example, the existing schema can be used,\n            whilst easily retaining tight control over what gets delivered to the client.  This \n            includes related entities as well as simple attributes.\n            <p>\n            Click a record in the grid to see the order's details in the form.  Edit the user\n            details and click \"Save Changes\".  Using the declared XPaths, the SmartClient Server \n            will populate any changed flattened field back into its correct place in the hierarchy,\n            allowing the data provider (Hibernate, in this case) to persist the change.\n            "
                            },
                            {
                                dataSource:"supplyItemSpringDMI",
                                id:"hibernateProduction",
                                jsURL:"dataIntegration/java/hibernateProduction.js",
                                requiresModules:"SCServer",
                                title:"Spring with Beans",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Spring applicationContext.xml",
                                        url:"dataIntegration/java/applicationContext.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemDao.java",
                                        url:"dataIntegration/java/SupplyItemDao.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItem.hbml.xml",
                                        url:"dataIntegration/java/SupplyItem.hbm.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItem.java",
                                        url:"dataIntegration/java/SupplyItem.java"
                                    }
                                ],
                                description:"\n            This example demonstrates how SmartClient can be used to call pre-existing Spring\n            business logic, and provides a general sample of integrating with beans-based\n            persistence systems.  <b>NOTE: if you want to use Hibernate in a new application,\n            use the built-in HibernateDataSource connector, not this code.</b>  The sample code\n            shown here has less features than the built-in connector (which supports\n            advanced search, multi-level sort, automatic transactions, and other features).\n            <P>\n            In this example, Hibernate's <code>Criteria</code> object can be created\n            from SmartClient's <code>DSRequest</code> in order to fulfill the\n            \"fetch\" operation, with data paging enabled.  Hibernate-managed beans can be\n            populated with inbound, validated data with a single method call.\n            "
                            },
                            {
                                dataSource:"supplyItemDMI",
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/coffeebean.png",
                                id:"javaBeans",
                                jsURL:"dataIntegration/java/javaBeans.js",
                                requiresModules:"SCServer",
                                showDataSource:"false",
                                title:"Java Beans",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemDMI.java",
                                        url:"dataIntegration/java/SupplyItemDMI.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItem.java",
                                        url:"dataIntegration/java/SupplyItem.java"
                                    }
                                ],
                                description:"\n\t\t\t\n            SmartClient DataSource operations can be fulfilled by returning Java Beans (aka EJBs \n            or POJOs) from existing business logic.  When the SmartClient \n            <code>DSResponse.setData()</code> API is called, the Java objects are automatically translated \n            to JavaScript, transmitted to the browser, and provided to the requesting component.\n            See the sample implementation of the \"fetch\" operation in <code>SupplyItemDMI.java</code>.\n            \n            "
                            },
                            {
                                dataSource:"supplyItemDMI",
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/code_java.png",
                                id:"DMI",
                                jsURL:"dataIntegration/java/dmi.js",
                                requiresModules:"SCServer",
                                title:"DMI",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemDMI.java",
                                        url:"dataIntegration/java/SupplyItemDMI.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItem.java",
                                        url:"dataIntegration/java/SupplyItem.java"
                                    }
                                ],
                                description:"\n\t\t\t\n            Direct Method Invocation (DMI) allows mapping of DataSource operations directly \n            to Java methods, via XML configuration in a DataSource descriptor (*.ds.xml file).\n            The arguments of your Java methods are automatically populated from the inbound \n            request.  See the sample implementation in <code>SupplyItemDMI.java</code>.\n            \n            "
                            },
                            {
                                dataSource:"supplyItemHB",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_gear.png",
                                id:"autoTransactionsHB",
                                jsURL:"serverExamples/hibernate/autoTransactions/autoTransactions.js",
                                requiresModules:"SCServer, transactions",
                                showSkinSwitcher:false,
                                showSource:true,
                                title:"Auto Transactions",
                                descriptionHeight:"100",
                                description:"\n            SmartClient Hibernate DataSources participate fully in automatic transaction \n            management (Power and Enterprise Editions only).<p>\n            Drag multiple records from the left-hand grid to the right.  SmartClient will \n            send the updates to the server in a single queue. SmartClient Server will \n            automatically treat that queue as a single database transaction.  This is the\n            default behavior, and requires no code or configuration to enable it. However, if required,\n            very flexible, fine-grained control over transactions is possible,\n            through configuration, code or a combination of the two.\n            "
                            },
                            {
                                id:"uploadHB",
                                jsURL:"serverExamples/hibernate/upload/upload.js",
                                requiresModules:"SCServer",
                                title:"Upload",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"mediaLibraryHB",
                                        name:"mediaLibraryHB"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"MediaItem.java",
                                        url:"serverExamples/hibernate/upload/MediaItem.java"
                                    }
                                ],
                                descriptionHeight:"190",
                                description:"\n\t\tThis example uses a DynamicForm bound to a DataSource with a field of type <code>imageFile</code> to\n\t\tenable files to be uploaded and both a ListGrid and TileGrid to display \n\t\tthe existing records, via a shared ResultSet.\n\t\t<P>\n\t\tEnter a Title and select a local image-file to upload and click \"Save\" to upload the file.\n\t\tNote that the file-size is limited to 50K via the DataSourceField property \n\t\t<code>maxFileSize</code> (see the mediaLibrary tab below).\n\t\t<P>\n\t\t<code>imageFile</code> fields can either display a download/save icon-pair and title, or can render\n\t\tthe image directly inline.  Use the buttons below to switch between the TileGrid and \n\t\tListGrid views to see each of these behaviors.  Note that both components can render\n\t\teither UI for <code>imageFile</code> fields and will do so automatically, based on the value of \n\t\t<code>field.showFileInline</code>.\n\t\t\n\t\t"
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        isOpen:false,
                        title:"JPA",
                        description:"\n        The SmartClient Server's built-in support for JPA/JPA2 allows you to easily use your JPA annotated entities\n        in SmartClient's client-side widgets.<p/>\n    ",
                        children:[
                            {
                                dataSource:"supplyItemJPAAutoDerive",
                                id:"jpaConnector",
                                jsURL:"serverExamples/jpa/jpaConnector.js",
                                requiresModules:"SCServer",
                                title:"Auto Derivation",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemHB.java",
                                        url:"serverExamples/jpa/SupplyItemHB.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/persistence1.xml"
                                    }
                                ],
                                descriptionHeight:"175",
                                description:"\n                If you have pre-existing JPA entities, SmartClient can automatically derive fully functional\n                DataSources given just the Java classname of the mapped JPA entity.  The grid below is connected\n                to a JPA-managed entity via the simple declarations in supplyItemJPAAutoDerive.ds.xml - no other\n                configuration or Java code is required beyond the entity itself with the JPA mapping.\n                <p/>\n                To search, use the controls above the grid's header. Note that data paging is automatically\n                enabled. Scroll down to load data on demand. Click on the red icon next to each record to\n                delete it. Click on a record to edit it and click \"Add New\" to add a new record.  Note that the\n                editing controls are type sensitive: a date picker appears for the \"Next Shipment\" field, and\n                the \"Units\" field shows a picklist because its Java type is an Enum.\n                <p/>\n                DMI can be used to add business logic that takes place before and after JPA operations to\n                enforce security or to add additional data validation rules.\n            "
                            },
                            {
                                dataSource:"worldJPA2",
                                id:"jpa2Connector",
                                jsURL:"serverExamples/jpa/jpa2Connector.js",
                                requiresModules:"SCServer,serverCriteria",
                                title:"Advanced Filtering",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"World.java",
                                        url:"serverExamples/jpa/World.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/persistence2.xml"
                                    }
                                ],
                                descriptionHeight:"140",
                                description:"\n                Use the FilterBuilder to construct queries of arbitrary complexity.  The FilterBuilder,\n                and the underlying <code>AdvancedCriteria</code> system, support building queries with subclauses\n                nested to any depth. Add clauses to the query with the \"+\" icon and add nested subclauses\n                with the \"+()\" button. Click \"Filter\" to see the result in the ListGrid.\n                <p/>\n                Note that this example is backed by a JPA 2.0 DataSource. The SmartClient Server is \n                automatically generating the JPA Criteria Queries to implement the filters that\n                the FilterBuilder can assemble. This works adaptively and seamlessly with client-side \n                Advanced Filtering The generated <code>Criteria</code> query will yield exactly the same resultset \n                as the client-side filtering.  This means SmartClient is able to switch to client-side\n                filtering when its cache is full, giving a more responsive, more scalable application.\n                <p/>\n            "
                            },
                            {
                                id:"jpaRelationManyToOneSimple",
                                jsURL:"serverExamples/jpa/relations/jpaRelationManyToOneSimple.js",
                                requiresModules:"SCServer",
                                title:"Many-to-One Relation",
                                tabs:[
                                    {
                                        dataSource:"cityManyToOneSimpleJPA",
                                        name:"cityManyToOneSimpleJPA"
                                    },
                                    {
                                        dataSource:"countryManyToOneSimpleJPA",
                                        name:"countryManyToOneSimpleJPA"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CountryManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/relations/persistenceManyToOneSimple.xml"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n                SmartClient handles JPA Many-to-One relationships transparently, such as Cities within\n                Countries.  Declare a <code>foreignKey</code> field on the \"City\" DataSource to indicate the\n                use of the related JPA entity \"Country\".\n                <P>\n                The grid below shows Cities, but the Country name is automatically shown even though the\n                \"countryName\" is stored in the related JPA entity \"Country\".  Any fields from any number of\n                related entities can be automatically loaded this way.\n                <P>\n                Click to edit and change the Country of a City.  The list of Countries is automatically\n                loaded from the related JPA entity, along with their IDs.  \n                <P>\n                Changing the Country of a City sends the ID of the new Country back to the server, and\n                SmartClient automatically makes all the required JPA calls to persist the change. No\n                server-side code needs to be written beyond the JPA beans themselves and their\n                annotations.\n            "
                            },
                            {
                                id:"jpaRelationOneToMany",
                                jsURL:"serverExamples/jpa/relations/jpaRelationOneToMany.js",
                                requiresModules:"SCServer",
                                title:"One-to-Many Relation",
                                tabs:[
                                    {
                                        dataSource:"cityOneToManyJPA",
                                        name:"cityOneToManyJPA"
                                    },
                                    {
                                        dataSource:"countryOneToManyJPA",
                                        name:"countryOneToManyJPA"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityOneToMany.java",
                                        url:"serverExamples/jpa/relations/CityOneToMany.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryOneToMany.java",
                                        url:"serverExamples/jpa/relations/CountryOneToMany.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/relations/persistenceOneToMany.xml"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n                SmartClient handles JPA One-to-Many relationships transparently, such as a Country that has\n                multiple Cities.  Declare a collection field (<code>multiple:true</code>) on the \"Country\"\n                DataSource to indicate you want to load its list of Cities.\n                <P>\n                Click on a Country below. It's list of Cities is revealed without a separate round trip to the\n                server.  Cities can be now edited in the lower grid.  \n                <P>\n                When data is saved, all changes to the Country and its Cities are sent in one save\n                request, and SmartClient automatically makes all the required JPA calls to persist the\n                changes. No server-side code needs to be written beyond the JPA beans themselves and\n                their annotations.\n            "
                            },
                            {
                                id:"jpaIncludeFrom",
                                jsURL:"serverExamples/jpa/relations/jpaIncludeFrom.js",
                                requiresModules:"SCServer",
                                title:"Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromJPA",
                                        name:"cityIncludeFromJPA"
                                    },
                                    {
                                        dataSource:"countryIncludeFromJPA",
                                        name:"countryIncludeFromJPA"
                                    },
                                    {
                                        dataSource:"continentIncludeFromJPA",
                                        name:"continentIncludeFromJPA"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CountryManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ContinentManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/ContinentManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/relations/persistenceManyToOneSimple.xml"
                                    }
                                ],
                                descriptionHeight:"135",
                                description:"\n                Fields can be included from other, related DataSources by just declaring the name of the\n                DataSource and field to include, using the <code>includeFrom</code> property.\n                <p>\n                In the example below, a DataSource that stores Cities is shown.  It includes the\n                field \"Country Name\" from a related DataSource that stores Countries.  It also\n                includes the field \"Continent\" from an <i>indirectly</i> related DataSource that stores\n                Continents.  At the database layer, this related data is all being fetched using an efficient\n                SQL join.\n                <p>\n                Click to edit the \"Country Name\" field for a row - this field has been configured as a\n                ComboBox that edits which Country a City belongs to.  Try shifting a City to a Country on a\n                different Continent, and note how the \"Continent\" field automatically updates.  This happens\n                because <code>includeFrom</code> declarations are automatically applied when the server returns\n                data to update the UI.\n            "
                            },
                            {
                                id:"jpaIncludeFromDynamic",
                                jsURL:"serverExamples/jpa/relations/jpaIncludeFromDynamic.js",
                                requiresModules:"SCServer",
                                title:"Dynamic Field Include",
                                tabs:[
                                    {
                                        dataSource:"cityIncludeFromJPA",
                                        name:"cityIncludeFromJPA"
                                    },
                                    {
                                        dataSource:"countryIncludeFromJPA",
                                        name:"countryIncludeFromJPA"
                                    },
                                    {
                                        dataSource:"continentIncludeFromJPA",
                                        name:"continentIncludeFromJPA"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CityManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CityManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CountryManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/CountryManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ContinentManyToOneSimple.java",
                                        url:"serverExamples/jpa/relations/ContinentManyToOneSimple.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"persistence.xml",
                                        url:"serverExamples/jpa/relations/persistenceManyToOneSimple.xml"
                                    }
                                ],
                                descriptionHeight:"110",
                                description:"\n                Fields can be included from other, related DataSources on-demand, on a screen-specific basis,\n                by using the <code>includeFrom</code> attribute on a ListGridField.  \n                <p>\n                In the grid below, declarations in the DataSource cause the \"Country Name\" field to appear for\n                each City.  However the field \"Country Code\" is included on-demand, just for this grid, by\n                declaring the <code>includeFrom</code> attribute.\n            "
                            },
                            {
                                id:"uploadJPA",
                                jsURL:"serverExamples/jpa/upload/upload.js",
                                requiresModules:"SCServer",
                                title:"Upload",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"mediaLibraryJPA",
                                        name:"mediaLibraryJPA"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"MediaItem.java",
                                        url:"serverExamples/jpa/upload/MediaItem.java"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n\t\tThis example uses a DynamicForm bound to a DataSource with a field of type <code>imageFile</code> to\n\t\tenable files to be uploaded and for both a ListGrid< and  a TileGrid to display \n\t\tthe existing records, via a shared ResultSet.  It demonstrates SmartClient's binary \n        upload and download capabilities when using the built-in JPA DataSource.\n\t\t<P>\n\t\tEnter a Title and select a local image-file to upload and click \"Save\" to upload the file.\n\t\tNote that the file-size is limited to 50k via the DataSourceField property \n\t\t<code>maxFileSize</code> (see the mediaLibrary tab below).\n\t\t<P>\n\t\t<code>imageFile</code> fields can either display a download/save icon-pair and title, or can render\n\t\tthe image directly inline.  Use the buttons below to switch between the TileGrid and \n\t\tListGrid views to see each of these behaviors.  Note that both components can render\n\t\teither UI for \"imageFile\" fields and will do so automatically, based on the value of \n\t\t<code>field.showFileInline</code>.\n\t\t\n\t\t"
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        id:"transactionsFolder",
                        isOpen:false,
                        title:"Transactions",
                        description:"\n            SmartClient provides robust support for transactional applications.\n            <P>\n            Queueing makes combining operations together into a single\n            transaction extremely easy, for more efficient data loading and transactional saves.\n            <P>\n            Automatic Transaction Management support in the SmartClient Server, with \n            specific implementations for the built-in SQL and Hibernate DataSources, allows \n            for queued requests to be committed or rolled back as a single database transaction.\n            This feature is only available in Power and Enterprise editions.\n            <P>\n            Transaction Chaining allows for declarative handling of data dependencies\n            between operations submitted together in a queue.  This feature is only available\n            in Power and Enterprise editions.\n     ",
                        children:[
                            {
                                id:"queuing",
                                jsURL:"serverExamples/hibernate/queuing/queuing.js",
                                requiresModules:"SCServer",
                                title:"Simple Queueing",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"queuing_userHB",
                                        name:"queuing_user"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"queuing_order",
                                        name:"queuing_order"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"User.java",
                                        url:"serverExamples/hibernate/queuing/User.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"User.hbm.xml",
                                        url:"serverExamples/hibernate/queuing/User.hbm.xml"
                                    }
                                ],
                                description:"\n            Queueing allows any set of DataSource operations to be combined into a single HTTP\n            request, without requiring any special code to be written to transport the combined\n            inputs and outputs.\n            <p>\n            Click the \"Find Orders\" button and the example will load both the selected user's\n            details and all the orders associated with that user, as a single request.\n            Queueing works transparently to the components involved, so for example, scrolling down\n            in the orders grid causes data paging to be activated, exactly as though the grid had\n            done a fetch that was not combined into a queue.  \n            <P>\n            As queueing is transparent to components, a screen full of various components\n            which need to load data from different sources can participate in a queue without\n            any special component-specific code, and with no need to rework how data is\n            transferred if new components are added. Each component can be treated as though\n            it were standalone.\n            <P>\n            On the Server-side, queueing makes it simple to focus on secure, reusable data\n            operations and other services, which can then be accessed in arbitrary combinations\n            according to the data loading and saving requirements of particular screens, with\n            no need to write brittle, screen-specific server code.\n            <P>\n            Queueing works even when the operations are on different data providers (as in this \n            case, where the user details are coming from Hibernate and the order details are coming\n            from the SmartClient Server SQL provider).\n            "
                            },
                            {
                                dataSource:"supplyItem",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/database_gear.png",
                                id:"autoTransactions",
                                jsURL:"serverExamples/transactions/autoTransactions/autoTransactions.js",
                                requiresModules:"SCServer, transactions",
                                showSkinSwitcher:false,
                                showSource:true,
                                title:"Automatic Transaction Management",
                                description:"\n            Drag multiple records from the left-hand grid to the right.  SmartClient will \n            send the updates to the server in a single queue; SmartClient Server will \n            automatically treat that queue as a single database transaction.  This is the\n            default behavior, and requires no code or configuration to enable it. However, if required,\n            very flexible, fine-grained control over transactions is possible,\n            through configuration, code, or a combination of the two.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_row_insert.png",
                                id:"queuedAdd",
                                jsURL:"serverExamples/sql/queuedAdd/queuedMasterDetailAdd.js",
                                requiresModules:"SCServer, chaining",
                                title:"Master/Detail Add",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"queuedAdd_order",
                                        name:"order"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"queuedAdd_orderItem",
                                        name:"orderItem"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    }
                                ],
                                descriptionHeight:"100",
                                description:"\n            This example makes use of the SmartClient server's support for setting <code>DSRequest</code> \n            properties dynamically at runtime, based on responses to requests earlier in the \n            same queue.<p>\n            Edit the order header details, then add one or more lines.  When \"Save Order\" is \n            clicked, SmartClient will send multiple DataSource requests to the server. One to\n            save the Order Header, and one each for every line that has been entered. However, they will combined\n            into a single HTTP request, so that a transactional commit is possible. As\n            this sample is backed by SmartClient SQLDataSources, the queue is \n            automatically assembled into a single transaction (in Power Edition and above).<p>\n            New orders are given an automatically generated sequence value as a primary\n            key, which the \"orderItems\" use to establish a relationship with\n            their order.<P>\n            As a result of the <code>values</code> tag in the \"queuedAdd_orderItem\"\n            DataSource definition, the server will set the \"orderID\" property on each order\n            item to the unique sequence value assigned to the order header when it was\n            saved.<P>\n            This entire interaction is accomplished by simply re-using the capability of the\n            DataSource to add new records, without the need to write any server-side\n            code.  SQL DataSources are shown, but this interaction works with any DataSource\n            that can support CRUD operations, including custom DataSources and even a mix of\n            DataSources that use different storage systems.<P>\n            The <code>values</code> tag (and the similar \n            property <code>criteria</code> tag) are specified using the Velocity\n            Template Language, so the support is very flexible.<P>\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"massUpdateFSTrans",
                                ref:"massUpdate",
                                title:"Mass Update",
                                description:"\n        Click on any cell to start editing, then Tab or Down Arrow past the\n        last row in the grid to create a new row. Alternatively, click the \"Edit New\" button\n        to create a new data-entry row at the end of the grid.  When the \"Save\" button is clicked,\n        all changes (changed rows and new orows) are sent to the server in a queue, as a \n        single HTTP request.<p>\n        Because all of the changes arrive on the server at once, committing them as a single \n        transaction becomes possible. If the built-in SQL or Hibernate dataSources are being used,\n        and the Power edition or above is used, automatic transactional commit is the default.  Plus,\n        because SmartClient's queueing support is completely unobtrusive and requires no extra \n        code on either client or server, as soon there is an operation that can update a \n        single record, then it automatically becomes an operation that can participate in SmartClient\n        queued updates and automatic transactional commits.\n        "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_go.png",
                                id:"databoundDragCopyFST",
                                ref:"databoundDragCopy",
                                title:"Multi-Row Drag & Save",
                                descriptionHeight:"160",
                                description:"\n        Drag employee records into the Project Team Members list.  SmartClient recognizes that the \n        two DataSources are linked by a <code>foreignKey</code> relationship, and automatically uses that \n        relationship to populate values in the records that are added when a drop occurs. SmartClient \n        also populates fields based on current criteria and maps explicit title Fields as necessary.<p>\n        Multi-row selection is enabled on the Employees grid, so select multiple employees \n        can be selected and dragged to the Teams grid in one action.  As the grids are databound, this drag and \n        drop action will send data operations to the server automatically, using SmartClient \n        queueing to ensure all the updates arrive on the server together and, as this example\n        is backed by a SmartClient SQLDataSource, are committed together in a single database \n        transaction (in Power Edition and above).<p>\n         "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_go.png",
                                id:"row-drag-save-pivot",
                                jsURL:"serverExamples/transactions/manyToManyDragSave/manyToManyDragSave.js",
                                showSource:true,
                                title:"Many-to-Many Drag & Save",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"employees",
                                        title:"employees"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"teams",
                                        title:"teams"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"teamMembers2",
                                        title:"teamMembers2"
                                    }
                                ],
                                descriptionHeight:"160",
                                description:"\n            Select a team.  The ListGrid on the left will populate with employees not currently\n            in the selected team while the ListGrid on the right will populate with the employees who\n            are currently part of the selected team.  Drag employees from the left to the right\n            to add the selected employees to the team.\n            <p>This sample uses a traditional pivot table design for the many-to-many relationship\n            (many employees can be part of a team and an employee can be part of many teams).\n            <p>Multi-record selection is enabled on the Employees grid, so multiple employees can be\n            selected and dropped into a team in one action.  Because these two grids are databound, \n            SmartClient will automatically utilize queueing to ensure that all updates are performed on\n            the server in a single transaction.\n             "
                            },
                            {
                                descriptionHeight:"140",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_undo.png",
                                id:"rollback",
                                jsURL:"serverExamples/transactions/rollback/rollback.js",
                                requiresModules:"SCServer, transactions",
                                showSkinSwitcher:false,
                                showSource:true,
                                title:"Rollback",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"rbCountryTransactions",
                                        name:"rbCountryTransactions"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"worldDS",
                                        name:"worldDS"
                                    }
                                ],
                                description:"\n            SmartClient Server detects when a <code>DSRequest</code> that is part of a transaction fails,\n            and automatically rolls the complete transaction back.<p>\n            Change several records in the grid, then click \"Save\".  The underlying DataSource \n            specifies a <code>hasRelatedRecord</code> validation on the country name, looking up against \n            all the countries of the world. if country's name  is changed to something \n            non-existent, that validation will fail and the entire transaction will be rolled\n            back.  All of the changes will remain pending (the changed values will still be \n            shown in blue), and if the page is refreshed note that the data is \n            unchanged on the server.<p>\n            Correct the validation error and click \"Save\" again, the transaction will \n            be committed and the changes will be persisted.\n            "
                            },
                            {
                                descriptionHeight:"215",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/user_go.png",
                                id:"jdbcOperations",
                                jsURL:"serverExamples/transactions/jdbcOperations/jdbcOperations.js",
                                requiresModules:"SCServer, transactions",
                                showSkinSwitcher:false,
                                showSource:true,
                                title:"Transactional User Operations",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"countryTransactions",
                                        name:"countryTransactions"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"lastUpdated",
                                        name:"lastUpdated"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        name:"JDBCOperations.java",
                                        url:"serverExamples/transactions/jdbcOperations/JDBCOperations.java"
                                    }
                                ],
                                description:"\n            User-written operations, in this example, hand-crafted JDBC updates, can be \n            included in SmartClient automatic transactions, and will be committed or rolled \n            back alongside the normal SmartClient operations.<p>\n            Edit rows in the grid, then click \"Good Save\".  The changes will be \n            persisted to the database as part of a queue that also includes a user-written \n            JDBC update to a \"lastChanged\" table; the DMI method has been written to use \n            the SmartClient transaction (see the \"JS\" and \"JDBCOperations.java\" tabs). \n\t\t\tThe example will then fetch the current value from the lastUpdated table and display it\n\t\t\tin the blue label. Note that it has been updated.<p>\n            Now make further changes and click \"Bad Save\".  This causes a deliberately \n            broken version of the user-written JDBC update to be run, resulting in an SQL error\n            and a rolled-back transaction (with an error dialog referring to an unknown column).  \n            Note that the changes have not been saved (they are still presented in blue, to show that\n\t\t\tthey are pending) and the \"lastUpdated\" label has not changed. The entire transaction, \n\t\t\tboth SmartClient requests and the user-written query, have all been rolled back. Now click \"Good Save\",\n\t\t\tand see how the pending changes are persisted and the \"lastUpdated\" label changes to reflect this.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        isOpen:false,
                        title:"Custom DataSources",
                        description:"\n        Examples showing how to leverage the SmartClient Server to create partially or completely\n        customized DataSource implementations.\n    ",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/coffeebean.png",
                                id:"javabeanWizard",
                                jsURL:"serverExamples/other/vb_Javabean.js",
                                requiresModules:"SCServer",
                                showSkinSwitcher:false,
                                showSource:false,
                                title:"Javabean Wizard",
                                description:"\n            SmartClient's Visual Builder tool provides an extremely easy and completely codeless \n            way to create DataSources based on your existing Javabeans and POJOs.  Click\n            the \"New\" button, select \"JavaBean\", and enter the name of an existing Javabean \n            class.  Visual Builder will create a DataSource descriptor that is almost complete -\n            Simply connect it up to the custom DataSource implementation with the \n            <code>serverConstructor</code> property and it's ready to go.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/table_row_insert.png",
                                id:"customDataSource",
                                jsURL:"serverExamples/other/customDataSource/customDataSource.js",
                                requiresModules:"SCServer",
                                title:"Simple (Hardcoded)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"customDataSource_user",
                                        name:"user"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"UserDataSource.java",
                                        url:"serverExamples/other/customDataSource/UserDataSource.java"
                                    }
                                ],
                                descriptionHeight:"150",
                                description:"\n            This example shows an entirely custom DataSource.  It is created by extending \n            <code>BasicDataSource</code> and implementing the four core CRUD methods.  In this \n            case, a static List of Maps is maintained that are initialized with hard-coded data\n            every time the server starts (although this code could actually do anything). This \n            approach allows completely custom data operations to be simply plugged in to the\n            SmartClient Server framework.<p>\n            Note also that this code deals directly with Java Maps and \n            Lists, without worrying about format conversions, even custom code \n            leverages the SmartClient Server's automatic and transparent translation of request\n            data, from JSON to Java and back to JSON.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/objects_exchange.png",
                                id:"ormDataSource",
                                jsURL:"serverExamples/other/ormDataSource/ormDataSource.js",
                                requiresModules:"SCServer",
                                title:"ORM DataSource",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"ormDataSource_country",
                                        name:"ormDataSource_country"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ORMDataSource.java",
                                        url:"serverExamples/other/ormDataSource/ORMDataSource.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Country.java",
                                        url:"serverExamples/other/ormDataSource/Country.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Country.hbm.xml",
                                        url:"serverExamples/other/ormDataSource/Country.hbm.xml"
                                    }
                                ],
                                descriptionHeight:"200",
                                description:"\n            This example shows an entirely custom DataSource that connects SmartClient Server to\n            Hibernate. It is a very simple implementation created by extending <code>BasicDataSource</code>\n            and implementing the four core CRUD methods. In this case, a single DataSource\n            implementation handles a single Hibernate entity. Features such as data pagination, server-side sorting\n            and filtering are not implemented here.<p>\n            Creating an equivalent adapter for Toplink or Ibatis or some other ORM solution would\n            be a fairly simple matter of replacing the Hibernate-specific code in this example\n            with the equivalent specifics from the other ORM system.\n            <p>\n            As with the other custom DataSource examples, note how the <code>ORMDataSource.java</code> \n            code deals entirely in native Java objects - even entirely custom DataSources benefit\n            from SmartClient Server's robust and comprehensive Javascript<->Java translation.\n            "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/objects_exchange.png",
                                id:"reusableORMDataSource",
                                jsURL:"serverExamples/other/reusableORMDataSource/reusableORMDataSource.js",
                                requiresModules:"SCServer",
                                title:"Reusable ORM DataSource",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"reusableORMDataSource_supplyItem",
                                        name:"reusableORMDataSource_supplyItem"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"reusableORMDataSource_country",
                                        name:"reusableORMDataSource_country"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"ReusableORMDataSource.java",
                                        url:"serverExamples/other/reusableORMDataSource/ReusableORMDataSource.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"SupplyItemHB.java",
                                        url:"serverExamples/other/reusableORMDataSource/SupplyItemHB.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Country.java",
                                        url:"serverExamples/other/reusableORMDataSource/Country.java"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"Country.hbm.xml",
                                        url:"serverExamples/other/reusableORMDataSource/Country.hbm.xml"
                                    }
                                ],
                                descriptionHeight:"220",
                                description:"\n            This example shows an entirely custom DataSource that connects SmartClient Server to\n            Hibernate (note that this is just an example of the principles involved - SmartClient\n            Server's built-in Hibernate support is considerably more sophisticated than the\n            simple adapter shown here).  It is created by extending <code>BasicDataSource</code>\n            and implementing the four core CRUD methods.  In this case, the DataSource is connecting\n            requests to Hibernate <code>Criteria</code> queries and the <code>saveOrUpdate</code>\n            method.<p>\n            This implementation, though simple, is fully functional and could be used unchanged\n            in a real application.  It supports all four CRUD operations, as well as data pagination,\n            server-side sorting and filtering, client cache synchronization and \n            is actually persisting the data to a real database. In this case, the single DataSource\n            implementation handles two different entities using Reflection.\n            Note that it is a simplified version of the built-in connector that handles <code>AdvancedCriteria</code>\n            filtering.<p>\n            As with other custom DataSource examples, note how the <code>ORMDataSource.java</code>\n            code deals entirely in native Java objects. Even entirely custom DataSources benefit\n            from SmartClient Server's robust and comprehensive Javascript<->Java translation.\n            "
                            },
                            {
                                dataSource:"dynamicDSFields",
                                id:"editableServerSideDataSource",
                                jsURL:"serverExamples/other/editableServerSideDataSource/editableServerSideDataSource.js",
                                requiresModules:"SCServer",
                                title:"Editable Server-Side DataSource",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"DynamicDSFields.data.xml",
                                        url:"serverExamples/other/editableServerSideDataSource/dynamicDSFields.data.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"GeneratorSetup.java",
                                        url:"serverExamples/other/editableServerSideDataSource/GeneratorSetup.java"
                                    }
                                ],
                                descriptionHeight:"100",
                                description:"\n           \tThis example demonstrates a DataSource whose definition is stored in a SQL database rather\n\t\t    than in a static *.ds.xml file.  The fields of the DataSource can be editted in the grid\n\t\t    below. Pressing \"Reload\" shows a DynamicForm bound to the modified DataSource.\n\t\t    <P>\n\t\t    This pattern can be used to allow end users to dynamically change the definition of\n\t\t    DataSources in an application. For example, add new fields, or add additional validators\n\t\t    to existing fields.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Export",
                        description:"\n    Exporting Data from DataSources and DataBoundComponents.\n    ",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"exportFS",
                                ref:"export",
                                title:"Excel Export"
                            },
                            {
                                descriptionHeight:"160",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"formattedExportBuiltin",
                                jsURL:"grids/formattedExportBuiltin.js",
                                requiresModules:"SCServer",
                                title:"Formatted Export (Declared Formats)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDSExport",
                                        url:"grids/ds/worldSQLDSExport.ds.xml"
                                    }
                                ],
                                description:"\n            Exporting client-side data from a DataBoundComponent is possible with SmartClient using \n            declared format strings that are auto-converted to Excel format at export time. This means that\n            data is exported as seen in the component, including all of the effects of client-side formatters\n            (except for custom formatters written in Javascript code) and hilites.  Data exported this way\n            appears in the spreadsheet with the same formatting seen in the client, but retains its underlying\n            date, time or number value - this is usually the ideal scenario.\n            <p>In the example below, choose an export format from the list, decide \n            whether to download the results or view them in a window using the checkbox and \n            click the \"Export\" button.  \n            <p>Data is exported according to the filters and sort order on the grid and includes\n            the formatted values and field-titles as seen in the grid.\n        "
                            },
                            {
                                descriptionHeight:"160",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"formattedExport",
                                jsURL:"grids/formattedExport.js",
                                requiresModules:"SCServer",
                                title:"Formatted Export (Custom Formatters)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDSExport",
                                        url:"grids/ds/worldSQLDSExport.ds.xml"
                                    }
                                ],
                                description:"\n            Exporting client-side data from a DataBoundComponent is possible with SmartClient. This means \n            the data as seen in the component, including all of the effects of all client-side formatters \n            (including custom formatters) and hilites.  Data exported this way appears in the spreadsheet\n            as formatted text.  In the particular case of dates, this behavior can be overridden for  \n            each export, because exporting formatted dates can be undesirable - if the value \n            is exported as a formatted string, Excel is not able to recognize it as a date.<p>\n            Custom formatting for dates is often not necessary, because declared FormatStrings are flexible\n            enough to handle the majority of formatting requirements.  The example below shows a \n            (rather contrived) case where custom formatters really are required.\n            <p>In the example, choose an export format from the list, decide \n            whether to download the results or view them in a window using the checkbox and \n            click the \"Export\" button.  \n            <p>Data is exported according to the filters and sort order on the grid and includes\n            the formatted values and field-titles as seen in the grid.  Depending on how you set the\n            \"export dates as formatted strings\" checkbox, you will either see dates with some default\n            format, or the actual strings shown in the ListGrid.\n        "
                            },
                            {
                                descriptionHeight:"160",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"formattedServerExport",
                                jsURL:"grids/formattedServerExport.js",
                                requiresModules:"SCServer",
                                title:"Formatted Server-Side Export",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"worldDSExportWithFormats",
                                        url:"grids/ds/worldSQLDSExportWithFormats.ds.xml"
                                    }
                                ],
                                description:"\n            Using format strings declared in your DataSource descriptor files, it is possible to export \n            formatted values to spreadsheet packages like Excel purely server-side.  Declaring formats in the \n            DataSource descriptor also allows you to share formats between client-side and server-side code, so\n            a single declaration pervasively affects how a field is formatted, both client-side and for exports.\n            Data exported this way appears in the spreadsheet with the same formatting seen in the client\n            (assuming you are sharing the format client-side), but retains its underlying date, time or number \n            value - this is usually the ideal scenario.\n            <p>In the example below, choose an export format from the list, decide \n            whether to download the results or view them in a window using the checkbox and \n            click the \"Export\" button.  \n            <p>Data is exported according to the filters and sort order on the grid and includes\n            the formatted values and field-titles as seen in the grid.\n        "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"pdfExport",
                                jsURL:"serverExamples/export/pdfExport.js",
                                requiresModules:"SCServer",
                                title:"PDF Export",
                                tabs:[
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryDataDetail.js"
                                    }
                                ],
                                description:"\n            HTML content can be directly exported to a PDF object. Click the \"Export\" button to see the sample in action.<P>\n            Also by clicking the \"Show Print Preview\" button will generate a preview of what will be exported.\n        "
                            },
                            {
                                descriptionHeight:"125",
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"customExport",
                                jsURL:"serverExamples/sql/customExport/customExport.js",
                                requiresModules:"SCServer",
                                title:"Custom Export",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CustomExportDMI.java",
                                        url:"serverExamples/sql/customExport/CustomExportDMI.java"
                                    },
                                    {
                                        canEdit:"false",
                                        title:"worldDSExportCustom",
                                        url:"serverExamples/sql/customExport/worldDSExportCustom.ds.xml"
                                    }
                                ],
                                description:"\n            It is possible to produce a formatted export using DMI and affect the data server-side.\n            This example shows a normal export via a DMI in an <code>operationBinding</code>, where the DMI\n            enhances the exported data, formatting the \"Independence\" date field and \n            adding a calculated field \"gdppercapita\" on the server-side.\n            <p>Choose an Export format from the list, decide \n            whether to download the results or view them in a window using the checkbox and \n            click the \"Export\" button.  In this case, exporting to all formats is achieved via\n            <code>operationBindings</code> that specify the server DMI and, in the case of exports to JSON,\n            with the <code>exportAs</code> flag.  See the \"JS\" and \"worldDSExportCustom\"\n            tabs below.\n        "
                            },
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_out.png",
                                id:"customExportCustomResponse",
                                jsURL:"serverExamples/sql/customExport/customExportCustomResponse.js",
                                requiresModules:"SCServer",
                                title:"Custom Export (Custom Response)",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CustomExportCustomResponseDMI.java",
                                        url:"serverExamples/sql/customExport/CustomExportCustomResponseDMI.java"
                                    },
                                    {
                                        canEdit:"false",
                                        title:"supplyItemExport.ds.xml",
                                        url:"serverExamples/sql/customExport/supplyItemExport.ds.xml"
                                    }
                                ],
                                description:"\n            Entirely custom data can be exported via a DMI.  Click the button to issue a call\n            to <code>dataSource.exportData()</code> with an <code>operationId</code> that specifies a server DMI.  In\n            this example, the DMI method ignores all the regular export parameters, calls\n            <code>doCustomResponse()</code> and writes directly into the response output stream.\n        "
                            }
                        ]
                    },
                    {
                        dataSource:"supplyItemAudited",
                        id:"auditing",
                        jsURL:"serverExamples/auditing/auditing.js",
                        requiresModules:"SCServer",
                        title:"Auditing",
                        descriptionHeight:"177",
                        description:"\n                DataSources can automatically capture a log of changes by storing Records in a second\n            DataSource, called an \"audit DataSource\".\n            <p>\n            Click in the grid below to edit <code>supplyItem</code> records.  Underneath the separator\n            labeled \"Browse Audit Data\" is a second grid that shows the audit data captured as changes\n            are made to <code>supplyItem</code> records.\n            <p>\n            The audit DataSource is automatically created, and because it's a normal DataSource, you\n            can simply bind a ListGrid to it to view the audit data, and the audit data is also\n            searchable just like any DataSource.\n            <p>\n            Enabling this feature just requires setting <code>audit=\"true\"</code> in the DataSource\n            definition (.ds.xml file); the audit DataSource is created automatically, and for\n            SQLDataSource, even the underlying SQL table is automatically created so no other settings\n            are needed.\n         "
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/server_lightning.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Upload / Download",
                        description:"\n                Samples with Upload and Download files\n            ",
                        children:[
                            {
                                id:"upload",
                                jsURL:"serverExamples/sql/upload/upload.js",
                                requiresModules:"SCServer",
                                title:"Upload",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"mediaLibrary",
                                        name:"mediaLibrary"
                                    }
                                ],
                                descriptionHeight:"177",
                                description:"\n                This example uses a DynamicForm bound to a DataSource with a field of type <code>imageFile</code> to\n                enable files to be uploaded to both a ListGrid and a TileGrid, to display \n                the existing records, via a shared ResultSet.\n                <P>\n                Enter a Title and select a local image-file to upload and click \"Save\" to upload the file.\n                Note that the file-size is limited to 50k via the DataSourceField property \n                <code>maxFileSize</code> (see the mediaLibrary tab below).\n                <P>\n                <code>imageFile</code> fields can either display a download/save icon-pair and title, or can render\n                the image directly inline.  Use the buttons below to switch between the TileGrid and \n                ListGrid views to see each of these behaviors.  Note that both components can render\n                either UI for <code>imageFile</code> fields and will do so automatically, according to the value of \n                <code>field.showFileInline</code>.\n         "
                            },
                            {
                                dataSource:"supplyItem",
                                icon:"[ISO_DOCS_SKIN]/images/iconexperience/server_from_client.png",
                                id:"batchUpload",
                                jsURL:"serverExamples/other/batchUpload/batchUploadExample.js",
                                requiresModules:"SCServer, batchUploader",
                                title:"Batch Data Upload",
                                descriptionHeight:"100",
                                description:"\n                 This example shows the BatchUploader in action.  The BatchUploader encapsulates the \n                 end-to-end process of importing flat data into a DataSource, including validation of\n                 the import data, all without any client or server-side code required.\n                 <P>\n                 Follow the instructions in the example.  Note that the download link is provided to \n                 give suitable example data to try with the BatchUploader - this would not\n                 normally be downloaded.\n         "
                            },
                            {
                                id:"multiFileItem",
                                jsURL:"forms/dataTypes/multiFileItem.js",
                                title:"MultiFileItem",
                                description:"\n                     <P>This example shows the behavior of a multiple file upload widget.</P>\n                     <P>When using this, implement cascading record deletion server-side,\n                     either with the SQL schema for the table, or with DMI.</P>\n                 ",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        title:"uploadTest",
                                        url:"forms/dataTypes/uploadTest.ds.xml"
                                    },
                                    {
                                        canEdit:"false",
                                        title:"uploadedFiles",
                                        url:"forms/dataTypes/uploadedFiles.ds.xml"
                                    }
                                ]
                            },
                            {
                                dataSource:"supplyItemDownload",
                                descriptionHeight:"100",
                                id:"customDownload",
                                jsURL:"serverExamples/other/customDownload/customDownload.js",
                                requiresModules:"SCServer",
                                showDataSource:"true",
                                title:"Custom Download",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CustomDownload.java",
                                        url:"serverExamples/other/customDownload/CustomDownload.java"
                                    }
                                ],
                                description:"\n                This sample shows a custom download in which server-side Java logic generates data to be downloaded to the user. \n\t\t\t\tThis pattern can be used to download a file that is derived from DataSource data, such as exporting DataSource\n\t\t\t\tdata in a custom text format, or via third-party libraries that can produce some kind of structured file \n\t\t\t\t(such as an .rtf or .pdf file).\n                <P>\n                Select multiple records in the grid below and press the \"Download Descriptions\" button. This will produce a simple\n\t\t\t\ttext file with descriptions for all of the selected records.\n                "
                            },
                            {
                                dataSource:"customBinaryField",
                                id:"customBinaryField",
                                jsURL:"serverExamples/other/customBinaryField/customBinaryField.js",
                                requiresModules:"SCServer",
                                showDataSource:"true",
                                title:"Custom Binary Field",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"CustomBinaryFieldDataSource.java",
                                        url:"serverExamples/other/customBinaryField/CustomBinaryFieldDataSource.java"
                                    }
                                ],
                                description:"\n                Support for binary fields is built-in to the framework, including saving binary field values to SQL, Hibernate or JPA,\n\t\t\t\tfor downloading later.\n                <P>\n                This example shows how to implement a binary field if a Custom DataSource were being built, that does not use the built-in\n\t\t\t\tsupport for binary field persistence.\n                <P>\n                UI controls with built-in support for binary fields will then show controls for uploading and downloading files, which will \n\t\t\t\twork with any custom DataSource.\n                "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Real-Time Messaging",
                        description:"\n\t     RTM module provides low-latency, high data volume streaming\n         capabilities for latency-sensitive applications such as trading desks and operations\n         centers.\n\t    ",
                        children:[
                            {
                                dataSource:"stockQuotes",
                                id:"portfolioGrid",
                                jsURL:"serverExamples/other/rtm/stockQuotes.js",
                                requiresModules:"RealtimeMessaging",
                                showDataSource:"true",
                                title:"Portfolio Grid",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"stockQuotesData",
                                        url:"serverExamples/other/rtm/stockQuotes.data.xml"
                                    }
                                ],
                                descriptionHeight:"115",
                                description:"\n\t\t        The grid below is receiving simulated, real-time updates of stock data via the Real Time Messaging\n\t\t        (RTM) module.  The RTM module provides low-latency, high data volume streaming\n\t\t        capabilities for latency-sensitive applications such as trading desks and operations\n\t\t        centers.\n\t\t        <P>\n\t\t        Randomly generated updates will stream from the server for 90 seconds. Click \"Generate\n\t\t        More Updates\" to restart streaming.  \n\t\t        <P>\n\t\t        The RTM module can connect to Java Message Service (JMS) channels without writing any\n\t\t        code, or can be connected to custom messaging solutions with a simple adapter.\n\t\t        \n\t\t        "
                            },
                            {
                                dataSource:"stockQuotes",
                                id:"stockQuotesChart",
                                jsURL:"serverExamples/other/rtm/stockQuotesChart.js",
                                requiresModules:"Drawing,Analytics,RealtimeMessaging",
                                showDataSource:"true",
                                title:"Stock Chart",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        doEval:"false",
                                        title:"stockQuotesData",
                                        url:"serverExamples/other/rtm/stockQuotes.data.xml"
                                    }
                                ],
                                descriptionHeight:"120",
                                description:"\n\t\t        The chart below is receiving simulated, real-time updates to stock values via the Real\n\t\t        Time Messaging (RTM) module.  The RTM module provides low-latency, high data\n\t\t        volume streaming capabilities for latency-sensitive applications such as\n\t\t        trading desks and operations centers.\n\t\t        <P>\n\t\t        Randomly generated updates will stream from the server for 90 seconds. Click \"Generate\n\t\t        More Updates\" to restart streaming.  \n                <P>\n                Right click on the chart to switch the type of visualization.\n\t\t        \n\t\t        "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/database_lightning.png",
                        isOpen:false,
                        title:"Server Scripting",
                        description:"\n        Simple business logic and validation rules can be embedded directly in *.ds.xml files.  Use Java, or scripting\n\t\tlanguages such as Groovy or JavaScript.\n\t\t",
                        children:[
                            {
                                icon:"[ISO_DOCS_SKIN]/images/silkicons/user_orange.png",
                                id:"scriptingUserSpecificData",
                                jsURL:"serverExamples/scripting/userSpecificData/userSpecificData.js",
                                requiresModules:"SCServer,customSQL",
                                title:"User-Specific Data",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"scripting_cartItem",
                                        name:"scripting_cartItem"
                                    },
                                    {
                                        canEdit:"false",
                                        title:"supplyItem",
                                        url:"supplyItem.ds.xml"
                                    }
                                ],
                                descriptionHeight:"180",
                                description:"\n\t\t\t\tThis example shows the use of some simple user-written server code in conjunction with\n\t\t\t\tSmartClient databound dragging features and the SmartClient SQL DataSource and Server Scripting,\n\t\t\t\tto implement a simple, but secure, shopping cart example.\n\t\t\t\t<p>\n\t\t\t\tVia Server-side Scripting, the \"cartItem\" DataSource declares\n\t\t\t\tthat all DataSource operations should go through a custom server script\n\t\t\t\t<code>DataSource.script</code> before proceeding to read or write\n\t\t\t\tthe database. <code>DataSource.script</code> adds the current sessionId to the\n\t\t\t\t<code>DSRequest</code>, so that the user can only read and write to their own shopping cart.\n\t\t\t\t<P>\n\t\t\t\tDrag items from the left-hand grid to the right-hand grid.  Edit the quantity\n\t\t\t\tin the right-hand grid, or delete records.  Verify that the example\n\t\t\t\tis protecting each user's data from others by running the example in two different\n\t\t\t\tbrowsers (eg one Firefox and one IE). This creates distinct sessions with separate\n\t\t\t\tcarts.\n\t\t\t\t"
                            },
                            {
                                dataSource:"inlineScript_orderForm",
                                id:"inlineScriptValidation2",
                                jsURL:"dataIntegration/java/inlineScriptValidation.js",
                                requiresModules:"SCServer",
                                title:"Validation",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"StockItem",
                                        title:"StockItem"
                                    }
                                ],
                                description:"\n                        Use the \"Item Id\" ComboBox to select an item,  enter a very large quantity (999999)\n                        and press the \"Submit Order\" button.\n                        <P/>\n                        The resulting validation error is based upon a server-side condition specified in\n                        the validator using inline scripting. It checks a related DataSource (StockItem)\n                        to see if there is sufficient quantity in stock to fulfill the order.\n                        "
                            }
                        ]
                    },
                    {
                        id:"HrssFeed",
                        ref:"rssFeed",
                        requiresModules:"SCServer",
                        title:"HTTP Proxy",
                        descriptionHeight:"120",
                        description:"\n            The SmartClient Server includes an HTTP Proxy servlet which supports contacting REST and\n            WSDL web services as though they were hosted by a local web server, avoiding the \"same origin\n            policy\" restriction which normally prevents web applications from accessing remote\n            services.\n            <P>\n            The proxy is used automatically whenever an attempt to contact a URL on another host is performed. No\n            special code is needed.  In this example, a DataSource is configured to download the\n            Slashdot RSS feed, with no server-side code or proxy configuration required.\n            <P>\n            Configuration files allow for restricting proxying to specific\n            services that should be accessible to users through your application.\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                isOpen:false,
                requiresModules:"Drawing,Analytics",
                title:"Charting",
                description:"\n    SmartClient supports advanced charting components that work in all supported browsers,\n    including mobile browsers, without requiring plugins and without writing browser-specific\n    code.\n    <P>\n    SmartClient charting components are data-aware, and allow end users to switch both the type\n    of chart and the placement of data on the fly.\n",
                children:[
                    {
                        id:"simpleChart",
                        jsURL:"charts/simpleChart.js",
                        requiresModules:"Drawing,Charts",
                        title:"Simple Chart",
                        description:"\n            <p>Charts can be created with inline Javascript data.</p>\n\n            <p>Use the \"Chart Type\" selector below to see same data rendered by multiple different chart types.\n            Right-click on the chart to change the way data is visualized.</p>\n        "
                    },
                    {
                        id:"multiSeriesChart",
                        jsURL:"charts/multiSeriesChart.js",
                        requiresModules:"Drawing,Charts",
                        title:"Multi-Series Chart",
                        tabs:[
                            {
                                title:"Data",
                                url:"charts/multiSeriesData.js"
                            }
                        ],
                        description:"\n            <p>Multi-series charts can be viewed with \"stacked\" data (to show totals) or \"unstacked\" to compare\n            values from each series. The \"Area\" chart type defaults to using stacked data, while the \"Line\" chart\n            type defaults to unstacked. Use the default setting, or explicitly specify whether to stack data.</p>\n            <p>Use the \"Chart Type\" selector to see the same data rendered by multiple different chart types.\n            Right-click on the chart to change the way data is visualized.</p>\n        "
                    },
                    {
                        id:"gridCharting",
                        jsURL:"charts/gridChart.js",
                        requiresModules:"Drawing,Charts",
                        title:"Grid Charting",
                        description:"\n        Data loaded into a ListGrid can be charted with a single API call.\n        <P>\n        Use the \"Chart Type\" selector below to see the same data rendered by multiple different\n        chart types.  Right-click on the chart to change the way data is\n        visualized.\n        <P>\n        Edit the data in the grid to have the chart regenerated automatically.\n        "
                    },
                    {
                        dataSource:"productRevenue",
                        id:"dynamicDataCharting",
                        jsURL:"charts/dynamicData.js",
                        requiresModules:"Drawing,Charts",
                        title:"Dynamic Data",
                        descriptionHeight:"110",
                        description:"\n            <p>Charts can be created directly from a DataSource without a ListGrid.</p>\n            <p>Use the \"Time Period\" menu to change the criteria passed to the DataSource.</p>\n            <p>Use the \"Chart Type\" selector below to see same data rendered by multiple different chart types.\n            Right-click on the chart to change the way data is visualized.</p>\n        "
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                        isOpen:false,
                        requiresModules:"Drawing,Analytics",
                        title:"Multi-Axis",
                        description:"\n           ",
                        children:[
                            {
                                id:"dualAxisChartMA",
                                jsURL:"charts/multiAxis/dualAxisChartMA.js",
                                requiresModules:"Drawing,Charts",
                                title:"Dual Axis",
                                description:"\n            Dual axis charts can be used to create a single chart that shows two data sets with different units (for example: sales in dollars and total units shipped) and/or very different ranges (for example: gross revenue, profit).\n            "
                            },
                            {
                                id:"multiSeriesChartMA",
                                jsURL:"charts/multiAxis/multiSeriesChartMA.js",
                                requiresModules:"Drawing,Charts",
                                title:"Multi-Series",
                                description:"\n            Multi-Axis charts can show multiple series of data.  Here, a stacked column chart represents multi-series data denominated in percents, while multiple lines show a second multi-series data denominate in number of events</p>\n            "
                            },
                            {
                                id:"threePlusChartMA",
                                jsURL:"charts/multiAxis/threePlusChartMA.js",
                                requiresModules:"Drawing,Charts",
                                title:"3+ Axes",
                                description:"\n            3+ axis charts can be created to show 3 different types or ranges of data. Click on axes to rearrange them.</p>\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                        isOpen:false,
                        requiresModules:"Drawing,Analytics",
                        title:"Zoom Charts",
                        description:"\n        These examples show charts that allow users to intuitively\n        navigate very large datasets by providing the ability to zoom into parts of the data.\n        <p>\n        These charts intelligently choose which data labels to show based on whether the data set\n        covers a date range, a numeric range, or is simply a large set of unrelated values.\n        ",
                        children:[
                            {
                                descriptionHeight:"150",
                                id:"stockPriceCharting",
                                jsURL:"charts/stockChart.js",
                                requiresModules:"Drawing,Charts",
                                title:"Stock Prices",
                                tabs:[
                                    {
                                        title:"Stock prices DataSource",
                                        url:"charts/stockData.js"
                                    },
                                    {
                                        title:"Stock symbol DataSource",
                                        url:"charts/nasdaqSymbolData.js"
                                    }
                                ],
                                description:"\n            This example shows historical pricing data for several well-known stocks.\n            The miniature \"zoom chart\" (underneath the main chart) shows the complete dataset\n            and the main chart shows a subset of this range in more detail.\n            <P>\n            Use the zoom chart to choose a visible range in the main chart, either by dragging the\n            scrollbar or by dragging the range boundaries.\n            <p>\n            Note how the main chart will intelligently use different labels for the horizontal axis\n            depending on how much data is shown - years, months or days may be labeled depending on\n            how deeply you zoom.\n            "
                            },
                            {
                                id:"bodePlotCharting",
                                jsURL:"charts/bodePlots.js",
                                requiresModules:"Drawing,Charts",
                                title:"Measurement Data",
                                description:"\n            This example shows about 800 samples points from a continuous varying numerical\n            function (a <a href=\"http://en.wikipedia.org/wiki/Bode_plot\">Bode plot</a> of a\n            <a href=\"http://en.wikipedia.org/wiki/Chebyshev_filter\">Chebyshev filter</a>).\n            <P>\n            Use the zoom chart to focus in on small parts of the dataset and note that the line\n            remains smooth due to the large number of samples.  Horizontal gradations automatically\n            adjust to the range of the dataset being shown.\n            <P>\n            This zoom mode can be used to show various kinds of measurement data where many samples\n            are taken.\n            "
                            },
                            {
                                id:"populationCharting",
                                jsURL:"charts/populationChangeCharts.js",
                                requiresModules:"Drawing,Charts",
                                title:"City Populations",
                                tabs:[
                                    {
                                        title:"Population Data",
                                        url:"charts/populationData.js"
                                    }
                                ],
                                description:"\n            This example shows population change for 271 U.S. cities, sorted\n            alphabetically.\n            <P>\n            When there are too many cities visible, the chart will label every <b>N</b>th city depending\n            on available space.  This allows the end user to rapidly find the desired city since\n            the cities are sorted alphabetically; it would also work with other sorting orders such\n            as east to west or north to south.\n            "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/chart_bar.png",
                        isOpen:false,
                        requiresModules:"Drawing,Charts",
                        title:"Statistics",
                        children:[
                            {
                                id:"regressionLines",
                                jsURL:"charts/regressionLines.js",
                                requiresModules:"Drawing,Charts",
                                title:"Regression Lines",
                                tabs:[
                                    {
                                        title:"Time Series Data",
                                        url:"charts/regressionLinesData.js"
                                    }
                                ],
                                description:"\n            Scatter Charts support calculation of best-fit lines or curves, also known as \"trend\n            lines\" for time series data.  Shown below is a 3rd-degree polynomial regression curve.\n            <P>\n            Use the controls above the chart to switch between linear or polynomial regression and\n            change the regression degree.  Users can also enable or disable regression lines via the\n            default context menu.\n            "
                            },
                            {
                                id:"meanAndDeviation",
                                jsURL:"charts/meanAndDeviation.js",
                                requiresModules:"Drawing,Charts",
                                title:"Mean & Deviation",
                                description:"\n            Charts can show the average value for the data set as well as the standard deviation (a\n            measure of how far data points are from the average).\n            <P>\n            In the chart below, the bright green line shows the average, and the bright blue lines\n            show one standard deviation above average and below average.\n            <P>\n            Use the \"Regenerate Random Data\" button to see a new random data set.\n            "
                            },
                            {
                                dataSource:"productRevenue",
                                id:"errorBars",
                                jsURL:"charts/errorBars.js",
                                requiresModules:"Drawing,Charts",
                                title:"Error Bars",
                                description:"\n                Charts can display error bars showing intervals of values around each data point.\n            "
                            }
                        ]
                    },
                    {
                        id:"scatterPlotCharting",
                        jsURL:"charts/scatterPlot.js",
                        requiresModules:"Drawing,Charts",
                        title:"Scatter Plot",
                        descriptionHeight:"110",
                        description:"\n         Scatter plots can show two axes of continuous numeric data.  Multiple data sets can be plotted in different colors.</p>\n        "
                    },
                    {
                        id:"bubbleChart",
                        jsURL:"charts/bubbleChart.js",
                        requiresModules:"Drawing,Charts",
                        title:"Bubble Chart",
                        tabs:[
                            {
                                title:"Bubble Chart Data",
                                url:"charts/bubbleChartData.js"
                            }
                        ],
                        description:"\n        A Bubble Chart is a scatter plot where an additional data value is represented by the size of the shape drawn at the data point.\n        <p>\n        Bubble Charts can show multiple data series, optionally as different shapes.  Use the \"Use Multiple Shapes\" checkbox below to switch between modes.\n        "
                    },
                    {
                        dataSource:"productRevenue",
                        id:"drillCharting",
                        jsURL:"charts/drill.js",
                        requiresModules:"Drawing,Charts",
                        title:"Drill Up/Down",
                        descriptionHeight:"150",
                        description:"\n         Click on any swatch in the legend area of the chart below to focus in on data for just one Region.  \n         <p>\n         Click the red \"X\" button next to the \"Selected Region\" text to return to viewing all Regions.\n         <p>\n         Click on any segment in the stacked columns to likewise focus in on data for just the region that the segment represents.\n         <p>\n         This simple Drill-down interface is created by responding to the <code>valueClick</code> and <code>legendClick</code> events - see the code for details.\n        "
                    },
                    {
                        id:"chartingAnalytics",
                        ref:"analytics",
                        requiresModules:"Drawing,Analytics",
                        title:"CubeGrid Charting",
                        description:"\n       This example shows binding to a multi-dimensional dataset, where each cell value has a\n       series of attributes, called \"facets\", that appear as headers labelling the cell value.\n       Drag facets onto the grid to expand the cube model.<BR>\n       Right click on any cell and pick \"Chart\" to chart values by any two facets.\n       "
                    },
                    {
                        id:"logScalingChart",
                        jsURL:"charts/logScaling.js",
                        requiresModules:"Drawing,Charts",
                        title:"Log Scaling",
                        tabs:[
                            {
                                title:"Data",
                                url:"charts/sp500.js"
                            }
                        ],
                        description:"\n            Charts can use logarithmic scaling, which shows equal percentage changes as the same\n            difference in height.  This is useful for data that spans a very large range.\n        "
                    },
                    {
                        id:"dataPointsChart",
                        jsURL:"charts/dataPoints.js",
                        requiresModules:"Drawing,Charts",
                        title:"Interactive Data Points",
                        tabs:[
                            {
                                title:"Data",
                                url:"charts/animalData.js"
                            }
                        ],
                        description:"\n            <p>The data points in a chart can be interactive. Hover over a data point to see additional information,\n            and click to edit.</p>\n        "
                    },
                    {
                        dataSource:"productRevenue",
                        id:"addingElements",
                        jsURL:"charts/addingElements.js",
                        requiresModules:"Drawing,Charts",
                        title:"Adding Elements",
                        description:"\n            <p>This example show a column chart where the average value is computed and shown as a red line \n            by drawing the line when <code>chartBackgroundDrawn()</code> fires</p>\n        "
                    },
                    {
                        id:"ChartExportExamples",
                        isOpen:false,
                        requiresModules:"Drawing",
                        title:"Export",
                        description:"\n        Exporting Charts and other widgets based on DrawPane (such as Gauge) as images.\n        ",
                        children:[
                            {
                                id:"chartImageExport",
                                jsURL:"charts/imageExport.js",
                                requiresModules:"Drawing",
                                title:"Chart Image Export",
                                tabs:[
                                    {
                                        title:"Data",
                                        url:"charts/multiSeriesData.js"
                                    }
                                ],
                                description:"\n            FacetCharts and other widgets based on DrawPane (such as Gauge) can be exported to PNG and other\n            image formats.\n            <p>\n            Click the \"Download as Image\" button to save an image of the chart.  You can drag resize the\n            chart or right-click it to change how it displays so that you can obtain multiple\n            different images of the chart.\n            <p>\n            Click the \"Get Data URL\" button to display a PNG snapshot of the chart in the blue\n            bordered area to the right of the chart.  In most browsers, right-clicking the PNG will\n            then provide options to save it.  Note that this particular feature cannot be supported\n            for Internet Explorer 7 and earlier.\n            "
                            },
                            {
                                id:"chartPDFExport",
                                jsURL:"charts/pdfExport.js",
                                requiresModules:"Drawing",
                                title:"Chart PDF Export",
                                description:"\n            Screens can be exported as PDF (Portable Document Format) files.  This includes\n            FacetCharts and other widgets based on DrawPane (such as Gauge).\n            <p>\n            Press the \"View as PDF\" button below to view the exported version of the grid and chart\n            below in a new browser window.  Press \"Download as PDF\" to save the generated PDF to\n            disk.\n            <p>\n            Note that the grid is editable and the chart will react to changes in the grid, so you\n            can view or download several different PDFs showing the grid and chart.\n            "
                            },
                            {
                                id:"drawingExport",
                                jsURL:"serverExamples/export/drawingExport.js",
                                requiresModules:"SCServer,Drawing",
                                title:"Drawing Export",
                                description:"Select an export format and then click the Save button.\n            SmartClient will trigger the browser's save dialog allowing the DrawPane to be saved\n            as an image in the specified format."
                            }
                        ]
                    },
                    {
                        css:"charts/customHovers.css",
                        id:"chartCustomHovers",
                        jsURL:"charts/customHovers.js",
                        requiresModules:"Drawing,Charts",
                        title:"Custom Hovers",
                        tabs:[
                            {
                                title:"Data",
                                url:"charts/multiSeriesData.js"
                            }
                        ],
                        description:"\n    \tThis example shows a custom hover interaction built using the\n    \t<code>getNearestDrawnValue()</code> API to identify which point is nearest the mouse\n    \tcursor.\n    \t<p>\n    \tA bright blue marker is placed over the data point nearest the mouse.  Clicking anywhere on\n    \tthe chart shows the information the chart provides about the nearest data point in a\n    \tDetailViewer under the chart.\n    \t<p>\n    \tNote that FacetChart has two built-in hover behaviors - <code>showValueOnHover</code> and\n    \t<code>showDataPoints</code> (which can show custom hovers).  This example focuses on custom\n    \thovers.\n        "
                    }
                ]
            },
            {
                id:"DDdragDropExamples",
                isOpen:false,
                title:"Drag & Drop",
                description:"\n    Drag & drop services and built-in drag & drop interactions.\n",
                children:[
                    {
                        id:"DDdragListCopy",
                        ref:"dragListCopy",
                        title:"Drag list (copy)"
                    },
                    {
                        id:"DDdragListMove",
                        ref:"dragListMove",
                        title:"Drag list (move)"
                    },
                    {
                        id:"DDdragListSelect",
                        ref:"dragListSelect",
                        title:"Drag list (select)"
                    },
                    {
                        id:"DDdragTree",
                        ref:"dragTree",
                        title:"Drag tree (move)"
                    },
                    {
                        id:"DDdragTiles",
                        ref:"dragTiles",
                        title:"Drag tiles (move)"
                    },
                    {
                        id:"DDdataDraggingCopy",
                        isOpen:false,
                        title:"Data Binding",
                        description:"\n        Databound components have built-in dragging behaviors that operate on persistent\n        datasets.\n    ",
                        children:[
                            {
                                id:"DDtreeReparent",
                                ref:"treeReparent",
                                title:"Tree Reparent"
                            },
                            {
                                id:"DDtreeRecategorize",
                                ref:"treeRecategorize",
                                title:"Recategorize (Tree)"
                            },
                            {
                                id:"DDlistRecategorize",
                                ref:"listRecategorize",
                                title:"Recategorize (List)"
                            },
                            {
                                id:"DDrecategorizeTile",
                                ref:"recategorizeTiles",
                                title:"Recategorize (Tile)"
                            },
                            {
                                id:"DDdataboundDragCopy",
                                ref:"databoundDragCopy",
                                title:"Copy"
                            },
                            {
                                id:"DDpersistentReorderableListGrid",
                                ref:"persistentReorderableListGrid",
                                title:"Persistent Reorder (Grid)"
                            },
                            {
                                id:"DDpersistentReorderableTreeGrid",
                                ref:"persistentReorderableTreeGrid",
                                title:"Persistent Reorder (Tree)"
                            }
                        ]
                    },
                    {
                        id:"DDdragMenu",
                        ref:"dragMenu",
                        title:"Drag from Menu"
                    },
                    {
                        id:"DDdragMove",
                        ref:"dragMove",
                        title:"Drag move"
                    },
                    {
                        id:"DDdragReorder",
                        ref:"dragReorder",
                        title:"Drag reorder"
                    },
                    {
                        id:"DDdragTypes",
                        ref:"dragTypes",
                        title:"Drag types"
                    },
                    {
                        id:"DDdragCreate",
                        ref:"dragCreate",
                        title:"Drag create"
                    },
                    {
                        id:"DDdragEffects",
                        ref:"dragEffects",
                        title:"Drag effects"
                    },
                    {
                        id:"DDdragReposition",
                        ref:"dragReposition",
                        title:"Drag reposition"
                    },
                    {
                        id:"DDdragResize",
                        ref:"dragResize",
                        title:"Drag resize"
                    },
                    {
                        id:"DDdragTracker",
                        ref:"dragTracker",
                        title:"Drag tracker"
                    },
                    {
                        id:"DDdragPan",
                        ref:"dragPan",
                        title:"Drag pan"
                    },
                    {
                        id:"DDportalDraggingSamples",
                        isOpen:false,
                        ref:"portalDragRearrangeSamples",
                        title:"Portal Dragging"
                    },
                    {
                        id:"DDcrossWindowDragExamples",
                        isOpen:false,
                        title:"Cross-Window Drag",
                        description:"\n        HTML5 drag & drop support enabling drag & drop between browser windows.\n    ",
                        children:[
                            {
                                id:"DDnativeDragCreate",
                                ref:"nativeDragCreate",
                                title:"Native drag create"
                            },
                            {
                                id:"DDrecordsAcrossWindows",
                                ref:"recordsAcrossWindows",
                                title:"Records across Windows"
                            },
                            {
                                id:"DDportletAcrossWindows",
                                ref:"portalCrossWindowDrag",
                                title:"Portlet across Windows"
                            }
                        ]
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/brick.png",
                isOpen:false,
                title:"Control",
                description:"\n    Navigation and action controls.\n",
                children:[
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/brick.png",
                        isOpen:false,
                        title:"Buttons",
                        description:"\n    SmartClient buttons are visually appealing, easily skinned, and easy to use.\n    ",
                        children:[
                            {
                                id:"buttonAppearance",
                                jsURL:"actions/buttons/appearance.js",
                                showSkinSwitcher:true,
                                title:"Appearance",
                                description:"\n            Buttons come in three basic types: CSS buttons, single-image buttons, and \n            multiple-image stretch buttons.  All share a basic set of capabilities.\n        "
                            },
                            {
                                css:"actions/buttons/states.css",
                                id:"buttonStates",
                                jsURL:"actions/buttons/states.js",
                                title:"States",
                                description:"\n            Move the mouse over the buttons, and click and hold to see buttons in different\n            states.  Click \"Disable All\" to put all buttons in the disabled state.\n            \n            Edit the CSS style definitions to change the appearance of various states.\n        "
                            },
                            {
                                id:"buttonIcons",
                                jsURL:"actions/buttons/icons.js",
                                title:"Icons",
                                description:"\n            Click and hold on the \"Save\" button to see the icon change as the button goes\n            down.  Note that the binoculars icon does not change when the button goes down.\n            Click \"Disable Save\" to see the icon change to reflect disabled state.\n            \n            Button icons can be left or right oriented, and can optionally react to the\n            state of the button.\n        "
                            },
                            {
                                id:"buttonAutoFit",
                                jsURL:"actions/buttons/autoFit.js",
                                title:"Auto Fit",
                                description:"\n            Buttons can automatically size to accommodate the title and icon, and resize\n            automatically when the title is changed, notifying components around them they have\n            changed size.\n        "
                            },
                            {
                                id:"buttonRadioToggle",
                                jsURL:"actions/buttons/radioCheckbox.js",
                                title:"Radio / Toggle Behavior",
                                description:"\n            Click on the buttons for Bold, Italic, and Underline and note that they stick in a\n            down state.  Click on the buttons for left, center and right justify and note that\n            they are mutually exclusive.\n        "
                            }
                        ]
                    },
                    {
                        icon:"[ISO_DOCS_SKIN]/images/silkicons/application_osx.png",
                        isOpen:false,
                        showSkinSwitcher:"true",
                        title:"Menus",
                        description:"\n    Dynamic, appealing menus that can bind directly to data.\n    ",
                        children:[
                            {
                                id:"fullMenu",
                                jsURL:"actions/menus/appearance.js",
                                title:"Appearance",
                                description:"\n            Click \"File\" to see a typical File menu with icons, submenus, checks,\n            separators, disabled items, and keyboard shortcut hints.  Note the bevelled edge and\n            drop shadow.\n            "
                            },
                            {
                                id:"menuDynamicItems",
                                jsURL:"actions/menus/dynamicItems.js",
                                title:"Dynamic Items",
                                description:"\n            Open the \"File\" menu to see the \"New file in..\" item initially disabled.  Select a\n            project and note that the menu item has become enabled, changed title and changed\n            icon.  Pick \"Project Listing\" to show and hide the project list, and note the item\n            checks and unchecks itself.\n            "
                            },
                            {
                                id:"menuFullMenu",
                                ref:"fullMenu",
                                title:"Submenus",
                                description:"\n            Click \"File\" and navigate over \"Recent Documents\" or \"Export as...\" to see\n            submenus.\n            "
                            },
                            {
                                id:"menuColumns",
                                jsURL:"actions/menus/columns.js",
                                title:"Custom Columns",
                                description:"\n            Open the menu to see a standard column showing item titles, and an additional\n            column showing an option to close menu items. Clicking in the second column will\n            remove the item from the menu.\n            "
                            },
                            {
                                dataSource:"supplyCategory",
                                id:"treeBinding",
                                jsURL:"actions/menus/treeBinding.js",
                                title:"Tree Binding",
                                description:"\n            Click on \"Department\" or \"Category\" below to show hierarchical menus.  The\n            \"Category\" menu loads options dynamically from the \"SupplyCategory\" DataSource.\n            "
                            }
                        ]
                    },
                    {
                        id:"toolstrip",
                        jsURL:"actions/toolStrips.js",
                        title:"ToolStrips",
                        description:"\n        Click the icons at left to see \"radio\"-style selection.  Click the drop-down to see\n        font options.\n        ",
                        bestSkin:"Enterprise",
                        badSkins:[
                            "BlackOps",
                            "SilverWave"
                        ]
                    },
                    {
                        id:"toolstripVertical",
                        jsURL:"actions/toolStripVertical.js",
                        title:"ToolStrips (Vertical)",
                        description:"\n        Toolstrips can also be vertically aligned.\n        ",
                        bestSkin:"Enterprise",
                        badSkins:[
                            "BlackOps",
                            "SilverWave"
                        ]
                    },
                    {
                        descriptionHeight:"190",
                        id:"ribbonBar",
                        jsURL:"actions/ribbonBar.js",
                        title:"RibbonBar",
                        description:"\n        A RibbonBar is a customized ToolStrip which displays controls in separately titled\n        RibbonGroups.\n        <P>\n        The RibbonBar controls the overall presence, placement and text-alignment of each \n        group's title and these can be overridden for individual groups.  Groups can have\n        multiple rows of controls (<code>group.numRows</code>) and additional columns of rows \n        are automatically added when that number is exceeded.  Controls can also span\n        multiple rows (<code>control.rowSpan</code>).\n        <P>\n        The example below demonstrates a RibbonBar using it's default RibbonGroup and \n        <code>IconButton/IconMenuButton</code> classes to show various groups with different layouts. \n        Groups with horizontal and vertical buttons can be seen, some of each showing their \n        <code>menuIcons</code>.  The \"New\" and \"Undo\" buttons also have their <code>showMenuIconOver</code>\n        attribute set to false, which disables <code>mouseOver</code> styling on the <code>menuIcon</code>.\n        ",
                        bestSkin:"Enterprise",
                        badSkins:[
                            "BlackOps",
                            "SilverWave"
                        ]
                    },
                    {
                        id:"dialogs",
                        jsURL:"actions/dialogs.js",
                        showSkinSwitcher:true,
                        title:"Dialogs",
                        description:"\n        Click \"Confirm\", \"Ask\" or \"Ask For Value\" to show three of the pre-built, skinnable \n        SmartClient Dialogs for common interactions.  \n        "
                    },
                    {
                        id:"loginDialog",
                        jsURL:"actions/loginDialog.js",
                        showSkinSwitcher:true,
                        title:"Login Dialog",
                        description:"\n        Click \"Login\" to show SmartClient's built-in user login dialog.  Try entering both good\n        and bad credentials - user \"barney\", password \"rubble\" is a valid user.\n        "
                    },
                    {
                        id:"slider",
                        title:"Slider",
                        xmlURL:"actions/slider.js",
                        description:"\n        Move either Slider to update the other.  You can change the value by clicking and\n        dragging the thumb, clicking on the track, or using the keyboard (once you've focused\n        on one of the sliders)\n        "
                    },
                    {
                        id:"colorPicker",
                        jsURL:"actions/colorPicker.js",
                        showSkinSwitcher:true,
                        title:"ColorPicker",
                        description:"\n        Use the radio buttons to set which mode the ColorPicker initially appears in, and the \n        window position policy.  Click \"Pick a Color\" and select a color from either the simple\n        or complex picker - the \"Selected color\" label changes to reflect your selection.  The \n        ColorPicker also supports selecting semi-transparent colors - this is more easily seen\n        in a skin that shows a background image (eg BlackOps).\n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/overlays.png",
                isOpen:false,
                title:"Basics",
                description:"\n    Basic capabilities shared by all SmartClient visual components.\n",
                children:[
                    {
                        isOpen:false,
                        title:"Components",
                        description:"\n    Basic capabilities shared by all SmartClient visual components.\n",
                        children:[
                            {
                                id:"create",
                                jsURL:"basics/create.js",
                                title:"Create",
                                description:"\n        Click the button to create new cube objects.\n        "
                            },
                            {
                                id:"autodraw",
                                jsURL:"basics/draw.js",
                                title:"Draw",
                                description:"\n        Click the button to draw another Label component. The first Label is configured\n        to draw automatically.\n        "
                            },
                            {
                                id:"showAndHide",
                                jsURL:"basics/show.js",
                                title:"Show & Hide",
                                description:"\n        Click the buttons to show or hide the message.\n        "
                            },
                            {
                                id:"move",
                                jsURL:"basics/move.js",
                                title:"Move",
                                description:"\n        Click and hold the arrow to move the van. Click on the solid circle to return to\n        the starting position.\n        "
                            },
                            {
                                id:"resize",
                                jsURL:"basics/resize.js",
                                title:"Resize",
                                description:"\n        Click the buttons to expand or collapse the text box.\n        "
                            },
                            {
                                id:"layer",
                                jsURL:"basics/layer.js",
                                title:"Layer",
                                description:"\n        Click the buttons to move the draggable box above or below the other boxes.\n        "
                            },
                            {
                                jsURL:"basics/stack.js",
                                title:"Stack",
                                description:"\n        <code>HStack</code> and <code>VStack</code> containers manage the stacked positions\n        of multiple member components.\n        "
                            },
                            {
                                jsURL:"basics/layout.js",
                                title:"Layout",
                                description:"\n        <code>HLayout</code> and <code>VLayout</code> containers manage the stacked positions and\n        sizes of multiple member components. Resize the browser window to reflow these layouts.\n        "
                            },
                            {
                                doEval:"false",
                                id:"inlineComponents",
                                iframe:"true",
                                title:"Inline components",
                                url:"inlineComponents/inlineComponents.html",
                                tabs:[
                                    {
                                        title:"cssLayout.css",
                                        url:"inlineComponents/cssLayout.css"
                                    }
                                ],
                                description:"\n        SmartClient GUI components are assembled from the same standard HTML and CSS as\n        plain old web pages. So SmartClient controls can be added above, below, inline,\n        and inside existing web page elements.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"HTML",
                        description:"\n    Mixing SmartClient components with HTML pages, chunks, and elements.\n",
                        children:[
                            {
                                id:"htmlInlineComponents",
                                ref:"inlineComponents",
                                title:"Inline Components"
                            },
                            {
                                title:"Back Button",
                                description:"\n        SmartClient supports browser history management.  Click the browser's Back button to go\n        to a previous example, and click forward to return to this example.  Even\n        navigate off the SmartClient site and navigate back.  SmartClient's History module\n        allows for picking which application events create history entries.\n        "
                            },
                            {
                                id:"htmlFlow",
                                jsURL:"html/htmlFlow.js",
                                title:"HTMLFlow",
                                xmlURL:"html/htmlFlow.xml",
                                description:"\n        The <code>HTMLFlow</code> component displays a chunk of standard HTML in a free-form,\n        flowable region.\n        "
                            },
                            {
                                id:"htmlPane",
                                jsURL:"html/htmlPane.js",
                                title:"HTMLPane",
                                xmlURL:"html/htmlPane.xml",
                                description:"\n        The <code>HTMLPane</code> component displays a chunk or page of standard HTML in a\n        sizeable, scrollable pane.\n        "
                            },
                            {
                                id:"label",
                                jsURL:"html/htmlLabel.js",
                                title:"Label",
                                xmlURL:"html/htmlLabel.xml",
                                description:"\n        The <code>Label</code> component adds alignment, text wrapping, and icon support for\n        small chunks of standard HTML.\n        "
                            },
                            {
                                id:"RichTextEditor",
                                jsURL:"html/richTextEditor.js",
                                requiresModules:"RichTextEditor",
                                title:"Editing HTML",
                                xmlURL:"html/richTextEditor.xml",
                                description:"\n\t\t\n       The <code>RichTextEditor</code> supports editing of HTML with a configurable set of\n       styling controls.\n        \n\t   "
                            },
                            {
                                id:"img",
                                jsURL:"html/htmlImg.js",
                                title:"Img",
                                description:"\n        The <code>Img</code> component displays images in the standard web formats\n        (png, gif, jpg) and other image formats supported by the web browser.\n        "
                            },
                            {
                                id:"dynamicContents",
                                jsURL:"html/htmlDynamic1.js",
                                title:"Dynamic HTML (inline)",
                                description:"\n        Embed JavaScript expressions inside chunks of HTML to create simple dynamic elements.\n        "
                            },
                            {
                                id:"setContents",
                                jsURL:"html/htmlDynamic2.js",
                                title:"Dynamic HTML (set)",
                                description:"\n        Click the buttons to display different chunks of HTML.\n        "
                            },
                            {
                                id:"loadImages",
                                jsURL:"html/htmlLoadImg.js",
                                title:"Load images",
                                description:"\n        Click the buttons to load different images.\n        "
                            },
                            {
                                id:"loadHtmlChunks",
                                jsURL:"html/htmlLoadChunks.js",
                                title:"Load HTML chunks",
                                description:"\n        Click the buttons to load different chunks of HTML.\n        "
                            },
                            {
                                id:"loadHtmlPages",
                                jsURL:"html/htmlLoadPages.js",
                                title:"Load HTML pages",
                                description:"\n        Click the buttons to display different websites.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Interaction",
                        description:"\n        \n        Basic interactive component capabilities.\n        <br>\n        <br>\n        SmartClient components provide hundreds of hooks for event handlers, including\n        all the standard mouse, keyboard, and communication events.\n        \n    ",
                        children:[
                            {
                                id:"customMouseEvents",
                                jsURL:"interact/mouseEvents.js",
                                title:"Mouse events",
                                description:"\n\t\n        Mouse over the blue square to see the color respond to that position.  Click and hold\n        to see a fade.  If a mousewheel is present, roll up and down to change size.\n        SmartClient components support the standard mouse events in addition to custom events\n        like <code>mouseStillDown</code>.\n        \n        "
                            },
                            {
                                id:"customDrag",
                                jsURL:"interact/dragEvents.js",
                                title:"Drag events",
                                description:"\n        Click and drag the pawn over \"Show Drop Reticle\" to see a simple custom drag and drop\n        interaction.\n        "
                            },
                            {
                                css:"interact/hover.css",
                                id:"customHovers",
                                jsURL:"interact/hover.js",
                                showSkinSwitcher:true,
                                title:"Hovers / Tooltips",
                                description:"\n        Hover over the button, the image, the \"Interesting Facts\" field of the grid and the\n        \"Severity\" form label to see various hovers.\n        "
                            },
                            {
                                id:"contextMenus",
                                jsURL:"interact/contextmenu.js",
                                showSkinSwitcher:true,
                                title:"Context menus",
                                description:"\n        Right click (or option-click on Macs) on the Yin Yang image to access a context menu.\n        Click on the \"Widget\" button to access the identical menu.\n        "
                            },
                            {
                                id:"interactionFieldEnableDisable",
                                ref:"fieldEnableDisable",
                                title:"Enable / Disable"
                            },
                            {
                                id:"focus",
                                jsURL:"interact/focus.js",
                                title:"Focus & Tabbing",
                                description:"\n        Press the Tab key to cycle through the tab order starting from the blue\n        piece.  Then drag reorder either piece, click on the leftmost piece and use Tab to\n        cycle through again. Tab order is automatically updated to reflect the visual order.\n        "
                            },
                            {
                                id:"cursors",
                                jsURL:"interact/cursor.js",
                                title:"Cursors",
                                description:"\n        Mouse over the draggable labels for a 4-way move cursor.  Move over drag resizeable\n        edges to see resize cursors.  Mouse over the \"Save\" button to see the hand cursor,\n        which is not shown if the \"Save\" button is disabled.\n        "
                            },
                            {
                                id:"keyboardEvents",
                                jsURL:"interact/keyboard.js",
                                title:"Keyboard events",
                                description:"\n        Click the \"Move Me\" label, then use the arrow keys to move it around.  Hold down keys to see the\n        component respond to key repetition. SmartClient unifies keyboard event handling across browsers.\n        "
                            },
                            {
                                id:"modality",
                                jsURL:"interact/modality.js",
                                showSkinSwitcher:true,
                                title:"Modality",
                                description:"\n        Click on \"Show Window\" to show a modal window.  Note that the \"Touch This\" button no\n        longer shows rollovers or an interactive cursor, nothing outside the window can be\n        clicked. Clicks outside the window cause the window to flash and tabbing remains in a\n        closed loop, cycling through only the contents of the window.\n        "
                            }
                        ]
                    },
                    {
                        id:"printing",
                        jsURL:"basics/printing.js",
                        showSkinSwitcher:true,
                        title:"Printing",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"worldDS",
                                url:"grids/ds/worldSQLDS.ds.xml"
                            }
                        ],
                        descriptionHeight:"130",
                        description:"\n        SmartClient provides comprehensive support for rendering UI into a print-friendly\n        fashion.  Click the \"Print Preview\" button and note the following things:\n        <ul>\n        <li>All components have simplified appearance (eg gradients omitted) to be legible in \n        black and white\n        <li>The ListGrid had a scrollbar because it wasn't big enough to show all records, \n        but the printable view shows all data\n        <li>Buttons and other interactive controls that are not meaningful in print view are omitted\n        \n        "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/layers.png",
                isOpen:false,
                title:"Effects",
                description:"\n    Effects for creating a polished, branded, appealing application.\n    <BR>\n    <BR>\n    SmartClient supports rich skinning and styling capabilities, drag and drop interactions,\n    and built-in animations.\n",
                children:[
                    {
                        id:"dragDropExamples",
                        isOpen:false,
                        title:"Drag & Drop",
                        description:"\n    Drag & drop services and built-in drag & drop interactions.\n",
                        children:[
                            {
                                id:"dragListCopy",
                                jsURL:"dragdrop/dragListCopy.js",
                                showSkinSwitcher:true,
                                title:"Drag list (copy)",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"dragdrop/dragList_data.js"
                                    }
                                ],
                                description:"\n        Drag and drop to copy items from the first list to the second list.\n        Drag over the top or bottom edge of a scrolling list to scroll\n        in that direction before dropping.\n        "
                            },
                            {
                                id:"dragListMove",
                                jsURL:"dragdrop/dragListMove.js",
                                showSkinSwitcher:true,
                                title:"Drag list (move)",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"dragdrop/dragList_data.js"
                                    }
                                ],
                                description:"\n        Drag and drop to move items within or between the lists.\n        Drag over the top or bottom edge of a scrolling list to scroll\n        in that direction before dropping.\n        "
                            },
                            {
                                id:"dragListSelect",
                                jsURL:"dragdrop/dragListSelect.js",
                                showSkinSwitcher:true,
                                title:"Drag list (select)",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"dragdrop/dragList_data.js"
                                    }
                                ],
                                description:"\n        Drag to select items in the first list. The second list will\n        mirror the selection.\n        "
                            },
                            {
                                id:"dragTree",
                                jsURL:"dragdrop/dragTreeMove.js",
                                showSkinSwitcher:true,
                                title:"Drag tree (move)",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"dragdrop/dragTree_data.js"
                                    }
                                ],
                                description:"\n        Drag and drop to move parts and folders within and between the trees.\n        Open a closed folder by pausing over it during a drag interaction\n        (aka \"spring loaded folders\").\n        "
                            },
                            {
                                id:"dragTiles",
                                jsURL:"dragdrop/dragTilesMove.js",
                                showSkinSwitcher:true,
                                title:"Drag tiles (move)",
                                tabs:[
                                    {
                                        title:"animalData",
                                        url:"grids/data/animalData2.js"
                                    }
                                ],
                                description:"\n        Drag and drop animals from the ListGrid on the left to the TileGrid on the right. \n        Animals can also be dragged from the TileGrid back to the ListGrid.\n        "
                            },
                            {
                                id:"dataDraggingCopy",
                                isOpen:false,
                                title:"Data Binding",
                                description:"\n        Databound components have built-in dragging behaviors that operate on persistent\n        datasets.\n    ",
                                children:[
                                    {
                                        dataSource:"employees",
                                        id:"treeReparent",
                                        jsURL:"databind/drag/treeReparent.js",
                                        title:"Tree Reparent",
                                        description:"\n            Dragging employees between managers in this tree automatically saves the new\n            relationship to a DataSource, without writing any code.  Make changes, then \n            reload the page. The changes persist.\n            "
                                    },
                                    {
                                        dataSource:"supplyCategory",
                                        id:"treeRecategorize",
                                        jsURL:"databind/drag/treeRecategorize.js",
                                        title:"Recategorize (Tree)",
                                        tabs:[
                                            {
                                                title:"supplyItem",
                                                url:"supplyItem.ds.xml"
                                            }
                                        ],
                                        description:"\n            Dragging items from the list and dropping them on categories in the tree automatically\n            re-categorizes the item, without any code needed.  Make changes, then \n            reload the page. The changes persist.  This behavior is (optionally) automatic where\n            SmartClient can establish a relationship, via foreign key, between the DataSources\n            two components that are bound to it.\n            "
                                    },
                                    {
                                        dataSource:"supplyItem",
                                        id:"listRecategorize",
                                        jsURL:"databind/drag/listRecategorize.js",
                                        title:"Recategorize (List)",
                                        description:"\n            The two lists are showing items in different categories.  Drag items from one list to\n            another to automatically recategorize the items without writing any code.  Make\n            changes, then reload the page. The changes persist.\n            "
                                    },
                                    {
                                        id:"recategorizeTiles",
                                        jsURL:"dragdrop/recategorizeTile.js",
                                        showSkinSwitcher:true,
                                        title:"Recategorize (Tile)",
                                        tabs:[
                                            {
                                                canEdit:"false",
                                                title:"animalsDS",
                                                url:"grids/ds/animalsSQLDS.ds.xml"
                                            }
                                        ],
                                        description:"\n            Drag and drop animals between the grids in either direction, and the status of the dropped tile will change to \n            match the filtered status of the TileGrid in which it was dropped. Select different values\n            in the drop down lists above each TileGrid to change the animals that will appear in each grid.\n            "
                                    },
                                    {
                                        dataSource:"employees",
                                        id:"databoundDragCopy",
                                        jsURL:"databind/drag/listCopy.js",
                                        showSkinSwitcher:true,
                                        title:"Copy",
                                        tabs:[
                                            {
                                                title:"teamMembers",
                                                url:"teamMembers.ds.xml"
                                            }
                                        ],
                                        descriptionHeight:"155",
                                        description:"\n            Drag employee records into the Project Team Members list.  SmartClient recognizes that the \n            two DataSources are linked by a foreign key relationship, and automatically uses that \n            relationship to populate values in the record that is added when the drop occurs. SmartClient\n            also populates fields based on current criteria and maps explicit title Fields as \n            necessary.<p>\n            In this example, note that SmartClient is automatically populating all three\n            of the fields in the teamMembers DataSource, even though none of those fields are present \n            in the employees DataSource being dragged from.  Change the \"Team for Project\" select \n            box, then try dragging employees across. Note that the Project Code column is being \n            correctly populated for the dropped records.\n            "
                                    },
                                    {
                                        dataSource:"employees",
                                        id:"persistentReorderableListGrid",
                                        jsURL:"grids/queuing/persistentReorderableListGrid.js",
                                        title:"Persistent Reorder (Grid)",
                                        descriptionHeight:"105",
                                        description:"\n            Click on a record to select it, or use Ctrl-Click on several records to select multiple records.  Click on one of \n            the selected records and drag the selection to a new position within the ListGrid.  Release the mouse once the\n            selection is in the desired position to drop the selected records.<br><br>\n            On the ISC Developer Console, RPC tab, check the \"Track RPCs\" checkbox to be able to\n            monitor DSRequests.  Notice that there is only one request sent per drag &amp; drop operation.\n            "
                                    },
                                    {
                                        dataSource:"employees",
                                        id:"persistentReorderableTreeGrid",
                                        jsURL:"grids/queuing/persistentReorderableTreeGrid.js",
                                        title:"Persistent Reorder (Tree)",
                                        descriptionHeight:"105",
                                        description:"\n            Click on a node to select it, or, use Ctrl-Click on several nodes to select multiple nodes.  Click on one of the \n            selected nodes and drag the selection to a new position among their siblings.  Release the mouse once the\n            selection is in the desired position to drop the selected nodes.<br><br>\n            On the ISC Developer Console, RPC tab, check the \"Track RPCs\" checkbox to be able to\n            monitor DSRequests.  Notice that there is only one request sent per drag &amp; drop operation.\n            "
                                    }
                                ]
                            },
                            {
                                id:"dragMenu",
                                jsURL:"dragdrop/dragFromMenu.js",
                                showSkinSwitcher:true,
                                title:"Drag from Menu",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"dragdrop/dragList_data.js"
                                    }
                                ],
                                description:"\n        Open the parts menu and drag parts from the menu onto the grid.\n        Menus support all the drag and drop behaviors supported by grids.\n        "
                            },
                            {
                                id:"dragMove",
                                jsURL:"dragdrop/dragMove.js",
                                title:"Drag move",
                                description:"\n        Drag and drop to move pieces between the boxes. The green box sets a thicker green\n        \"drop line\" indicator to match its border. The blue box shows a \"drag placeholder\"\n        outline at the original location of the dragged object while dragging.\n        "
                            },
                            {
                                id:"dragReorder",
                                jsURL:"dragdrop/dragReorder.js",
                                title:"Drag reorder",
                                description:"\n        Drag and drop to rearrange the order of the pieces.\n        "
                            },
                            {
                                id:"dragTypes",
                                jsURL:"dragdrop/dragTypes.js",
                                title:"Drag types",
                                description:"\n        Drag and drop to move pieces between the three boxes.\n        The gray box accepts any piece.\n        The blue and green boxes accept pieces of the same color only.\n        "
                            },
                            {
                                id:"dragCreate",
                                jsURL:"dragdrop/dragCreate.js",
                                title:"Drag create",
                                description:"\n        Drag the large cubes into the boxes to create new small cubes.\n        The blue, yellow, and green boxes accept cubes with the same color only.\n        The gray box accepts any color.\n        Right-click on the small cubes to remove them from the boxes.\n        "
                            },
                            {
                                id:"dragEffects",
                                jsURL:"dragdrop/dragEffects.js",
                                title:"Drag effects",
                                description:"\n        Click and drag to move the labels.\n        "
                            },
                            {
                                id:"dragReposition",
                                jsURL:"dragdrop/dragReposition.js",
                                title:"Drag reposition",
                                description:"\n        Click and drag to move the piece.\n        "
                            },
                            {
                                id:"dragResize",
                                jsURL:"dragdrop/dragResize.js",
                                title:"Drag resize",
                                description:"\n        Click and drag on the edges of the labels to resize.\n        "
                            },
                            {
                                id:"dragTracker",
                                jsURL:"dragdrop/dragTracker.js",
                                title:"Drag tracker",
                                description:"\n        Drag and drop the pieces onto the box.\n        "
                            },
                            {
                                id:"dragPan",
                                jsURL:"dragdrop/dragPan.js",
                                title:"Drag pan",
                                description:"\n        Click and drag to pan the image inside its frame.\n        "
                            },
                            {
                                id:"portalDraggingSamples",
                                isOpen:false,
                                ref:"portalDragRearrangeSamples",
                                title:"Portal Dragging"
                            },
                            {
                                id:"crossWindowDragExamples",
                                isOpen:false,
                                title:"Cross-Window Drag",
                                description:"\n        HTML5 drag & drop support enabling drag & drop between browser windows.\n    ",
                                children:[
                                    {
                                        id:"nativeDragCreate",
                                        jsURL:"dragdrop/nativeDragCreate.js",
                                        title:"Native drag create",
                                        description:"\n            Drag the large cubes into the boxes to create new small cubes.\n            The blue, yellow, and green boxes accept cubes with the same color only.\n            The gray box accepts any color.\n            <p>\n            Try opening this sample in two different tabs or browser windows and dragging\n            a cube from one window to the drop boxes of the other.\n            "
                                    },
                                    {
                                        id:"recordsAcrossWindows",
                                        jsURL:"dragdrop/recordsAcrossWindows.js",
                                        title:"Records across Windows",
                                        tabs:[
                                            {
                                                title:"exampleData",
                                                url:"dragdrop/dragList_data.js"
                                            }
                                        ],
                                        description:"\n            This sample demonstrates dragging data between two different browser windows.\n            <p>\n            Open a second browser window (or browser tab) with this same sample running.  Drag\n            records from the grid and drop them on the grid shown in the other browser.\n            <p>\n            Depending on your browser and operating system, it may be necessary to hover over the\n            second browser tab or over an application icon to cause the tab or browser to come to\n            the front so you can drop on it.\n            <p>\n            Data is transferred directly from one browser instance to another using HTML5\n            techniques.  This allows you to build applications that span multiple browser windows or\n            tabs, and makes it easier to take advantage of multiple physical screens.\n            "
                                    },
                                    {
                                        id:"portletAcrossWindows",
                                        ref:"portalCrossWindowDrag",
                                        title:"Portlet across Windows"
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Animation",
                        description:"\n    Animation services and built-in animation effects.\n",
                        children:[
                            {
                                id:"animateTree",
                                jsURL:"animate/animateTree.js",
                                showSkinSwitcher:true,
                                title:"Tree Folders",
                                description:"\n        Click the open/close icon for any folder.\n        ",
                                tabs:[
                                    {
                                        title:"exampleData",
                                        url:"animate/animateTreeData.js"
                                    }
                                ]
                            },
                            {
                                id:"windowMinimize",
                                jsURL:"animate/animateMinimize.js",
                                showSkinSwitcher:true,
                                title:"Window Minimize",
                                description:"\n        Click on the minimize button (round button in header with flat line).\n        "
                            },
                            {
                                id:"animateSections",
                                jsURL:"animate/animateSections.xml",
                                showSkinSwitcher:true,
                                title:"Section Reveal",
                                description:"\n        Click on any section header to expand/collapse sections.\n        "
                            },
                            {
                                id:"animateLayout",
                                jsURL:"animate/animateLayout.js",
                                title:"Layout Add & Remove",
                                description:"\n        Click on the buttons to hide and show the green star.\n        "
                            },
                            {
                                id:"animateMove",
                                jsURL:"animate/animateMove.js",
                                title:"Fly Onscreen",
                                description:"\n        Click the buttons to move the Label into view or out of view.\n        "
                            },
                            {
                                id:"animateResize",
                                jsURL:"animate/animateResize.js",
                                title:"Resize",
                                description:"\n        Click the buttons to expand or collapse the text box.\n        "
                            },
                            {
                                id:"animateWipe",
                                jsURL:"animate/animateWipe.js",
                                title:"Wipe Show & Hide",
                                description:"\n        Click the buttons to show or hide the Label with a \"wipe\" effect.\n        "
                            },
                            {
                                id:"animateSlide",
                                jsURL:"animate/animateSlide.js",
                                title:"Slide Show & Hide",
                                description:"\n        Click the buttons to show or hide the Label with a \"slide\" effect.\n        "
                            },
                            {
                                id:"animateFade",
                                jsURL:"animate/animateFade.js",
                                title:"Fade Show & Hide",
                                description:"\n        Click the buttons to fade the image.\n        "
                            },
                            {
                                id:"animateZoom",
                                jsURL:"animate/animateZoom.js",
                                title:"Zoom & Shrink",
                                description:"\n        Click the buttons to zoom or shrink the image.\n        "
                            },
                            {
                                id:"animateSeqSimple",
                                jsURL:"animate/animateSeqSimple.js",
                                title:"Sequence (simple)",
                                description:"\n        Click the buttons for a 2-stage expand or collapse effect.\n        "
                            },
                            {
                                id:"animateSeqComplex",
                                jsURL:"animate/animateSeqComplex.js",
                                title:"Sequence (complex)",
                                description:"\n        Click to select and zoom each piece.\n        "
                            },
                            {
                                id:"customAnimation",
                                jsURL:"animate/animateCustom.js",
                                title:"Custom Animation",
                                description:"\n        Click on the globe for a custom \"orbit\" animation.\n        "
                            },
                            {
                                id:"tilingFilter2",
                                ref:"tilingFilter",
                                title:"Tile Filter & Sort"
                            },
                            {
                                fullScreen:"true",
                                id:"portalAnimation",
                                jsURL:"animate/portal.js",
                                needServer:"true",
                                screenshot:"animate/portal.png",
                                screenshotHeight:"337",
                                screenshotWidth:"480",
                                showSkinSwitcher:true,
                                title:"Simple Portal",
                                description:"Animations built into SmartClient layouts can be used to create a drag and drop portal\n      experience.  Click on the portlet list to the left to create portlets and see them\n      animate into place.  Drag portlets around to new locations and they animate into place.\n        "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        title:"Look & Feel",
                        description:"\n    Apply rich visual styles to SmartClient components.\n",
                        children:[
                            {
                                id:"edges",
                                jsURL:"lookfeel/edges.js",
                                title:"Edges",
                                description:"\n        Drag the text boxes. These boxes show customized frame and glow effects\n        using edge images.\n        "
                            },
                            {
                                id:"corners",
                                jsURL:"lookfeel/corners.js",
                                title:"Corners",
                                description:"\n        Drag the text boxes. These boxes show customized rounded-corner effects\n        using edge images.        \n        "
                            },
                            {
                                id:"shadows",
                                jsURL:"lookfeel/shadows.js",
                                title:"Shadows",
                                description:"\n        Drag the slider to change the shadow depth for the text box.\n        ",
                                badSkins:"BlackOps",
                                bestSkin:"TreeFrog"
                            },
                            {
                                id:"backgroundColor",
                                jsURL:"lookfeel/bgColor.js",
                                title:"Background color",
                                visibility:"sdk",
                                description:"\n        Click on the color picker to select a background color for the box.\n        "
                            },
                            {
                                id:"backgroundTexture",
                                jsURL:"lookfeel/bgImage.js",
                                title:"Background texture",
                                visibility:"sdk",
                                description:"\n        Click any button to change the background texture for the box.\n        "
                            },
                            {
                                id:"translucency",
                                jsURL:"lookfeel/opacity.js",
                                title:"Translucency",
                                description:"\n        Drag the slider to change opacity.\n        "
                            },
                            {
                                jsURL:"lookfeel/boxAttrs.js",
                                title:"Box attributes",
                                visibility:"sdk",
                                description:"\n        Drag the sliders to change the CSS box attributes.\n        <P>\n        Containers in SmartClient automatically react to changes in CSS styling on contained elements\n        \n        "
                            },
                            {
                                id:"styles",
                                jsURL:"lookfeel/styles.js",
                                title:"CSS styles",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"lookfeel/styles.css"
                                    }
                                ],
                                description:"\n        Click the radio buttons to apply different CSS styles to the text. Click the CSS tab for\n        CSS class definitions.<BR>\n        This container auto-sizes to the styled text.\n        ",
                                badSkins:"BlackOps",
                                bestSkin:"TreeFrog"
                            },
                            {
                                css:"lookfeel/consistentSizing.css",
                                id:"consistentSizing",
                                jsURL:"lookfeel/consistentSizing.js",
                                title:"Consistent sizing",
                                description:"\n      Drag the slider to resize all three text boxes. The box sizes match despite different\n      edge styling specified in CSS, enabling CSS-based skinning without affecting\n      application layout.\n    "
                            },
                            {
                                id:"gridCells",
                                jsURL:"grids/formatting/cellStyles.js",
                                showSkinSwitcher:true,
                                title:"Grid cells",
                                tabs:[
                                    {
                                        title:"CSS",
                                        url:"grids/formatting/cellStyles.css"
                                    },
                                    {
                                        title:"countryData",
                                        url:"grids/data/countryData.js"
                                    }
                                ],
                                description:"\n        Mouse over the rows and click-drag to select rows, to see the effects of different\n        base styles on these two grids.\n        "
                            }
                        ]
                    }
                ]
            },
            {
                external:true,
                icon:"[ISO_DOCS_SKIN]/images/cube_blue.png",
                isOpen:false,
                requiresModules:"Drawing",
                title:"Drawing",
                description:"\n    SmartClient leverages the native browser drawing capabilities to provide a consistent cross\n    browser drawing interface.\n",
                children:[
                    {
                        id:"ShapeGallery",
                        requiresModules:"Drawing",
                        title:"Shape Gallery",
                        url:"drawing/shapeGallery.js",
                        description:"Below is a sample of the shapes available in the SmartClient\n        drawing package.\n        "
                    },
                    {
                        id:"Rotation",
                        requiresModules:"Drawing",
                        title:"Rotation",
                        url:"drawing/rotation.js",
                        description:"A Sample of the Rotation feature of the Drawing module.\n        "
                    },
                    {
                        id:"ZoomAndPan",
                        requiresModules:"Drawing",
                        title:"Zoom and Pan",
                        url:"drawing/zoomAndPan.js",
                        description:"A Sample of the zooming and panning features of the Drawing module. Use\n        the slider for zoom and drag the image with the mouse.\n        "
                    },
                    {
                        id:"LinesAndArrowheads",
                        requiresModules:"Drawing",
                        title:"Lines and Arrowheads",
                        url:"drawing/linesAndArrowheads.js",
                        description:"A Sample of using lines and curves with selects for line width, style, \n        and arrowhead style, generated at random coordinates.\n        "
                    },
                    {
                        isOpen:true,
                        requiresModules:"Drawing",
                        title:"Gradients",
                        description:"\n            Different types of gradients can be used with shapes.\n        ",
                        children:[
                            {
                                id:"SimpleGradient",
                                requiresModules:"Drawing",
                                title:"Simple",
                                url:"drawing/gradients/simpleGradient.js",
                                description:"A Sample of using simple types of gradient.\n                "
                            },
                            {
                                id:"LinearGradient",
                                requiresModules:"Drawing",
                                title:"Linear",
                                url:"drawing/gradients/linearGradient.js",
                                description:"A Sample of using linear types of gradient.\n                "
                            },
                            {
                                id:"RadialGradient",
                                requiresModules:"Drawing",
                                title:"Radial",
                                url:"drawing/gradients/radialGradient.js",
                                description:"A Sample of using radial types of gradient.\n                "
                            }
                        ]
                    },
                    {
                        id:"Gauge",
                        requiresModules:"Drawing",
                        title:"Gauge",
                        url:"drawing/gauge.js",
                        description:"Use the controls below to set the needle position, as well as\n        the number of sectors on the dial and their colors.  The Gauge component also supports\n        configurable tick marks and labels."
                    },
                    {
                        id:"circletoCommand",
                        requiresModules:"Drawing",
                        title:"\"circleto\" Command",
                        url:"drawing/circletoCommand.js",
                        description:"\n        This example demonstrates the \"circleto\" DrawShape command. The side form can be used\n        to configure the arguments to the \"circleto\" command except for the circle's center point,\n        which is fixed. The filled-black oval can be dragged to change the last \"current point\"\n        when executing the \"circleto\" command.\n        "
                    },
                    {
                        id:"drawKnobs",
                        requiresModules:"Drawing",
                        title:"DrawItem Knobs",
                        url:"drawing/knobs.js",
                        description:"\n        This example demonstrates the different draw knobs supported by the DrawItem classes\n        which support draw knobs.\n        "
                    }
                ]
            },
            {
                id:"portal",
                isOpen:false,
                requiresModules:"Tools",
                showSkinSwitcher:"true",
                title:"Dashboards & Tools",
                description:" \n        <p>The Dashboards &amp; Tools framework provides a set of components for building\n        customizable user interfaces and tools.  Examples include portals that can persist\n        layout, report builders that allow end users to arrange data into a sharable\n        \"dashboard\", diagramming or flow charting tools, and tools for UI creation such as\n        form designers.</p>\n\n        <p>The components in the Dashboards &amp; Tools framework are the foundation on which\n        Visual Builder was created, and many of the features of Visual Builder can achieved by\n        simply creating and configuring components in the Dashboards &amp; Tools framework.</p>\n    ",
                children:[
                    {
                        isOpen:false,
                        requiresModules:"Tools",
                        title:"Palettes",
                        description:"\n                Palettes organize and present the components available for the user\n                to select and customize. Users choose items from palettes by clicking and/or dragging\n                (depending on the palette type).\n            ",
                        children:[
                            {
                                id:"treePalette",
                                jsURL:"portal/palettes/treePalette.js",
                                requiresModules:"Tools",
                                title:"Tree Palette",
                                description:"\n                      Tree Palettes organize available components in a tree structure.\n                      The user can double-click or drag to create a component.\n                   "
                            },
                            {
                                id:"listPalette",
                                jsURL:"portal/palettes/listPalette.js",
                                requiresModules:"Tools",
                                title:"List Palette",
                                description:"\n                      List Palettes organize available components in a list grid structure.\n                      The user can double-click or drag to create a component.\n                   "
                            },
                            {
                                id:"tilePalette",
                                jsURL:"portal/palettes/tilePalette.js",
                                requiresModules:"Tools",
                                title:"Tile Palette",
                                description:"\n                      Tile Palettes organize available components in a tile grid structure.\n                      The user can double-click or drag to create a component.\n                   "
                            },
                            {
                                id:"menuPalette",
                                jsURL:"portal/palettes/menuPalette.js",
                                requiresModules:"Tools",
                                title:"Menu Palette",
                                description:"\n                      Menu Palettes present available components as a menu.\n                      The user can click or drag to create a component.\n                   "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        requiresModules:"Tools",
                        title:"Edit Mode",
                        description:"\n                <p>Edit Mode uses Edit Contexts manage a set of components that have persistent state.  Edit\n                Contexts automatically understand drag and drop from Palettes as a means of\n                adding a persistent component; persistent components can also be added\n                programmatically.</p>\n\n                <p>Edit Contexts separate persistent state from transient or incidental state\n                so that only intentional changes by the user result in changes to persistent\n                data.</p>\n            ",
                        children:[
                            {
                                id:"tilePalette2",
                                ref:"tilePalette",
                                requiresModules:"Tools",
                                title:"Edit Pane",
                                description:"\n                        An Edit Pane is a container that allows drag and drop instantiation\n                        of visual components from a Palette, and direct manipulation of the \n                        position and size of those components.\n                    "
                            },
                            {
                                id:"automaticPersistence2",
                                isOpen:"false",
                                ref:"automaticPersistence",
                                requiresModules:"Tools",
                                title:"Coordinate Persistence",
                                description:"\n                        <p>By default, an Edit Context will automatically persist the position and size of\n                        components.</p>\n                        <p>Try dragging some components from the Tile Palette to the Edit Pane, and then move\n                        and resize them. Clicking on the \"Destroy and Recreate\" button will recreate the\n                        Edit Pane from saved state. Note how the position and size of components has been\n                        preserved.</p>\n                    "
                            },
                            {
                                descriptionHeight:"160",
                                id:"editPortalLayout",
                                jsURL:"portal/contexts/portalLayout.js",
                                requiresModules:"Tools",
                                title:"Portal Layout",
                                description:"\n                        <p>With the Tools framework you can create Palettes from which to drag Portlets.\n                        Try dragging from the TreePalette to the PortalLayout. \n                        Portlets will be created on the fly.</p>\n                        <p>The PortalLayout in this example is embedded in an EditPane, so that the state of the\n                        PortalLayout (and its Portlets) can be persisted. \n                        Here the state is saved to a JavaScript variable, but other persistence mechanisms may be used. \n                        Click the \"Destroy and Recreate\" button to save the PortalLayout's state, destroy it, and then\n                        recreate it. The sequence is animated to illustrate the process.</p>\n                        <p>Once you've created some Canvas portlets, try right-clicking on them to change\n                        their background color. Notice how code in the example updates the PortalLayout's edited state,\n                        so that the colors persist when you \"Destroy and Recreate\".</p>\n                    "
                            },
                            {
                                id:"editGridPortalLayout",
                                jsURL:"portal/contexts/gridPortalLayout.js",
                                requiresModules:"Tools",
                                title:"Grid Portlets",
                                tabs:[
                                    {
                                        canEdit:"false",
                                        dataSource:"animals",
                                        name:"animals"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyItem",
                                        name:"supplyItem"
                                    },
                                    {
                                        canEdit:"false",
                                        dataSource:"supplyCategory",
                                        name:"supplyCategory"
                                    }
                                ],
                                description:"\n                        <p>With the Tools framework you can create Palettes from which to drag Portlets.\n                        This example uses a palette of pre-configured grid portlets that can be modified\n                        directly.</p>\n                        <p>Try dragging from the Grid Palette to the Portal Layout.  Portlets will be\n                        created on the fly. Change the criteria, field order or size, sort order, highlights,\n                        or grouping to see how these properties are persisted.</p>\n                        <p>The Portal Layout in this example mixes in the Edit Context interface so\n                        that the state of the Portlets can be persisted.  Here the state is saved to a\n                        JavaScript variable, but other persistence mechanisms may be used.  Click the\n                        \"Destroy and Recreate\" button to save the Portal Layout's state, destroy it, and then\n                        recreate it.  The process is animated to illustrate the process.</p>\n                    "
                            },
                            {
                                id:"editDrawPane",
                                jsURL:"portal/contexts/drawing.js",
                                requiresModules:"Drawing, Tools",
                                title:"Drawing",
                                description:"\n                        <p>With the Tools framework it is easy to create a simple drawing editor.\n                        This example uses a palette of pre-configured DrawItems that can be dropped into the\n                        target DrawPane. Position, size, and styling properties of the DrawItems are\n                        automatically persisted.</p>\n                        <p>This sample persists into a JavaScript variable.  Double-click or drag some\n                        Draw Items into the Draw Pane, then click the \"Destroy and Recreate\" button to\n                        save the state, destroy the DrawItems, and then recreate them.</p>\n                    "
                            }
                        ]
                    },
                    {
                        isOpen:false,
                        requiresModules:"Tools",
                        title:"Persistence",
                        description:"\n                <p>There are several persistence strategies to persist and recreate Edit Contexts.</p>\n                <ul>\n                    <li>Use a variable to store state, and recreate or duplicate an Edit Context from that variable.</li>\n                    <li>Persist state to a DataSource.</li>\n                    <li>Use Offline storage to persist state.</li>\n                </ul>\n            ",
                        children:[
                            {
                                id:"automaticPersistence",
                                isOpen:"false",
                                jsURL:"portal/persistence/automaticPersistence.js",
                                requiresModules:"Tools",
                                title:"Automatic",
                                tabs:[
                                    {
                                        title:"Tile Palette",
                                        url:"portal/palettes/tilePalette.js"
                                    }
                                ],
                                description:"\n                        <p>The state of an Edit Context can be saved to a variable. That variable can then be used to\n                        duplicate or recreate the Edit Context.</p>\n                        <p>Try dragging some components from the Tile Palette to the Edit Pane. Click the\n                        \"Destroy and Recreate\" button to save the Edit Pane's state, destroy it, and then\n                        recreate it. The process is animated, to illustrate the process\n                        (which would otherwise occur instantly).</p>\n                    "
                            },
                            {
                                dataSource:"editNodes",
                                id:"dataSourcePersistence",
                                isOpen:"false",
                                jsURL:"portal/persistence/datasource.js",
                                requiresModules:"Tools",
                                title:"DataSource",
                                tabs:[
                                    {
                                        title:"Tile Palette",
                                        url:"portal/palettes/tilePalette.js"
                                    }
                                ],
                                description:"\n                        The state of an Edit Context can be connected to a DataSource. Try dragging some\n                        components from the Tile Palette to the Edit Pane. Click on \"Save\" to save the state\n                        of the Edit Pane to a DataSource. Make some changes to the Edit Pane, and then click\n                        \"Restore\". Note how the state of the Edit Pane is restored to its saved state.\n                    "
                            },
                            {
                                id:"offlinePersistence",
                                isOpen:"false",
                                jsURL:"portal/persistence/offline.js",
                                requiresModules:"Tools",
                                title:"Offline",
                                tabs:[
                                    {
                                        title:"Tile Palette",
                                        url:"portal/palettes/tilePalette.js"
                                    }
                                ],
                                description:"\n                        <p>The state of an Edit Context can be connected to Offline storage. Try dragging some\n                        components from the Tile Palette to the Edit Pane. Click on \"Save\" to save the state\n                        of the Edit Pane to a DataSource. Make some changes to the Edit Pane, and then click\n                        \"Restore\". Note how the state of the Edit Pane is restored to its saved state.</p>\n                        <p>Try reloading the page to see saved state automatically restored. (Note that the\n                        example does not automatically save state).</p>\n                    "
                            }
                        ]
                    },
                    {
                        id:"portalDashboard",
                        jsURL:"portal/dashboard.js",
                        requiresModules:"Tools",
                        title:"Portal Dashboard",
                        tabs:[
                            {
                                canEdit:"false",
                                dataSource:"dashboards",
                                name:"dashboards"
                            },
                            {
                                canEdit:"false",
                                dataSource:"animals",
                                name:"animals"
                            },
                            {
                                canEdit:"false",
                                dataSource:"supplyItem",
                                name:"supplyItem"
                            },
                            {
                                canEdit:"false",
                                dataSource:"supplyCategory",
                                name:"supplyCategory"
                            }
                        ],
                        description:"\n                <p>With the Tools framework, you can create dashboards of portlets.\n                This example uses a list of pre-configured grid portlets that can be viewed or edited.</p>\n                <p>Select a pre-configured dashboard to view the saved portal layout consisting of\n                one or more grids. To make changes, select the dashboard and click Edit button.\n                Change the criteria, field order or size, sort order, highlights,\n                or grouping to see how these properties are persisted.</p>\n                <p>Select a dashboard and click Clone button to generate another copy for experimentation.</p>\n            "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/application_osx.png",
                isOpen:false,
                title:"Applications",
                description:"\n    Demos of complete applications based on SmartClient.\n",
                children:[
                    {
                        id:"applicationShowcaseApp",
                        ref:"showcaseApp",
                        title:"Office Supply Catalog"
                    },
                    {
                        dataSource:"productRevenue",
                        fullScreen:"true",
                        id:"analytics",
                        jsURL:"advanced/cubegrid/databound_cubegrid.js",
                        requiresModules:"Drawing,Analytics",
                        screenshot:"advanced/cubegrid/databound_cubegrid.png",
                        screenshotHeight:"327",
                        screenshotWidth:"468",
                        showSkinSwitcher:true,
                        title:"Interactive Analytics",
                        tabs:[
                            {
                                loadAtEnd:"true",
                                title:"facet controls",
                                url:"advanced/cubegrid/facet_controls.js"
                            }
                        ],
                        descriptionHeight:"100",
                        description:"\n        This example shows binding to a multi-dimensional dataset, where each\n        cell value has a series of attributes, called \"facets\", that appear as headers\n        labelling data values.  Facets can be added to the view, exposing more detail, by\n        dragging the menu buttons onto the grid, or into the \"Row Facets\" and \"Column Facets\"\n        listings.\n        <P>\n        Facets can be removed from the view by using the menus to set a facet to a fixed\n        value.  For example, use the \"Time\" menu to show just data from 2002: \"Time -> Fix Time\n        Value -> All Years -> 2002\".\n        <P>\n        Click the turndown controls on facet values to expand tree facets.  Note that data\n        loads as it is revealed by expanding and collapsing tree facets, by adding facets, or\n        by scrolling in either direction.  This allows users to navigate extremely large data sets.\n        <P>\n        Right click on any data value or facet value to generate a chart showing how that\n        particular value varies along up to two facets.  For example, click on any data\n        value for \"Office Paper Products\" and choose \"Chart -> Sales by Time and Region\" to see\n        how this category of products is selling in different regions and time periods.  Switch to \n\t\tdifferent chart types (eg Radar) on the fly.\n        <i><b>(Note: Chart support requires the Drawing Module. \n        If not installed, the Analytics Module, including the CubeGrid and\n        the remainder of this sample, will continue to function normally.)</b></i>\n        <P>\n        Because the CubeGrid uses a DataSource to load data, it can be connected to any kind\n        of server or data provider.  This sample loads data from an SQL database.\n    "
                    }
                ]
            },
            {
                icon:"[ISO_DOCS_SKIN]/images/silkicons/arrow_branch.png",
                isOpen:false,
                title:"Extending",
                description:"\n    Examples of extending SmartClient functionality\n",
                children:[
                    {
                        css:"extending/portlet.css",
                        jsURL:"extending/componentReuse.js",
                        title:"Component Reuse",
                        description:"\n        The portlets below are a custom component created with less than one page of code\n        (see the \"JS\" tab).  The portlets support drag repositioning, drag resizing, a close\n        button, can contain any HTML content, and are skinnable.\n    "
                    },
                    {
                        dataSource:"supplyItem",
                        id:"patternReuse",
                        jsURL:"extending/patternReuse.js",
                        title:"Pattern Reuse",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"countryDS",
                                url:"grids/ds/countrySQLDS.ds.xml"
                            }
                        ],
                        description:"\n        Click to select a DataSource, click on records to edit them in the adjacent form, then\n        click the \"Save\" button to save changes.<br>\n        This custom component combines a databound form and grid into a reusable application\n        pattern of side-by-side editing, that can be used with any DataSource.\n    "
                    },
                    {
                        id:"extSchemaChaining",
                        ref:"schemaChaining",
                        title:"Schema Reuse"
                    },
                    {
                        id:"extCustomSimpleType",
                        ref:"DBcustomSimpleType",
                        title:"Type Reuse"
                    },
                    {
                        id:"changeLocales",
                        jsURL:"extending/changeLocales.js",
                        title:"Localization",
                        tabs:[
                            {
                                canEdit:"false",
                                title:"worldDS",
                                url:"grids/ds/worldSQLDS.ds.xml"
                            }
                        ],
                        descriptionHeight:"105",
                        description:"\n         Select a different language from the Locale drop down list, and click the \"Change\n        Locale\" button to change the default language. The following UI elements will change \n        the language in which they are displayed. The month chooser of the date picker, the operator\n        chooser of the custom filter, and the header context menus of the ListGrid. \n        SmartClient supports localization via configurable property files. See the \n        documentation under \"Internationalization and Localization\" for more information about using existing locale files or\n        creating custom locales.\n        "
                    },
                    {
                        id:"dateFormat",
                        jsURL:"extending/dateFormat_local.js",
                        showSource:false,
                        title:"Standard Date Format",
                        tabs:[
                            {
                                doEval:"false",
                                title:"JS",
                                url:"extending/dateFormat.js"
                            },
                            {
                                title:"employees",
                                url:"extending/employees.js"
                            }
                        ],
                        description:"\n         Dates displayed in the \"Hire Date\" field in this example are formatted using the\n        standard <code>\"toJapanShortDate\"</code> formatter. Click on a record to edit it in the\n        Form, or double click to edit inline in the ListGrid.\n        "
                    },
                    {
                        id:"customDateFormat",
                        jsURL:"extending/customDateFormat_local.js",
                        showSource:false,
                        title:"Custom Date Format",
                        tabs:[
                            {
                                doEval:"false",
                                title:"JS",
                                url:"extending/customDateFormat.js"
                            },
                            {
                                title:"employees",
                                url:"extending/employees.js"
                            }
                        ],
                        description:"\n         Dates displayed in the \"Hire Date\" field in this example are formatted\n        using a custom formatting function. Click on a record to edit it in the\n        Form, or double click to edit inline in the ListGrid.\n        "
                    },
                    {
                        dataSource:"supplyItemCurrency",
                        id:"customDataType",
                        jsURL:"extending/customDataType.js",
                        title:"Custom Data Type",
                        description:"\n         This example demonstrates using a custom SimpleType to provide standard\n        type based validation, formatting and parsing logic across components. The \"unitCost\"\n        field is of type <code>\"currency\"</code> which is explicitly defined in the source\n        as a SimpleType inheriting from float. Both the (editable) ListGrid and the DynamicForm\n        respect the settings defined in this type definition.\n        "
                    },
                    {
                        dataSource:"supplyItem",
                        jsURL:"extending/customizeFields.js",
                        title:"Customize Fields",
                        description:"\n        Edit field definitions in the grid below to override how this form binds to the \n        \"supplyItem\" DataSource.  This is a simplified example of how\n        an application can be delivered that can be customized with organization-specific fields\n        and rules. Dynamic schema binding makes building WYSIWYG editing interfaces very\n        simple.  \n        "
                    },
                    {
                        id:"customFormItem",
                        jsURL:"extending/customFormItem.js",
                        title:"Custom Form Items",
                        description:"\n            Developers can create a custom item and still hook into the standard \"change()\" / \"changed()\" notifications that a normal user interaction would fire.\n        "
                    },
                    {
                        id:"extCustomDrag",
                        ref:"customDrag",
                        title:"Drag and Drop"
                    },
                    {
                        id:"extCustomHovers",
                        ref:"customHovers",
                        title:"Hovers"
                    },
                    {
                        id:"extCustomMouseEvents",
                        ref:"customMouseEvents",
                        title:"Mouse Handling"
                    },
                    {
                        id:"extCustomAnimation",
                        ref:"customAnimation",
                        title:"Animation"
                    },
                    {
                        id:"extPortalAnimation",
                        ref:"portalAnimation",
                        title:"Simple Portal"
                    }
                ]
            }
        ]
    }
})

