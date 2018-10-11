/**
 * Stores session data in Android delegated via global Android object.
 * @type {InstanceDataStore}
 */
var androidSessionStore = {

  getSessionData: function() {
    var sessionData = android.getSessionData()
    return JSON.parse(sessionData)
  },

  setSessionData: function(sessionData) {
    android.setSessionData(JSON.stringify(sessionData))
    return true
  },

  deleteSessionData: function() {
    return android.deleteSessionData()
  }
}

getByte = function() {
return 1;
}

global.generate=function (len) {
  var res=new Uint8Array(len);for(var i=0;i<res.length;i++){res[i]=getByte();}
  return res;
}
