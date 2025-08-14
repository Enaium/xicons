import cn.enaium.xicons.jfx.icons.FluentIcons;
import cn.enaium.xicons.jfx.icons.AntdIcons;
import cn.enaium.xicons.jfx.icons.CarbonIcons;
import cn.enaium.xicons.jfx.icons.FaIcons;
import cn.enaium.xicons.jfx.icons.Ionicons4Icons;
import cn.enaium.xicons.jfx.icons.Ionicons5Icons;
import cn.enaium.xicons.jfx.icons.MaterialIcons;
import cn.enaium.xicons.jfx.icons.TablerIcons;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.lang.reflect.Field;

/**
 * @author Enaium
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("XIcons JFX");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);

        TabPane tabPane = new TabPane();
        
        // Fluent
        tabPane.getTabs().add(createIconTab("Fluent Regular", FluentIcons.Regular, "Fluent Regular Icons"));
        tabPane.getTabs().add(createIconTab("Fluent Filled", FluentIcons.Filled, "Fluent Filled Icons"));
        
        // Antd
        tabPane.getTabs().add(createIconTab("Antd Filled", AntdIcons.Filled, "Antd Filled Icons"));
        tabPane.getTabs().add(createIconTab("Antd Outlined", AntdIcons.Outlined, "Antd Outlined Icons"));
        tabPane.getTabs().add(createIconTab("Antd Twotone", AntdIcons.Twotone, "Antd Twotone Icons"));
        
        // Carbon
        tabPane.getTabs().add(createIconTab("Carbon Default", CarbonIcons.Default, "Carbon Default Icons"));
        tabPane.getTabs().add(createIconTab("Carbon Filled", CarbonIcons.Filled, "Carbon Filled Icons"));
        tabPane.getTabs().add(createIconTab("Carbon Round", CarbonIcons.Round, "Carbon Round Icons"));
        
        // Fa
        tabPane.getTabs().add(createIconTab("Fa Default", FaIcons.Default, "Fa Default Icons"));
        tabPane.getTabs().add(createIconTab("Fa Regular", FaIcons.Regular, "Fa Regular Icons"));
        
        // Ionicons4
        tabPane.getTabs().add(createIconTab("Ionicons4 Default", Ionicons4Icons.Default, "Ionicons4 Default Icons"));
        
        // Ionicons5
        tabPane.getTabs().add(createIconTab("Ionicons5 Default", Ionicons5Icons.Default, "Ionicons5 Default Icons"));
        tabPane.getTabs().add(createIconTab("Ionicons5 Sharp", Ionicons5Icons.Sharp, "Ionicons5 Sharp Icons"));
        
        // Material
        tabPane.getTabs().add(createIconTab("Material Filled", MaterialIcons.Filled, "Material Filled Icons"));
        tabPane.getTabs().add(createIconTab("Material Outlined", MaterialIcons.Outlined, "Material Outlined Icons"));
        tabPane.getTabs().add(createIconTab("Material Round", MaterialIcons.Round, "Material Round Icons"));
        tabPane.getTabs().add(createIconTab("Material Sharp", MaterialIcons.Sharp, "Material Sharp Icons"));
        tabPane.getTabs().add(createIconTab("Material Twotone", MaterialIcons.Twotone, "Material Twotone Icons"));
        
        // Tabler
        tabPane.getTabs().add(createIconTab("Tabler Default", TablerIcons.Default, "Tabler Default Icons"));
        tabPane.getTabs().add(createIconTab("Tabler Filled", TablerIcons.Filled, "Tabler Filled Icons"));
        tabPane.getTabs().add(createIconTab("Tabler Sharp", TablerIcons.Sharp, "Tabler Sharp Icons"));
        
        Scene scene = new Scene(tabPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createIconTab(String tabTitle, Object iconContainer, String panelTitle) {
        Tab tab = new Tab(tabTitle);
        tab.setClosable(false);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        FlowPane iconPane = new FlowPane();
        iconPane.setHgap(10);
        iconPane.setVgap(10);
        iconPane.setPadding(new Insets(10));
        iconPane.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(panelTitle);
        titleLabel.setFont(Font.font(16));
        titleLabel.setPadding(new Insets(0, 0, 10, 0));
        iconPane.getChildren().add(titleLabel);

        try {
            for (Field field : iconContainer.getClass().getFields()) {
                try {
                    Object icon = field.get(iconContainer);
                    if (icon instanceof javafx.scene.Node) {
                        VBox iconBox = new VBox(5);
                        iconBox.setAlignment(Pos.CENTER);
                        iconBox.setPadding(new Insets(5));
                        iconBox.getChildren().add((javafx.scene.Node) icon);
                        Tooltip tooltip = new Tooltip(field.getName());
                        Tooltip.install(iconBox, tooltip);
                        
                        iconPane.getChildren().add(iconBox);
                    }
                } catch (Exception e) {
                    System.err.println("Error accessing field: " + field.getName() + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating tab: " + tabTitle + " - " + e.getMessage());
        }
        
        scrollPane.setContent(iconPane);
        tab.setContent(scrollPane);
        
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
