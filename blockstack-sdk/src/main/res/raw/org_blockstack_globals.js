var blockstack={};
var navigator={userAgent:"Android"};
var setLocation = (location) => {
    android.setLocation(location)
};
// window location is needed for redirecting
var window={set location(location) {
        setLocation(location);
    }
};
var global = typeof(global) == 'undefined' ? {Uint8Array:Uint8Array} : global;
if (!global.crypto){
  global.crypto = {
    // overwritten in blockstack_android.js
    getRandomValues : function(array) {
      throw Error("not secure getRandomValues")
    }
  };
}
var self = global;
var module = {};
var exports = {};