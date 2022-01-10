# tsca-webapp

## requirements

```
> lein -version
Leiningen 2.9.5 on Java 12.0.2 OpenJDK 64-Bit Server VM

> npm --version
6.14.8

> keytool -importkeystore -srckeystore cert.p12 -srcstoretype pkcs12 -destkeystore 
cert.jks

> cp cert.jks <repo-root>/ssl/keystroke.jks

> vi <repo-root>/project.clj
# edit [:shadow-cljs :ssl :password]
```



## build

```
# generate css
> lein garden once

# generate file "resources/public/js/compiled/app.js"
# kill other lein process if failed.
# this may take a few minutes...

> lein shadow release app


# generate file "resources/bookapp/js/compiled/book-app.js"

> lein shadow release book-app
```