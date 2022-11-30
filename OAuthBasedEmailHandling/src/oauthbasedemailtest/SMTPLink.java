/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oauthbasedemailtest;

import javax.mail.Session;
import javax.mail.Transport;

/**
 *
 * @author csaba.vegso
 */
public class SMTPLink {

    public Session smtpSession = null;
    public Transport smtpTransport = null; 
    
    public SMTPLink(Session _smtpSession, Transport _smtpTransport) {
        
        smtpSession = _smtpSession;
        smtpTransport = _smtpTransport;
    }
}
