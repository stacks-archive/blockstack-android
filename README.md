# Blockstack Android SDK

[![](https://jitpack.io/v/blockstack/blockstack-android.svg)](https://jitpack.io/#blockstack/blockstack-android)

This repository contains a pre-release version of the Blockstack Android SDK, a sample app that uses it and some tools used during development.

- [`/blockstack-sdk`](blockstack-sdk/) - the Blockstack Android SDK
- [`/example`](example/) - a demonstration Android app that uses the SDK
- [`/tools`](tools/) - tools used during development


## Get started

Below are step-by-step instructions for how to add Blockstack to your
Android app. Code samples are in Kotlin but the equivalent Java code should
work without any issues.

## Minimum requirements

This SDK assumes Android API 19 or higher.

### Configure your Blockstack web app

#### Step 1 - Choose a custom protocol handler

You'll need to choose a custom protocol handler that is unique to your app.

This is so that your app's web-based authentication redirect endpoint can redirect the user
back to your Android app.

In this example, we use `myblockstackapp:`.

#### Step 2 - Create redirect endpoint on web app

 Blockstack apps are identified by their domain names. You'll need to
 create an endpoint on the web version of your app that redirects users back
 to your mobile app.

 The endpoint will receive a get request with the query parameter `authResponse=XXXX`
 and should redirect the browser to `myblockstackapp:XXXX`.

 See the [example in the example web app in this repository](tools/blockstack-android-web-app/public/redirect.html).

### Configure your Android app project

#### Step 1 - Add custom protocol handler to app

In your app's `AndroidManifest.xml` file, add the following:

```XML
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data android:scheme="myblockstackapp" />
</intent-filter>
```

#### Step 2 - Add the Jitpack repository

Add the Jitpack repository to your root `build.gradle` at the end of repositories:

```JS
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

#### Step 3 - Add the dependency

Add the Blockstack Android SDK dependency to your project's dependency list:

```JS
dependencies {
  implementation 'com.github.blockstack:blockstack-android:-SNAPSHOT'
}
```

#### Step 4 - Import the package

```Kotlin
import org.blockstack.android.sdk.*
```


### Add session & authentication code

#### Step 1 - Create a new session

You'll need to replace `appDomain`, `redirectURI`, `manifestURI` and `scopes`
with values appropriate for your app.

```Kotlin
val appDomain = URI("https://flamboyant-darwin-d11c17.netlify.com")
val redirectURI = URI("${appDomain}/redirect")
val manifestURI = URI("${appDomain}/manifest.json")
val scopes = arrayOf("store_write")

val session = BlockstackSession(this, appDomain, redirectURI, manifestURI, scopes,
  onLoadedCallback = {
    // Enable sign in your app
    // signInButton.isEnabled = true
})
```

#### Step 2 - Handle sign in requests

In your app's Activity, retrieve the authentication token
from the custom protocol handler call and send it to the
Blockstack session.

```Kotlin
override fun onNewIntent(intent: Intent?) {
       super.onNewIntent(intent)  
  if (intent?.action == Intent.ACTION_VIEW) {
     val response = intent?.dataString
     if (response != null) {
       val authResponseTokens = response.split(':')
       if (authResponseTokens.size > 1) {
           val authResponse = authResponseTokens[1]
           session.handlePendingSignIn(authResponse)
       }
     }
   }

 }
```


#### Step 3 - Prompt the user to sign in

```Kotlin
session.redirectUserToSignIn { userData ->
  // signed in!

  // update your UI with signed in state
  // runOnUiThread {
  //     onSignIn(userData)
  // }
}
```

### Store and retrieve a file

#### Step 1 - Store a file

```Kotlin

// Configure options such as file encryption
val options = PutFileOptions()

val fileName = "message.txt"

// Can be a `String` or `ByteArray`
val content = "Hello Android!"

session.putFile(fileName, content, options,
  {readURL: String ->
    // File stored at URL in `readURL`
})
```

#### Step 2 - Read a file

```Kotlin

// Configure options such as whether to attempt file decryption
// or user/app from which to read a file
val options = GetFileOptions()

val fileName = "message.txt"

session.getFile(fileName, options, {content: Any ->
  // content can be a `String` or a `ByteArray`

  // do something with `content`
})
```

### Using the Sign in with Blockstack button

This SDK includes a themed "Sign in with Blockstack" button. You should use
this button in your app.

To use the themed button, use
the `org.blockstack.android.sdk.ui.BlockstackSignInButton` class.
