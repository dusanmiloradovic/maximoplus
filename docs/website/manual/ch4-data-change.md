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
