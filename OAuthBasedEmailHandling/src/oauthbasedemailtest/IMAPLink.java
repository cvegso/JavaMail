/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oauthbasedemailtest;

import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Folder;

/**
 *
 * @author csaba.vegso
 */
public class IMAPLink {

    public Session imapSession = null;
    public Store imapStore = null; 
    public Folder imapInboxFolder = null; 
    
    public IMAPLink(Session _imapSession, Store _imapStore, Folder _imapInboxFolder) {
        
        imapSession = _imapSession;
        imapStore = _imapStore;
        imapInboxFolder = _imapInboxFolder;
    }    
}
