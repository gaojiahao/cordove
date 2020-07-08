cordova.define("cordova-plugin-dsService.DsService", function(require, exports, module) {
var exec = require('cordova/exec');

exports.getDsMsg = function (success, error) {
    exec(success, error, 'DsService', 'getDsMsg', []);
};
exports.onNotificationClick = function(handler){
    exec(handler,null,"DsService","onNotificationClick",null);
}
exports.login = function (uid, success, error) {
    var protocol = (window.baseURL||'').indexOf('https') == 0 ? 'wss':'ws',
        dsUrl = "175.roletask.com:6021/deepstream",
        url = protocol + '://' + dsUrl;
        
    exec(success, error, 'DsService', 'login', [url,uid]);
};
exports.close = function(success,error){
    exec(success, error, 'DsService', 'close', []);
}

});
