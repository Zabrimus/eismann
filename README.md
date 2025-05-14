# Epg/IPTV stream manager 

This web application uses the data of several repositories of https://github.com/iptv-org to manage EPG sites, live channel lists and so.
It is possible to check the m3u Link (including a video preview), to select an epg provider for a channel, to preview the epg provider homepage and to preview the channel homepage.

The export of the url list and the custom channel list is very rudimentary.

The application uses a sqlite3 database which collects all data of iptv-org in a custom database.
The database will be created once a day and can be found at https://github.com/Zabrimus/iptvdb/releases/latest.

## Build
### Requirements
Java JDK 17 or higher
Node.js 24 or higher (lower versions are not tested)

### Clone the repository
```
git clone <URL>
```

### Start the build
```
./gradlew quarkusBuild -Pvaadin.productionMode
```

## Running the Application (1. Test)
After the build a quick test of the application can be done with
```
java -Diptv.database=<path>/iptv-database.db -Dbookmark.database=<path>/bookmark.db -jar build/quarkus-app/quarkus-run.jar
```
Replace `<path>` with the path to the iptv database and the location, where you want to save the bookmark database.


## Deployment of the application
The folder `build/quarkus-app` contains the application and all necessary libraries and can copied to a folder of your choice.
```
java -Diptv.database=<path>/iptv-database.db -Dbookmark.database=<path>/bookmark.db -jar <path>/quarkus-run.jar
```
Change all `path` as described in the 1.Test and the destination of your deployment. 

## Development mode
If you want to run the application in development mode, then use the command
```
./gradlew -Diptv.database=<path>/iptv-database.db -Dbookmark.database=<path>/bookmark.db quarkusDev
```

## Browser
Open [http://localhost:8080/](http://localhost:8080/) in browser and enjoy the application.



# Create incus container
The whole installation of the epg grabber can be found at https://github.com/iptv-org/epg. The following chapter described a short description of an incus container for the epg grabber. 

## Launch container
```incus launch images:debian/stable epg-test``` 

## Install packages
```
apt install build-essential nano git curl unzip zip xsel
apt clean
```

## install node and java helper
```
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
curl -s "https://get.sdkman.io" | bash
```

## start a new bash shell
```
bash
```

## install node and java
```
nvm install node
sdk install java
```

## check version
```
root@epg-test:~# node -v
v24.0.1

root@epg-test:~# java -version
openjdk version "21.0.7" 2025-04-15 LTS
OpenJDK Runtime Environment Temurin-21.0.7+6 (build 21.0.7+6-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.7+6 (build 21.0.7+6-LTS, mixed mode, sharing)
```

## install iptv-epg
```
git clone --depth 1 -b master https://github.com/iptv-org/epg.git
cd epg
npm install
```

## copy, create or export a custom-channel.conf
```
nano /root/custom-channel.conf
```

## grab epg data
```
npm run grab --- --days=14 --channels=/root/custom-channel.conf
```

## check epg data
```
npx serve 
```
The command shows the ip and howto access the guide.xml.
`http://<ip>:3000/guide.xml`
But be aware, that there is no security at all! And possibly another solution is required.

# TODO:
Import the guide.xml in epg2vdr and/or tvscraper or via xml2vdr/xml4vdr.
If the server has been started with `npx serve` the guide.xml can be accessed via `http://<ip>:3000/guide.xml`.  


