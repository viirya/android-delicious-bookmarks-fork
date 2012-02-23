# A fork of android-delicious-bookmarks project

[android-delicious-bookmarks] (http://code.google.com/p/android-delicious-bookmarks/) is an android application used to post your bookmarks from browsers to Delicious service.

The reason to fork the project is there is a SSL exception when posting bookmarks recently. The issue ["javax.net.ssl.SSLException: Not trusted server certificate"] (http://code.google.com/p/android/issues/detail?id=1946) seems a known problem on Android. In experiment, even importing keystore into the application, it still does not solve the issue. So currently the modification accepts the certificate from Delicious even it fails the checking.

## Update

On Android 4.0.X, the problem seems not existing. So it doesn't to accept all certificates.


