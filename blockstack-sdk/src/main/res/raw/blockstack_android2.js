blockstack = module.exports
blockstack.handlePendingSignIn = function(authResponseToken) {
  userSession.handlePendingSignIn(authResponseToken)
  .then(function(userData) {
    console.log("user data")
    var userDataString = JSON.stringify(userData)
    console.log("userData " + userDataString)
    android.signInSuccess(userDataString)
  }, function(error) {
  console.log("user data"  + error.toString())
    android.signInFailure(error.toString())
  })
}

blockstack.loadUserData2 = function() {
  var userData = userSession.loadUserData()
  if (userData != null) {
    return JSON.stringify(userData)
  } else {
    return null
  }
}

blockstack.lookupProfile2 = function(username, zoneLookupFileURL) {
  blockstack.lookupProfile(username, zoneLookupFileURL)
  .then(function(userData) {
      android.lookupProfileResult(username, JSON.stringify(userData))
  }, function(error) {
      android.lookupProfileFailure(username, error.toString())
  })
}

blockstack.getFile = function(path, options, uniqueIdentifier) {
    const opts = JSON.parse(options)
    userSession.getFile(path, opts)
      .then(function(result) {
         console.log("get file result")
         var isArrayBuffer = result instanceof ArrayBuffer
         var isBuffer = result instanceof Uint8Array
         var binary = isArrayBuffer || isBuffer
         if (binary) {
           result = Base64.encode(result)
         }
         android.getFileResult(result, uniqueIdentifier, binary)
      }, function(error) {
        console.log("get file failure:" + error)
        android.getFileFailure(error.toString(), uniqueIdentifier)
      })
}

blockstack.putFile = function(path, contentString, options, uniqueIdentifier, binary) {
  var content = null;
  if (binary) {
    content = Base64.decode(contentString)
  } else {
    content = contentString
  }
  userSession.putFile(path, content, JSON.parse(options))
    .then(function(result) {
      console.log("put result:" + result)
      android.putFileResult(result, uniqueIdentifier)
    }, function(error) {
      console.log("put failure:" + error)
      android.putFileFailure(error.toString(), uniqueIdentifier)
    })
}


blockstack.encryptContent = function(contentString, options) {
    console.log("encrypt content");
    result = userSession.encryptContent(contentString, JSON.parse(options));
    return result;
}

blockstack.decryptContent = function(cipherTextString, options, binary) {
    console.log("decrypt content");
    return userSession.decryptContent(cipherTextString, JSON.parse(options))
}

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

/*****************
 Methods related to javascript fetch
******************/

function Response(r) {
  this.status = r.status
  if (r.bodyEncoded) {
    this.body = Base64.decode(r.body)
  } else {
    this.body = r.body
  }
  this.headers = r.headers
  this.headers.get = function(name) {
    return r.headers[name.toLowerCase()]
  }
  this.ok = this.status >= 200 && this.status < 300;
}


Response.prototype.json = function() {

  return this.text().then(function(text) {
    return JSON.parse(text);
  });

}


Response.prototype.text = function() {

  var _this = this;

  return new Promise(function(resolve, reject) {
    resolve(_this.body);
  });

}

Response.prototype.arrayBuffer = function() {
  var _this = this;

  return new Promise(function(resolve, reject) {
    console.log("arrayBuffer() - body is ArrayBuffer: " + (_this.body instanceof ArrayBuffer).toString())
    resolve(_this.body)
  });
}

var fetchPromises = {}
fetch = function(url, options){
  var promise = new Promise(function(resolve, reject) {
    console.log('fetch ' + url)
    fetchPromises.resolve = resolve
    if (options) {
        if (options.body instanceof ArrayBuffer) {
          options.body = Base64.encode(options.body)
          options.bodyEncoded = true
        }
    } else {
      options = {};
    }
        try {
          android.fetch2(url, JSON.stringify(options))
        } catch (e) {
          console.log("fetch error " + e.toString())
        }
  })
  return promise
}

blockstack.fetchResolve = function(url, response) {
  console.log('resolved ' + url)
  try {
    var resp = new Response(JSON.parse(response))
    console.log('resolved ' + resp.status)
    fetchPromises.resolve(resp)
    return "success"
  } catch (e) {
    console.log("error:" + e.toString())
    return "error " + e.toString()
  }
}

blockstack.timeout = function() {
  fakeEventLoop()
}

global.generate=function (len) {
  var res=new Uint8Array(len);
  global.crypto.getRandomValues(res);
  return res;
}

var fakeEventLoop;
var setTimeout;
var clearTimeout;

(function () {
    var timers = [];

    setTimeout = function (fn, timeout) {
        console.log("setTimeout " + fn)
        timers.push(fn);
    };

    clearTimeout = function () {};

    fakeEventLoop = function () {
        console.log('fake eventloop, run timers');
        while (timers.length > 0) {
            var fn = timers.shift();
            console.log('run timer ' + fn);
            try {
                fn();
            } catch (e) {
                console.log("error in eventLoop " + e)
                throw Error("failed to run " + fn, e)
            }
        }
        console.log('fake eventloop exiting, no more timers');
    };
})();

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/****************
Methods related to redirect in blockstack.js
****************/
setLocation = (location) => {
  var auth = location.substring(location.indexOf("?") + 13)
  blockstack.verifyAuthRequest(auth).then(function(result) {
    android.setLocation(location)
  }, function(error) { console.log("error " + error.toString())})
}