var userSession = {}

blockstack.newUserSession=function(domainName) {
   const appConfig = new blockstack.AppConfig(domainName);
   userSession = new blockstack.UserSession({ appConfig:appConfig, sessionStore:androidSessionStore });

   return JSON.stringify(userSession);
}

blockstack.isUserSignedIn = function() {
  return userSession.isUserSignedIn()
}

blockstack.signIn = function(domainName, appPrivateKey, identityAddress, hubUrl, userDataString) {
  const appConfig = new blockstack.AppConfig(domainName);
  console.log("userData : " + userDataString)
  const userData = JSON.parse(userDataString);
  const sessionOptions = { appPrivateKey, identityAddress, hubUrl, userData };
  userSession = new blockstack.UserSession({ appConfig, sessionOptions })
  console.log("signedIn: " + userSession.isUserSignedIn())
}

blockstack.signUserOut = function() {
  userSession.signUserOut()
}

blockstack.getFile = function(path,options, uniqueIdentifier) {
    const opts = JSON.parse(options)
    console.log("opts" + opts)
    userSession.getFile(path, opts)
      .then(function(result) {
         console.log("get file result:" + result)
         android.getFileResult(result, uniqueIdentifier)
      }, function(error) {
        console.log("get file failure:" + error)
        android.getFileFailure(error.toString(), uniqueIdentifier)
      })
}

blockstack.putFile = function(path, contentString, options, uniqueIdentifier, binary) {
  userSession.putFile(path, contentString, JSON.parse(options))
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
    console.log("result " + result);
    return result;
}

blockstack.decryptContent = function(cipherTextString, options, binary) {
    console.log("decrypt content");
    return userSession.decryptContent(cipherTextString, JSON.parse(options))
}


/**
 * Response class
 *
 * @param   Object  opts  Response options
 * @return  Void
 */

function Response(r) {
  this.status = r.status
  this.body = r.body
  this.ok = this.status >= 200 && this.status < 300;
}

/**
 * Decode response as json
 *
 * @return  Promise
 */
Response.prototype.json = function() {

  return this.text().then(function(text) {
    return JSON.parse(text);
  });

}

/**
 * Decode response body as text
 *
 * @return  Promise
 */
Response.prototype.text = function() {

  var _this = this;

  return new blockstack.Promise(function(resolve, reject) {
    resolve(_this.body);
  });

}


var fetchPromises = {}
fetch = function(url, options){
  var promise = new blockstack.Promise(function(resolve, reject) {
    console.log('fetch ' + url)
    fetchPromises.resolve = resolve
    if (options) {
        android.fetch2(url, JSON.stringify(options))
    } else {
        android.fetch2(url, "{}")
    }
  })
  console.log("after promise created")
  return promise
}

blockstack.fetchResolve = function(url, response) {
  console.log('resolved ' + url)
  console.log('response ' + response)
  try {
    var resp = new Response(JSON.parse(response))
    console.log('resp ' + resp)
    var resolved = fetchPromises.resolve(resp)
    console.log('result ' + resolved)
    fakeEventLoop()
  } catch (e) {
   console.log("error:" + e.toString())
  }
  return "success"
}

blockstack.timeout = function() {
  fakeEventLoop()
}

getByte = function() {
return 1;
}

global.generate=function (len) {
  var res=new Uint8Array(len);for(var i=0;i<res.length;i++){res[i]=getByte();}
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
                const Promise = blockstack.Promise;
                const global = blockstack.global;
                fn();
            } catch (e) {
                console.log("error in eventLoop " + e)
                throw Error("failed to run " + fn, e)
            }
            console.log('timer len:' + timers.length)
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