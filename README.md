# Blockstack Android SDK (Pre-release)

[![](https://jitpack.io/v/blockstack/blockstack-android.svg)](https://jitpack.io/#blockstack/blockstack-android)

Blockstack is a platform for developing a new, decentralized internet, where
users control and manage their own information. Interested developers can create
applications for this new internet using the Blockstack platform.

This repository contains a pre-release for Android developers:

- the Blockstack Android SDK ([`/blockstack-sdk`](blockstack-sdk/))
- tools that assist development ([`/tools`](tools/))
- a tutorial that teaches you [how to use the SDK](docs/tutorial.md)


All of the material in this is a pre-release, if you encounter an issue please
feel free to log it [on this
repository](https://github.com/blockstack/blockstack-android/issues).

## Get started

Use the [detailed tutorial](docs/tutorial.md) and to build your first Blockstack
Android application with React. You can also work through three example apps in
module ([`/example`](examples/)),
([`/example-multi-activity`](example-multi-activity/)) and ([`/example-service`](example-service/)).

## Adding to your project
```
    repositories {
          maven { url 'https://jitpack.io' }
    }

    dependencies {
        implementation 'com.github.blockstack:blockstack-android:$blockstack_sdk_version'
    }
```

## Handling Blockstack sessions
The current implementation of the Blockstack Android SDK uses the j2v8 engine and blockstack.js. 

### Redirect with app links
The example applications and tutorial uses a custom url scheme to handle the redirect from the sign-in process. In production, the redirect should be handled by [app links](https://developer.android.com/studio/write/app-link-indexing) such that no other apps could hijack the custom url scheme. (There is no security risk, it is just a bad user experience if an app chooser pops up and the user has to choose how to finish the sign-in.)

Replace the custom scheme intent filter with the intent filter with your domain/host name like this:
```
   <activity android:name="./SignInActivity"
     ...>
     <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="https" android:host="example.com" />
     </intent-filter>
     ...
   </activity>
```           
   
Note, when using app links you do not need a web server anymore that redirects to the custom scheme. 
All you need to host is a `manifest.json` file for the app details and the assetlinks.json file for the app links.

### Thread Handling
The j2v8 engine requires that calls to the Blockstack session are made from only one thread.

The SDK comes with a default implementation of an `Executor` that manages thread switching. 
By default, network threads are done in the background, calls to the Blockstack session are done
on the main thread.

If the Blockstack session is not created on the main thread then a custom implementation of `Excecutor`
needs to be provided in the constructor of the Blockstack session. See the [service example](/example-service) for some code.   

It is also possible to manually switch threads buy using `.releaseThreadLock` and `.aquireThreadLock`.
These methods allow to make calls to the Blockstack session on a different thread. The thread lock
needs to be released on the current thread of the session. Then the new thread can acquire the thread lock.

### Sign-In Flow
The most basic way to sign-in a user with Blockstack is to use `redirectUserToSignIn`, 
`handlePendingAuthResponse` and all subsequent method calls in the same activity. This is shown in the 
[simple example](/example). However, applications usually have a separate screen to handle user sessions. 

In the [multi activity example](/example-multi-activities) a sign-in flow with two separate activities
is implemented, one for the main activity and one for the account handling.
The account handling activity updates the session data and the main activity
uses the same session store to retrieve the session data. The default
session store is using the default `SharedPreferences`, therefore, the 
session data is shared between all activities of the same app.


### Document Provider
Files stored on a gaia hub can be included in the user's device using
Android [Storage Access Framework (SAF)](https://developer.android.com/guide/topics/providers/document-provider). 
You should consider providing a document provider that allows the user
to access the files in the context of other apps as well.

The Android documentation provides a details guide how to build a 
 document provider. There exist open source examples provided by 
 the community, e.g. [OI ConvertCSV](https://github.com/openintents/convertcsv).



## API Reference Documentation
Please see [generated documenatation](https://124-124568327-gh.circle-artifacts.com/0/javadoc/blockstack-sdk/index.html)
on the project's circle CI.

## Regulatory Notes

### Export Compliance
The Blockstack Android SDK includes methods to encrypt data. 
Please consider whether you have to be compliant with US export law 
when you distribute your app via Google Play. See for example [Export Compliance](https://support.google.com/googleplay/android-developer/answer/113770?hl=en)

### Privacy and Data Protection
Blockstack helps you to create privacy-by-design apps as for example 
required by GDPR. 

In the context of GDPR, you should consider features 
to export and delete gaia files.


## Contributing
Please see the [contribution guidelines](CONTRIBUTING.md).

## License
Please see [license file](LICENSE)
