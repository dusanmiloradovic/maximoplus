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
