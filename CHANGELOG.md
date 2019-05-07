# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.4.5] - 2019-05-07

### Added
- `BlockstackSession.getFileUrl` added, giving access to the read url for a file.
- Support for 64bit-apks

### Changed
- Upgraded to blockstack.js 19.1.0
- Upgraded android tools and plugins
- Replace custom scheme in example with verified app links

## [0.4.4] - 2019-03-08

### Added
- `betaMode` flag in constructor of `BlockstackSession` to enable auth with beta.browser.blockstack.org

## [0.4.3] - 2019-02-01

### Added
- `Network` added, providing API methods for the blockstack network
- `BlockstackSession.network` added, giving access to the blockstack network used in this session
- `BlockstackSession.listFiles` iterates through the user's app files
- `BlockstackSession.generateAndStoreTransitKey`, and `.redirectToSignInWithAuthRequest` to allow custom auth flow
- `Scope.fromJSName` creates `Scope` from its javascript name as used in blockstack.js
- `contentType` property in `PutFileOptions`


### Changed
- Use current blockstack.js version from feature/session-storage (18.2.1 with user session handling)
- removed params from appConfig of `makeAuthRequest`



## [0.4.2] - 2018-12-08

### Changed
- fixed issues #117 and #118 for putFile and getFile
- Uses custom tabs instead of normal browser view for auth

## [0.4.1] - 2018-11-05

### Changed
- `Executor` API: `.onWorkerThread` renamed to `.onNetworkThread`, added: `.onV8Thread`
- removed the requirement for `singleTask` launch mode on sign in


## [0.4.0] - 2018-10-29

### Added
- `BlockstackSession.validateProofs` method and `Proof` object to verify social acounts.
- `BlockstackSession.getAppBucketUrl` method to retrieve the user's bucket url of the app.
- `BlockstackSession.getUserAppFileUrl` method to retrieve a user's file of an app with a given path.
- `UserData.hubUrl` property for use with `getUserAppFileUrl`
- Parameter `sessionStore`, `executor` and `scriptRepo` to `BlockstackSession` constructor
- New example for using `Blockstack` in a background service


### Changed
- Improved integration tests
- Updated `blockstack.js` to branch `storage-strategies`
- `BlockstackConfig.redirectUrl` and `.manifestUrl` take a relative path
  instead of fully-qualified URL
- Switched javascript engine from `WebView` to `j2v8` which enables
  usage of SDK as a background service and improved execution speed

### Removed
- `Blockstack.makeAuthRequest`
- callback parameter from `BlockstackSession.isUserSignedIn`, `.loadUserData`, `encryptContent`, `decryptContent` and `.signUserOut`

## [0.3.0] - 2018-07-27

### Added
- A `BlockstackConfig` object to make it easier to use across several activities. Thanks
to @friedger.
- `BlockstackSession.encryptContent`, `BlockstackSession.decryptContent` methods
- `BlockstackSession.lookupProfile` method which allows lookup of the profile of an arbitrary user
- `UserData.Profile` object that contains avatar image and email as well
- `Result<T>` object that can have a value of type T or errors, used for callbacks

### Changed
- Fixed a bug where loadUserData would throw an exception if the user is not logged in.
Thanks to @friedger.
- Updated `blockstack.js` to v18.0.0
- Renamed `UserData.did` to `UserData.decentralizedID`
- All method callbacks (except `isUserSignedIn`, `lookupProfile`) now take a `Result<T>`` object as parameter.


## [0.2.0] - 2018-06-25

### Added
- This change log.
- A themed "Sign in with Blockstack" button that supports English, German, French,
and Chinese. Thanks to @friedger for this.

### Changed
- Start using "changelog" over "change log" since it's the common usage.
- API operations will now throw an `IllegalStateException` if the Blockstack
session has not fully initialized. You should wait for `onLoadedCallback`
to fire before using other API methods.
- Properly set version in grade

## [0.1.0] - 2018-06-23
- Initial release.
