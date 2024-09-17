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
