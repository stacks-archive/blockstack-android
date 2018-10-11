
getByte = function() {
return 1;
}

global.generate=function (len) {
  var res=new Uint8Array(len);for(var i=0;i<res.length;i++){res[i]=getByte();}
  return res;
}
