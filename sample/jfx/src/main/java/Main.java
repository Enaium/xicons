import cn.enaium.xicons.jfx.utility.ExtendPath;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @author Enaium
 */
public class Main extends Application {
    private static final String[] LIBS = {"antd", "carbon", "fa", "fluent", "ionicons4", "ionicons5", "material", "tabler"};
    private static final String[] STYLES = {"Filled", "Outlined", "Twotone", "Default", "Regular", "Sharp", "Round"};

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("XIcons JFX");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);

        TabPane tabPane = new TabPane();
        for (String lib : LIBS) {
            Map<String, List<Class<?>>> byStyle = scanLib(lib);
            byStyle.forEach((style, classes) ->
                    tabPane.getTabs().add(createIconTab(cap(lib) + " " + style, classes, cap(lib) + " " + style + " Icons")));
        }

        Scene scene = new Scene(tabPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Map<String, List<Class<?>>> scanLib(String lib) {
        Map<String, List<Class<?>>> byStyle = new LinkedHashMap<>();
        String pkg = "cn/enaium/xicons/jfx/icons/" + lib;
        String cp = System.getProperty("java.class.path");
        for (String entry : cp.split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isDirectory()) {
                File dir = new File(f, pkg);
                File[] files = dir.listFiles((d, name) -> name.endsWith(".class"));
                if (files != null) {
                    for (File cf : files) {
                        addIconClass(cf.getName().replace(".class", ""), lib, byStyle);
                    }
                }
            } else if (f.isFile() && f.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(f)) {
                    jar.stream()
                            .filter(e -> e.getName().startsWith(pkg + "/") && e.getName().endsWith(".class")
                                    && !e.getName().contains("$"))
                            .forEach(e -> {
                                String simple = e.getName().substring(e.getName().lastIndexOf('/') + 1).replace(".class", "");
                                addIconClass(simple, lib, byStyle);
                            });
                } catch (Exception ignored) {
                }
            }
        }
        return byStyle;
    }

    private static void addIconClass(String simple, String lib, Map<String, List<Class<?>>> byStyle) {
        String style = "Default";
        for (String s : STYLES) {
            if (simple.endsWith(s)) {
                style = s;
                break;
            }
        }
        try {
            Class<?> c = Class.forName("cn.enaium.xicons.jfx.icons." + lib + "." + simple);
            if (javafx.scene.Node.class.isAssignableFrom(c)) {
                byStyle.computeIfAbsent(style, k -> new ArrayList<>()).add(c);
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    private Tab createIconTab(String tabTitle, List<Class<?>> iconClasses, String panelTitle) {
        Tab tab = new Tab(tabTitle);
        tab.setClosable(false);

        Label titleLabel = new Label(panelTitle);
        titleLabel.setFont(Font.font(16));
        titleLabel.setPadding(new Insets(0, 0, 10, 0));

        FlowPane iconPane = new FlowPane();
        iconPane.setHgap(10);
        iconPane.setVgap(10);
        iconPane.setPadding(new Insets(10));
        iconPane.setAlignment(Pos.TOP_LEFT);
        iconPane.getChildren().add(titleLabel);

        List<NamedNode> namedNodes = new ArrayList<>();
        for (Class<?> c : iconClasses) {
            try {
                Node icon = (Node) c.getDeclaredConstructor().newInstance();
                VBox iconBox = new VBox(5);
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setPadding(new Insets(5));
                iconBox.getChildren().add(icon);
                Tooltip tooltip = new Tooltip(c.getSimpleName());
                Tooltip.install(iconBox, tooltip);
                iconPane.getChildren().add(iconBox);
                namedNodes.add(new NamedNode(c.getSimpleName(), iconBox));
            } catch (Exception ignored) {
            }
        }

        TextField searchField = new TextField();
        searchField.setPromptText("Search icons");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            iconPane.getChildren().clear();
            iconPane.getChildren().add(titleLabel);
            namedNodes.stream()
                    .filter(node -> query.isEmpty() || node.name.toLowerCase().contains(query))
                    .forEach(node -> iconPane.getChildren().add(node.node));
        });

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(iconPane);

        VBox root = new VBox(5);
        root.setPadding(new Insets(5));
        root.getChildren().addAll(searchField, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        tab.setContent(root);

        return tab;
    }

    private static class NamedNode {
        final String name;
        final VBox node;

        NamedNode(String name, VBox node) {
            this.name = name;
            this.node = node;
        }
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}