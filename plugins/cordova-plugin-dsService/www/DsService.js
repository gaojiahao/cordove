var exec = require('cordova/exec');

exports.getDsMsg = function (success, error) {
    exec(success, error, 'DsService', 'getDsMsg', []);
};
exports.getToken = function (success, error) {
    exec(success, error, 'DsService', 'getToken', []);
};
exports.onNotificationClick = function(handler){
    exec(handler,null,"DsService","onNotificationClick",null);
}
exports.login = function (dsUrl,uid,token,success, error) {
    var protocol = (window.baseURL||'').indexOf('https') == 0 ? 'wss':'ws',
        url = protocol + '://' + dsUrl;
        
    exec(success, error, 'DsService', 'login', [url,uid,token]);
};
exports.close = function(success,error){
    exec(success, error, 'DsService', 'close', []);
}
exports.getCache = function(key,success,error){
    exec(success,error,'DsService','getCache',[key]);
}
exports.setCache = function(key,value,success,error){
    exec(success,error,'DsService','setCache',[key,value]);
}