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


