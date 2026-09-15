# Prototype privacy notes

Capit loads Instagram's HTTPS website in Android WebView. Your login, messages and other account activity
are transmitted to Instagram, under Instagram's policies. Capit has no separate service receiving this data.
The bundled filtering script inspects page links and limited post labels locally to hide distractions.
It does not save message text or credentials and does not call a Capit server.

WebView stores Instagram cookies and website storage within Capit's private app data to keep you signed in.
Android backup is disabled. Use **More → Clear Instagram session** or uninstall Capit to remove the session.
The app does not request contacts, broad photo-library access, accessibility, microphone or camera permissions.
If you choose a file using the system picker, Instagram can receive that selected file when you upload it.
External HTTPS links require a confirmation and open outside Capit, where its filters do not apply.

This file describes the code in this prototype, not a guarantee about Instagram's own data practices.
