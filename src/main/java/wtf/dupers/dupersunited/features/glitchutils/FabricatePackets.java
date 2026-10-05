package wtf.dupers.dupersunited.features.glitchutils;

import wtf.dupers.dupersunited.utils.ColorUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.ContainerInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.commands.MainCommand.sendMessage;
import static wtf.dupers.dupersunited.utils.ColorUtil.toAwtColor;

public class FabricatePackets {

    private static final Logger LOGGER = LoggerFactory.getLogger("DU/FabricatePackets");

    private static final Color BG = toAwtColor(ColorUtil.DEEP_SAPPHIRE);
    private static final Color BG2 = toAwtColor(ColorUtil.MANTLE);
    private static final Color BG3 = toAwtColor(ColorUtil.CRUST);
    private static final Color COLOUR = toAwtColor(ColorUtil.MAUVE);
    private static final Color COLOUR_DIM = toAwtColor(ColorUtil.FADED_INDIGO);
    private static final Color FG = toAwtColor(ColorUtil.PALE_NAVY);
    private static final Color FG_DIM = toAwtColor(ColorUtil.SUBTEXT);
    private static final Color GREEN = toAwtColor(ColorUtil.GREEN);
    private static final Color RED = toAwtColor(ColorUtil.RED);
    private static final Color BORDER = toAwtColor(ColorUtil.DEEP_INDIGO);
    private static final float OPACITY = 0.85f;
    private static final Font MC_FONT = loadMcFont(12f);
    private static final Font MC_FONT_SM = loadMcFont(10f);

    private static Font loadMcFont(float size) {
        try (InputStream is = FabricatePackets.class.getResourceAsStream("/assets/dupersunited/font/minecraftfont.otf")) {
            if (is != null) return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception ignored) {}
        LOGGER.debug("Minecraft font not found, falling back to Arial");
        return new Font("Arial", Font.PLAIN, (int) size);
    }

    private static void applyLaf() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.put("ComboBox.background", BG3);
            UIManager.put("ComboBox.foreground", FG);
            UIManager.put("ComboBox.selectionBackground", COLOUR_DIM);
            UIManager.put("ComboBox.selectionForeground", FG);
            UIManager.put("ComboBox.buttonBackground", BG3);
            UIManager.put("ComboBox.buttonShadow", BG3);
            UIManager.put("ComboBox.buttonDarkShadow", BG3);
            UIManager.put("ComboBox.buttonHighlight", BG3);
            LOGGER.debug("Look and feel applied successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to apply look and feel", e);
        }
    }

    public static void open() {
        if (Util.getPlatform() == Util.OS.OSX) {
            LOGGER.debug("Packet fabricator blocked: macOS is not supported");
            return;
        }

        applyLaf();

        // if this ends up bad then we can revert
//        MinecraftClient mcEarly = MinecraftClient.getInstance();
        int capSyncId   = mc.player != null ? mc.player.containerMenu.containerId : 0;
        int capRevision = mc.player != null ? mc.player.containerMenu.getStateId() : 0;

        JFrame frame = buildFrame(420, 140);

        JLabel title = new JLabel("Select packet type", SwingConstants.CENTER);
        title.setFont(MC_FONT);
        title.setForeground(COLOUR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        title.setBorder(new EmptyBorder(10, 0, 16, 0));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setBackground(BG2);
        btnRow.setBorder(new EmptyBorder(0, 24, 0, 24));

        JButton clickSlot = COLOURBtn("Click Slot");
        JButton buttonClick = COLOURBtn("Button Click");
        btnRow.add(clickSlot);
        btnRow.add(buttonClick);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG2);
        content.setBorder(new EmptyBorder(8, 0, 20, 0));
        content.add(title);
        content.add(btnRow);

        frame.add(headerBar(frame, "Fabricate Packet"), BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);

        clickSlot.addActionListener(e -> { frame.dispose(); openClickSlot(capSyncId, capRevision); });
        buttonClick.addActionListener(e -> { frame.dispose(); openButtonClick(capSyncId); });

        frame.setAutoRequestFocus(false);
        frame.setVisible(true);
    }

    private static void openClickSlot(int capSyncId, int capRevision) {
        applyLaf();
        JFrame f = buildFrame(400, 360);

        JTextField syncId = new JTextField(String.valueOf(capSyncId));
        JTextField revision = new JTextField(String.valueOf(capRevision));
        JTextField slot = new JTextField("0");
        JTextField button = new JTextField("0");
        JTextField times = new JTextField("1");

        JComboBox<String> action = styledCombo(new String[]{
            "PICKUP", "QUICK_MOVE", "SWAP", "CLONE", "THROW", "QUICK_CRAFT", "PICKUP_ALL"});

        JLabel status = statusLabel();
        JButton send  = COLOURBtn("Send");

        JPanel form = formPanel();
        form.add(formRow("Sync Id", syncId));
        form.add(formRow("Revision", revision));
        form.add(formRow("Slot", slot));
        form.add(formRow("Button", button));
        form.add(formRow("Action", action));
        form.add(formRow("Times", times));
        form.add(Box.createVerticalStrut(8));
        form.add(status);
        form.add(Box.createVerticalStrut(8));
        form.add(btnRow(send));

        send.addActionListener(e -> {
            try {
                int syncIdVal = Integer.parseInt(syncId.getText().trim());
                int revisionVal = Integer.parseInt(revision.getText().trim());
                short slotVal = Short.parseShort(slot.getText().trim());
                byte buttonVal = Byte.parseByte(button.getText().trim());
                String actionStr = action.getSelectedItem().toString();
                int n = Math.max(1, Integer.parseInt(times.getText().trim()));

                ServerboundContainerClickPacket pkt = new ServerboundContainerClickPacket(
                    syncIdVal,
                    revisionVal,
                    slotVal,
                    buttonVal,
                    toAction(actionStr),
                    new Int2ObjectArrayMap<>(),
                    HashedStack.EMPTY
                );

                if (mc.getConnection() == null) throw new IllegalStateException("Network handler is null");

                for (int i = 0; i < n; i++) mc.execute(() -> mc.getConnection().send(pkt));

                flash(status, "Sending packet " + n + " time(s)!", GREEN);
                mc.execute(() -> sendMessage("Sending custom packet to slot " + slotVal + ", button " + buttonVal + ", action " + actionStr + ", " + n + " time(s)", true));
            } catch (Exception ex) {
                flash(status, "Invalid input or not connected!", RED);
            }
        });

        f.add(headerBar(f, "Click Slot Packet"), BorderLayout.NORTH);
        f.add(wrapScroll(form), BorderLayout.CENTER);
        f.setVisible(true);
    }

    private static void openButtonClick(int capSyncId) {
        applyLaf();
        LOGGER.debug("Opening ButtonClick dialog — syncId={}", capSyncId);

        JFrame f = buildFrame(400, 240);

        JTextField syncId = new JTextField(String.valueOf(capSyncId));
        JTextField buttonId = new JTextField("0");
        JTextField times = new JTextField("1");

        JLabel status = statusLabel();
        JButton send  = COLOURBtn("Send");

        JPanel form = formPanel();
        form.add(formRow("Sync Id", syncId));
        form.add(formRow("Button Id", buttonId));
        form.add(formRow("Times", times));
        form.add(Box.createVerticalStrut(8));
        form.add(status);
        form.add(Box.createVerticalStrut(8));
        form.add(btnRow(send));

        send.addActionListener(e -> {
            try {
                int syncIdVal = Integer.parseInt(syncId.getText().trim());
                int buttonVal = Integer.parseInt(buttonId.getText().trim());
                int n = Math.max(1, Integer.parseInt(times.getText().trim()));

                ServerboundContainerButtonClickPacket pkt = new ServerboundContainerButtonClickPacket(syncIdVal, buttonVal);

                if (mc.getConnection() == null) throw new IllegalStateException("Network handler is null");

                for (int i = 0; i < n; i++) mc.execute(() -> mc.getConnection().send(pkt));

                flash(status, "Sending packets " + n + " time(s)!", GREEN);
                mc.execute(() -> sendMessage("Sending custom packet, syncId: " + syncIdVal + ", buttonId: " + buttonVal + ", " + n + " time(s)", true));
            } catch (Exception ex) {
                flash(status, "Invalid input or not connected!", RED);
            }
        });

        f.add(headerBar(f, "Button Click Packet"), BorderLayout.NORTH);
        f.add(wrapScroll(form), BorderLayout.CENTER);
        f.setVisible(true);
    }

    private static JFrame buildFrame(int w, int h) {
        JFrame f = new JFrame();
        f.setUndecorated(true);
        f.setSize(w, h);
        f.setLocationRelativeTo(null);
        f.setAlwaysOnTop(true);
        f.setLayout(new BorderLayout());
        f.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER, 1));
        f.getContentPane().setBackground(BG2);
        f.setOpacity(OPACITY);
        makeDraggable(f);
        return f;
    }

    private static JPanel headerBar(JFrame owner, String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG);
        bar.setBorder(new EmptyBorder(6, 12, 6, 8));
        bar.setPreferredSize(new Dimension(0, 36));

        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(3, 0));
        accent.setBackground(COLOUR);
        bar.add(accent, BorderLayout.WEST);

        JLabel lbl = new JLabel("  " + title);
        lbl.setForeground(FG);
        lbl.setFont(MC_FONT);
        bar.add(lbl, BorderLayout.CENTER);

        JButton x = new JButton("x");
        x.setFont(MC_FONT);
        x.setForeground(FG_DIM);
        x.setBackground(BG);
        x.setBorder(new EmptyBorder(2, 8, 2, 4));
        x.setFocusPainted(false);
        x.setOpaque(true);
        x.setContentAreaFilled(true);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                x.setForeground(RED);
            }
            public void mouseExited(MouseEvent e) {
                x.setForeground(FG_DIM);
            }
        });
        x.addActionListener(e -> {
            LOGGER.debug("Dialog '{}' closed by user", title);
            owner.dispose();
        });
        bar.add(x, BorderLayout.EAST);

        makeDraggable(owner, bar);
        makeDraggable(owner, lbl);

        return bar;
    }

    private static JPanel formPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG2);
        p.setBorder(new EmptyBorder(12, 20, 12, 20));
        return p;
    }

    private static JPanel formRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG2);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(new EmptyBorder(3, 0, 3, 0));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(FG_DIM);
        lbl.setFont(MC_FONT_SM);
        lbl.setPreferredSize(new Dimension(80, 24));
        row.add(lbl, BorderLayout.WEST);

        styleInput(field);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JPanel btnRow(JButton... btns) {
        JPanel row = new JPanel(new GridLayout(1, btns.length, 10, 0));
        row.setBackground(BG2);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        for (JButton b : btns) row.add(b);
        return row;
    }

    private static JScrollPane wrapScroll(JPanel content) {
        JScrollPane sp = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBackground(BG2);
        sp.getViewport().setBackground(BG2);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private static void styleInput(JComponent c) {
        c.setFont(MC_FONT_SM);
        c.setBackground(BG3);
        c.setForeground(FG);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(2, 6, 2, 6)));
        if (c instanceof JTextField tf) tf.setCaretColor(COLOUR);
    }

    private static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(MC_FONT_SM);
        cb.setBackground(BG3);
        cb.setForeground(FG);
        cb.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBackground(isSelected ? COLOUR_DIM : BG3);
                l.setForeground(FG);
                l.setFont(MC_FONT_SM);
                l.setBorder(new EmptyBorder(2, 6, 2, 6));
                return l;
            }
        });
        return cb;
    }

    private static JButton COLOURBtn(String label) {
        JButton b = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getModel().isRollover() ? COLOUR : COLOUR_DIM);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(MC_FONT_SM);
        b.setForeground(FG);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOUR, 1),
            new EmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JLabel statusLabel() {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setFont(MC_FONT_SM);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return l;
    }

    private static void flash(JLabel l, String msg, Color color) {
        l.setText(msg);
        l.setForeground(color);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            public void run() { SwingUtilities.invokeLater(() -> l.setText("")); }
        }, 2500L);
    }

    private static void makeDraggable(JFrame frame) {
        makeDraggable(frame, frame.getContentPane());
    }

    private static void makeDraggable(JFrame frame, Component c) {
        final Point[] origin = {null};
        c.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { origin[0] = e.getPoint(); }
        });
        c.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) return;
                Point loc = frame.getLocation();
                frame.setLocation(loc.x + e.getX() - origin[0].x, loc.y + e.getY() - origin[0].y);
            }
        });
    }

    private static ContainerInput toAction(String s) {
        return switch (s) {
            case "QUICK_MOVE" -> ContainerInput.QUICK_MOVE;
            case "SWAP" -> ContainerInput.SWAP;
            case "CLONE" -> ContainerInput.CLONE;
            case "THROW" -> ContainerInput.THROW;
            case "QUICK_CRAFT" -> ContainerInput.QUICK_CRAFT;
            case "PICKUP_ALL" -> ContainerInput.PICKUP_ALL;
            default -> ContainerInput.PICKUP;
        };
    }
}