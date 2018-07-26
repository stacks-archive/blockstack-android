# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- A `BlockstackConfig` object to make it easier to use across several activities. Thanks
to @friedger.
- `BlockstackSession.encryptContent`, `BlockstackSession.decryptContent` methods
- `BlockstackSession.lookupProfile` method
- `UserData.Profile` object that contains avatar image and email as well
- `Result<T>` object that can have a value of type T or errors, used for callbacks

### Changed
- Fixed a bug where loadUserData would throw an exception if the user is not logged in.
Thanks to @friedger.
- Using blockstack.js 18.0.0
- Renaming UserData.did to UserData.decentralizedID
- All method callbacks (but isUserSignedIn, lookupProfile) now take a Result<T> object as parameter.


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
