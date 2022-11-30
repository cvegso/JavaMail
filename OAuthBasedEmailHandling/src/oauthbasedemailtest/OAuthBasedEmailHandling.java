/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package oauthbasedemailtest;

import java.net.URI;
import java.util.Properties;
import java.util.Date;

import org.dmfs.oauth2.client.*;
import org.dmfs.oauth2.client.grants.ClientCredentialsGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.httpessentials.client.*;
import org.dmfs.httpessentials.httpurlconnection.*;

import javax.mail.*;
import javax.mail.internet.*;

/**
 * Test application to show how to receive and send emails using JavaMail
 * and OAuth together.
 * 
 * @author csaba.vegso
 */
public class OAuthBasedEmailHandling {
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        /**********************************************************
         * Getting an OAuth access token from the identity provider.
         *********************************************************/
        
        String tokenEndpointUrl = "https://login.microsoftonline.com/4b06f4ad-d7a3-4447-81be-1171e5282290/oauth2/v2.0/token";
        String clientId = "625aee37-afbe-4a54-956d-80ed0a9c7cf7";
        String clientSecret = "XXXXXXXXXXXXXX";
        String clientScopes = "https://outlook.office365.com/.default"; // comma separated list of scopes
        
        String accessToken = authenticateToIdentityServer(tokenEndpointUrl, clientId, clientSecret, clientScopes.split(","));
               
        if(accessToken == null) {
            System.out.println("Failed to get an OAuth access token");
            return;
        }
        
        /**********************************************************
         * Setting common parameters for IMAP and SMTP.
         *********************************************************/

        String userName = "cetest@geomantpartner.com";
        int connectionTimeoutMsecs = 10000;
        int commandTimeoutMsecs = 15000;
        
        /**********************************************************
         * Checking mailbox (e.g. unread email count) via IMAP.
         * 
         * NOTE: JavaMail does not support OAuth for POP3. It supports 
         * OAuth2 for IMAP only!!!
         *********************************************************/

        String imapHost = "outlook.office365.com";
        int imapPort = 993;
        
        IMAPLink imapLink = connectToIMAPServer(userName, accessToken, 
                imapHost, imapPort, connectionTimeoutMsecs, commandTimeoutMsecs);
        
        if(imapLink == null) {
            System.out.println("Failed to connect to the mail server via IMAP");
            return;
        }
        
        Integer emailCount = getEmailCountFromIMAPService(imapLink);
        
        if(emailCount == null) {
            System.out.println("Number of unread email in inboux: UNKNOWN");
        }
        else {
            System.out.println("Number of unread email in inboux: " + String.valueOf(emailCount.intValue()));
        }
        
        disconnectFromIMAPServer(imapLink);
        
        /**********************************************************
         * Sending an email via SMTP.
         * 
         * NOTE: Microsoft Exchange Online still does not support 
         * the Client Credential Grant flow for SMTP. Thus the 
         * following SMTP + OAuth2 based email sending procedure 
         * will fail with Microsoft Exchange Online!!!
         *********************************************************/

        String smtpHost = "smtp.office365.com";
        int smtpPort = 587;

        SMTPLink smtpLink = connectToSMTPServer(userName, accessToken, 
                smtpHost, smtpPort, connectionTimeoutMsecs, commandTimeoutMsecs);

        if(smtpLink == null) {
            System.out.println("Failed to connect to the mail server via SMTP");
            return;
        }

        String toAddress = "csaba.vegso@gmail.com";
        String subject = "Test message from CE";
        String emailText = "Testing Exchange Online via OAuth";

        boolean emailSent = sendEmail(smtpLink, userName, toAddress, subject, emailText);

        System.out.println("Email sent: " + String.valueOf(emailSent));
        
        disconnectFromSMTPServer(smtpLink);
    }
    
    /**
     * It authenticates to the OAuth identity server and returns an access token upon successful
     * authentication.
     * 
     * @param tokenEndpointUrl The URL of the identity server endpoint responsible to issue access
     * token.
     * @param clientId The unique id of the application.
     * @param clientSecret The secret associated with the client id.
     * @param clientScopes The scopes to request the access token for.
     * @return The access token upon successful authentication. Null of failure.
     */
    private static String authenticateToIdentityServer(String tokenEndpointUrl, String clientId, String clientSecret, String[] clientScopes) {
        
        try {
            /*
            Creating HttpRequestExecutor to execute HTTP requests.
            Any other HttpRequestExecutor implementaion will would be suitable.
            */
            
            HttpRequestExecutor executor = new HttpUrlConnectionExecutor();

            /*
            Creating OAuth2 provider.
            */
            
            OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
                null, // specifying no authorization endpoint URL; it is not required for the Client Credential Grant flow
                URI.create(tokenEndpointUrl),
                null // specifying no token lifetime; purely relying on the identity server's lifetimne policy
            );

            /*
            Creating OAuth2 client credentials.
            */
            
            OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(
                clientId, clientSecret);        

            /*
            Creating OAuth2 client.
            */
            
            OAuth2Client client = new BasicOAuth2Client(
                provider,
                credentials,
                (LazyUri)null // specifying no redirect URL; it is not required for the Client Credential Grant flow
            );     

            /*
            Requesting access token using a Client Credentials Grant.
            */
            
            OAuth2AccessToken token = new ClientCredentialsGrant(client, new BasicScope(clientScopes)).accessToken(executor);   
        
            String accessToken = token.accessToken().toString();
            DateTime expirationTimeUtc = token.expirationDate();
            DateTime currentTimeUtc = DateTime.now();
            
            System.out.println("Access token: " + accessToken);
            System.out.println("Expiration time (UTC): " + expirationTimeUtc.toString() + ", current time (UTC): " + currentTimeUtc);
            
            return accessToken;
        }
        catch(Exception ex) {
            System.out.print(ex);
            return null;
        }
    }
    
    /**
     * It connect to the specified IMAP server using the given OAuth access token.
     * 
     * @param userName The user name to send email with.
     * @param accessToken The OAuth access token.
     * @param imapHost The FQDN of the IMAP server.
     * @param imapPort The FQDN of the IMAP server.
     * @param connectionTimeoutMsecs The connection timeout in milliseconds.
     * @param commandTimeoutMsecs Command timeout on the connection in milliseconds.
     * @return The IMAP link upon successful connect. Null on failure.
     */
    private static IMAPLink connectToIMAPServer(String userName, String accessToken,
            String imapHost, int imapPort, int connectionTimeoutMsecs, 
            int commandTimeoutMsecs) {
        
        try {
            Properties prop = new Properties();

            prop.put("mail.store.protocol", "imap");
            prop.put("mail.imap.connectiontimeout", String.valueOf(connectionTimeoutMsecs));
            prop.put("mail.imap.timeout", String.valueOf(commandTimeoutMsecs));
            prop.put("mail.imap.ssl.trust", imapHost);
            prop.put("mail.imap.ssl.enable", "true");
            prop.put("mail.imap.auth.mechanisms", "XOAUTH2");
            prop.put("mail.debug", "true");
            prop.put("mail.debug.auth", "true");

            Session imapSession = Session.getInstance(prop);
            Store imapStore = imapSession.getStore("imap");
                               
            imapStore.connect(imapHost, imapPort, userName, accessToken);
            
            if(!imapStore.isConnected()) {
                return null;
            }
            
            Folder imapInboxFolder = imapStore.getFolder("INBOX");
            imapInboxFolder.open(Folder.READ_WRITE);

            if(!imapInboxFolder.isOpen()) {
                imapStore.close();
                return null;
            }
            
            return new IMAPLink(imapSession, imapStore, imapInboxFolder);
        }
        catch(Exception ex) {
            System.out.print(ex);
            return null;
        }        
    }
    
    /**
     * It disconnects from the specified IMAP folder.
     * 
     * @param imapLink IMAP folder to disconnect from.
     */
    private static void disconnectFromIMAPServer(IMAPLink imapLink) {
        
        try {
            if(imapLink.imapInboxFolder.isOpen()) {
                imapLink.imapInboxFolder.close();
            }
            
            if(imapLink.imapStore.isConnected()) {
                imapLink.imapStore.close();
            }
        }
        catch(Exception ex) {
            System.out.print(ex);
        }        
    }
    
    /**
     * It returns the number of unread messages in the specified email inbox.
     * 
     * @param imapLink The email inbox to return unread message count for.
     * @return The number of unread messages on success. Null on failure.
     */
    private static Integer getEmailCountFromIMAPService(IMAPLink imapLink) {
        
        try {
            return imapLink.imapInboxFolder.getMessageCount();
        }
        catch(Exception ex) {
            System.out.print(ex);
            return null;
        }        
    }
    
    /**
     * It connect to the specified SMTP server using the given OAuth access token.
     * 
     * @param userName The user name to send email with.
     * @param accessToken The OAuth access token.
     * @param smtpHost The FQDN of the SMTP server.
     * @param smtpPort The TCP port of the SMTP server.
     * @param connectionTimeoutMsecs The connection timeout in milliseconds.
     * @param commandTimeoutMsecs Command timeout on the connection in milliseconds.
     * @return The SMTP link upon successful connect. Null on failure.
     */
    private static SMTPLink connectToSMTPServer(String userName, String accessToken,
            String smtpHost, int smtpPort, int connectionTimeoutMsecs, 
            int commandTimeoutMsecs) {
        
        try {
            Properties prop = new Properties();
            
            prop.put("mail.transport.protocol", "smtp");
            prop.put("mail.smtp.host", smtpHost);
            prop.put("mail.smtp.port", String.valueOf(smtpPort));            
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.starttls.required", "true");            
            prop.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeoutMsecs));
            prop.put("mail.smtp.timeout", String.valueOf(commandTimeoutMsecs));
            prop.put("mail.stmp.sendpartial", "true");            
            prop.put("mail.smtp.ssl.enable", "true");
            prop.put("mail.smtp.ssl.trust", smtpHost);            
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            
            Session smtpSession = Session.getInstance(prop);
            Transport smtpTransport = smtpSession.getTransport("smtp");
            
            smtpTransport.connect(smtpHost, smtpPort, userName, accessToken);
            
            return smtpTransport.isConnected() ? new SMTPLink(smtpSession, smtpTransport) : null;
        }
        catch(Exception ex) {
            System.out.print(ex);
            return null;
        }
    }
    
    /**
     * It closes the specified SMTP connection.
     * 
     * @param smtpLink SMTP connection to be closed.
     */
    private static void disconnectFromSMTPServer(SMTPLink smtpLink) {
        
        try {
            if(smtpLink.smtpTransport.isConnected()) {
                smtpLink.smtpTransport.close();
            }
        }
        catch(Exception ex) {
            System.out.print(ex);
        }
    }
    
    /**
     * It sends an email via the specified SMTP link with the given properties.
     * 
     * @param smtpLink The SMTP link to send the email trhough.
     * @param fromAddress The From address of the email.
     * @param toAddress The To address of the email.
     * @param subject The subject of the email.
     * @param messageText The message body of the email.
     * @return True if the email is send successfully.
     */
    private static boolean sendEmail(SMTPLink smtpLink, String fromAddress, String toAddress,
            String subject, String messageText) {
        
        try {
            String charSet = "UTF-8";
            
            MimeMessage msg = new MimeMessage(smtpLink.smtpSession);
            
            msg.setFrom(new InternetAddress(fromAddress));
            msg.setSender(new InternetAddress(fromAddress));
            
            Address replyToAddrs[] = new InternetAddress[1];
            replyToAddrs[0] = new InternetAddress(fromAddress);
            msg.setReplyTo(replyToAddrs);
            
            Address toAddrs[] = InternetAddress.parse(toAddress);
            msg.setRecipients(Message.RecipientType.TO, toAddrs);
            
            msg.setSubject(subject, charSet);            
            msg.setText(messageText, charSet);
            msg.setSentDate(new Date());
            
            smtpLink.smtpTransport.sendMessage(msg, msg.getAllRecipients());
            
            return true;
        }
        catch(Exception ex) {
            System.out.print(ex);
            return false;
        }
    }
}
