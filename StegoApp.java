import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class StegoApp extends JFrame {

    private JTextArea coverArea, secretArea, outputArea;
    private JLabel statusLabel;

    // Zero-width characters for encoding bits
    private static final char ZW0 = '\u200B'; // zero-width space  = bit 0
    private static final char ZW1 = '\u200C'; // zero-width non-joiner = bit 1
    private static final char SEP = '\u200D'; // zero-width joiner = separator

    public StegoApp() {
        setTitle("Text Steganography System");
        setSize(820, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Header
        JLabel hdr = new JLabel("🔒 Text Steganography System", SwingConstants.CENTER);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 20));
        hdr.setOpaque(true); hdr.setBackground(new Color(50, 60, 120));
        hdr.setForeground(Color.WHITE); hdr.setBorder(new EmptyBorder(12,0,12,0));
        add(hdr, BorderLayout.NORTH);

        // Center: 3 text areas
        JPanel center = new JPanel(new GridLayout(1, 3, 8, 0));
        center.setBorder(new EmptyBorder(10, 10, 6, 10));

        coverArea  = area("Cover Text (visible content)");
        secretArea = area("Secret Message");
        outputArea = area("Encoded / Decoded Output");

        center.add(scroll(coverArea, "📄 Cover Text"));
        center.add(scroll(secretArea, "🔑 Secret Message"));
        center.add(scroll(outputArea, "📤 Output"));
        add(center, BorderLayout.CENTER);

        // Bottom: buttons + status
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(4, 10, 10, 10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.add(btn("📂 Load Cover File",   new Color(70,130,180),  e -> loadFile()));
        btnRow.add(btn("🔐 Encode & Save",     new Color(34,139,87),   e -> encode()));
        btnRow.add(btn("📂 Load Encoded File", new Color(130,80,180),  e -> loadEncoded()));
        btnRow.add(btn("🔓 Decode Message",    new Color(200,100,30),  e -> decode()));
        btnRow.add(btn("🗑 Clear All",         new Color(160,60,60),   e -> clearAll()));
        bottom.add(btnRow, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setBorder(new EmptyBorder(6,0,0,0));
        bottom.add(statusLabel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    // ── Encode ───────────────────────────────────────────────────
    private void encode() {
        String cover  = coverArea.getText().trim();
        String secret = secretArea.getText().trim();
        if (cover.isEmpty() || secret.isEmpty()) { status("Cover text and secret message required."); return; }

        // Convert secret to binary string, wrapped with separator
        StringBuilder bits = new StringBuilder();
        for (char c : secret.toCharArray())
            for (int i = 7; i >= 0; i--)
                bits.append(((c >> i) & 1) == 0 ? ZW0 : ZW1);
        String hidden = SEP + bits.toString() + SEP;

        // Inject hidden string after first word (visible content unchanged)
        int spaceIdx = cover.indexOf(' ');
        String encoded = spaceIdx < 0
            ? cover + hidden
            : cover.substring(0, spaceIdx) + hidden + cover.substring(spaceIdx);

        outputArea.setText(encoded);

        // Save to file
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("encoded_output.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(fc.getSelectedFile().toPath(), encoded);
                status("✅ Encoded and saved to: " + fc.getSelectedFile().getName());
            } catch (IOException ex) { status("Save error: " + ex.getMessage()); }
        }
    }

    // ── Decode ───────────────────────────────────────────────────
    private void decode() {
        String text = outputArea.getText();
        if (text.isEmpty()) { status("Load an encoded file first."); return; }

        int start = text.indexOf(SEP);
        int end   = text.indexOf(SEP, start + 1);
        if (start < 0 || end <= start) { status("❌ No hidden message found."); return; }

        String bits = text.substring(start + 1, end);
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i + 7 < bits.length(); i += 8) {
            int val = 0;
            for (int b = 0; b < 8; b++)
                val = (val << 1) | (bits.charAt(i + b) == ZW1 ? 1 : 0);
            msg.append((char) val);
        }

        secretArea.setText(msg.toString());
        status("✅ Hidden message decoded successfully!");
    }

    // ── File I/O ─────────────────────────────────────────────────
    private void loadFile() {
        String content = pickFile();
        if (content != null) { coverArea.setText(content); status("Cover file loaded."); }
    }

    private void loadEncoded() {
        String content = pickFile();
        if (content != null) { outputArea.setText(content); status("Encoded file loaded. Click Decode."); }
    }

    private String pickFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        try { return Files.readString(fc.getSelectedFile().toPath()); }
        catch (IOException e) { status("Read error: " + e.getMessage()); return null; }
    }

    private void clearAll() {
        coverArea.setText(""); secretArea.setText(""); outputArea.setText("");
        status("Cleared.");
    }

    // ── Helpers ───────────────────────────────────────────────────
    private JTextArea area(String placeholder) {
        JTextArea ta = new JTextArea();
        ta.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setToolTipText(placeholder);
        return ta;
    }

    private JScrollPane scroll(JTextArea ta, String title) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    private JButton btn(String text, Color bg, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    private void status(String msg) { statusLabel.setText(msg); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StegoApp::new);
    }
}
