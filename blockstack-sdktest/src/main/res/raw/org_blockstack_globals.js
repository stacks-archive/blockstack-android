var blockstack={};
var navigator={userAgent:"Android"};
var setLocation = (location) => {
    android.setLocation(location)
};

var crypto = {
    // overwritten in blockstack_android.js
    getRandomValues : function(array) {
      throw Error("not secure getRandomValues")
    }
}


var global = typeof(global) == 'undefined' ? {Uint8Array:Uint8Array} : global;
if (!global.crypto) {
  global.crypto = crypto
};


global.navigator = navigator;

// window location is needed for redirecting
global.location = {
    set href(loc) {
        setLocation(loc);
    }
};

var window = global;
var self = global;

var module = {};
var exports = {};
