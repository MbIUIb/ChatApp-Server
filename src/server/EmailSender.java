package server;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Класс, позволяющий работать с email
 *
 * @author Kirill Chezlov
 * @version 1.0
 */
public class EmailSender {
    private final String serverEmailAddress = System.getenv("EMAIL_ADDR");
    private final String password = System.getenv("EMAIL_PASSWORD");
    private final String emailAddress;
    private final Session session;

    /**
     * конструктор класса {@code EmailSender}.
     * создает сессию и подкличается к серверу почтового хоста
     * @param emailAddress email получателя
     */
    public EmailSender(String emailAddress) {
        this.emailAddress = emailAddress;
        Properties prop = new Properties();
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

    /**
     * отправляет письмо пользователю
     * @param subj тема письма
     * @param text сообщение
     */
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
