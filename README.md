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

If you want to use the [device operations](https://developers.insgroup.fr/iot/device.html#change_status) of the Sensor API, you have to provide a developer authorization token to the manager. For that purpose, you have a second constructor:
```java
SensorApiManager apiManager = new SensorApiManager(ctx, "authtokenexample==");
```

This is because in order to perform **device operations**, the library needs to know the **id** of the device. In order to obtain this id, it has to query the *Developer API*. Alternatively, you can programatically set the id by yourself, if you already know it. We'll take a look into that.

### The `IotDevice` class:
The `IotDevice` class represents an IoT device as seen by the API. You can perform all the CRUD operations that you can perform in the `Device API` of Telecom Design Sensor's platform.

To create an `IotDevice` instance you have to do it via the `SensorApiManager` instance:
```java
IotDevice device0 = apiManager.newIotDeviceForDeviceApi("0123", "89ABCDEF", ...);
IotDevice device1 = apiManager.newIotDeviceForDeveloperApi("124953171", "0123", ...);
IotDevice device2 = apiManager.newIotDevice("124953171", "0123", "89ABCDEF", ...);
```
As you can see, there are three different constructors:
* The first one is used when you want to create a device for use with the *Device API* (that will allow you to check its messages but you won't be able to retrieve information from the device itself). You need the serial number and the key of the device.
* The second one is for when you want to create a device for use with the *Developer API* (you'll be able to check the information from the device itself, but won't be able to check its messages). You need the id and the serial number of the device.
* The last one provides both device information **and** message checking. You have to pass to the constructor the id, the serial number and the key of the device.

Also, on all constructors you have to supply a pair of `Runnable`s, that will be called whenever the API manager has obtained an authentication token from the *Device API*, and when the API manager has obtained the information of this device, respectively. Also, a `SensorApiErrorListener` has to be supplied too, for error checking, as it it consists of an asynchronous operation. For more info you can check that on the javadocs.

