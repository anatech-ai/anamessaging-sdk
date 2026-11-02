# Desktop (Java Swing)

A one-file Swing window that sends a WhatsApp text or SMS. Use the same pattern for POS, billing and
other desktop tools. The send runs off the UI thread, and errors (including an inactive
subscription) are shown in the window.

```bash
cd ../../java && mvn package && cd -
javac -cp ../../java/target/anamessaging-sdk-1.0.0-all.jar SendMessageApp.java
ANAMESSAGING_CLIENT_ID=... ANAMESSAGING_CLIENT_SECRET=... \
  java -cp ../../java/target/anamessaging-sdk-1.0.0-all.jar:. SendMessageApp
```

For a desktop app installed on many customer PCs, don't put one shared secret in the installer.
Either have each customer enter their own ANA credentials (and store them in the OS keychain), or
route sends through your own server.
