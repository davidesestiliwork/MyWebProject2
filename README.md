# MyWebProject2

GenerateAndDownloadHash is a servlet that generate checksums in GNU-style.

Has been tested on Wildlfy 10.1/Tomcat 9/Tomcat 8.

See src/config.properties for configuration.

New in release 2:

- Use a relative path instead of an absolute path
- Don't save a temporary file when calling SOAP Web Service
- Can exclude symbolic links
- Can exclude hidden files
- Use a security token (UUID) when calling SOAP Web Service
- The downloaded file is a zip file containing checksum and gpg signature
