/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },

    // deviceready Event Handler
    //
    // Bind any cordova events here. Common events are:
    // 'pause', 'resume', etc.
    onDeviceReady: function() {
        this.receivedEvent('deviceready');
    },
    addEvent:function(e,fn){
        document.addEventListener(e,fn.bind(this),false);
    },
    initEvents:function(){
        this.addEvent('pause',this.pause);
        notification.init();
        badge.init();
    },
    pause:function(){
        console.log('pause');
    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);
        this.initEvents();
    }
};
var badge = {
    init:function(){
        var me = this;
        this.badge = cordova.plugins.notification.badge;
        this.badge.hasPermission(function (granted) {
            if(granted == false){
               me.badeg.requestPermission(function(granted1){
                   alert(granted1);
                   if(granted1){
                       me.test();
                   }
               })
            } else {
                me.test();
            }
        });
    },
    set:function(n){
        this.badge.set(n,function(){
            console.log('设置成功！');
        });
    },
    test:function(){
        var me = this;
        this.set(10);
        setTimeout(function(){
            var fn = arguments.callee;
            me.badge.increase(1);
            setTimeout(fn,3000);
        },1000)
    }
} 
var notification = {
    init:function(){
        var me =this;
        me.Api = cordova.plugins.notification.local;
        me.test();
    },
    test:function(){
        var count = 1
            me = this;

        setTimeout(function(){
            var fn = arguments.callee;
            me.Api.schedule({
                id:count,
                badge:count,
                title: 'My notification count' + count,
                text: 'Thats pretty easy...',
                foreground: true
            });
            count ++;
            setTimeout(fn,60000);
        })
    }
}
app.initialize();