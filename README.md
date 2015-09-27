# nanodav - tiny, basic WebDAV server in Java
#### Work in progress. Works some of the time...
nanodav is a light-weight WebDAV server designed for embeding in Android and JVM applications. It is based on [NanoHttpd](https://github.com/NanoHttpd/nanohttpd) and inspired by [GCDWebServer](https://github.com/swisspol/GCDWebServer). 

## Description
Implements a class 1 WebDAV server with partial class 2 support for OS X Finder using fake LOCK / UNLOCK.

## Usage

* Run the provided android-app and java-app demo applications
* Include it in your own projects:
  * On Android: copy the sources from the lib/ directories into your project.
  * On the JVM: copy the sources from the lib/ and xmlpull/ directories.
  
## Developer notes

nanodav uses a patched NanoHttpd server as a base class. The patches add support for DAV methods and for some extra HTTP status codes. If you need an updated version of NanoHttpd, just add the extra methods and status codes after updating.

For XML parsing, it uses the Android XmlPullParser library. As such, for usage on the JVM, the sources for XmlPullParser are included in the subproject named xmlpull. These sources are taken from the Android Open Source Project. 
