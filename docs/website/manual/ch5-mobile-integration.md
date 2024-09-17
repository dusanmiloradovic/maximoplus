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
