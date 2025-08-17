import cn.enaium.xicons.swing.icons.*;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * @author Enaium
 */
public class Main {
    public static void main(String[] args) throws IllegalAccessException {
        FlatLightLaf.setup();
        JFrame frame = new JFrame("XIcons Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        JTabbedPane tabbedPane = new JTabbedPane();
        // Fluent
        tabbedPane.addTab("Fluent Regular", createIconPanel(FluentIcons.Regular, "Fluent Regular Icons"));
        tabbedPane.addTab("Fluent Filled", createIconPanel(FluentIcons.Filled, "Fluent Filled Icons"));
        // Antd
        tabbedPane.addTab("Antd Filled", createIconPanel(AntdIcons.Filled, "Antd Filled Icons"));
        tabbedPane.addTab("Antd Outlined", createIconPanel(AntdIcons.Outlined, "Antd Outlined Icons"));
        tabbedPane.addTab("Antd Twotone", createIconPanel(AntdIcons.Twotone, "Antd Twotone Icons"));
        // Carbon
        tabbedPane.addTab("Carbon Default", createIconPanel(CarbonIcons.Default, "Carbon Default Icons"));
        tabbedPane.addTab("Carbon Filled", createIconPanel(CarbonIcons.Filled, "Carbon Filled Icons"));
        tabbedPane.addTab("Carbon Round", createIconPanel(CarbonIcons.Round, "Carbon Round Icons"));
        // Fa
        tabbedPane.addTab("Fa Default", createIconPanel(FaIcons.Default, "Fa Default Icons"));
        tabbedPane.addTab("Fa Regular", createIconPanel(FaIcons.Regular, "Fa Regular Icons"));
        // Ionicons4
        tabbedPane.addTab("Ionicons4 Default", createIconPanel(Ionicons4Icons.Default, "Ionicons4 Default Icons"));
        // Ionicons5
        tabbedPane.addTab("Ionicons5 Default", createIconPanel(Ionicons5Icons.Default, "Ionicons5 Default Icons"));
        tabbedPane.addTab("Ionicons5 Sharp", createIconPanel(Ionicons5Icons.Sharp, "Ionicons5 Sharp Icons"));
        // Material
        tabbedPane.addTab("Material Filled", createIconPanel(MaterialIcons.Filled, "Material Filled Icons"));
        tabbedPane.addTab("Material Outlined", createIconPanel(MaterialIcons.Outlined, "Material Outlined Icons"));
        tabbedPane.addTab("Material Round", createIconPanel(MaterialIcons.Round, "Material Round Icons"));
        tabbedPane.addTab("Material Sharp", createIconPanel(MaterialIcons.Sharp, "Material Sharp Icons"));
        tabbedPane.addTab("Material Twotone", createIconPanel(MaterialIcons.Twotone, "Material Twotone Icons"));
        // Tabler
        tabbedPane.addTab("Tabler Default", createIconPanel(TablerIcons.Default, "Tabler Default Icons"));
        tabbedPane.addTab("Tabler Filled", createIconPanel(TablerIcons.Filled, "Tabler Filled Icons"));
        tabbedPane.addTab("Tabler Sharp", createIconPanel(TablerIcons.Sharp, "Tabler Sharp Icons"));

        frame.setContentPane(tabbedPane);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
    }

    private static JPanel createIconPanel(Object iconContainer, String title) throws IllegalAccessException {
        JPanel contentPane = new JPanel(new WrapLayout(FlowLayout.LEFT));
        contentPane.setBorder(BorderFactory.createTitledBorder(title));

        for (Field field : iconContainer.getClass().getFields()) {
            Object icon = field.get(iconContainer);
            if (icon instanceof Icon) {
                JLabel label = new JLabel((Icon) icon);
                label.setToolTipText(field.getName());
                contentPane.add(label);
            }
        }

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}
