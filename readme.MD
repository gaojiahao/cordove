## 目录
```
|--hooks //codova hooks  
|--node_modules //插件的node模块安装目录。  
|--platforms 平台目录，android和ios。  
|--plugins 插件目录。  
|--scripts 自定义的一个目录，存放一些小工具  
|--typings ts接口 定义文件。  
|--www 要打包的web文件存放目录。  
|--www-bak 系统生成的web文件的备份  
|--config.xml cordova 的配置定义文件，编译时会同步到各个平台  
|--jsconfig.json 项目中的js定义文件  
|--package.json npm包声明 
``` 

## 项目运行方法
1.cordova 安装  
`npm i -g cordova` //可以全局安装   
2.使用cordova命令生成一个项目   
`cordova create hello com.example.hello HelloWorld`   
本项目已经生成，只用修改config.xml就好了。   
3.把web文件放到www目录,然后run或者build。   
### android平台
  
  `cordova run android` 测试android
  android平台要在path里配置ANDROID_HOME，也就是android-sdk的目录。   
  `cordova run` 在window下只能启用android模拟器。   
  android 的build要使用android签名文件，生成的android签名文件要配置相关配置文件里。 
### ios 平台
  ios只是生成项目文件，编译要到xcode里，因为ios需要的临时签名只能xcode生成保管
