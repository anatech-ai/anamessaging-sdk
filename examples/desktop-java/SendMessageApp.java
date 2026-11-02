import com.anamessaging.sdk.AnaMessagingClient;
import com.anamessaging.sdk.exception.AnaMessagingException;
import com.anamessaging.sdk.exception.SubscriptionInactiveException;
import com.anamessaging.sdk.model.SendSmsRequest;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal desktop (Swing) sender - the pattern for POS, billing or Tally-style desktop tools.
 *
 *   javac -cp anamessaging-sdk-1.0.0-all.jar SendMessageApp.java
 *   java  -cp anamessaging-sdk-1.0.0-all.jar:. SendMessageApp      (use ; instead of : on Windows)
 *
 * Credentials come from ANAMESSAGING_CLIENT_ID / ANAMESSAGING_CLIENT_SECRET, never from the UI or source.
 */
public class SendMessageApp {
    public static void main(String[] args) {
        AnaMessagingClient ana = AnaMessagingClient.fromEnv();

        JTextField to = new JTextField("919876543210", 20);
        JTextArea text = new JTextArea("Your invoice is ready.", 5, 20);
        JComboBox<String> channel = new JComboBox<>(new String[] {"WhatsApp", "SMS"});
        JButton send = new JButton("Send");
        JLabel status = new JLabel(" ");

        send.addActionListener(e -> {
            send.setEnabled(false);
            status.setText("Sending...");
            // Network off the UI thread.
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    if ("SMS".equals(channel.getSelectedItem())) {
                        return ana.sms().send(SendSmsRequest.to(to.getText(), text.getText())).message;
                    }
                    return ana.whatsapp().sendText(to.getText(), text.getText()).message;
                }

                @Override
                protected void done() {
                    try {
                        status.setText(get());
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                        if (c instanceof SubscriptionInactiveException) status.setText("Subscription inactive - renew on anamessaging.com");
                        else if (c instanceof AnaMessagingException) status.setText("Error: " + c.getMessage());
                        else status.setText("Error: " + c);
                    }
                    send.setEnabled(true);
                }
            }.execute();
        });

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("To:"));
        top.add(to);
        top.add(channel);
        form.add(top, BorderLayout.NORTH);
        form.add(new JScrollPane(text), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        form.add(bottom, BorderLayout.SOUTH);
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JFrame frame = new JFrame("ANA Messaging");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(form);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
