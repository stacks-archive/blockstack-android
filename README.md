# Blockstack Android

[![](https://jitpack.io/v/blockstack/blockstack-android.svg)](https://jitpack.io/#blockstack/blockstack-android)

This repository contains a pre-release version of the Blockstack Android SDK, a sample app that uses it and some tools used during development.

- [`/blockstack-sdk`](blockstack-sdk/) - the Blockstack Android SDK
- [`/example`](example/) - a demonstration Android app that uses the SDK
- [`/tools`](tools/) - tools used during development


## Get started

### Step 1 - Add the Jitpack repository

Add the Jitpack repository to your root build.gradle at the end of repositories:

```JS
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

### Step 2 - Add the dependency

Add the Blockstack Android SDK dependency to your project's dependency list:

```JS
dependencies {
	        implementation 'com.github.blockstack:blockstack-android:-SNAPSHOT'
	}
```

### Step 3 - Import the package

```Kotlin
import org.blockstack.android.sdk.*
```

### Step 4 - Create a new session

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

### Step 5 - Prompt the user to sign in

```Kotlin
session.redirectUserToSignIn { userData ->
                // signed in!

                // update your UI with signed in state
                // runOnUiThread {
                //     onSignIn(userData)
                // }
            }
```
