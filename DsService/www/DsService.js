var exec = require('cordova/exec');


exports._listener = {};
exports.sayHello= function (arg0, success, error) {
    exec(success, error, 'DsService', 'sayHello', [arg0]);
};
exports.login = function (uid, success, error) {
    var protocol = (window.baseURL||'').indexOf('https') == 0 ? 'wss':'ws',
        url = protocol + '://' + dsUrl;
    exec(success, error, 'DsService', 'login', [url,uid]);
};
