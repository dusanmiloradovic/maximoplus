# MaximoPlus installation

## Prerequisites

- [Java JDK 11 or JDK 1.8](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
- Valid Maximo installation
- [Node.js and npm](https://nodejs.org/en/download/package-manager/)
- [Yarn](https://classic.yarnpkg.com/en/docs/install)
- [Git](https://git-scm.com/downloads)

MaximoPlus is a platform for building native Maximo-based mobile applications. Let's first see what is under the hood before we create an actual app.

## MaximoPlus server

MaximoPlus comes with a custom standalone server that runs Maximo and manages the MaximoPlus application sessions.

Before creating an application, we need to prepare and run the MaximoPlus server. The server has no Maximo binaries included; it needs to copy them from your  Maximo installation. The preparation process extracts the required binaries from Maximo and creates the MaximoPlus deployment.

### Installation steps:

Note: you may skip the download and installation steps from below and use Docker container instead. For more details take a look at our blog post [MaximoPlus Server installation with Docker](https://maximoplus.com/blog/maximoplus-installation-with-docker/)

0. Download MaximoPlus server from [Download](https://maximoplus.com/download.html)
1. Unzip the downloaded file
2. Run the __prepare__ script:

```sh
    prepare.bat <deployment_name> <path_to_maximo.ear_file> (Windows), or
    ./prepare.sh <deployment_name> <path_to_maximo.ear file> (Linux or Mac)
    
    Example: ./prepare.sh first_deployment  ~/IBM/SMP/maximo/deployment/default/maximo.ear
```

3. Start the deployment:

Change your directory to __<INSTALATION_DIR>/deployment/<deployment_name>__ , and run the __start.bat__ (for Windows), or __start.sh__ for Linux and Mac. If you use Java 8, run the start8.bat or start8.sh. Check if the server has started successfully. By default, it runs on port 8080

For an introduction to MaximoPlus development, checkout [Getting Started with MaximoPlus](https://maximoplus.com/blog/getting-started-with-maximoplus/) before you continue.


## MaximoPlus template

A template is a React Native application starter with the built-in MaximoPlus components. You will use [Expo](https://expo.io/) to run the apps on your device during the development, so install the Expo app on your device or emulator if you don't have it already.

If not already installed install the Expo CLI, and MaximoPlus CLI
```sh
npm install expo-cli -g
npm install create-mp-app -g
```

Run the following in your terminal:

```sh
create-mp-app
```

Enter the application name, IP, and port number, and proceed with the instructions:

![Create an app](https://maximoplus.com/images/mpcli-min.png)

The last command starts the Expo development server. Scan the QR code with Expo on your device or emulator to get started.

![Expo running](https://maximoplus.com/images/expostart-min.png)
