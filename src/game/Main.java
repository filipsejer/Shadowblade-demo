package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Spellblade");
            List<Image> icons = new ArrayList<>();
            for (int size : new int[]{16, 32, 64, 128, 256}) icons.add(icon(size));
            frame.setIconImages(icons);
            try {                                                             // the Dock icon on a Mac (the window icon elsewhere)
                if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) Taskbar.getTaskbar().setIconImage(icons.get(icons.size() - 1));
            } catch (RuntimeException ignored) { }
            GamePanel panel = new GamePanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) { panel.onClose(); }
            });
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
            panel.start();
        });
    }

    /** The game's icon: the legendary sunsteel longsword, raised, on a night-blue tile. */
    static BufferedImage icon(int size) {
        Sprite sword = SwordArt.sprite(SwordArt.forItem(new Item(Item.Slot.WEAPON, Item.Rarity.LEGENDARY, "Longsword", 0, java.util.Map.of(), null, 0)), -Math.PI / 4, false);
        BufferedImage src = sword.img;
        int x0 = src.getWidth(), y0 = src.getHeight(), x1 = -1, y1 = -1;   // crop to the painted pixels
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
            if ((src.getRGB(x, y) >>> 24) == 0) continue;
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x);
            y1 = Math.max(y1, y);
        }
        int w = x1 - x0 + 1, h = y1 - y0 + 1;

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = Math.max(1, size / 16), arc = size / 4;
        g.setColor(new Color(212, 168, 80));
        g.fillRoundRect(pad, pad, size - 2 * pad, size - 2 * pad, arc, arc);
        int rim = Math.max(1, size / 32);
        g.setColor(new Color(22, 26, 52));
        g.fillRoundRect(pad + rim, pad + rim, size - 2 * (pad + rim), size - 2 * (pad + rim), arc, arc);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, size >= 64 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double room = size - 2 * (pad + rim) - size / 8.0;
        double k = room / Math.max(w, h);
        if (size >= 64) k = Math.max(1, Math.floor(k));                      // whole pixels, so the art stays crisp
        int dw = (int) Math.round(w * k), dh = (int) Math.round(h * k);
        g.drawImage(src, (size - dw) / 2, (size - dh) / 2, (size - dw) / 2 + dw, (size - dh) / 2 + dh, x0, y0, x1 + 1, y1 + 1, null);
        g.dispose();
        return out;
    }
}
