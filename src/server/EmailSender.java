package server;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {
    private final String serverEmailAddress = "";
    private final String password = "";
    private final String emailAddress;
    private Properties prop;
    private Session session;

    public EmailSender(String emailAddress) {
        this.emailAddress = emailAddress;
        prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(serverEmailAddress, password);
                    }
                });
    }

    public void sendMessage(String subj, String text) {
        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("mbiuib7@gmail.com"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emailAddress)
            );
            message.setSubject(subj);
            message.setText(text);

            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
