package Satu;

import javax.swing.*;
import java.awt.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

public class Chat extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JPanel mainMenu;
    private JPanel selectContactMenu;
    private HashMap<String, ChatPanel> chatPanels;  // Menyimpan panel chat untuk setiap kontak
    private ArrayList<String> contactList; // Menyimpan daftar kontak
    private Key secretKey;

    public Chat() {
        setTitle("Aplikasi Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        mainMenu = new JPanel();
        selectContactMenu = new JPanel();
        chatPanels = new HashMap<>();
        contactList = new ArrayList<>();
        
        JButton selectContactButton = new JButton("Pilih Kontak");
        selectContactButton.addActionListener(e -> cardLayout.show(cards, "selectContact"));
        mainMenu.add(selectContactButton);

        JButton addContactButton = new JButton("Tambah Kontak");
        addContactButton.addActionListener(e -> addContact());
        mainMenu.add(addContactButton);
                
        JButton backButton = new JButton("Kembali");
        backButton.addActionListener(e -> cardLayout.show(cards, "main")); // Tombol Back untuk kembali ke mainMenu
        selectContactMenu.add(backButton);

        cards.add(mainMenu, "main");
        cards.add(selectContactMenu, "selectContact");

        add(cards);
    }

    private void addContact() {
        String newContact = JOptionPane.showInputDialog(this, "Masukkan nama kontak baru:");
        if (newContact != null && !newContact.trim().isEmpty()) {
            addContact(newContact);
        }
    }

    private void addContact(String contact) {
        if (!contactList.contains(contact)) {
            JButton contactButton = new JButton(contact);
            contactButton.addActionListener(e -> showChatPanel(contact));
            selectContactMenu.add(contactButton);

            contactList.add(contact);
            cards.revalidate();
            cards.repaint();
        }
    }

    private void showChatPanel(String contact) {
        if (!chatPanels.containsKey(contact)) {
            try {
                // Generate atau pertukarkan kunci dengan cara yang aman
                if (secretKey == null) {
                    secretKey = AESUtil.generateSecretKey();
                }

                ChatPanel chatPanel = new ChatPanel(contact, secretKey);
                chatPanels.put(contact, chatPanel);
                cards.add(chatPanel, contact);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        cardLayout.show(cards, contact);
    }

    private class ChatPanel extends JPanel {
        private JTextArea chatArea;
        private JTextField inputField;
        private ArrayList<String> messages;
        private Key secretKey;

        public ChatPanel(String contact, Key secretKey) {
            this.secretKey = secretKey;

            setLayout(new BorderLayout());

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(chatArea);
            add(scrollPane, BorderLayout.CENTER);

            inputField = new JTextField();
            inputField.addActionListener(e -> sendMessage(contact));
            
            JButton backButton = new JButton("Kembali");
            backButton.addActionListener(e -> cardLayout.show(cards, "selectContact"));
            JPanel inputPanel = new JPanel(new BorderLayout());
            JButton sendButton = new JButton("Kirim");
            sendButton.addActionListener(e -> sendMessage(contact));
            
            inputPanel.add(sendButton, BorderLayout.EAST);
            inputPanel.add(inputField, BorderLayout.CENTER);
            inputPanel.add(backButton, BorderLayout.WEST);
            add(inputPanel, BorderLayout.SOUTH);

            messages = new ArrayList<>();
        }

        private void sendMessage(String contact) {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                try {
                    String encryptedMessage = AESUtil.encrypt(message, secretKey);
                    System.out.print("You: " + message + " (Encrypted: " + encryptedMessage + ")\n");
                    messages.add("You: " + message);
                    inputField.setText("");

                    // Mendekripsi pesan seolah-olah kita menerima pesan dari pihak lain
                    receiveMessage(Base64.getEncoder().encodeToString(secretKey.getEncoded()) + "|" + encryptedMessage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void receiveMessage(String encryptedMessage) {
            try {
                String[] parts = encryptedMessage.split("\\|", 2);
                String encodedKey = parts[0];
                String encryptedText = parts[1];

                byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
                Key receivedKey = AESUtil.generateSecretKeyFromBytes(keyBytes);

                String decryptedMessage = AESUtil.decrypt(encryptedText, receivedKey);
                chatArea.append("You: " + decryptedMessage + "\n");
                messages.add("You: " + decryptedMessage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Chat chatApp = new Chat();
            chatApp.setVisible(true);
        });
    }
}