# TDGateway
A library that provides access to the Devices and IoT Message layers on the Telecom Design Sensor IoT API.

## Installation
In your project's `build.gradle` add jitpack's repository:
```gradle
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```
And then just add the following dependency under your module's `build.gradle` file:
```gradle
dependencies {
	...
	compile 'com.github.mikiloz92:tdgateway:0.0.1'
}
```

## Usage
### The `SensorApiManager` class:
First of all, you need to instantiate the `SensorApiManager` class, that controls the network request and manages all your devices:
```java
SensorApiManager apiManager = new SensorApiManager(ctx);
```
The API manager constructor needs a `Context` instance. You can pass the current Activity, for example. Though if you plan to use the same API manager instance across different activities it would be better to pass the **base context**.

If you want to use the [device operations](https://developers.insgroup.fr/iot/device.html#change_status) of the Sensor API, you have to provide a developer authorization token to the manager. For that purpose, there is a second constructor:
```java
SensorApiManager apiManager = new SensorApiManager(ctx, "authtokenexample==");
```

This is because in order to perform **device operations**, the library needs to know the **ID** of the device. In order to obtain this ID, it has to query the *Developer API*. Alternatively, you can programmatically set the device ID by yourself, if you already know it. We'll take a look into that.

### The `IotDevice` class:
The `IotDevice` class represents an IoT device as seen by the API. You can do all the CRUD operations that you can perform in the `Device API` of Telecom Design Sensor's platform.

To create an `IotDevice` instance you have to do it via the `SensorApiManager` instance:
```java
IotDevice device0 = apiManager.newIotDeviceForDeviceApi("0123", "89ABCDEF", ...);
IotDevice device1 = apiManager.newIotDeviceForDeveloperApi("124953171", "0123", ...);
IotDevice device2 = apiManager.newIotDevice("124953171", "0123", "89ABCDEF", ...);
```
As you can see, there are three different constructors:
* The first one is used when you want to create a device for use with the *Device API* (this will allow you to check its messages but you won't be able to retrieve information from the device itself). You need the serial number and the key of the device. If you supplied a valid developer authentication token to the api manager, it will query the *Developer API* for this device ID and you'll be able to perform **device operations** with this device.
* The second one is for when you want to create a device for use with the *Developer API* (you'll be able to check the information from the device itself, but won't be able to check its messages). You need the ID and the serial number of the device.
* The last one provides both device information **and** message checking. You have to pass to the constructor the ID, the serial number and the key of the device.

Also, on all constructors you have to supply a pair of `Runnable`s, that will be called whenever the API manager has obtained an authentication token from the *Device API*, and when the API manager has obtained the information of this device, respectively. Also, a `SensorApiErrorListener` has to be supplied for error checking too, as it consists of an asynchronous operation. For more info you can check that on the javadocs.

#### Performing queries
You can query message data using the device instance. For this, the `IotDevice` class provides wrappers for every API call, as seen [here](https://developers.insgroup.fr/iot/device.html#msgs_history). Of course, to perform device operations, you need to supply a developer authentication token to the api manager, or set the device ID on its constructor, if you already know it.
