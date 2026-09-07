import cn.enaium.xicons.swing.utility.PathIcon;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @author Enaium
 */
public class Main {
    private static final String[] LIBS = {"antd", "carbon", "fa", "fluent", "ionicons4", "ionicons5", "material", "tabler"};
    private static final String[] STYLES = {"Filled", "Outlined", "Twotone", "Default", "Regular", "Sharp", "Round"};

    public static void main(String[] args) {
        FlatLightLaf.setup();
        JFrame frame = new JFrame("XIcons Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        JTabbedPane tabbedPane = new JTabbedPane();
        for (String lib : LIBS) {
            Map<String, List<Class<?>>> byStyle = scanLib(lib);
            byStyle.forEach((style, classes) ->
                    tabbedPane.addTab(cap(lib) + " " + style, createIconPanel(classes, cap(lib) + " " + style + " Icons")));
        }
        frame.setContentPane(tabbedPane);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
    }

    /** Scans the classpath for icon classes of one library, grouped by style. */
    private static Map<String, List<Class<?>>> scanLib(String lib) {
        Map<String, List<Class<?>>> byStyle = new LinkedHashMap<>();
        String pkg = "cn/enaium/xicons/swing/icons/" + lib;
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
            Class<?> c = Class.forName("cn.enaium.xicons.swing.icons." + lib + "." + simple);
            if (PathIcon.class.isAssignableFrom(c)) {
                byStyle.computeIfAbsent(style, k -> new ArrayList<>()).add(c);
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static JPanel createIconPanel(List<Class<?>> iconClasses, String title) {
        List<JLabel> labels = new ArrayList<>();
        for (Class<?> c : iconClasses) {
            try {
                PathIcon icon = (PathIcon) c.getDeclaredConstructor().newInstance();
                JLabel label = new JLabel(icon);
                label.setToolTipText(c.getSimpleName());
                label.setName(c.getSimpleName());
                labels.add(label);
            } catch (Exception ignored) {
            }
        }

        JPanel contentPane = new JPanel(new WrapLayout(FlowLayout.LEFT));
        contentPane.setBorder(BorderFactory.createTitledBorder(title));
        labels.forEach(contentPane::add);

        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search icons");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                String query = searchField.getText().trim().toLowerCase();
                contentPane.removeAll();
                labels.stream()
                        .filter(label -> query.isEmpty() || label.getName().toLowerCase().contains(query))
                        .forEach(contentPane::add);
                contentPane.revalidate();
                contentPane.repaint();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.add(searchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}