# Production deployment

## Client-side

Before creating production artifacts for the application, make sure the SERVER_ROOT variable from .env file points to the server URL. Always deploy the artifacts built with the production Expo mode.

## Server-side

There are a couple of MaximoPlus specific server properties you need to change before moving to production. All these properties can be defined either in the _maximo.properties_ file, or in the startup script (_start.sh_ or _start.bat_). By default, MaximoPlus uses the property file it reads from the  __maximo.ear__  file when it runs the _prepare_ script.
It is a good practice not to put MaximoPlus related properties in the main _maximo property file_. You should keep the separate copy of _maximo.properties_ instead, and make the changes there. To make MaximoPlus aware of that _maximo.properties_ file, you need to pass the _maximo.properties_ __parameter__ to the startup script

Example:

Copy the _maximo.properties_ file to the MaximoPlus root folder (where the start.sh script reside), and add the following to the _start.sh_ (or start.bat):

```sh
-Dmaximo.properties=maximo.properties
```

### MaximoPlus port

You may want to run several instances of MaximoPlus on one server. To change the port number, you have to use the __maximoplus.port__ property:

```sh
-Dmaximoplus.port=9001
```

Note: If you want to use a software load balancer or reverse proxy for MaximoPlus, prefer NGINX to Apache. MaximoPlus implements server-side events based streaming that may affect Apache performance.

### MaximoPlus login

A default installation of MaximoPlus aims at development - you can log in with any password, provided the user exists in Maximo, and have the appropriate privileges. To change this, set the __maximoplus.loginMethod__ property. If you set it to __maximo__, it uses the Maximo-based username and password authentication.
Put the following in your local _maximo.properties_ file:

```sh
maximoplus.loginMethod=maximo
```

### Manage the build process

Every time you deploy some java code changes to your Maximo application, you need to run the _prepare_ script you used when installing the  MaximoPlus server. It is undoubtedly a bad idea to edit the startup script and maximo.properties manually for every Maximo restart, so having a build script is essential.
After the _prepare_ script runs, all the Maximo changes are in the _maximo_jars/businessobjects.jar_ file, so you can copy just this file after the build.
If you use Gradle, Maven or Ant for your build process, it is easier to run the method on the class __maximoplus.prepare__ instead of running the shell script. Below is the example for Ant build:



```xml
<target name="prepare">
    <java classname="maximoplus.prepare">
        <classpath>
            <pathelement location="target/maximoplus-1.0.0-SNAPSHOT-standalone.jar" />
        </classpath>
        <arg value="depl" />
        <arg value="${maximo.ear}" />
    </java>
</target>
```

This snippet creates the _depl_ directory with the MaximoPlus server artifacts.
