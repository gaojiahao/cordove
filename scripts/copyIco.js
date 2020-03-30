/**
 * 辅助copy android用到的图标文件
 */
const fs = require('fs'),
      path = require('path'),
      pngName ='ic_launcher.png',
      imgPath = path.resolve('./ic_launcher.png'),
      target = path.resolve(__dirname,'../platforms/android/app/src/main/res');

fs.readdir(target,(err,paths)=>{
    var l,
        i = 0;

    if(err){
        console.log(err.message);
        return;
    }
    l = paths.length;
    loop();

    function loop(){
        var newPath = paths[i],
            targetImgPath;
        
        i++;
        if(newPath.indexOf('mipmap') == 0 && newPath.split('-').length == 2){
            targetImgPath = path.join(target,newPath,pngName)
            fs.createReadStream(imgPath).pipe(fs.createWriteStream(targetImgPath));
        }
        if(i<l)loop();
    }
})