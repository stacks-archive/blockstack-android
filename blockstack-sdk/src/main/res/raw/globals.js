var blockstack={};var navigator={userAgent:"Android"}
var setLocation = (location) => {};
var window={set location(location) {
        setLocation(location);
    }
};
var global = typeof(global) == 'undefined' ? {Uint8Array:Uint8Array} : global;
if (!global.crypto){
  global.crypto = {
    getRandomValues : function(array) {
      throw Error("not secure getRandomValues")
    }
  };
}
var self = global;
var module = {
};
var exports = {};