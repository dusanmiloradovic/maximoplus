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
# MaximoPlus UI - components and templates

## Components

Components are the core of every React Native application, and MaximoPlus based apps are not an exception.  MaximoPlus Components are the basic building blocks of an app. They are very similar to the controls you have in Maximo Application Designer, only much more flexible and powerful.

You need to be familiar with elementary React and JSX to be able to continue reading this tutorial. If you are not, spend a couple of hours to learn the React basics or study it while following this tutorial.

The components come with the template you installed in Chapter 1. They are entirely open to any modifications and customizations you may want to make. If you are an experienced React user, you can check out the __components__ folder of the template.

Components only are not enough to create the app. Like any other application, we need the structure and the navigation system. MaximoPlus template fulfills this role; you can think of it as the  "skeleton." of an app.

The template uses the standard Expo project structure and React Navigation library. These are its most essential parts:

- .env file - Before you run the application, you need to specify the IP and port of the MaximoPlus server in the SERVER_ROOT entry of this file.  You don't need to populate this during the development; the create-mp-app CLI does it.
- App.js - The entry application file. Typically you will not make changes in it
- Containers.js - you will declare the containers (more about them later) in this file.
- components - folder with built-in Components of MaximoPlus. Typically, you don't need to change anything.
- screens - put your application screens in this folder
- dialogs - folder with built-in and custom dialogs
- navigation - React Navigation files. File DialogNavigation.js defines the navigation for dialogs, and you will use MainTabNabigtor for the screens.

## Containers

Containers are the "workhorses" of a MaximoPlus application. They send and receive data to and from the server on one side and visual components attached to it on the other. Containers are considered "final" in all templates, i.e., you can't customize them. The only thing you can do is to declare them. We use the file Containers.js of the Template to do it.

### AppContainer

Every MaximoPlus app must have an AppContainer defined. AppContainer operates on the Mbo of the application. For example, in our demo app, we have the "_po_" Mbo used by the "_po_" application. MaximoPlus Signature Security controls the access to read data, change data, and execute commands on the AppContainer. That also means you must have the app already defined in Maximo. If you need to create an app not related to any application in Maximo, you must first create the dummy Maximo application and set signature security for it.

AppContainer has the following properties:

- id - arbitrary id of the container (needed for the other components to relate to it)
 - mboname - the name of the Mbo AppContainer connects to
 - appname - the name of the Maximo application
 - wfprocess - an optional property, required if you want to implement workflows in an app. The name of the primary workflow process for the application
 - offlineenabled - is offline mode enabled for the app. Triggers automatic collection of the offline data

Example:

```jsx
 <AppContainer
      mboname="po"
      appname="po"
      id="pocont"
      wfprocess="postatus"
      offlineenabled={true}
  />
```

### RelContainer

This type of container is bound to the MboSet defined by the Maximo relationship, as set in the Maximo Database Configuration. It always has a parent container (AppContainer, another RelContainer, or any other type of Container).

Properties:

 - id - arbitrary id
 - container - id of the parent container
 - relationship - name of the relationship

Example:
Child poline container with the parent contanier po and the relationship poline:
```jsx
<RelContainer id="polinecont" container="pocont" relationship="poline" />
```

### SingleMboContainer

This Container creates a separate MboSet with only _one_ Mbo. This Mbo has the same id as a current Mbo in parent MboSet. Why we need this? For the starter, this is the default behavior in Maximo itself (on the Main tab), and some operations work correctly only on the MboSet with one Mbo. We will go into detail when discussing running the Maximo functions from MaximoPlus.

Properties:

- id - arbitrary id
- container - id of the parent container

```jsx
<SingleMboContainer id="posingle" container="pocont" />
```

## Visual Components

The three basic types of visual components are __lists__, __sections__, and __qbe sections__. We use Qbe Sections to search the data in the container, lists to display the list of data in the container, and sections to view and edit the data of the current record in the container.

### Lists

Lists are widespread controls in many mobile applications - for example, Gmail uses Lists to display the emails in your inbox. In MaximoPlus, the List Component is bound to the Container and receives the data from it on demand.

You have full control of how the data in the list is displayed. Each List requires a template - a simple function that defines how each row will be displayed based on the row data. Every row coming from the container will pass to the list item template you have set, and the result adds to the list. 

Properties:

- container - id of the container from which list reads the data
 - columns - an array of column names that are used by the Container. Picking just the info you need significantly improves the performance.
 - initdata - should the data be automatically fetched and displayed into the list
 - norows - the number of rows that control brings initially. Once the user scrolls to the bottom of the list,  Component automatically fetches the new rows.
 - listTemplate - id of the template used to display the data row. It is located in components/listTemplates.js file of the application template (see the example below)
 - selectableF - by default, when a user taps on the list row, MaximoPlus navigates to that row in Maximo MboSet. Most of the time, you want your application to perform some additional action, for example, to open the details screen. selectableF property is the function performed after the current row of MboSet is changed.

Example:


```jsx
 <MaxList
        listTemplate="po"
        container="pocont"
        columns={["ponum", "description", "status"]}
        selectableF={_ => NavigationService.navigate("POSection")}
        norows={20}
        initdata={true}
      />
```

In the example above, the list initially has 20 rows. When the user scrolls to the end of the list, more rows come from Maximo. When the user selects the row, the app navigates to another screen of the application. This is the  _po_ listTemplate from the __listTemplates.js__ file:

```jsx
  ({ DESCRIPTION, PONUM, STATUS }) => (
    <ListItem title={DESCRIPTION} subtitle={PONUM + " " + STATUS} />
  )
```

#### Exercise:

1. Include the RelContainer with the _po_ parent container and _poline_ relationship in the application.
2. Create the fourth tab in the application that contains the list bound to the _poline_ container. You will have to define a new list template for this.


### Sections

MaximoPlus Section is the Component that displays the current row of data in Maximo. In our template, it is named _Section_.
Using the Section is easy, we have to supply the container id, and the array of field names in order how they should appear on the screen.

Example:

```jsx
 <Section
  container="pocont"
  columns={[
    "ponum",
    "description",
    "status",
    "vendor",
    "shipvia",
    "totalcost"
  ]}
/>
```

MaximoPlus automatically gets the information about these columns: labels, data type, domains, and more. Based on the data type, it automatically creates children components - text fields or the checkboxes, and display their default labels. 

#### Metadata
The information about the _Mbo_ __attributes__ is called metadata in MaximoPlus. It is used by sections (and other controls) to display labels and choose the appropriate child types. Metadata comes automatically from Maximo when the Section is created, along with the labels, data types, domain names, etc. of the attribute.

In addition to the metadata that we get from a MaximoPlus server, you can define additional metadata per column. We use this when we customize our controls to change its default behavior. You need to supply the object with the attribute name as a key, and the metadata information as a value.

Metadata is the only way to customize the behavior of the Section in MaximoPlus. You can use the built-in metadata configuration, or create your own. For better understanding, take a look at the example below:


```jsx
<Section
  container="pocont"
   columns={[
     "ponum",
     "description",
     "status",
     "vendor",
     "shipvia",
     "totalcost"
   ]}
   metadata={{
     SHIPVIA: {
       hasLookup: true,
       listTemplate: "valuelist"
     }
   }}
 />
```

We added the metadata to the _SHIPVIA_ field in which we inform the control that it has a lookup, and its list and filter templates are _valuelist_.

#### Value list dialogs

When entering data in sections, it is a frequent requirement to pick a value from the list.

To define the value list for the Section, you need to supply the following metadata attributes:

- hasLookup - true if there is the lookup on the field
- listTemplate - value list template


If we attach the metadata to the field with the lookup information (like in the previous paragraph), MaximoPlus will handle displaying the value list dialogs. The mechanism of value lists is the same as in Maximo itself; the system will return a domain data or the list of values defined by the field class.

Displaying of the list of values is handled by the same list and list template mechanism that we already discussed. Internally, MaximoPlus opens a dialog, creates a container for the list of values, and attaches the list control to it. You need only to provide the template in the __listTemplate__ metadata attribute. 

It is often the case that the list of values is too long, and we need to filter its result. To define the list filter, we use the __filterTemplate__ attribute.  The filter template is the Qbe Section that we will cover soon.

#### Built in metadata customizations

Apart from the value list metadata, we have two more custom metadata attributes:

- phonenum - if the field contains the telephone number, you can use this metadata attribute to add the phone calling control next to the field
- barcode - if you need to enter information by scanning the barcode, use this attribute to add  that  option for the field

Example:

```jsx
<Section
  container="posingle"
  columns={[
    "ponum",
    "description",
    "status",
    "shipvia",
    "orderdate",
    "vendor",
    "vendor.phone",
    "revcomments"
  ]}
  metadata={{
    "VENDOR.PHONE": { phonenum: true },
    REVCOMMENTS: { barcode: true, label: "Barcode test" }
  }}
/>
```

#### Developing the custom metadata attributes

We are not giving the step by step instruction on how to achieve this; high-level guidance is enough to provide the gist of the idea. You can skip this paragraph on the first read.

In our template, Section field can be either the Text Field, CheckBox, or the Picker. We will focus on the TextField, but the same principles apply for other field types.

The full code of the TextField Component is in components/section/TextField.js

Let's say we want to create the SMS sending metadata, with the control that sends some dummy messages to the phone number contained in the attribute. We will add the following piece of code in __render__ method of the components:

```js
 if (this.props.metadata && this.props.metadata.sms) {
   const smsF = () => SMS.sendSMSAsync(this.props.value, "Test Message");
   rightAction = { type: "font-awesome", name: "envelope", onPress: smsF};
 }
```


### Qbe Section

Qbe Sections (_QbeSection_ in the template) is the control used for searching the data. We define them in the same way sections are defined:
```jsx
<QbeSection
  container="pocont"
  columns={["ponum", "description", "status", "shipvia"]}
  navigateTo="POList"
  metadata={{
    SHIPVIA: {
      hasLookup: true,
      listTemplate: "qbevaluelist",
      filterTemplate: "qbevaluelist"
    },
    STATUS: {
      hasLookup: true,
      listTemplate: "qbevaluelist",
      filterTemplate: "qbevaluelist"
    }
  }}
/>
```

The only difference in appearance compared to Sections is that the boolean fields also appear as text boxes (same as in Maximo).
Lookup metadata is almost the same, with one exception: _listTemplate_ is now _qbevaluelist_.  Unlike Sections, you can pick more than one value from the value list (for example, search the Purchase Orders where the status is _APPR_ or _CLOSE_). You can check the source of _qbevaluelist_ in __list-templates.js__. The difference to the _valuelist_ template is that it uses a virtual column __SELECTED_, which sets to "Y" if the record is marked as selected in Maximo.

Once the user presses the Search button in a Qbe Section, they expect redirection to the tab containing the list of data. The property _navigateTo_ serves that purpose. Once the search is complete, the application will navigate to the screen defined in that property.

#### Exercise:

In our demo, there is no value list attached to the vendor field. The list of values for this field would return data from the __COMPANIES__ Mbo.  Insert the _companies_ and _qbecompanies_ templates in the **listTemplates.js**, and attach the value lookup to the vendor field, both in section and qbe section.
# MaximoPlus Components - changing the Maximo data

So far, all the examples we had were displaying the data. However, if you tried to navigate to some PO in status _WAPPR_, you could've seen that the data is editable. MaximoPlus controls the editability automatically, with the rules defined in Maximo.
Once you changed the data, you probably want the change reflected in Maximo. Unlike some of the Maximo mobile frameworks, it doesn't save data to Maximo automatically - you have full control over your transactions.

## Saving of the data

We use the core API utility function __save__  to save the record. It has only one argument, the id of the AppContainer:

```javascript
save("pocont");
```
 
### Container functions return promises

If you tried to execute the save function example in the browser console, you could see the function returns javascript _Promise_. For example:

```javascript
save("pocont")
.then(_=>console.log("save executed successfully|)
.catch(err=>cosnole.log(err));
```

All the functions running on Containers return Promises, which is a nice feature if you have a complex chain of interactions in your app.

## Running Mbo and MboSet functions and MaximoPlus dialogs

One unique feature of MaximoPlus is the option to execute the methods on Mbo or MboSet. The method has to be public and has no input parameters. We use this when we need to perform some non-trivial operation, like changing the status of adjusting the inventory balance, for example.

The following example executes the _execute_ method on the Mbo:

```javascript
mboCommand(containerId,"execute");
```

For a command on Mbo use the __mboCommand__ and for MboSet __mboSetCommand__ (the signature is the same).

Just like in Maximo, most of the time, you will use __dialogs__ to perform these actions.

### Creating Dialogs

We use the dialogs to perform the action that is independent of the rest of the application form, for example, route the workflow.
The dialogs in the Template are plain React Navigation Screens that open from anywhere in the application. Some of them open automatically from the MaximoPlus library when a specific action is required - open a value list, or a Workflow Dialog, for example. In contrast, others can be called explicitly from the application. Both kinds are in the _dialogs_ folder of the template.

Below is the example for the empty starter dialog:

```js
import React,{PureComponent} from "react";

export default class extends PureComponent {
  static navigationOptions = ({ navigation }) => {
    return {
      headerTitle: "Dialog Title"
    };
  };
  render() {
    return (
      <>
      </>
    );
  }
}
```

Like every other page in the template, we need to connect it explicitly in the React Navigation configuration. You should add all the dialogs into the DialogStack const object of the file __DialogNavigaiton.js__. For example:

```js
ournewdialog:{screen:OurDialog}
```

(You should, of course, first import the _OurDialog_ screen in the _DialogNavigation_.

To open the dialog, you use the __openDialog__ API function from the utils/utils.js file.

```js
openDialog({type:"ournewdialog"});
```

### Putting the logic inside the dialog

Use the following pattern:

- Define the relationship to the non-persistent MboSet that will do the actual change
- Bind the user control to that MboSet
- When a user presses the button or some other UI element, call the method on the Mbo of that MboSet (usually the __execute__ method)

If you have done any Maximo customizations in the past, you know that we do precisely that in Maximo itself.

Example of the PO status change:

```jsx
<>
  <RelContainer
    id="pochangestatus"
    container="posingle"
    relationship="pochangestatus"
  />
  <Section
    container="pochangestatus"
    columns={["status", "memo"]}
    metadata={{
      STATUS: {
        hasLookup: "true",
        listTemplate: "valuelist",
        listColumns: ["value", "description"]
      }
    }}
  />
  <Button title="OK" onPress={()=> mboSetCommand("pochangesatus","execute")} />
</>
```

In this example, _pochangestatus_ container relationship points to the non-persistent object _POCHANGESTATUS_, with the Java class _psdi.app.po.virtual.POChangeStatusSet_. Like most of the non-persistent Mbos, its class has the _execute_ method, and we use it to do the actual status change. 

As you can see from above, you need to know what method and object are required for the dialog. If you are a pure front-end developer without Maximo knowledge, you should get this information from your Maximo technical team.

## Running Mbo commands and Signature Security

You must have noticed that the mechanism described above makes it possible to execute _any_ public method on Mbo or MboSet. To protect access to Maximo, we use the same approach as Maximo itself - Signature Security.

There is one subtle difference between Maximo and MaximoPlus: In Maximo, you grant the access on the level of Maximo Control; for example, when you create a new dialog, you give access to that dialog. 

![Signature Security for MaximoPlus](https://maximoplus.com/doc/maximo_sig_sec.png)

In MaximoPlus, we define access for each Mbo method we need to call from MaximoPlus. In our above example, for the PO status, we need to grant access to the __execute__ method of the __POCHANGESTATUS__ Mbo. Maximo Signature Security is defined only on the level of the application, but we need something to designate the Mbo object. For that, we will use the __DESCRIPTION__ field in the Signature Security application. 
The format of the description field is **#[Name of the Relationship]**. In the above example, it is _#POCHANGESTATUS_.

It is also widespread to have the same method name used frequently in non-persistent relationships. For example, we can have the _execute_ method on another non-persistent Mbo related to our PO  object. The problem is, Signature Security name needs to be unique. In MaximoPlus, you name the options **EXECUTE, EXECUTE_1, EXECUTE_2**, and so on. MaximoPlus Signature Security ignores everything after the underscore sign. 


## Workflow routing

MaximoPlus has built-in functionality for routing the Maximo workflow. To route call the React Native Application API function  - _openWorkflowDialog_

```javascript
openWorkflowDialog(container,processName);
```

In our case : 
```javascript
openWorkflowDialog("posingle","POSTATUS")
```

The functionality is the same as in Maximo, just adapted for the mobile device. No separate signature security option is required.
# Mobile Features and Third-Party Libraries Integration

Integrating React Native functionality and libraries is trivial using MaximoPlus. Several essential features are available in the template itself, and we will also demonstrate how to add new ones.


## Built-in integrations

The most apparent feature we require is a  Phone call. We can achieve that quickly by adding the simple metadata attribute to the section.

### Phone calls

To enable  phone calls, add the metadata to the section field containing the phone number:

```js
metadata={{
  "VENDOR.PHONE": { phonenum: true }
}}
```

This code adds the phone call button to the  _VENDOR.PHONE_ field.

### Barcode Scanning

To enable the barcode scanning, we use a similar technique; the metadata attribute _barcode_ marks the field as a barcode enabled.

```js
metadata={{
  "VENDOR.PHONE": { phonenum: true },
  PO9: { barcode: true, title: "Barcode test1" }
}}
```

### Camera upload

The API function __openPhotoUpload__ uploads he photo to the Maximo Doclinks. You need to import it first:

```js
import {openPhotoUpload} from "../utils/utils";
```

The function takes one argument, the id of the container. For example:

```js
const takePicture = () => openPhotoUpload("posingle");
```

You may call this action from anywhere, for example, from a button on the React Navigation toolbar - see [_example_](https://maximoplus.com.blog/get-access-to-your).

### Document upload
To upload the file from the device, Google Drive or Apple Cloud, use the __openDocumentUpload__ API function:

```js
import { openDocumentUpload } from "../utils/utils";
```

The function takes one argument, the id of the container. For example:

```js
const takePicture = () => openDocumentUpload("posingle");
```

### View Maximo documents
To open the Document Viewer dialog, use the __openDocumentViewer__ API function. It also takes the contanier id as an argument:

```js
import { openDocumentViewer } from "../utils/utils";

const openDocViewer = () => openDocumentViewer("posingle");
```


## Custom Integrations

If you want to create the field action on the section, you use the __metadata__ mechanism, and customize the _components/section/TextField.js_ file. The following code for enabling the SMS sending is trivial and self-explanatory:

```js
if (this.props.metadata && this.props.metadata.sms) {
  const phoneF = () => SMS.sendSMSAsync(this.props.value,"Test message");
  rightAction = { type: "font-awesome", name: "phone", onPress: phoneF };
}
```


## Component Adapter

Sometimes you need to perform a bit more complex integration, for example, to integrate the full-featured Component like Google Maps. Other times you want to create the Component, which is not in the original set of Components. For example, let's say we want to create the Component that sends the SMS.
In our SMS example from above, the text in the SMS message is hardcoded, our Component will copy the text from some other attribute.

You can make the new Component that is either bound to a single row or multiple rows. Whether the Component is a single row or multi-row is determined by the _norows_ property passed to it.

To create the Adapter component, use the __getComponentAdapter__ API function:

```js
const MyAdapter = getComponentAdapter(Adapter)
```

__Adapter__ is a React Component you need to create to make this work. It needs to have the following props:

For a single-row component:

- __data__ - the current row data. You use this to display the data in the Component
- __setMaxValue__ function to change the Maximo value . It has two parameters - __columnName__ and __value__

For a multi-rows component:

- __maxrows__ an array of Maximo data
- __setMaxRowValue__ function to change the Maximo value. It has three parameters - __rowNum__ , __columnName__ and __value__
- __fetchMore__ a function to get the more data for the component. It has only one parameter __numberOfRows__
-- __moveToRow__ a function to move the current row of the MboSet. It has one parameter, __rowNum__.


If you need to read the adapter value from the outside, your __Adapter__ component class has to have the __getValue__ function to return the current value.

You use the adapter component in the same way you would've used the Section or List :

```jsx
<MyAdapter
  container="mycont"
  columns={["col1","col2",...]}
/>
```

For a more detailed example, look at the following blog post: [Beyond the template](https://maximoplus.com/blog/beyound-the-template/)
# Offline Mode

MaximoPlus provides an automatic offline mode option. When offline mode is enabled, it automatically stores and synchronizes offline data on the device.

## Enabling the offline mode

To enable the offline mode for the application, set the _offlineenabled_ property of the application container to _true_.

```jsx
<AppContainer
  mboname="po"
  appname="po"
  id="pocont"
  offlineenabled={true}
/>
```

Offline mode activates automatically when the device loses the network connection. You can enable or deactivate the offline mode manually by calling the following core API function.

```javascript
setOffline(true);
```

This function is useful for development, or if you want to have more control in your application.

## Offline lists

By default, MaximoPlus stores just the data that was visited when the device was online. Some value lists may have millions of records, so storing automatically every List in the application is not an option. Value lists are stored offline in full only when the attribute metadata column __storeOffline__ is __true__  :

```jsx
<Section
  container="pocont"
  columns={[
    "ponum",
    "description",
    "status",
    "shipvia",
    "orderdate",
    "vendor"
  ]}
  metadata={{
    STATUS: {
      hasLookup: "true",
      listTemplate: "valuelist",
      filterTemplate: "valuelist",
      offlineReturnColumn: "VALUE"
    },
    SHIPVIA: {
      hasLookup: "true",
      listTemplate: "valuelist",
      filterTemplate: "valuelist",
      storeOffline: "true",
      offlineReturnColumn: "VALUE"
    }
  }}
/>
```

As you can see from the snippet above, we need one more metadata attribute for the offline lists: __offlineReturnColumn__. In the offline mode, we need to know which column from the value list is populated into the field (while online that is taken care of by Maximo). For example, if we have the value list with the columns _VALUE_ and _DESCRIPTION_, setting the __offlineReturnColumn__ to _VALUE_ means that MaximoPlus copies the _VALUE_ content from the chosen record into the field.

Downloading the value lists is an expensive operation, and it is done automatically only once in the lifetime of the application. If there is a need to reload offline value lists, use the following core API function:

```javascript
maximoplus.basecontrols.reloadPreloadedLists();
```

## Data Preloading

Sometimes your application may need to offload the complete set of data required for the user of the mobile app, especially when the application is used mostly in offline mode. Below is the API function for that:

```javascript
preloadOffline();
```

Be careful when using this function; by default, it tries to offload the complete set of data of the Application Container and all its related Containers. Make sure there is a QBE set on the application container to restrict the number of rows. 

To clear the offline data set, use the following function:

```javascript
unloadOffline();
```

## Data Synchronization

When the device goes online, it sends the data changes to the server and saves all the changes. A MaximoPlus template reports the error for each record change submitted to the server.  In case of any failure, the application opens the _OfflineErrorDialog.js_ dialog from the template.

## Offline search

MaximoPlus supports full Maximo QBE search on its offline objects and lists. Make sure you educate the users that it only searches the offloaded data and design your applications accordingly.
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
