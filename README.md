# MyWebProject2

GenerateAndDownloadHash is a servlet that generate checksums in BSD-style.

Has been tested on Wildlfy 10.1/Tomcat 9.

See src/config.properties for configuration.

New in release 2:

- Use a relative path instead of an absolute path
- Don't save a temporary file when calling SOAP Web Service
- Can exclude symbolic links
- Use a security token (UUID) when calling SOAP Web Service
