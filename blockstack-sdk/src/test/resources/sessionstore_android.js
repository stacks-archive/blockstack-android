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