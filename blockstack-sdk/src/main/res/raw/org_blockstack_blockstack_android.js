blockstack = module.exports
var blockstackAndroid = {}
var userSessionAndroid = {}
var networkAndroid = {}

userSessionAndroid.handlePendingSignIn = function(authResponseToken) {
  try {
    userSession.handlePendingSignIn(authResponseToken)
    .then(function(userData) {
        var userDataString = JSON.stringify(userData)
        android.signInSuccess(userDataString)
    }, function(error) {
        android.signInFailure(error.toString())
    })
  } catch(error) {
    android.signInFailure(error.toString())
  }
}

blockstackAndroid.getAppBucketUrl = function(gaiaHubUrl, appPrivateKey) {
  blockstack.getAppBucketUrl(gaiaHubUrl, appPrivateKey)
  .then(function(url) {
        android.getAppBucketUrlResult(url)
    }, function(error) {
      android.getAppBucketUrlFailure(error.toString())
    })
  }

blockstackAndroid.getUserAppFileUrl = function(path, username, appOrigin) {
    blockstack.getUserAppFileUrl(path, username, appOrigin)
    .then(function(url) {
      if (url != null) {
        android.getUserAppFileUrlResult(url)
      } else {
       android.getUserAppFileUrlResult('NO_URL')
      }
    }, function(error) {
      android.getUserAppFileUrlFailure(error.toString())
    })
}

blockstackAndroid.getNameInfo = function(fullyQualifiedName) {
  blockstack.config.network.getNameInfo(fullyQualifiedName)
  .then(function(nameInfo) {
    console.log("nameInfo " + JSON.stringify(nameInfo))
    if (nameInfo != null) {
      android.getNameInfoResult(JSON.stringify(nameInfo))
    } else {
      android.getNameInfoFailure("NO_NAME_INFO")
    }
  }, function(error) {
    android.getNameInfoFailure(error.toString())
  })
}

userSessionAndroid.loadUserData = function() {
  var userData = userSession.loadUserData()
  if (userData != null) {
    return JSON.stringify(userData)
  } else {
    return null
  }
}

blockstackAndroid.lookupProfile = function(username, zoneLookupFileURL) {
  blockstack.lookupProfile(username, zoneLookupFileURL)
  .then(function(userData) {
      android.lookupProfileResult(username, JSON.stringify(userData))
  }, function(error) {
      android.lookupProfileFailure(username, error.toString())
  })
}

blockstackAndroid.validateProofs = function(profile, ownerAddress, name) {
  blockstack.validateProofs(JSON.parse(profile), ownerAddress, name)
  .then(function(proofs) {
    android.validateProofsResult(JSON.stringify(proofs))
  }, function(error) {
    android.validateProofsFailure(error.toString)
  })
}

userSessionAndroid.getFile = function(path, options, uniqueIdentifier) {
    const opts = JSON.parse(options)
    userSession.getFile(path, opts)
      .then(function(result) {
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

userSessionAndroid.putFile = function(path, contentString, options, uniqueIdentifier, binary) {
  var content = null;
  if (binary) {
    content = Base64.decode(contentString)
  } else {
    content = contentString
  }
  userSession.putFile(path, content, JSON.parse(options))
    .then(function(result) {
      android.putFileResult(result, uniqueIdentifier)
    }, function(error) {
      console.log("put failure:" + error)
      android.putFileFailure(error.toString(), uniqueIdentifier)
    })
}


userSessionAndroid.encryptContent = function(contentString, options) {
    return userSession.encryptContent(contentString, JSON.parse(options));
}

userSessionAndroid.decryptContent = function(cipherTextString, options, binary) {
    return userSession.decryptContent(cipherTextString, JSON.parse(options))
}

networkAndroid.getNameInfo = function(fullyQualifiedName) {
    blockstack.config.network.getNameInfo(fullyQualifiedName).then(function(nameInfo) {
        try {
            networkAndroid.getNameInfoResult(JSON.stringify(nameInfo))
        } catch (e) {
            networkAndroid.getNameInfoFailure(e.toString())
        }
    }, function(error) {
        networkAndroid.getNameInfoFailure(error.toString())
    })
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
    resolve(_this.body)
  });
}

function keyForUrlFetch(url) {
  return "k" + new Date().getTime();
}

var fetchPromises = {}
fetch = function(url, options){
  var promise = new Promise(function(resolve, reject) {
    var key = keyForUrlFetch(url)
    console.log('fetch ' + key + "=" + url)
    if (key in fetchPromises) {
        fetchPromises[key].reject()
    }
    fetchPromises[key] = {resolve:resolve, reject:reject}
    if (options) {
        if (options.body instanceof ArrayBuffer) {
          options.body = Base64.encode(options.body)
          options.bodyEncoded = true
        }
    } else {
      options = {};
    }
    try {
      android.fetchAndroid(url, JSON.stringify(options), key)
    } catch (e) {
      console.log("fetch error " + e.toString())
    }
  })
  return promise
}

blockstackAndroid.fetchResolve = function(key, response) {
  try {
    var resp = new Response(JSON.parse(response))
    console.log("resolve " + key)
    fetchPromises[key].resolve(resp)
    delete fetchPromises[key]
    return "success"
  } catch (e) {
    console.log("error fetchResolve "+ e.toString())
    return "error " + e.toString()
  }
}

blockstack.timeout = function() {
  fakeEventLoop()
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