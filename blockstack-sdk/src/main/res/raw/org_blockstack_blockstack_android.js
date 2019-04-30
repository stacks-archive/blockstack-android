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
        console.log("handlePendingSignIn "  + error.toString())
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

userSessionAndroid.continueListFiles = false

userSessionAndroid.listFiles = function() {
  var fileCount = userSession.listFiles(function(name) {
     android.listFilesResult(name)
     return userSessionAndroid.continueListFiles
  }).then(function(fileCount) {
    console.log("file count " + String(fileCount))
    android.listFilesCountResult(fileCount)
  }, function(error) {
    android.listFilesCountFailure(error.toString())
  })
}


userSessionAndroid.listFilesCallback = function(cont) {
    userSessionAndroid.continueListFiles = cont === true
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

blockstackAndroid.extractProfile = function(token, publicKeyOrAddress) {
  return JSON.stringify(blockstack.extractProfile(token, publicKeyOrAddress))
}

blockstackAndroid.verifyProfileToken = function(token, publicKeyOrAddress) {
  return JSON.stringify(blockstack.verifyProfileToken(token, publicKeyOrAddress))
}

blockstackAndroid.signProfileToken = function(profile, privateKey, subject, issuer, signingAlgorithm, issuedAt, expiresAt) {
  return JSON.stringify(blockstack.signProfileToken(JSON.parse(profile), privateKey, JSON.parse(subject), JSON.parse(issuer), signingAlgorithm, new Date(issuedAt), new Date(expiresAt)))
}

blockstackAndroid.wrapProfileToken = function(token) {
  return JSON.stringify(blockstack.wrapProfileToken(token))
}

blockstackAndroid.parseZoneFile = function(zoneFileContent) {
  return JSON.stringify(parseZoneFile(zoneFileContent))
}


blockstackAndroid.resolveZoneFileToProfile = function(zoneFileContent, publicKeyOrAddress) {
  blockstack.resolveZoneFileToProfile(zoneFileContent, publicKeyOrAddress)
  .then(function(result) {
    console.log("profile " + JSON.stringify(result))
    android.resolveZoneFileToProfileResult(JSON.stringify(result))
  }, function(error) {
    console.log("profile error " + error)
    android.resolveZoneFileToProfileFailure(error)
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

userSessionAndroid.getFileUrl = function(path, options, uniqueIdentifier) {
  userSession.getFileUrl(path, JSON.parse(options))
    .then(function(result) {
      android.getFileUrlResult(result, uniqueIdentifier)
    }, function(error) {
      console.log("getFileUrl failure:" + error)
      android.getFileUrlFailure(error.toString(), uniqueIdentifier)
    })
}

userSessionAndroid.encryptContent = function(contentString, options) {
    return userSession.encryptContent(contentString, JSON.parse(options));
}

userSessionAndroid.decryptContent = function(cipherTextString, options, binary) {
    return userSession.decryptContent(cipherTextString, JSON.parse(options))
}

/** network methods **/
networkAndroid.getNamePrice = function(fullyQualifiedName) {
  blockstack.config.network.getNamePrice(fullyQualifiedName).then(function(denomination){
    networkAndroid.getNamePriceResult(JSON.stringify(denomination))
  }, function(error) {
    networkAndroid.getNamePriceFailure(error.toString())
  })
}

networkAndroid.getNamespacePrice = function(namespaceId) {
  blockstack.config.network.getNamespacePrice(namespaceId).then(function(denomination){
    networkAndroid.getNamespacePriceResult(JSON.stringify(denomination))
  }, function(error) {
    networkAndroid.getNamespacePriceFailure(error.toString())
  })
}

networkAndroid.getGracePeriod = function() {
  blockstack.config.network.getGracePeriod().then(function(gracePeriod){
    networkAndroid.getGracePeriodResult(gracePeriod)
  }, function(error) {
    networkAndroid.getGracePeriodFailure(error.toString())
  })
}

networkAndroid.getNamesOwned = function(address) {
  try {
      blockstack.config.network.getNamesOwned(address).then(function(names){
          networkAndroid.getNamesOwnedResult(names)
      }, function(error) {
        networkAndroid.getNamesOwnedFailure(error.toString())
      })
  } catch (e) {
      networkAndroid.getNamesOwnedFailure(e.toString())
  }
}

networkAndroid.getNamespaceBurnAddress = function(namespaceId) {
  blockstack.config.network.getNamespaceBurnAddress(namespaceId).then(function(address){
    networkAndroid.getNamespaceBurnAddressResult(JSON.stringify(address))
  }, function(error) {
    networkAndroid.getNamespaceBurnAddressFailure(error.toString())
  })
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

networkAndroid.getNamespaceInfo = function(namespaceId) {
  blockstack.config.network.getNamespaceInfo(namespaceId).then(function(info){
    networkAndroid.getNamespaceInfoResult(JSON.stringify(info))
  }, function(error) {
    networkAndroid.getNamespaceInfoFailure(error.toString())
  })
}

networkAndroid.getZonefile = function(zonefileHash) {
  blockstack.config.network.getZonefile(zonefileHash).then(function(content){
    networkAndroid.getZonefileResult(content)
  }, function(error) {
    networkAndroid.getZonefileFailure(error.toString())
  })
}

networkAndroid.getAccountStatus = function(address, tokenType) {
  blockstack.config.network.getAccountStatus(address, tokenType).then(function(stati){
    networkAndroid.getAccountStatusResult(JSON.stringify(stati))
  }, function(error) {
    networkAndroid.getAccountStatusFailure(error.toString())
  })
}
networkAndroid.getAccountHistoryPage = function(address, page) {
  blockstack.config.network.getAccountHistoryPage(address, page).then(function(stati){
    networkAndroid.getAccountHistoryPageResult(JSON.stringify(stati))
  }, function(error) {
    networkAndroid.getAccountHistoryPageFailure(error.toString())
  })
}

networkAndroid.getAccountAt = function(address, blockHeight) {
  blockstack.config.network.getAccountAt(address, blockHeight).then(function(stati){
    networkAndroid.getAccountAtResult(JSON.stringify(stati))
  }, function(error) {
    networkAndroid.getAccountAtFailure(error.toString())
  })
}

networkAndroid.getAccountTokens = function(address) {
   try {
      blockstack.config.network.getAccountTokens(address).then(function(tokens) {
        networkAndroid.getAccountTokensResult(tokens["tokens"])
      }, function(error) {
        networkAndroid.getAccountTokensFailure(error.toString())
      })
    } catch(e) {
        networkAndroid.getAccountTokensFailure(e)
    }
}

networkAndroid.getAccountBalance = function(address, tokenType) {
try {
  blockstack.config.network.getAccountBalance(address, tokenType).then(function(balance){
    networkAndroid.getAccountBalanceResult(balance.toString())
  }, function(error) {
    networkAndroid.getAccountBalanceFailure(error.toString())
  })
  } catch(e) {
    networkAndroid.getAccountBalanceFailure(error.toString())
  }
}

var Bigi = function(value) {
    var num = parseInt(value);
    var hexString = num.toString(16)
    return {
        toHex: () => {
            return hexString
        }
    }
}

networkAndroid.estimateTokenTransfer = function(recipientAddress, tokenType, tokenAmount, scratchArea, senderUtxos, additionalOutputs) {
  try {
    blockstack.transactions.estimateTokenTransfer(recipientAddress, tokenType, Bigi(tokenAmount), scratchArea, senderUtxos, additionalOutputs).then(function(costs){
        networkAndroid.estimateTokenTransferResult(costs.toString())
    }, function(error) {
        networkAndroid.estimateTokenTransferFailure(error.toString())
    })
  } catch (e) {
    networkAndroid.estimateTokenTransferFailure(e.toString())
  }
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