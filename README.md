# Blockstack Android SDK (Pre-release)

[![](https://jitpack.io/v/blockstack/blockstack-android.svg)](https://jitpack.io/#blockstack/blockstack-android)

Blockstack is a platform for developing a new, decentralized internet, where
users control and manage their own information. Interested developers can create
applications for this new internet using the Blockstack platform.

This repository contains a pre-release for Android developers:

- the Blockstack Android SDK ([`/blockstack-sdk`](blockstack-sdk/))
- tools that assist development ([`/tools`](tools/))


All of the material in this is a pre-release, if you encounter an issue please
feel free to log it [on this
repository](https://github.com/blockstack/blockstack-android/issues).

## Upgrade to 0.5.0
Apps using SDK version below 0.5.0 need to make following changes:

- Use `BlockstackSignIn.redirectUserToSignIn` instead of `BlockstackSession.redirectUserToSignIn`.
- Replace result handling in callbacks of `BlockstackSession.getFile` by directly handling the result.
- Handle `IllegalStateException` exception when calling `BlockstackSession.loadUserData` while 
the user is not signed in.
- Handle `Result.error` as type `ResultError`, not as type `String`.

For a complete list of changes with the 0.5.0 upgrade, see [this commit](https://github.com/blockstack/blockstack-android/commit/ca88a12fa5e4fd028caef5d54253c6cb1fbd94b0).

## Get started

Use the [detailed tutorial](https://docs.blockstack.org/android/tutorial.html) and to build your first Blockstack
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
`BlockstackSession`s use a session store to persist their state. 

The default implementation of `ISessionStore` uses the default shared preferences. If an apps needs 
to use two `BlockstackSession`s then both should use the same session store instance. Failure to use 
the same session store instance can cause situations where a user is logged out of one session 
while still being logged into the other.


### Redirect with app links
The example applications and tutorial uses a custom url scheme to handle the redirect 
from the sign-in process. While suitable for samples, do not use this scheme in a production app.

In production, handle the redirect with [app links](https://developer.android.com/studio/write/app-link-indexing) 
such that no other apps could hijack the custom url scheme. Hijacking in this manner is not a security risk; It 
is simply a bad user experience if an app chooser pops up and the user has to choose how to finish the sign-in.

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
The Android SDK uses Kotlin's Coroutines. Network requests are using the [`Dispatchers.IO` dispatcher](https://developer.android.com/kotlin/coroutines#main-safety).

### Sign-In Flow
To sign-in a user with Blockstack your app should use `BlockstackSignIn.redirectUserToSignIn` and 
`BlockstackSession.handlePendingAuthResponse`. After the auth response is handled, 
use `BlockstackSession` to manage the user's data.  

In the [simple example](/example), both sign-in and data management happens in the same activity. 
However, applications usually have a separate activity to handle user sign-in. 

In the [multi activity example](/example-multi-activities) a sign-in flow with two separate activities
is implemented, one for the main activity and one for the account handling.
The account handling activity updates the session data and the main activity
uses the same session store to retrieve the session data. The
`SessionStore is using the default `SharedPreferences`, therefore, the 
session data is shared between the two activities of the app.


### Document Provider
Files stored on a gaia hub can be included in the user's device using
Android [Storage Access Framework (SAF)](https://developer.android.com/guide/topics/providers/document-provider). 
You should consider providing a document provider that allows the user
to access the files in the context of other apps as well.

The Android documentation provides a details guide how to build a 
 document provider. There exist open source examples provided by 
 the community, e.g. [OI ConvertCSV](https://github.com/openintents/convertcsv).



## API Reference Documentation
Please see [generated documenatation](https://blockstack.github.io/blockstack-android/index.html).

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
